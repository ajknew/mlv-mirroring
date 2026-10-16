package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.CameraFacing
import com.example.model.CameraScaleMode
import com.example.model.CameraState
import com.example.model.LayerType
import com.example.model.LayoutPreset
import com.example.model.MirrorScaleMode
import com.example.model.MirroringLayoutState
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsSheet(
    layoutState: MirroringLayoutState,
    cameraState: CameraState,
    onToggleCameraFacing: () -> Unit,
    onToggleCameraEnabled: () -> Unit,
    onSetCameraScale: (CameraScaleMode) -> Unit,
    onSetMirrorScale: (MirrorScaleMode) -> Unit,
    onApplyPreset: (LayoutPreset) -> Unit,
    onResetLayout: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .testTag("settings_dialog")
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_settings_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                    }
                }

                HorizontalDivider(color = DarkBorder)

                // Camera Controls Section
                Text(
                    text = "CAMERA PIP CONTROLS",
                    color = CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Camera Layer", color = TextPrimary, fontSize = 14.sp)
                    Switch(
                        checked = cameraState.isEnabled,
                        onCheckedChange = { onToggleCameraEnabled() },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent, checkedTrackColor = DarkSurfaceVariant)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Camera Lens", color = TextPrimary, fontSize = 14.sp)
                    OutlinedButton(
                        onClick = onToggleCameraFacing,
                        modifier = Modifier.testTag("settings_switch_camera_facing"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Text(if (cameraState.facing == CameraFacing.FRONT) "Front Lens" else "Back Lens")
                    }
                }

                Text("Camera Aspect Ratio Mode", color = TextSecondary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = layoutState.cameraLayer.cameraScaleMode == CameraScaleMode.PROPORTIONAL,
                        onClick = { onSetCameraScale(CameraScaleMode.PROPORTIONAL) },
                        label = { Text("Proportional", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyanAccent, selectedLabelColor = DarkSurface)
                    )
                    FilterChip(
                        selected = layoutState.cameraLayer.cameraScaleMode == CameraScaleMode.STRETCH,
                        onClick = { onSetCameraScale(CameraScaleMode.STRETCH) },
                        label = { Text("Free Stretch", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyanAccent, selectedLabelColor = DarkSurface)
                    )
                }

                HorizontalDivider(color = DarkBorder)

                // Mirror Scale Section
                Text(
                    text = "MIRROR SCREEN SCALE",
                    color = CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = layoutState.mirrorLayer.mirrorScaleMode == MirrorScaleMode.FIT,
                        onClick = { onSetMirrorScale(MirrorScaleMode.FIT) },
                        label = { Text("FIT (Keep Aspect)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyanAccent, selectedLabelColor = DarkSurface)
                    )
                    FilterChip(
                        selected = layoutState.mirrorLayer.mirrorScaleMode == MirrorScaleMode.FILL,
                        onClick = { onSetMirrorScale(MirrorScaleMode.FILL) },
                        label = { Text("FILL (Crop Fill)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyanAccent, selectedLabelColor = DarkSurface)
                    )
                }

                HorizontalDivider(color = DarkBorder)

                // Quick Presets
                Text(
                    text = "QUICK LAYOUT PRESETS",
                    color = CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = layoutState.activePreset == LayoutPreset.MIRROR_FULLSCREEN,
                        onClick = { onApplyPreset(LayoutPreset.MIRROR_FULLSCREEN) },
                        label = { Text("Fullscreen") }
                    )
                    FilterChip(
                        selected = layoutState.activePreset == LayoutPreset.FULLSCREEN_PIP,
                        onClick = { onApplyPreset(LayoutPreset.FULLSCREEN_PIP) },
                        label = { Text("PiP") }
                    )
                    FilterChip(
                        selected = layoutState.activePreset == LayoutPreset.SPLIT_TOP_BOTTOM,
                        onClick = { onApplyPreset(LayoutPreset.SPLIT_TOP_BOTTOM) },
                        label = { Text("Split") }
                    )
                }

                HorizontalDivider(color = DarkBorder)

                // Diagnostics & Reset Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenDiagnostics,
                        modifier = Modifier.weight(1f).testTag("settings_open_diagnostics"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent)
                    ) {
                        Text("Diagnostics", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onOpenGuide,
                        modifier = Modifier.weight(1f).testTag("settings_open_guide"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Text("Client Setup", fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = onResetLayout,
                    modifier = Modifier.fillMaxWidth().testTag("settings_reset_layout"),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = AmberWarning)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Reset Layout to Default", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
