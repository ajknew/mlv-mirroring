package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.CameraFacing
import com.example.model.ConnectionStatus
import com.example.model.LayerType
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMirroringScreen(viewModel: MirroringViewModel) {
    val context = LocalContext.current
    val layoutState by viewModel.layoutState.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()
    val showToolbar by viewModel.showToolbar.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val streamMetrics by viewModel.streamMetrics.collectAsState()
    val cameraState by viewModel.cameraState.collectAsState()

    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val isEditorOpen by viewModel.isEditorOpen.collectAsState()
    val isDiagnosticsOpen by viewModel.isDiagnosticsOpen.collectAsState()
    val isClientGuideOpen by viewModel.isClientGuideOpen.collectAsState()

    // Camera permission request
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.cameraHelper.detectAvailableCameras()
            viewModel.cameraHelper.setCameraEnabled(true)
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (isFullscreen) {
        CleanFullscreenView(
            layoutState = layoutState,
            cameraState = cameraState,
            showToolbar = showToolbar,
            onTapScreen = { viewModel.triggerToolbar() },
            onExitFullscreen = { viewModel.setFullscreen(false) },
            onOpenSettings = { viewModel.toggleSettings(true) },
            onToggleCameraFacing = { viewModel.toggleCameraFacing() },
            onOpenEditor = { viewModel.toggleEditor(true) },
            onMirrorSurfaceCreated = { viewModel.attachMirrorSurface(it) },
            onMirrorSurfaceDestroyed = { viewModel.detachMirrorSurface() },
            onCameraSurfaceCreated = { viewModel.attachCameraSurface(it) },
            onCameraSurfaceDestroyed = { viewModel.attachCameraSurface(null) }
        )
    } else {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(CyanAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = "Logo",
                                    tint = DarkBackground,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "MLV Mirroring",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 18.sp
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.toggleClientGuide(true) },
                            modifier = Modifier.testTag("btn_client_guide")
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Setup Guide", tint = CyanAccent)
                        }
                        IconButton(
                            onClick = { viewModel.toggleDiagnostics(true) },
                            modifier = Modifier.testTag("btn_diagnostics")
                        ) {
                            Icon(Icons.Default.Usb, contentDescription = "Diagnostics", tint = TextPrimary)
                        }
                        IconButton(
                            onClick = { viewModel.toggleSettings(true) },
                            modifier = Modifier.testTag("btn_settings")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = DarkSurface
                    )
                )
            },
            containerColor = DarkBackground,
            modifier = Modifier.testTag("main_mirroring_screen")
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Connection Status Card
                ConnectionStatusBadge(
                    status = connectionStatus,
                    statusMessage = statusMessage,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.toggleDiagnostics(true) }
                )

                // Central Video Mirroring + Camera PiP Surface Area
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                        .testTag("video_container_area")
                ) {
                    val containerW = constraints.maxWidth.toFloat()
                    val containerH = constraints.maxHeight.toFloat()

                    val sortedLayers = listOf(layoutState.mirrorLayer, layoutState.cameraLayer).sortedBy { it.zIndex }

                    for (layer in sortedLayers) {
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
                                .clip(RoundedCornerShape(if (layer.type == LayerType.CAMERA) 10.dp else 0.dp))
                                .border(
                                    if (layer.type == LayerType.CAMERA) 1.dp else 0.dp,
                                    if (layer.type == LayerType.CAMERA) CyanAccent.copy(alpha = 0.5f) else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .testTag(if (layer.type == LayerType.MIRROR) "mirror_surface_layer" else "camera_surface_layer")
                        ) {
                            if (layer.type == LayerType.MIRROR) {
                                VideoSurface(
                                    onSurfaceCreated = { viewModel.attachMirrorSurface(it) },
                                    onSurfaceDestroyed = { viewModel.detachMirrorSurface() }
                                )

                                if (connectionStatus != ConnectionStatus.STREAMING_ACTIVE) {
                                    // Placeholder guidance when idle
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Tv,
                                                contentDescription = "Screen",
                                                tint = TextMuted,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Text(
                                                text = "Client Screen Stream Area",
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = if (deviceInfo.deviceName != "No Device Connected")
                                                    "Device: ${deviceInfo.deviceName}"
                                                else
                                                    "Connect Client via USB OTG Cable",
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            } else if (cameraState.isEnabled) {
                                CameraPreviewSurface(
                                    onSurfaceCreated = { viewModel.attachCameraSurface(it) },
                                    onSurfaceDestroyed = { viewModel.attachCameraSurface(null) }
                                )
                            }
                        }
                    }
                }

                // Action Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isStreaming = (connectionStatus == ConnectionStatus.STREAMING_ACTIVE)

                    Button(
                        onClick = {
                            if (isStreaming) viewModel.stopMirroring() else viewModel.startMirroring()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_toggle_mirroring"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isStreaming) RoseError else CyanAccent,
                            contentColor = if (isStreaming) TextPrimary else DarkSurface
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = if (isStreaming) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "Mirroring Action",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isStreaming) "Stop Mirroring" else "Start Mirroring",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { viewModel.setFullscreen(true) },
                        modifier = Modifier.testTag("btn_fullscreen_trigger"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurfaceVariant,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Fullscreen", fontSize = 13.sp)
                    }

                    IconButton(
                        onClick = { viewModel.toggleEditor(true) },
                        modifier = Modifier
                            .background(DarkSurfaceVariant, RoundedCornerShape(14.dp))
                            .testTag("btn_open_editor")
                    ) {
                        Icon(Icons.Default.Dashboard, contentDescription = "Layout Editor", tint = CyanAccent)
                    }
                }
            }
        }
    }

    // Modal Overlays (Layout Editor, Settings, Diagnostics, Client Guide)
    if (isEditorOpen) {
        Dialog(onDismissRequest = { viewModel.toggleEditor(false) }) {
            LayoutEditorScreen(
                layoutState = layoutState,
                onUpdateBounds = { layer, bounds -> viewModel.updateLayerBounds(layer, bounds) },
                onSelectLayer = { viewModel.setSelectedLayer(it) },
                onSetMirrorScale = { viewModel.setMirrorScaleMode(it) },
                onSetCameraScale = { viewModel.setCameraScaleMode(it) },
                onToggleVisibility = { viewModel.toggleLayerVisibility(it) },
                onSwapOrder = { viewModel.swapLayerOrder() },
                onApplyPreset = { viewModel.applyPreset(it) },
                onResetLayout = { viewModel.resetLayout() },
                onClose = { viewModel.toggleEditor(false) }
            )
        }
    }

    if (isSettingsOpen) {
        SettingsSheet(
            layoutState = layoutState,
            cameraState = cameraState,
            onToggleCameraFacing = { viewModel.toggleCameraFacing() },
            onToggleCameraEnabled = { viewModel.toggleLayerVisibility(LayerType.CAMERA) },
            onSetCameraScale = { viewModel.setCameraScaleMode(it) },
            onSetMirrorScale = { viewModel.setMirrorScaleMode(it) },
            onApplyPreset = { viewModel.applyPreset(it) },
            onResetLayout = { viewModel.resetLayout() },
            onOpenGuide = {
                viewModel.toggleSettings(false)
                viewModel.toggleClientGuide(true)
            },
            onOpenDiagnostics = {
                viewModel.toggleSettings(false)
                viewModel.toggleDiagnostics(true)
            },
            onDismiss = { viewModel.toggleSettings(false) }
        )
    }

    if (isDiagnosticsOpen) {
        DiagnosticsDialog(
            status = connectionStatus,
            statusMessage = statusMessage,
            deviceInfo = deviceInfo,
            metrics = streamMetrics,
            onDismiss = { viewModel.toggleDiagnostics(false) }
        )
    }

    if (isClientGuideOpen) {
        ClientGuideDialog(onDismiss = { viewModel.toggleClientGuide(false) })
    }
}
