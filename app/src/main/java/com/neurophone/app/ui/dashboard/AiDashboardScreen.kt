package com.neurophone.app.ui.dashboard

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurophone.app.NeuroUiState
import com.neurophone.app.NeuroViewModel
import com.neurophone.app.domain.model.ContextState
import com.neurophone.app.ui.theme.*

@Composable
fun AiDashboardScreen(uiState: NeuroUiState, viewModel: NeuroViewModel, onBack: () -> Unit) {
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var predictedForFeedback by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(NeuroBackground)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeuroOnBackground)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("AI DASHBOARD", color = NeuroPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Real-time prediction, context, and learning signals", color = NeuroSubtext, fontSize = 12.sp)
            }
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            // Prediction Card
            val intent = uiState.predictedIntent
            if (intent != null) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = NeuroCard)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("CURRENT PREDICTION", color = NeuroSubtext, fontSize = 11.sp, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(intent.action, color = NeuroOnBackground, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ConfidenceBadge(intent.confidence)
                            Text("${intent.latencyMs}ms inference", color = NeuroSubtext, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterVertically))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("MODEL: ${intent.modelType}", color = NeuroSubtext, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = NeuroSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("SIGNALS CONTRIBUTING TO THIS PREDICTION", color = NeuroSubtext, fontSize = 11.sp, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        intent.contributingSignals.forEach { signal ->
                            Text(signal, color = NeuroOnSurface, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    predictedForFeedback = intent.action
                                    showFeedbackDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, NeuroError),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Wrong — Correct AI", color = NeuroError, fontSize = 12.sp) }
                            Button(
                                onClick = { viewModel.recordFeedback(intent.action, intent.action) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = NeuroAccent),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Correct ✓", fontSize = 12.sp) }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Context Selector
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = NeuroCard)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CURRENT CONTEXT", color = NeuroSubtext, fontSize = 11.sp, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ContextState.values().take(4).forEach { ctx ->
                            FilterChip(
                                selected = ctx == uiState.currentContext,
                                onClick = { viewModel.setContext(ctx) },
                                label = { Text(ctx.label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeuroPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = NeuroPrimary,
                                    containerColor = NeuroSurfaceVariant,
                                    labelColor = NeuroOnSurface
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ContextState.values().drop(4).forEach { ctx ->
                            FilterChip(
                                selected = ctx == uiState.currentContext,
                                onClick = { viewModel.setContext(ctx) },
                                label = { Text(ctx.label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeuroPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = NeuroPrimary,
                                    containerColor = NeuroSurfaceVariant,
                                    labelColor = NeuroOnSurface
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("⚠ Manual override active — AI auto-detection pending device deployment", color = NeuroWarning, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Anomaly Detection Status
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = NeuroCard)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ANOMALY DETECTION", color = NeuroSubtext, fontSize = 11.sp, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val anomalyColor = when {
                            uiState.isAnomalyDetected -> NeuroError
                            uiState.anomalyScore > 0.3f -> NeuroWarning
                            else -> NeuroAccent
                        }
                        Box(modifier = Modifier.size(12.dp).background(anomalyColor, shape = androidx.compose.foundation.shape.CircleShape))
                        Column {
                            Text(
                                text = if (uiState.isAnomalyDetected) "Unusual pattern detected" else "Interaction patterns normal",
                                color = anomalyColor, fontSize = 14.sp, fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Autoencoder reconstruction error: ${"%.4f".format(uiState.anomalyScore)}",
                                color = NeuroSubtext, fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Reconstruction error bar
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(NeuroSurfaceVariant, androidx.compose.foundation.shape.RoundedCornerShape(2.dp))) {
                        val barColor = when {
                            uiState.isAnomalyDetected -> NeuroError
                            uiState.anomalyScore > 0.3f -> NeuroWarning
                            else -> NeuroAccent
                        }
                        Box(modifier = Modifier.fillMaxWidth(uiState.anomalyScore.coerceIn(0f, 1f)).height(4.dp)
                            .background(barColor, androidx.compose.foundation.shape.RoundedCornerShape(2.dp)))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Trained Autoencoder compresses 16-dim interaction vector to 4-dim bottleneck and reconstructs. High reconstruction error = anomalous behavior.",
                        color = NeuroSubtext, fontSize = 11.sp, lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recent Learning Events
            if (uiState.recentLearningEvents.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = NeuroCard)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("WHAT NEUROPHONE LEARNED", color = NeuroSubtext, fontSize = 11.sp, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        uiState.recentLearningEvents.take(6).forEach { event ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(modifier = Modifier.size(6.dp).offset(y = 5.dp).background(NeuroPrimary, shape = androidx.compose.foundation.shape.CircleShape))
                                Text(event, color = NeuroOnSurface, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }


    // Correction Dialog
    if (showFeedbackDialog) {
        var selectedActual by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            title = { Text("Correct the AI", color = NeuroOnBackground) },
            text = {
                Column {
                    Text("AI predicted: $predictedForFeedback", color = NeuroSubtext, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("What did you actually do?", color = NeuroOnBackground, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf("Open Notes", "Open Music", "Study Mode", "Open Camera", "Calculator", "Return Home").forEach { action ->
                        TextButton(onClick = { selectedActual = action }) {
                            Text(action, color = if (selectedActual == action) NeuroPrimary else NeuroOnSurface)
                        }
                    }
                }
            },
            confirmButton = {
                if (selectedActual.isNotEmpty()) {
                    Button(
                        onClick = {
                            viewModel.recordFeedback(predictedForFeedback, selectedActual)
                            showFeedbackDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeuroPrimary)
                    ) { Text("Teach NeuroPhone") }
                }
            },
            dismissButton = { TextButton(onClick = { showFeedbackDialog = false }) { Text("Cancel") } },
            containerColor = NeuroCard
        )
    }
}

@Composable
fun ConfidenceBadge(confidence: Float) {
    val color = when {
        confidence >= 0.85f -> NeuroAccent
        confidence >= 0.65f -> NeuroWarning
        else -> NeuroError
    }
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.15f)) {
        Text("${(confidence * 100).toInt()}% confidence", color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
    }
}