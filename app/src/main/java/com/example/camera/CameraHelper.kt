package com.example.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import com.example.model.CameraFacing
import com.example.model.CameraState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CameraHelper(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val _cameraState = MutableStateFlow(CameraState())
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private var cameraDevice: CameraDevice? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var previewSurface: Surface? = null

    private var frontCameraId: String? = null
    private var backCameraId: String? = null

    init {
        detectAvailableCameras()
    }

    fun detectAvailableCameras() {
        var frontCount = 0
        var backCount = 0
        frontCameraId = null
        backCameraId = null

        try {
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    frontCount++
                    if (frontCameraId == null) frontCameraId = id
                } else if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    backCount++
                    if (backCameraId == null) backCameraId = id
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error discovering cameras: ${e.message}")
        }

        _cameraState.value = _cameraState.value.copy(
            availableFrontCameras = frontCount,
            availableBackCameras = backCount,
            activeCameraId = if (_cameraState.value.facing == CameraFacing.FRONT) frontCameraId else backCameraId
        )
    }

    fun setPreviewSurface(surface: Surface?) {
        previewSurface = surface
        if (surface != null && _cameraState.value.isEnabled) {
            restartCamera()
        } else if (surface == null) {
            closeCamera()
        }
    }

    fun toggleCameraFacing() {
        val newFacing = if (_cameraState.value.facing == CameraFacing.FRONT) CameraFacing.BACK else CameraFacing.FRONT
        _cameraState.value = _cameraState.value.copy(
            facing = newFacing,
            activeCameraId = if (newFacing == CameraFacing.FRONT) frontCameraId else backCameraId
        )
        if (_cameraState.value.isEnabled && previewSurface != null) {
            restartCamera()
        }
    }

    fun setCameraEnabled(enabled: Boolean) {
        _cameraState.value = _cameraState.value.copy(isEnabled = enabled)
        if (enabled && previewSurface != null) {
            restartCamera()
        } else {
            closeCamera()
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        closeCamera()
        startBackgroundThread()

        val targetId = if (_cameraState.value.facing == CameraFacing.FRONT) frontCameraId else backCameraId
        if (targetId == null) {
            _cameraState.value = _cameraState.value.copy(errorMessage = "Camera not available on this device")
            return
        }

        val surface = previewSurface
        if (surface == null || !surface.isValid) {
            Log.w(TAG, "No valid preview surface for camera")
            return
        }

        try {
            cameraManager.openCamera(targetId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createPreviewSession(camera, surface)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                    val msg = "Camera error code: $error"
                    Log.e(TAG, msg)
                    _cameraState.value = _cameraState.value.copy(errorMessage = msg)
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open camera: ${e.message}")
            _cameraState.value = _cameraState.value.copy(errorMessage = e.message)
        }
    }

    private fun createPreviewSession(camera: CameraDevice, surface: Surface) {
        try {
            val previewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            }

            @Suppress("DEPRECATION")
            camera.createCaptureSession(listOf(surface), object : android.hardware.camera2.CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: android.hardware.camera2.CameraCaptureSession) {
                    if (cameraDevice == null) return
                    try {
                        session.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler)
                        _cameraState.value = _cameraState.value.copy(errorMessage = null)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start camera preview: ${e.message}")
                    }
                }

                override fun onConfigureFailed(session: android.hardware.camera2.CameraCaptureSession) {
                    _cameraState.value = _cameraState.value.copy(errorMessage = "Camera capture session configuration failed")
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating camera preview session: ${e.message}")
        }
    }

    fun restartCamera() {
        openCamera()
    }

    fun closeCamera() {
        try {
            cameraDevice?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing camera: ${e.message}")
        } finally {
            cameraDevice = null
        }
        stopBackgroundThread()
    }

    private fun startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("CameraBackground").apply { start() }
            backgroundHandler = Handler(backgroundThread!!.looper)
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
        } catch (e: Exception) {
            Log.w(TAG, "Error joining background thread: ${e.message}")
        } finally {
            backgroundThread = null
            backgroundHandler = null
        }
    }

    companion object {
        private const val TAG = "CameraHelper"
    }
}
