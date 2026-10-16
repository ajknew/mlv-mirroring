package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CameraScaleMode
import com.example.model.LayerConfig
import com.example.model.LayerType
import com.example.model.LayoutPreset
import com.example.model.MirrorScaleMode
import com.example.model.MirroringLayoutState
import com.example.model.NormalizedRect
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun LayoutEditorScreen(
    layoutState: MirroringLayoutState,
    onUpdateBounds: (LayerType, NormalizedRect) -> Unit,
    onSelectLayer: (LayerType) -> Unit,
    onSetMirrorScale: (MirrorScaleMode) -> Unit,
    onSetCameraScale: (CameraScaleMode) -> Unit,
    onToggleVisibility: (LayerType) -> Unit,
    onSwapOrder: () -> Unit,
    onApplyPreset: (LayoutPreset) -> Unit,
    onResetLayout: () -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(if (layoutState.selectedLayer == LayerType.MIRROR) 0 else 1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .testTag("layout_editor_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Layout & Layer Editor",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Preset: ${layoutState.activePreset.title}",
                    color = CyanAccent,
                    fontSize = 12.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onResetLayout,
                    modifier = Modifier.testTag("reset_layout_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberWarning)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", fontSize = 12.sp)
                }
                IconButton(onClick = onClose, modifier = Modifier.testTag("close_editor_button")) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Preset Selector Horizontal Scroll
        Text(
            text = "PRESET LAYOUTS",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().testTag("presets_row")
        ) {
            items(LayoutPreset.values()) { preset ->
                val isSelected = layoutState.activePreset == preset
                FilterChip(
                    selected = isSelected,
                    onClick = { onApplyPreset(preset) },
                    label = { Text(preset.title, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanAccent,
                        selectedLabelColor = DarkSurface,
                        containerColor = DarkSurfaceVariant,
                        labelColor = TextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Visual Drag & Drop Canvas (Normalized aspect container)
        Text(
            text = "CANVAS PREVIEW (DRAG & RESIZE)",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                .testTag("editor_canvas")
        ) {
            val canvasWidthPx = constraints.maxWidth.toFloat()
            val canvasHeightPx = constraints.maxHeight.toFloat()

            // Sort layers by zIndex so higher is drawn on top
            val layers = listOf(layoutState.mirrorLayer, layoutState.cameraLayer).sortedBy { it.zIndex }

            for (layer in layers) {
                if (!layer.isVisible) continue

                val isSelected = (layer.type == layoutState.selectedLayer)
                val bounds = layer.bounds

                val layerLeft = maxWidth * bounds.x
                val layerTop = maxHeight * bounds.y
                val layerWidth = maxWidth * bounds.width
                val layerHeight = maxHeight * bounds.height

                val borderColor = if (isSelected) CyanAccent else Color.Gray.copy(alpha = 0.5f)
                val bgColor = if (layer.type == LayerType.MIRROR) Color(0xFF1E293B) else Color(0xFF0F766E)

                Box(
                    modifier = Modifier
                        .offset(x = layerLeft, y = layerTop)
                        .size(width = layerWidth, height = layerHeight)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor.copy(alpha = 0.85f))
                        .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(8.dp))
                        .pointerInput(layer.type, bounds) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onSelectLayer(layer.type)
                                val deltaNormX = dragAmount.x / canvasWidthPx
                                val deltaNormY = dragAmount.y / canvasHeightPx
                                val newRect = NormalizedRect(
                                    x = bounds.x + deltaNormX,
                                    y = bounds.y + deltaNormY,
                                    width = bounds.width,
                                    height = bounds.height
                                )
                                onUpdateBounds(layer.type, newRect)
                            }
                        }
                        .testTag(if (layer.type == LayerType.MIRROR) "editor_mirror_box" else "editor_camera_box"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (layer.type == LayerType.MIRROR) "Mirror Screen" else "Camera PiP",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${(bounds.width * 100).toInt()}% x ${(bounds.height * 100).toInt()}%",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }

                    // Resize corner handle at bottom right
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .padding(4.dp)
                                .background(CyanAccent, CircleShape)
                                .pointerInput(layer.type, bounds) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val deltaW = dragAmount.x / canvasWidthPx
                                        val deltaH = dragAmount.y / canvasHeightPx
                                        val newRect = NormalizedRect(
                                            x = bounds.x,
                                            y = bounds.y,
                                            width = bounds.width + deltaW,
                                            height = bounds.height + deltaH
                                        )
                                        onUpdateBounds(layer.type, newRect)
                                    }
                                }
                                .testTag("resize_handle")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Layer selection tabs (Mirror vs Camera)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkSurfaceVariant,
            contentColor = CyanAccent,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CyanAccent
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = {
                    selectedTab = 0
                    onSelectLayer(LayerType.MIRROR)
                },
                text = { Text("Mirror Layer", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_mirror_layer")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = {
                    selectedTab = 1
                    onSelectLayer(LayerType.CAMERA)
                },
                text = { Text("Camera Layer", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_camera_layer")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selected layer controls (Scrollable)
        val activeLayer = if (selectedTab == 0) layoutState.mirrorLayer else layoutState.cameraLayer

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Visibility & Z-Index controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onToggleVisibility(activeLayer.type) },
                    modifier = Modifier.testTag("toggle_visibility_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Icon(
                        imageVector = if (activeLayer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Visibility",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (activeLayer.isVisible) "Visible" else "Hidden")
                }

                Button(
                    onClick = onSwapOrder,
                    modifier = Modifier.testTag("swap_layer_order_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = CyanAccent)
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "Order", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (layoutState.isMirrorOnTop()) "Mirror on Top" else "Camera on Top", fontSize = 12.sp)
                }
            }

            // Scale mode selector
            if (activeLayer.type == LayerType.MIRROR) {
                Text(
                    text = "MIRROR SCALE MODE",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = activeLayer.mirrorScaleMode == MirrorScaleMode.FIT,
                        onClick = { onSetMirrorScale(MirrorScaleMode.FIT) },
                        label = { Text("FIT (Keep Aspect)") },
                        modifier = Modifier.testTag("mirror_scale_fit"),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyanAccent, selectedLabelColor = DarkSurface)
                    )
                    FilterChip(
                        selected = activeLayer.mirrorScaleMode == MirrorScaleMode.FILL,
                        onClick = { onSetMirrorScale(MirrorScaleMode.FILL) },
                        label = { Text("FILL (Crop Fill)") },
                        modifier = Modifier.testTag("mirror_scale_fill"),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyanAccent, selectedLabelColor = DarkSurface)
                    )
                }
            } else {
                Text(
                    text = "CAMERA SCALE MODE",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = activeLayer.cameraScaleMode == CameraScaleMode.PROPORTIONAL,
                        onClick = { onSetCameraScale(CameraScaleMode.PROPORTIONAL) },
                        label = { Text("Proportional (No Crop)") },
                        modifier = Modifier.testTag("camera_scale_proportional"),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyanAccent, selectedLabelColor = DarkSurface)
                    )
                    FilterChip(
                        selected = activeLayer.cameraScaleMode == CameraScaleMode.STRETCH,
                        onClick = { onSetCameraScale(CameraScaleMode.STRETCH) },
                        label = { Text("Free Stretch (No Crop)") },
                        modifier = Modifier.testTag("camera_scale_stretch"),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyanAccent, selectedLabelColor = DarkSurface)
                    )
                }
            }

            // Numeric Sliders for fine-tuning
            Text(
                text = "POSITION & SIZE FINE-TUNING",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            SliderControl(
                label = "Width",
                value = activeLayer.bounds.width,
                range = 0.1f..1.0f,
                onValueChange = { newW ->
                    onUpdateBounds(activeLayer.type, activeLayer.bounds.copy(width = newW))
                }
            )

            SliderControl(
                label = "Height",
                value = activeLayer.bounds.height,
                range = 0.1f..1.0f,
                onValueChange = { newH ->
                    onUpdateBounds(activeLayer.type, activeLayer.bounds.copy(height = newH))
                }
            )

            SliderControl(
                label = "Horizontal Position (X)",
                value = activeLayer.bounds.x,
                range = 0f..(1.0f - activeLayer.bounds.width).coerceAtLeast(0f),
                onValueChange = { newX ->
                    onUpdateBounds(activeLayer.type, activeLayer.bounds.copy(x = newX))
                }
            )

            SliderControl(
                label = "Vertical Position (Y)",
                value = activeLayer.bounds.y,
                range = 0f..(1.0f - activeLayer.bounds.height).coerceAtLeast(0f),
                onValueChange = { newY ->
                    onUpdateBounds(activeLayer.type, activeLayer.bounds.copy(y = newY))
                }
            )
        }
    }
}

@Composable
private fun SliderControl(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = TextPrimary, fontSize = 12.sp)
            Text(
                text = "${(value * 100).toInt()}%",
                color = CyanAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = CyanAccent,
                activeTrackColor = CyanAccent,
                inactiveTrackColor = DarkBorder
            )
        )
    }
}
