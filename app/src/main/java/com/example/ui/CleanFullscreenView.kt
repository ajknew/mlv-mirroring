package com.example.ui

import android.app.Activity
import android.view.Surface
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.model.CameraFacing
import com.example.model.CameraState
import com.example.model.LayerType
import com.example.model.MirroringLayoutState
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.TextPrimary
import kotlin.math.roundToInt

@Composable
fun CleanFullscreenView(
    layoutState: MirroringLayoutState,
    cameraState: CameraState,
    showToolbar: Boolean,
    onTapScreen: () -> Unit,
    onExitFullscreen: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleCameraFacing: () -> Unit,
    onOpenEditor: () -> Unit,
    onMirrorSurfaceCreated: (Surface) -> Unit,
    onMirrorSurfaceDestroyed: () -> Unit,
    onCameraSurfaceCreated: (Surface) -> Unit,
    onCameraSurfaceDestroyed: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    BackHandler {
        onExitFullscreen()
    }

    DisposableEffect(Unit) {
        activity?.let { act ->
            val window = act.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            activity?.let { act ->
                val window = act.window
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onTapScreen() }
            .testTag("clean_fullscreen_view")
    ) {
        // Video and Camera rendering according to shared layout configuration
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()

            val layers = listOf(layoutState.mirrorLayer, layoutState.cameraLayer).sortedBy { it.zIndex }

            for (layer in layers) {
                if (!layer.isVisible) continue

                val bounds = layer.bounds
                val layerLeft = maxWidth * bounds.x
                val layerTop = maxHeight * bounds.y
                val layerWidth = maxWidth * bounds.width
                val layerHeight = maxHeight * bounds.height

                Box(
                    modifier = Modifier
                        .offset(x = layerLeft, y = layerTop)
                        .size(width = layerWidth, height = layerHeight)
                        .clip(RoundedCornerShape(if (layer.type == LayerType.CAMERA) 8.dp else 0.dp))
                ) {
                    if (layer.type == LayerType.MIRROR) {
                        VideoSurface(
                            onSurfaceCreated = onMirrorSurfaceCreated,
                            onSurfaceDestroyed = onMirrorSurfaceDestroyed
                        )
                    } else if (cameraState.isEnabled) {
                        CameraPreviewSurface(
                            onSurfaceCreated = onCameraSurfaceCreated,
                            onSurfaceDestroyed = onCameraSurfaceDestroyed
                        )
                    }
                }
            }
        }

        // 3-Second Auto-Hiding Floating Toolbar
        AnimatedVisibility(
            visible = showToolbar,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.92f)),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
                modifier = Modifier.testTag("fullscreen_toolbar")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("fullscreen_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = onToggleCameraFacing,
                        modifier = Modifier.testTag("fullscreen_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera",
                            tint = if (cameraState.facing == CameraFacing.FRONT) CyanAccent else TextPrimary
                        )
                    }

                    IconButton(
                        onClick = onOpenEditor,
                        modifier = Modifier.testTag("fullscreen_editor_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = "Layout Editor",
                            tint = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = onExitFullscreen,
                        modifier = Modifier.testTag("exit_fullscreen_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloseFullscreen,
                            contentDescription = "Exit Fullscreen",
                            tint = CyanAccent
                        )
                    }
                }
            }
        }
    }
}
