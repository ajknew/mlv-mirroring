package com.example.model

enum class LayerType {
    MIRROR,
    CAMERA
}

enum class MirrorScaleMode {
    FIT,   // Maintain aspect ratio, letterboxed/pillarboxed, no cropping
    FILL   // Maintain aspect ratio, center-cropped to fill bounds
}

enum class CameraScaleMode {
    PROPORTIONAL, // Maintain camera native aspect ratio, whole frame shown without crop
    STRETCH       // Fill entire layer bounds, whole frame shown without crop (free stretch)
}

enum class LayoutPreset(val title: String) {
    MIRROR_FULLSCREEN("Mirror Fullscreen"),
    FULLSCREEN_PIP("Mirror Fullscreen + Camera PiP"),
    SPLIT_TOP_BOTTOM("Mirror Top + Camera Bottom"),
    SPLIT_BOTTOM_TOP("Camera Top + Mirror Bottom"),
    SPLIT_70_30("Mirror 70% + Camera 30%"),
    SPLIT_30_70("Camera 70% + Mirror 30%"),
    SIDE_BY_SIDE("Side by Side"),
    CUSTOM("Custom Layout")
}

data class NormalizedRect(
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 1f,
    val height: Float = 1f
) {
    fun clamped(): NormalizedRect {
        val safeW = width.coerceIn(0.1f, 1.0f)
        val safeH = height.coerceIn(0.1f, 1.0f)
        val safeX = x.coerceIn(0f, 1.0f - safeW)
        val safeY = y.coerceIn(0f, 1.0f - safeH)
        return NormalizedRect(safeX, safeY, safeW, safeH)
    }
}

data class LayerConfig(
    val type: LayerType,
    val bounds: NormalizedRect,
    val isVisible: Boolean = true,
    val zIndex: Int = 0, // Higher is drawn on top
    val mirrorScaleMode: MirrorScaleMode = MirrorScaleMode.FIT,
    val cameraScaleMode: CameraScaleMode = CameraScaleMode.PROPORTIONAL,
    val lockAspectRatio: Boolean = true
)

data class MirroringLayoutState(
    val mirrorLayer: LayerConfig = LayerConfig(
        type = LayerType.MIRROR,
        bounds = NormalizedRect(0f, 0f, 1f, 1f),
        isVisible = true,
        zIndex = 0,
        mirrorScaleMode = MirrorScaleMode.FIT
    ),
    val cameraLayer: LayerConfig = LayerConfig(
        type = LayerType.CAMERA,
        bounds = NormalizedRect(0.68f, 0.68f, 0.28f, 0.28f),
        isVisible = true,
        zIndex = 1,
        cameraScaleMode = CameraScaleMode.PROPORTIONAL
    ),
    val activePreset: LayoutPreset = LayoutPreset.FULLSCREEN_PIP,
    val selectedLayer: LayerType = LayerType.MIRROR
) {
    fun isMirrorOnTop(): Boolean = mirrorLayer.zIndex > cameraLayer.zIndex

    companion object {
        fun forPreset(preset: LayoutPreset): MirroringLayoutState {
            return when (preset) {
                LayoutPreset.MIRROR_FULLSCREEN -> MirroringLayoutState(
                    mirrorLayer = LayerConfig(
                        type = LayerType.MIRROR,
                        bounds = NormalizedRect(0f, 0f, 1f, 1f),
                        isVisible = true,
                        zIndex = 1,
                        mirrorScaleMode = MirrorScaleMode.FIT
                    ),
                    cameraLayer = LayerConfig(
                        type = LayerType.CAMERA,
                        bounds = NormalizedRect(0.7f, 0.7f, 0.26f, 0.26f),
                        isVisible = false,
                        zIndex = 0
                    ),
                    activePreset = LayoutPreset.MIRROR_FULLSCREEN
                )
                LayoutPreset.FULLSCREEN_PIP -> MirroringLayoutState(
                    mirrorLayer = LayerConfig(
                        type = LayerType.MIRROR,
                        bounds = NormalizedRect(0f, 0f, 1f, 1f),
                        isVisible = true,
                        zIndex = 0,
                        mirrorScaleMode = MirrorScaleMode.FIT
                    ),
                    cameraLayer = LayerConfig(
                        type = LayerType.CAMERA,
                        bounds = NormalizedRect(0.68f, 0.68f, 0.28f, 0.28f),
                        isVisible = true,
                        zIndex = 1,
                        cameraScaleMode = CameraScaleMode.PROPORTIONAL
                    ),
                    activePreset = LayoutPreset.FULLSCREEN_PIP
                )
                LayoutPreset.SPLIT_TOP_BOTTOM -> MirroringLayoutState(
                    mirrorLayer = LayerConfig(
                        type = LayerType.MIRROR,
                        bounds = NormalizedRect(0f, 0f, 1f, 0.5f),
                        isVisible = true,
                        zIndex = 0,
                        mirrorScaleMode = MirrorScaleMode.FIT
                    ),
                    cameraLayer = LayerConfig(
                        type = LayerType.CAMERA,
                        bounds = NormalizedRect(0f, 0.5f, 1f, 0.5f),
                        isVisible = true,
                        zIndex = 1,
                        cameraScaleMode = CameraScaleMode.PROPORTIONAL
                    ),
                    activePreset = LayoutPreset.SPLIT_TOP_BOTTOM
                )
                LayoutPreset.SPLIT_BOTTOM_TOP -> MirroringLayoutState(
                    mirrorLayer = LayerConfig(
                        type = LayerType.MIRROR,
                        bounds = NormalizedRect(0f, 0.5f, 1f, 0.5f),
                        isVisible = true,
                        zIndex = 0,
                        mirrorScaleMode = MirrorScaleMode.FIT
                    ),
                    cameraLayer = LayerConfig(
                        type = LayerType.CAMERA,
                        bounds = NormalizedRect(0f, 0f, 1f, 0.5f),
                        isVisible = true,
                        zIndex = 1,
                        cameraScaleMode = CameraScaleMode.PROPORTIONAL
                    ),
                    activePreset = LayoutPreset.SPLIT_BOTTOM_TOP
                )
                LayoutPreset.SPLIT_70_30 -> MirroringLayoutState(
                    mirrorLayer = LayerConfig(
                        type = LayerType.MIRROR,
                        bounds = NormalizedRect(0f, 0f, 1f, 0.7f),
                        isVisible = true,
                        zIndex = 0,
                        mirrorScaleMode = MirrorScaleMode.FIT
                    ),
                    cameraLayer = LayerConfig(
                        type = LayerType.CAMERA,
                        bounds = NormalizedRect(0f, 0.7f, 1f, 0.3f),
                        isVisible = true,
                        zIndex = 1,
                        cameraScaleMode = CameraScaleMode.PROPORTIONAL
                    ),
                    activePreset = LayoutPreset.SPLIT_70_30
                )
                LayoutPreset.SPLIT_30_70 -> MirroringLayoutState(
                    mirrorLayer = LayerConfig(
                        type = LayerType.MIRROR,
                        bounds = NormalizedRect(0f, 0.7f, 1f, 0.3f),
                        isVisible = true,
                        zIndex = 0,
                        mirrorScaleMode = MirrorScaleMode.FIT
                    ),
                    cameraLayer = LayerConfig(
                        type = LayerType.CAMERA,
                        bounds = NormalizedRect(0f, 0f, 1f, 0.7f),
                        isVisible = true,
                        zIndex = 1,
                        cameraScaleMode = CameraScaleMode.PROPORTIONAL
                    ),
                    activePreset = LayoutPreset.SPLIT_30_70
                )
                LayoutPreset.SIDE_BY_SIDE -> MirroringLayoutState(
                    mirrorLayer = LayerConfig(
                        type = LayerType.MIRROR,
                        bounds = NormalizedRect(0f, 0f, 0.5f, 1f),
                        isVisible = true,
                        zIndex = 0,
                        mirrorScaleMode = MirrorScaleMode.FIT
                    ),
                    cameraLayer = LayerConfig(
                        type = LayerType.CAMERA,
                        bounds = NormalizedRect(0.5f, 0f, 0.5f, 1f),
                        isVisible = true,
                        zIndex = 1,
                        cameraScaleMode = CameraScaleMode.PROPORTIONAL
                    ),
                    activePreset = LayoutPreset.SIDE_BY_SIDE
                )
                LayoutPreset.CUSTOM -> MirroringLayoutState(
                    activePreset = LayoutPreset.CUSTOM
                )
            }
        }
    }
}
