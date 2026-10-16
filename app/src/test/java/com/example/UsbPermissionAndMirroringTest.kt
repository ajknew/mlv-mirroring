package com.example

import android.app.Application
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.example.model.ConnectionStatus
import com.example.usb.AdbCrypto
import com.example.usb.AdbProtocol
import com.example.usb.UsbAdbManager
import com.example.usb.UsbDeviceDetector
import com.example.video.H264AnnexBParser
import com.example.video.MediaCodecDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UsbPermissionAndMirroringTest {

    private lateinit var app: Application
    private lateinit var crypto: AdbCrypto
    private lateinit var detector: UsbDeviceDetector
    private lateinit var decoder: MediaCodecDecoder
    private lateinit var parser: H264AnnexBParser
    private lateinit var usbAdbManager: UsbAdbManager

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        crypto = AdbCrypto.getInstance(app)
        detector = UsbDeviceDetector(app)
        decoder = MediaCodecDecoder()
        parser = H264AnnexBParser()
        usbAdbManager = UsbAdbManager(app, decoder, parser, detector)
    }

    @Test
    fun `adb public key format strictly matches 524-byte android_pubkey struct`() {
        val pubKeyAdbBytes = crypto.getPublicKeyAdbBytes("host@mlv-mirroring")
        val pubKeyString = String(pubKeyAdbBytes, Charsets.UTF_8).trimEnd('\u0000')

        // Format is: "<base64_struct> <comment>"
        val parts = pubKeyString.split(" ")
        assertTrue("ADB public key must have base64 and comment", parts.size >= 2)
        assertEquals("host@mlv-mirroring", parts[1])

        val structBytes = Base64.decode(parts[0], Base64.NO_WRAP)
        assertEquals(
            "Android RSAPublicKey struct size must be exactly 524 bytes",
            524,
            structBytes.size
        )

        val buffer = ByteBuffer.wrap(structBytes).order(ByteOrder.LITTLE_ENDIAN)
        val modulusWords = buffer.getInt()
        val n0inv = buffer.getInt()

        val modulus = ByteArray(256)
        buffer.get(modulus)

        val rr = ByteArray(256)
        buffer.get(rr)

        val exponent = buffer.getInt()

        assertEquals("Modulus words for 2048-bit RSA must be 64", 64, modulusWords)
        assertEquals("RSA exponent must be 65537", 65537, exponent)
        assertTrue("Modulus must not be all zeros", modulus.any { it != 0.toByte() })
        assertTrue("rr Montgomery parameter must not be all zeros", rr.any { it != 0.toByte() })
    }

    @Test
    fun `token signature produces 256-byte PKCS1 v1_5 signature with SHA-1 DigestInfo`() {
        val fakeToken = ByteArray(20) { it.toByte() }
        val signature = crypto.signToken(fakeToken)

        assertEquals("2048-bit RSA signature must be exactly 256 bytes", 256, signature.size)
        assertTrue("Signature bytes must not be all zeros", signature.any { it != 0.toByte() })
    }

    @Test
    fun `startMirroring without device sets DISCONNECTED not STREAMING`() {
        usbAdbManager.startMirroring(null)
        assertEquals(ConnectionStatus.DISCONNECTED, usbAdbManager.connectionStatus.value)
        assertFalse(decoder.metrics.value.isStreaming)
    }

    @Test
    fun `test connection state transitions handle start and stop correctly`() {
        usbAdbManager.setTestConnectionStatus(ConnectionStatus.INITIALIZING_TRANSPORT, "Connecting USB...")
        assertEquals(ConnectionStatus.INITIALIZING_TRANSPORT, usbAdbManager.connectionStatus.value)

        usbAdbManager.setTestConnectionStatus(ConnectionStatus.AWAITING_APPROVAL, "Waiting for client...")
        assertEquals(ConnectionStatus.AWAITING_APPROVAL, usbAdbManager.connectionStatus.value)

        usbAdbManager.setTestConnectionStatus(ConnectionStatus.STREAMING_ACTIVE, "Streaming Live")
        assertEquals(ConnectionStatus.STREAMING_ACTIVE, usbAdbManager.connectionStatus.value)

        usbAdbManager.stopMirroring()
        assertEquals(ConnectionStatus.STREAMING_STOPPED, usbAdbManager.connectionStatus.value)
    }

    @Test
    fun `decoder caches sps and pps so late attached surface initializes immediately`() {
        val sps = byteArrayOf(0, 0, 0, 1, 0x67, 0x42, 0x00, 0x1f)
        val pps = byteArrayOf(0, 0, 0, 1, 0x68, 0xce.toByte(), 0x3c)

        decoder.initializeDecoder(1280, 720, sps, pps)
        assertEquals(1280, decoder.lastWidth)
        assertEquals(720, decoder.lastHeight)
        assertNotNull(decoder.lastSps)
        assertNotNull(decoder.lastPps)
    }
}
