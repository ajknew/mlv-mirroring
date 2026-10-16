package com.example.model

enum class ConnectionStatus {
    DISCONNECTED,
    DEVICE_DETECTED,
    AWAITING_APPROVAL,
    INITIALIZING_TRANSPORT,
    INITIALIZING_STREAM,
    CONNECTED,
    STREAMING_ACTIVE,
    STREAMING_STOPPED,
    ERROR
}

data class ClientDeviceInfo(
    val deviceName: String = "No Device Connected",
    val manufacturer: String = "Unknown",
    val model: String = "Unknown",
    val vendorId: Int = 0,
    val productId: Int = 0,
    val hasAdbInterface: Boolean = false,
    val hasUvcInterface: Boolean = false,
    val usbPermissionGranted: Boolean = false
)

data class StreamMetrics(
    val isStreaming: Boolean = false,
    val receivedBytesTotal: Long = 0L,
    val bitrateKbps: Float = 0f,
    val receivedFrames: Long = 0L,
    val decodedFrames: Long = 0L,
    val droppedFrames: Long = 0L,
    val currentFps: Float = 0f,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val codecName: String = "video/avc (H.264)",
    val latencyMs: Long = 0L,
    val lastError: String? = null
)

enum class CameraFacing {
    BACK,
    FRONT
}

data class CameraState(
    val isEnabled: Boolean = true,
    val facing: CameraFacing = CameraFacing.FRONT,
    val availableBackCameras: Int = 0,
    val availableFrontCameras: Int = 0,
    val activeCameraId: String? = null,
    val errorMessage: String? = null
)
