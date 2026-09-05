package com.neurosight.app.ui

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Everything the UI needs to render is passed in as simple state + callbacks
 * from MainActivity, keeping this composable free of Android lifecycle /
 * camera / TFLite concerns (those live in their respective modules).
 */
data class NeuroSightUiState(
    val isRunning: Boolean = false,
    val currentClass: String = "-",
    val confidence: Float = 0f,
    val demoModeEnabled: Boolean = false,
    val activeBackendName: String = "-",
    val hasCameraPermission: Boolean = false
)

@Composable
fun MainScreen(
    uiState: NeuroSightUiState,
    onPreviewViewCreated: (PreviewView) -> Unit,
    onToggleRunning: () -> Unit,
    onToggleDemoMode: (Boolean) -> Unit,
    onRequestPermission: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // ---- Camera preview (full screen background) ----
        if (uiState.hasCameraPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    PreviewView(context).also { onPreviewViewCreated(it) }
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Camera permission is required for NeuroSight to work.",
                        color = Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Grant camera permission")
                    }
                }
            }
        }

        // ---- Top overlay: current class + confidence ----
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Class: ${uiState.currentClass}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Confidence: ${"%.2f".format(uiState.confidence)}",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium
            )
            if (uiState.demoModeEnabled) {
                Text(
                    text = "Backend: ${uiState.activeBackendName}",
                    color = Color(0xFF38BDF8),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = if (uiState.isRunning) "Pipeline: RUNNING" else "Pipeline: STOPPED",
                    color = Color(0xFF22D3EE),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // ---- Bottom controls: start/stop + demo mode toggle ----
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                .padding(PaddingValues(horizontal = 20.dp, vertical = 14.dp)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Demo Mode", color = Color.White)
                Switch(
                    checked = uiState.demoModeEnabled,
                    onCheckedChange = onToggleDemoMode,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Button(
                onClick = onToggleRunning,
                enabled = uiState.hasCameraPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isRunning) Color(0xFFEF4444) else Color(0xFF22C55E)
                )
            ) {
                Text(if (uiState.isRunning) "Stop" else "Start")
            }
        }
    }
}
