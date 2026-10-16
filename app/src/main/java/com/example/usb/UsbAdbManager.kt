package com.example.usb

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.util.Log
import com.example.model.ConnectionStatus
import com.example.video.H264AnnexBParser
import com.example.video.MediaCodecDecoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder

class UsbAdbManager(
    private val context: Context,
    private val decoder: MediaCodecDecoder,
    private val parser: H264AnnexBParser
) {
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val crypto = AdbCrypto.getInstance(context)

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _statusMessage = MutableStateFlow("Ready to connect")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    fun setTestConnectionStatus(status: ConnectionStatus, message: String) {
        _connectionStatus.value = status
        _statusMessage.value = message
    }

    private var connection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var endpointIn: UsbEndpoint? = null
    private var endpointOut: UsbEndpoint? = null

    private var streamJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun startMirroring(device: UsbDevice?) {
        if (_connectionStatus.value == ConnectionStatus.STREAMING_ACTIVE) {
            Log.d(TAG, "Already streaming")
            return
        }

        stopMirroring()

        if (device == null) {
            _statusMessage.value = "No USB device detected. Please attach client device with USB OTG cable."
            _connectionStatus.value = ConnectionStatus.ERROR
            return
        }

        if (!usbManager.hasPermission(device)) {
            _statusMessage.value = "USB permission required for ${device.deviceName}."
            _connectionStatus.value = ConnectionStatus.AWAITING_APPROVAL
            return
        }

        _connectionStatus.value = ConnectionStatus.INITIALIZING_TRANSPORT
        _statusMessage.value = "Opening USB connection to client..."

        streamJob = scope.launch {
            try {
                runAdbSession(device)
            } catch (e: Exception) {
                Log.e(TAG, "Error in ADB mirroring session: ${e.message}", e)
                _statusMessage.value = "Streaming error: ${e.message}"
                _connectionStatus.value = ConnectionStatus.ERROR
                cleanup()
            }
        }
    }

    private suspend fun runAdbSession(device: UsbDevice) {
        val conn = usbManager.openDevice(device)
        if (conn == null) {
            _statusMessage.value = "Failed to open USB device connection."
            _connectionStatus.value = ConnectionStatus.ERROR
            return
        }
        connection = conn

        var adbIntf: UsbInterface? = null
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            if (intf.interfaceClass == AdbProtocol.ADB_CLASS &&
                intf.interfaceSubclass == AdbProtocol.ADB_SUBCLASS &&
                intf.interfaceProtocol == AdbProtocol.ADB_PROTOCOL
            ) {
                adbIntf = intf
                break
            }
        }
        if (adbIntf == null) {
            for (i in 0 until device.interfaceCount) {
                val intf = device.getInterface(i)
                if (intf.interfaceClass == UsbConstants.USB_CLASS_VENDOR_SPEC) {
                    adbIntf = intf
                    break
                }
            }
        }

        if (adbIntf == null) {
            _statusMessage.value = "No ADB interface detected on client device. Ensure USB Debugging is turned ON in Developer Options."
            _connectionStatus.value = ConnectionStatus.ERROR
            conn.close()
            return
        }

        if (!conn.claimInterface(adbIntf, true)) {
            _statusMessage.value = "Failed to claim USB ADB interface."
            _connectionStatus.value = ConnectionStatus.ERROR
            conn.close()
            return
        }
        usbInterface = adbIntf

        for (i in 0 until adbIntf.endpointCount) {
            val ep = adbIntf.getEndpoint(i)
            if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (ep.direction == UsbConstants.USB_DIR_IN) {
                    endpointIn = ep
                } else {
                    endpointOut = ep
                }
            }
        }

        val epIn = endpointIn
        val epOut = endpointOut
        if (epIn == null || epOut == null) {
            _statusMessage.value = "Bulk endpoints missing on ADB interface."
            _connectionStatus.value = ConnectionStatus.ERROR
            cleanup()
            return
        }

        // Phase 1: ADB Handshake (CNXN)
        _statusMessage.value = "Sending ADB handshake..."
        val connectPayload = "host::mlv_mirroring\u0000".toByteArray(Charsets.UTF_8)
        val connectMsg = AdbProtocol.createMessage(
            AdbProtocol.A_CNXN,
            AdbProtocol.CONNECT_VERSION,
            AdbProtocol.CONNECT_MAXDATA,
            connectPayload
        )
        sendAdbMessage(conn, epOut, connectMsg)

        var authenticated = false
        val headerBuffer = ByteArray(AdbProtocol.HEADER_LENGTH)

        while (scope.isActive && !authenticated) {
            val readLen = conn.bulkTransfer(epIn, headerBuffer, AdbProtocol.HEADER_LENGTH, 5000)
            if (readLen < AdbProtocol.HEADER_LENGTH) {
                _statusMessage.value = "Waiting for client response (Check client screen for USB debugging prompt)..."
                _connectionStatus.value = ConnectionStatus.AWAITING_APPROVAL
                delay(1000L)
                continue
            }

            val response = AdbProtocol.parseHeader(headerBuffer)
            val payload = if (response.length > 0) {
                val p = ByteArray(response.length)
                conn.bulkTransfer(epIn, p, response.length, 5000)
                p
            } else null

            when (response.command) {
                AdbProtocol.A_AUTH -> {
                    if (response.arg0 == AdbProtocol.AUTH_TYPE_TOKEN && payload != null) {
                        _statusMessage.value = "Authenticating with client device (RSA signature)..."
                        _connectionStatus.value = ConnectionStatus.AWAITING_APPROVAL
                        
                        // Send RSA signature
                        val sig = crypto.signToken(payload)
                        val authSigMsg = AdbProtocol.createMessage(
                            AdbProtocol.A_AUTH,
                            AdbProtocol.AUTH_TYPE_SIGNATURE,
                            0,
                            sig
                        )
                        sendAdbMessage(conn, epOut, authSigMsg)

                        // Send public key so client can save authorization
                        val pubKeyBytes = crypto.getPublicKeyAdbBytes()
                        val authPubMsg = AdbProtocol.createMessage(
                            AdbProtocol.A_AUTH,
                            AdbProtocol.AUTH_TYPE_RSA_PUBLIC,
                            0,
                            pubKeyBytes
                        )
                        sendAdbMessage(conn, epOut, authPubMsg)
                    }
                }
                AdbProtocol.A_CNXN -> {
                    authenticated = true
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    _statusMessage.value = "ADB connected! Initializing screenrecord capture on client..."
                    Log.d(TAG, "ADB handshake complete: ${payload?.let { String(it) }}")
                }
                else -> {
                    Log.d(TAG, "Received unexpected command during handshake: ${response.commandName()}")
                }
            }
        }

        if (!authenticated) {
            _statusMessage.value = "ADB authentication incomplete."
            _connectionStatus.value = ConnectionStatus.ERROR
            cleanup()
            return
        }

        // Phase 2: Open screenrecord stream on Client
        _connectionStatus.value = ConnectionStatus.INITIALIZING_STREAM
        val localId = 1
        val screenrecordCmd = "exec:screenrecord --output-format=h264 --bit-rate 6000000 --size 1080x2400 -\u0000"
        val openMsg = AdbProtocol.createMessage(
            AdbProtocol.A_OPEN,
            localId,
            0,
            screenrecordCmd.toByteArray(Charsets.UTF_8)
        )
        sendAdbMessage(conn, epOut, openMsg)

        // Phase 3: Streaming Loop
        var remoteId = 0
        var isStreamOpen = false
        val dataBuffer = ByteArray(16384)

        _connectionStatus.value = ConnectionStatus.STREAMING_ACTIVE
        _statusMessage.value = "Streaming Client Screen Live"

        while (scope.isActive) {
            val readHdr = conn.bulkTransfer(epIn, headerBuffer, AdbProtocol.HEADER_LENGTH, 3000)
            if (readHdr < AdbProtocol.HEADER_LENGTH) {
                continue
            }

            val msg = AdbProtocol.parseHeader(headerBuffer)
            if (!msg.isValid()) {
                Log.w(TAG, "Invalid magic in ADB message: ${msg.commandName()}")
                continue
            }

            when (msg.command) {
                AdbProtocol.A_OKAY -> {
                    remoteId = msg.arg0
                    isStreamOpen = true
                    Log.d(TAG, "Screen stream opened: remoteId=$remoteId")
                }
                AdbProtocol.A_WRTE -> {
                    val lengthToRead = msg.length
                    if (lengthToRead > 0) {
                        val payloadBytes = ByteArray(lengthToRead)
                        var totalRead = 0
                        while (totalRead < lengthToRead) {
                            val chunkSize = (lengthToRead - totalRead).coerceAtMost(dataBuffer.size)
                            val r = conn.bulkTransfer(epIn, dataBuffer, chunkSize, 2000)
                            if (r <= 0) break
                            System.arraycopy(dataBuffer, 0, payloadBytes, totalRead, r)
                            totalRead += r
                        }

                        // Send OKAY ack back
                        val okayMsg = AdbProtocol.createMessage(AdbProtocol.A_OKAY, localId, remoteId, null)
                        sendAdbMessage(conn, epOut, okayMsg)

                        // Process H.264 NAL units
                        val nalUnits = parser.parseStream(payloadBytes, 0, totalRead)
                        for (nal in nalUnits) {
                            if (nal.type == 7 && parser.spsData != null && parser.ppsData != null) {
                                decoder.initializeDecoder(
                                    width = parser.parsedWidth.takeIf { it > 0 } ?: 1080,
                                    height = parser.parsedHeight.takeIf { it > 0 } ?: 2400,
                                    sps = parser.spsData,
                                    pps = parser.ppsData
                                )
                            }
                            decoder.feedNalUnit(nal)
                        }
                    }
                }
                AdbProtocol.A_CLSE -> {
                    Log.d(TAG, "Client closed ADB stream")
                    _statusMessage.value = "Client disconnected or stopped screenrecord"
                    _connectionStatus.value = ConnectionStatus.STREAMING_STOPPED
                    break
                }
            }
        }
    }

    private fun sendAdbMessage(conn: UsbDeviceConnection, epOut: UsbEndpoint, msg: AdbProtocol.Message) {
        val serialized = AdbProtocol.serializeMessage(msg)
        conn.bulkTransfer(epOut, serialized, serialized.size, 2000)
    }

    fun stopMirroring() {
        streamJob?.cancel()
        streamJob = null
        cleanup()
        decoder.stopDecoder()
        parser.reset()
        _connectionStatus.value = ConnectionStatus.STREAMING_STOPPED
        _statusMessage.value = "Mirroring stopped"
    }

    private fun cleanup() {
        try {
            usbInterface?.let { connection?.releaseInterface(it) }
            connection?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error during USB cleanup: ${e.message}")
        } finally {
            connection = null
            usbInterface = null
            endpointIn = null
            endpointOut = null
        }
    }

    companion object {
        private const val TAG = "UsbAdbManager"
    }
}
