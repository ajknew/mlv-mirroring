package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ClientDeviceInfo
import com.example.model.ConnectionStatus
import com.example.model.StreamMetrics
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DiagnosticsDialog(
    status: ConnectionStatus,
    statusMessage: String,
    deviceInfo: ClientDeviceInfo,
    metrics: StreamMetrics,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .testTag("diagnostics_dialog")
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Diagnostics & Telemetry",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider(color = DarkBorder)

                // USB Status Section
                Text(
                    text = "USB OTG TRANSPORT",
                    color = CyanAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                DiagnosticRow("Device Name", deviceInfo.deviceName)
                DiagnosticRow("Manufacturer", deviceInfo.manufacturer)
                DiagnosticRow("Product Model", deviceInfo.model)
                DiagnosticRow("Vendor / Product ID", "0x${deviceInfo.vendorId.toString(16)} : 0x${deviceInfo.productId.toString(16)}")
                DiagnosticRow("ADB Interface Found", if (deviceInfo.hasAdbInterface) "YES (0xFF:0x42:0x01)" else "NO")
                DiagnosticRow("UVC Video Interface", if (deviceInfo.hasUvcInterface) "YES" else "NO")
                DiagnosticRow("USB Host Permission", if (deviceInfo.usbPermissionGranted) "GRANTED" else "PENDING")

                HorizontalDivider(color = DarkBorder)

                // Stream & Decoder Section
                Text(
                    text = "H.264 CODEC & PIPELINE",
                    color = CyanAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                DiagnosticRow("Session Status", status.name)
                DiagnosticRow("Detailed Message", statusMessage)
                DiagnosticRow("Decoder Name", metrics.codecName)
                DiagnosticRow("Stream Resolution", if (metrics.videoWidth > 0) "${metrics.videoWidth} x ${metrics.videoHeight}" else "Awaiting stream")
                DiagnosticRow("Frame Rate", if (metrics.currentFps > 0) String.format("%.1f FPS", metrics.currentFps) else "N/A")
                DiagnosticRow("Bitrate", if (metrics.bitrateKbps > 0) String.format("%.1f kbps", metrics.bitrateKbps) else "N/A")
                DiagnosticRow("Decoded Frames", "${metrics.decodedFrames}")
                DiagnosticRow("Dropped Frames", "${metrics.droppedFrames}")
                DiagnosticRow("Total Data Received", "${metrics.receivedBytesTotal / 1024} KB")
                DiagnosticRow("Measured Latency", if (metrics.latencyMs > 0) "${metrics.latencyMs} ms" else "N/A")

                if (metrics.lastError != null) {
                    Text(
                        text = "Error: ${metrics.lastError}",
                        color = RoseError,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("close_diagnostics_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DarkSurface)
                ) {
                    Text("Close Diagnostics", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}
