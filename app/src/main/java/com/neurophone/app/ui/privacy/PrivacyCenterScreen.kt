package com.neurophone.app.ui.privacy

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurophone.app.NeuroUiState
import com.neurophone.app.NeuroViewModel
import com.neurophone.app.ui.theme.*

@Composable
fun PrivacyCenterScreen(uiState: NeuroUiState, viewModel: NeuroViewModel, onBack: () -> Unit) {
    var showResetDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(NeuroBackground)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeuroOnBackground)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("PRIVACY CENTER", color = NeuroPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Control what NeuroPhone learns and stores", color = NeuroSubtext, fontSize = 12.sp)
            }
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {

            // On-Device Notice
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2010))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = NeuroAccent, modifier = Modifier.size(20.dp))
                        Text("ON-DEVICE ONLY", color = NeuroAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "NeuroPhone stores all learned data exclusively on your device. " +
                        "No raw camera footage is stored — only derived hand landmark coordinates and feature vectors.",
                        color = NeuroOnSurface, fontSize = 12.sp, lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Live status banner
            val anyDisabled = !uiState.sensorEnabled || !uiState.learningActive || !uiState.cameraEnabled
            if (anyDisabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = NeuroWarning.copy(alpha = 0.15f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = NeuroWarning, modifier = Modifier.size(18.dp))
                        Text("Some NeuroPhone features are currently disabled.", color = NeuroWarning, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text("DATA COLLECTION CONTROLS", color = NeuroSubtext, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            // Camera Toggle — controls cameraEnabled state; Teach screen respects it
            PrivacyToggle(
                title = "Camera (Air Gesture Mode)",
                description = "Used for hand landmark detection only. Raw video is NOT recorded or stored.",
                icon = Icons.Default.Camera,
                enabled = uiState.cameraEnabled,
                onToggle = { viewModel.setCameraEnabled(it) }
            )

            // Sensor Toggle — actually starts/stops SensorFusionManager
            PrivacyToggle(
                title = "Sensor Data (IMU)",
                description = "Accelerometer and gyroscope for context detection. Disabling stops live motion tracking.",
                icon = Icons.Default.Sensors,
                enabled = uiState.sensorEnabled,
                onToggle = { viewModel.setSensorEnabled(it) }
            )

            // Learning Toggle — gates saveNewGesture and feedback recording
            PrivacyToggle(
                title = "Interaction Learning",
                description = "Records anonymized touch patterns to improve prediction. Disable to freeze the AI.",
                icon = Icons.Default.TouchApp,
                enabled = uiState.learningActive,
                onToggle = { viewModel.setLearningEnabled(it) }
            )

            // Personalization Toggle — gates runPrediction updates
            PrivacyToggle(
                title = "Personalization",
                description = "Adapts home screen and shortcuts based on learned usage patterns.",
                icon = Icons.Default.Person,
                enabled = uiState.personalizationEnabled,
                onToggle = { viewModel.setPersonalizationEnabled(it) }
            )

            // Memory Toggle — gates gesture/routine persistence
            PrivacyToggle(
                title = "Neural Memory",
                description = "Stores learned gestures, routines, and correction history on-device.",
                icon = Icons.Default.Memory,
                enabled = uiState.memoryEnabled,
                onToggle = { viewModel.setMemoryEnabled(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("WHAT IS NOT STORED", color = NeuroSubtext, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            listOf(
                "✗  Passwords or PINs",
                "✗  Private messages or emails",
                "✗  Camera footage or screenshots",
                "✗  Microphone recordings",
                "✗  Location history"
            ).forEach { item ->
                Text(item, color = NeuroError, fontSize = 13.sp, modifier = Modifier.padding(vertical = 3.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Live stats card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = NeuroCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ON-DEVICE DATA SUMMARY", color = NeuroSubtext, fontSize = 11.sp, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        DataStat("${uiState.learnedGestures.size}", "Gestures", NeuroPrimary)
                        DataStat("${uiState.routines.size}", "Routines", NeuroAccent)
                        DataStat("${uiState.feedbackHistory.size}", "Corrections", NeuroWarning)
                        DataStat(if (uiState.sensorEnabled) "ON" else "OFF", "Sensors", if (uiState.sensorEnabled) NeuroAccent else NeuroError)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("DATA MANAGEMENT", color = NeuroSubtext, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, NeuroError),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = NeuroError, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset AI Profile — Delete All Data", color = NeuroError)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset AI Profile?", color = NeuroOnBackground) },
            text = {
                Text(
                    "This will permanently delete all learned gestures, routines, corrections, " +
                    "and interaction history. This cannot be undone.",
                    color = NeuroOnSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.resetAllData(); showResetDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeuroError)
                ) { Text("Delete All Data") }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Cancel") } },
            containerColor = NeuroCard
        )
    }
}

@Composable
fun DataStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = NeuroSubtext, fontSize = 11.sp)
    }
}

@Composable
fun PrivacyToggle(
    title: String,
    description: String,
    icon: ImageVector,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NeuroCard)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon, contentDescription = null,
                tint = if (enabled) NeuroPrimary else NeuroSubtext,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = NeuroOnBackground, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(description, color = NeuroSubtext, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NeuroPrimary,
                    checkedTrackColor = NeuroPrimary.copy(alpha = 0.3f),
                    uncheckedThumbColor = NeuroSubtext,
                    uncheckedTrackColor = NeuroSurfaceVariant
                )
            )
        }
    }
}