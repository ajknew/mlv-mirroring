package com.example

import com.example.model.CameraScaleMode
import com.example.model.LayerConfig
import com.example.model.LayerType
import com.example.model.LayoutPreset
import com.example.model.MirrorScaleMode
import com.example.model.MirroringLayoutState
import com.example.model.NormalizedRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutConfigUnitTest {

    @Test
    fun `clamped prevents negative coordinates and zero size`() {
        val invalidRect = NormalizedRect(x = -0.5f, y = -0.2f, width = 0.01f, height = 0.05f)
        val clamped = invalidRect.clamped()

        assertEquals(0f, clamped.x, 0.001f)
        assertEquals(0f, clamped.y, 0.001f)
        assertEquals(0.1f, clamped.width, 0.001f) // min width enforced
        assertEquals(0.1f, clamped.height, 0.001f) // min height enforced
    }

    @Test
    fun `clamped prevents overflowing container boundaries`() {
        val overflowingRect = NormalizedRect(x = 0.8f, y = 0.9f, width = 0.5f, height = 0.5f)
        val clamped = overflowingRect.clamped()

        assertEquals(0.5f, clamped.x, 0.001f) // 1.0 - width (0.5)
        assertEquals(0.5f, clamped.y, 0.001f) // 1.0 - height (0.5)
        assertEquals(0.5f, clamped.width, 0.001f)
        assertEquals(0.5f, clamped.height, 0.001f)
    }

    @Test
    fun `all 8 layout presets produce valid configurations`() {
        for (preset in LayoutPreset.values()) {
            val state = MirroringLayoutState.forPreset(preset)
            assertEquals(preset, state.activePreset)
            
            // Mirror layer bounds validity
            val mb = state.mirrorLayer.bounds
            assertTrue("Mirror X must be >= 0", mb.x >= 0f)
            assertTrue("Mirror Y must be >= 0", mb.y >= 0f)
            assertTrue("Mirror X+W must be <= 1", mb.x + mb.width <= 1.001f)
            assertTrue("Mirror Y+H must be <= 1", mb.y + mb.height <= 1.001f)

            // Camera layer bounds validity
            val cb = state.cameraLayer.bounds
            assertTrue("Camera X must be >= 0", cb.x >= 0f)
            assertTrue("Camera Y must be >= 0", cb.y >= 0f)
            assertTrue("Camera X+W must be <= 1", cb.x + cb.width <= 1.001f)
            assertTrue("Camera Y+H must be <= 1", cb.y + cb.height <= 1.001f)
        }
    }

    @Test
    fun `layer order and zIndex comparison`() {
        val state1 = MirroringLayoutState(
            mirrorLayer = LayerConfig(LayerType.MIRROR, NormalizedRect(), zIndex = 0),
            cameraLayer = LayerConfig(LayerType.CAMERA, NormalizedRect(), zIndex = 1)
        )
        assertFalse(state1.isMirrorOnTop())

        val state2 = MirroringLayoutState(
            mirrorLayer = LayerConfig(LayerType.MIRROR, NormalizedRect(), zIndex = 2),
            cameraLayer = LayerConfig(LayerType.CAMERA, NormalizedRect(), zIndex = 1)
        )
        assertTrue(state2.isMirrorOnTop())
    }

    @Test
    fun `scale modes maintain distinct semantics`() {
        assertEquals(MirrorScaleMode.FIT, MirrorScaleMode.valueOf("FIT"))
        assertEquals(MirrorScaleMode.FILL, MirrorScaleMode.valueOf("FILL"))
        assertEquals(CameraScaleMode.PROPORTIONAL, CameraScaleMode.valueOf("PROPORTIONAL"))
        assertEquals(CameraScaleMode.STRETCH, CameraScaleMode.valueOf("STRETCH"))
    }
}
