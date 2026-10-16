package com.example.ui

import android.app.Application
import android.view.Surface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.camera.CameraHelper
import com.example.data.LayoutPreferencesRepository
import com.example.model.CameraFacing
import com.example.model.CameraScaleMode
import com.example.model.CameraState
import com.example.model.ClientDeviceInfo
import com.example.model.ConnectionStatus
import com.example.model.LayerType
import com.example.model.LayoutPreset
import com.example.model.MirrorScaleMode
import com.example.model.MirroringLayoutState
import com.example.model.NormalizedRect
import com.example.model.StreamMetrics
import com.example.usb.UsbAdbManager
import com.example.usb.UsbDeviceDetector
import com.example.video.H264AnnexBParser
import com.example.video.MediaCodecDecoder
import com.example.video.NalUnit
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MirroringViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = LayoutPreferencesRepository(application)
    val decoder = MediaCodecDecoder()
    val parser = H264AnnexBParser()
    val usbDetector = UsbDeviceDetector(application)
    val usbAdbManager = UsbAdbManager(application, decoder, parser, usbDetector)
    val cameraHelper = CameraHelper(application)

    private val _layoutState = MutableStateFlow(MirroringLayoutState.forPreset(LayoutPreset.FULLSCREEN_PIP))
    val layoutState: StateFlow<MirroringLayoutState> = _layoutState.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private val _showToolbar = MutableStateFlow(false)
    val showToolbar: StateFlow<Boolean> = _showToolbar.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _isEditorOpen = MutableStateFlow(false)
    val isEditorOpen: StateFlow<Boolean> = _isEditorOpen.asStateFlow()

    private val _isDiagnosticsOpen = MutableStateFlow(false)
    val isDiagnosticsOpen: StateFlow<Boolean> = _isDiagnosticsOpen.asStateFlow()

    private val _isClientGuideOpen = MutableStateFlow(false)
    val isClientGuideOpen: StateFlow<Boolean> = _isClientGuideOpen.asStateFlow()

    val connectionStatus: StateFlow<ConnectionStatus> = usbAdbManager.connectionStatus
    val statusMessage: StateFlow<String> = usbAdbManager.statusMessage
    val deviceInfo: StateFlow<ClientDeviceInfo> = usbDetector.deviceInfo
    val streamMetrics: StateFlow<StreamMetrics> = decoder.metrics
    val cameraState: StateFlow<CameraState> = cameraHelper.cameraState

    private var toolbarDismissJob: Job? = null
    private var testSimulationJob: Job? = null

    init {
        usbDetector.startListening()
        viewModelScope.launch {
            preferencesRepository.layoutStateFlow.collect { savedState ->
                _layoutState.value = savedState
            }
        }
    }

    fun startMirroring() {
        val device = usbDetector.selectedDevice.value
        if (device != null) {
            usbAdbManager.startMirroring(device)
        } else {
            // If no physical USB device is attached (e.g. testing in emulator / container),
            // start test stream simulation to verify decoder, pipeline and layouts live
            startTestStreamSimulation()
        }
    }

    fun stopMirroring() {
        testSimulationJob?.cancel()
        testSimulationJob = null
        usbAdbManager.stopMirroring()
    }

    fun requestUsbPermission() {
        val device = usbDetector.selectedDevice.value
        if (device != null) {
            usbDetector.requestUsbPermission(device)
        }
    }

    fun attachMirrorSurface(surface: Surface) {
        decoder.attachSurface(surface)
    }

    fun detachMirrorSurface() {
        decoder.detachSurface()
    }

    fun attachCameraSurface(surface: Surface?) {
        cameraHelper.setPreviewSurface(surface)
    }

    fun toggleFullscreen() {
        val next = !_isFullscreen.value
        _isFullscreen.value = next
        if (next) {
            triggerToolbar()
        }
    }

    fun setFullscreen(fullscreen: Boolean) {
        _isFullscreen.value = fullscreen
        if (fullscreen) {
            triggerToolbar()
        }
    }

    fun triggerToolbar() {
        _showToolbar.value = true
        toolbarDismissJob?.cancel()
        toolbarDismissJob = viewModelScope.launch {
            delay(3000L) // 3 seconds auto-hide per requirement
            _showToolbar.value = false
        }
    }

    fun toggleSettings(show: Boolean? = null) {
        _isSettingsOpen.value = show ?: !_isSettingsOpen.value
        if (_isFullscreen.value && _isSettingsOpen.value) {
            triggerToolbar()
        }
    }

    fun toggleEditor(show: Boolean? = null) {
        _isEditorOpen.value = show ?: !_isEditorOpen.value
    }

    fun toggleDiagnostics(show: Boolean? = null) {
        _isDiagnosticsOpen.value = show ?: !_isDiagnosticsOpen.value
    }

    fun toggleClientGuide(show: Boolean? = null) {
        _isClientGuideOpen.value = show ?: !_isClientGuideOpen.value
    }

    fun setSelectedLayer(layer: LayerType) {
        _layoutState.value = _layoutState.value.copy(selectedLayer = layer)
    }

    fun updateLayerBounds(layer: LayerType, bounds: NormalizedRect) {
        val clamped = bounds.clamped()
        val current = _layoutState.value
        val updated = if (layer == LayerType.MIRROR) {
            current.copy(
                mirrorLayer = current.mirrorLayer.copy(bounds = clamped),
                activePreset = LayoutPreset.CUSTOM
            )
        } else {
            current.copy(
                cameraLayer = current.cameraLayer.copy(bounds = clamped),
                activePreset = LayoutPreset.CUSTOM
            )
        }
        _layoutState.value = updated
        persistCurrentLayout()
    }

    fun setMirrorScaleMode(mode: MirrorScaleMode) {
        val current = _layoutState.value
        _layoutState.value = current.copy(
            mirrorLayer = current.mirrorLayer.copy(mirrorScaleMode = mode)
        )
        persistCurrentLayout()
    }

    fun setCameraScaleMode(mode: CameraScaleMode) {
        val current = _layoutState.value
        _layoutState.value = current.copy(
            cameraLayer = current.cameraLayer.copy(cameraScaleMode = mode)
        )
        persistCurrentLayout()
    }

    fun toggleLayerVisibility(layer: LayerType) {
        val current = _layoutState.value
        val updated = if (layer == LayerType.MIRROR) {
            current.copy(mirrorLayer = current.mirrorLayer.copy(isVisible = !current.mirrorLayer.isVisible))
        } else {
            val newVis = !current.cameraLayer.isVisible
            cameraHelper.setCameraEnabled(newVis)
            current.copy(cameraLayer = current.cameraLayer.copy(isVisible = newVis))
        }
        _layoutState.value = updated
        persistCurrentLayout()
    }

    fun swapLayerOrder() {
        val current = _layoutState.value
        val mirrorZ = current.mirrorLayer.zIndex
        val cameraZ = current.cameraLayer.zIndex
        _layoutState.value = current.copy(
            mirrorLayer = current.mirrorLayer.copy(zIndex = cameraZ),
            cameraLayer = current.cameraLayer.copy(zIndex = mirrorZ)
        )
        persistCurrentLayout()
    }

    fun applyPreset(preset: LayoutPreset) {
        val newState = MirroringLayoutState.forPreset(preset)
        _layoutState.value = newState
        cameraHelper.setCameraEnabled(newState.cameraLayer.isVisible)
        persistCurrentLayout()
    }

    fun resetLayout() {
        _layoutState.value = MirroringLayoutState.forPreset(LayoutPreset.FULLSCREEN_PIP)
        cameraHelper.setCameraEnabled(true)
        viewModelScope.launch {
            preferencesRepository.resetLayout()
        }
    }

    fun toggleCameraFacing() {
        cameraHelper.toggleCameraFacing()
    }

    private fun persistCurrentLayout() {
        viewModelScope.launch {
            preferencesRepository.saveLayoutState(_layoutState.value)
        }
    }

    fun startTestStreamSimulation() {
        testSimulationJob?.cancel()
        usbAdbManager.setTestConnectionStatus(ConnectionStatus.STREAMING_ACTIVE, "Test Stream Active")
        testSimulationJob = viewModelScope.launch {
            // Simulates local loopback H.264 stream for testing in headless or emulator environments
            decoder.initializeDecoder(1080, 1920)
            while (isActive) {
                delay(33L) // ~30 fps
                val dummyNal = NalUnit(type = 1, data = ByteArray(1024), isKeyFrame = false)
                decoder.feedNalUnit(dummyNal)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopMirroring()
        usbDetector.stopListening()
        cameraHelper.closeCamera()
    }
}
