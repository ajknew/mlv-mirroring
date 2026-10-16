package com.example

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.model.CameraFacing
import com.example.model.CameraScaleMode
import com.example.model.ConnectionStatus
import com.example.model.LayerType
import com.example.model.LayoutPreset
import com.example.model.MirrorScaleMode
import com.example.model.NormalizedRect
import com.example.ui.MainMirroringScreen
import com.example.ui.MirroringViewModel
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RobotMlvMirroringTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var app: Application
    private lateinit var viewModel: MirroringViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        viewModel = MirroringViewModel(app)
    }

    @Test
    fun `robot test 1 - verify main screen displays core mirroring elements`() {
        composeTestRule.setContent {
            MyApplicationTheme {
                MainMirroringScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        // Verify main controls exist
        composeTestRule.onNodeWithTag("main_mirroring_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("video_container_area").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_toggle_mirroring").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_fullscreen_trigger").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_open_editor").assertIsDisplayed()
    }

    @Test
    fun `robot test 2 - start and stop mirroring state transitions`() {
        assertEquals(ConnectionStatus.DISCONNECTED, viewModel.connectionStatus.value)

        // Starting test simulation
        viewModel.startMirroring()
        assertEquals(ConnectionStatus.STREAMING_ACTIVE, viewModel.connectionStatus.value)

        // Stopping mirroring
        viewModel.stopMirroring()
        assertEquals(ConnectionStatus.STREAMING_STOPPED, viewModel.connectionStatus.value)
    }

    @Test
    fun `robot test 3 - dialog navigation and dismissal`() {
        composeTestRule.setContent {
            MyApplicationTheme {
                MainMirroringScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        // Open Client Setup Guide
        composeTestRule.onNodeWithTag("btn_client_guide").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("client_guide_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("close_guide_button").performClick()
        composeTestRule.waitForIdle()

        // Open Diagnostics Telemetry
        composeTestRule.onNodeWithTag("btn_diagnostics").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("diagnostics_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("close_diagnostics_button").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `robot test 4 - layout editor layer modifications and presets`() {
        composeTestRule.setContent {
            MyApplicationTheme {
                MainMirroringScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        // Open layout editor
        composeTestRule.onNodeWithTag("btn_open_editor").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("layout_editor_screen").assertIsDisplayed()

        // Test layer selection
        viewModel.setSelectedLayer(LayerType.MIRROR)
        assertEquals(LayerType.MIRROR, viewModel.layoutState.value.selectedLayer)

        viewModel.setSelectedLayer(LayerType.CAMERA)
        assertEquals(LayerType.CAMERA, viewModel.layoutState.value.selectedLayer)

        // Move and resize layer bounds
        val customRect = NormalizedRect(x = 0.2f, y = 0.3f, width = 0.4f, height = 0.4f)
        viewModel.updateLayerBounds(LayerType.CAMERA, customRect)
        assertEquals(customRect, viewModel.layoutState.value.cameraLayer.bounds)
        assertEquals(LayoutPreset.CUSTOM, viewModel.layoutState.value.activePreset)

        // Swap layer zIndex
        val initialOrder = viewModel.layoutState.value.isMirrorOnTop()
        viewModel.swapLayerOrder()
        assertEquals(!initialOrder, viewModel.layoutState.value.isMirrorOnTop())

        // Toggle layer visibility
        val initialCamVis = viewModel.layoutState.value.cameraLayer.isVisible
        viewModel.toggleLayerVisibility(LayerType.CAMERA)
        assertEquals(!initialCamVis, viewModel.layoutState.value.cameraLayer.isVisible)

        // Scale mode changes
        viewModel.setMirrorScaleMode(MirrorScaleMode.FILL)
        assertEquals(MirrorScaleMode.FILL, viewModel.layoutState.value.mirrorLayer.mirrorScaleMode)

        viewModel.setCameraScaleMode(CameraScaleMode.STRETCH)
        assertEquals(CameraScaleMode.STRETCH, viewModel.layoutState.value.cameraLayer.cameraScaleMode)

        // Test all presets
        for (preset in LayoutPreset.values()) {
            viewModel.applyPreset(preset)
            assertEquals(preset, viewModel.layoutState.value.activePreset)
        }

        // Reset layout
        viewModel.resetLayout()
        composeTestRule.waitForIdle()
        assertEquals(LayoutPreset.FULLSCREEN_PIP, viewModel.layoutState.value.activePreset)
    }

    @Test
    fun `robot test 5 - clean fullscreen and auto-hiding toolbar lifecycle`() {
        assertFalse(viewModel.isFullscreen.value)

        // Enter fullscreen
        viewModel.setFullscreen(true)
        assertTrue(viewModel.isFullscreen.value)
        assertTrue(viewModel.showToolbar.value) // Toolbar shown initially

        // Camera lens switch in fullscreen
        val initialFacing = viewModel.cameraState.value.facing
        viewModel.toggleCameraFacing()
        val expectedFacing = if (initialFacing == CameraFacing.FRONT) CameraFacing.BACK else CameraFacing.FRONT
        assertEquals(expectedFacing, viewModel.cameraState.value.facing)

        // Exit fullscreen
        viewModel.setFullscreen(false)
        assertFalse(viewModel.isFullscreen.value)
    }
}
