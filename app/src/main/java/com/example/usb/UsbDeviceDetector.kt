package com.example.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.example.model.ClientDeviceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UsbDeviceDetector(private val context: Context) {

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private val _deviceInfo = MutableStateFlow(ClientDeviceInfo())
    val deviceInfo: StateFlow<ClientDeviceInfo> = _deviceInfo.asStateFlow()

    private val _selectedDevice = MutableStateFlow<UsbDevice?>(null)
    val selectedDevice: StateFlow<UsbDevice?> = _selectedDevice.asStateFlow()

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_USB_PERMISSION -> {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    Log.d(TAG, "USB Permission result for ${device?.deviceName}: granted=$granted")
                    refreshDevices()
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    Log.d(TAG, "USB Device Attached")
                    refreshDevices()
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    Log.d(TAG, "USB Device Detached")
                    refreshDevices()
                }
            }
        }
    }

    fun startListening() {
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }
        refreshDevices()
    }

    fun stopListening() {
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering receiver: ${e.message}")
        }
    }

    fun refreshDevices() {
        val deviceList = usbManager.deviceList
        var matchedDevice: UsbDevice? = null
        var hasAdb = false
        var hasUvc = false

        for (device in deviceList.values) {
            val (adb, uvc) = inspectInterfaces(device)
            if (adb || uvc || matchedDevice == null) {
                matchedDevice = device
                hasAdb = adb
                hasUvc = uvc
                if (adb) break // prioritize ADB client device
            }
        }

        _selectedDevice.value = matchedDevice

        if (matchedDevice != null) {
            val hasPermission = usbManager.hasPermission(matchedDevice)
            _deviceInfo.value = ClientDeviceInfo(
                deviceName = matchedDevice.deviceName ?: "USB Device",
                manufacturer = matchedDevice.manufacturerName ?: "Android Client",
                model = matchedDevice.productName ?: "Client Device",
                vendorId = matchedDevice.vendorId,
                productId = matchedDevice.productId,
                hasAdbInterface = hasAdb,
                hasUvcInterface = hasUvc,
                usbPermissionGranted = hasPermission
            )
        } else {
            _deviceInfo.value = ClientDeviceInfo()
        }
    }

    fun requestUsbPermission(device: UsbDevice) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION),
            flags
        )
        usbManager.requestPermission(device, permissionIntent)
    }

    fun findAdbInterface(device: UsbDevice): UsbInterface? {
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            if (intf.interfaceClass == AdbProtocol.ADB_CLASS &&
                intf.interfaceSubclass == AdbProtocol.ADB_SUBCLASS &&
                intf.interfaceProtocol == AdbProtocol.ADB_PROTOCOL
            ) {
                return intf
            }
        }
        // Fallback: check first bulk interface if vendor specific
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            if (intf.interfaceClass == UsbConstants.USB_CLASS_VENDOR_SPEC) {
                return intf
            }
        }
        return null
    }

    private fun inspectInterfaces(device: UsbDevice): Pair<Boolean, Boolean> {
        var hasAdb = false
        var hasUvc = false
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            if (intf.interfaceClass == AdbProtocol.ADB_CLASS &&
                intf.interfaceSubclass == AdbProtocol.ADB_SUBCLASS &&
                intf.interfaceProtocol == AdbProtocol.ADB_PROTOCOL
            ) {
                hasAdb = true
            }
            if (intf.interfaceClass == 14) { // UVC Video
                hasUvc = true
            }
        }
        return Pair(hasAdb, hasUvc)
    }

    companion object {
        private const val TAG = "UsbDeviceDetector"
        const val ACTION_USB_PERMISSION = "com.example.USB_PERMISSION"
    }
}
