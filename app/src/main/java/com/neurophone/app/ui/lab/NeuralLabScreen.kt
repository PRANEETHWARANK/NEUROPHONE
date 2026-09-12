package com.neurophone.app.ui.lab

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurophone.app.ml.lab.BenchmarkResult
import com.neurophone.app.ml.lab.ModelBenchmarkRunner
import com.neurophone.app.ml.lab.OptimizerExperimentResult
import com.neurophone.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NeuralLabScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Model Compare", "Optimizers", "Activations", "Regularization")

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
                Text("NEURAL NETWORK LAB", color = NeuroPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Real model training experiments and metric analysis", color = NeuroSubtext, fontSize = 12.sp)
            }
        }

        TabRow(selectedTabIndex = selectedTab, containerColor = NeuroSurface, contentColor = NeuroPrimary) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = idx == selectedTab,
                    onClick = { selectedTab = idx },
                    text = { Text(title, fontSize = 11.sp, color = if (idx == selectedTab) NeuroPrimary else NeuroSubtext) }
                )
            }
        }

        when (selectedTab) {
            0 -> ModelComparisonTab()
            1 -> OptimizerTab()
            2 -> ActivationTab()
            3 -> RegularizationTab()
        }
    }
}

@Composable
fun ModelComparisonTab() {
    var results by remember { mutableStateOf<List<BenchmarkResult>>(emptyList()) }
    var isRunning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NeuroSurfaceVariant), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("BENCHMARK RUNNER", color = NeuroPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Trains and evaluates MLP, LSTM, GRU, and Transformer on synthetic gesture sequences.\nAll metrics are computed from real inference and evaluation passes.",
                    color = NeuroOnSurface, fontSize = 12.sp, lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (!isRunning) {
                    isRunning = true
                    scope.launch {
                        val r = withContext(Dispatchers.Default) { ModelBenchmarkRunner.runModelComparison() }
                        results = r
                        isRunning = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeuroPrimary),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (isRunning) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Running Experiments...")
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("RUN MODEL COMPARISON", fontWeight = FontWeight.Bold)
            }
        }

        if (results.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text("RESULTS — REAL MEASURED METRICS", color = NeuroSubtext, fontSize = 11.sp, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))

            // Accuracy Bar Chart
            results.forEach { result ->
                ModelResultCard(result)
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
            // Comparison Table
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NeuroCard), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("METRICS TABLE", color = NeuroSubtext, fontSize = 11.sp, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TableHeader("Model", 0.35f)
                        TableHeader("Acc", 0.15f)
                        TableHeader("F1", 0.15f)
                        TableHeader("Loss", 0.15f)
                        TableHeader("ms", 0.10f)
                        TableHeader("Params", 0.10f)
                    }
                    Divider(color = NeuroSubtext.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
                    results.forEach { r ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            TableCell(r.modelName.substringBefore(" "), 0.35f, NeuroPrimary)
                            TableCell("${(r.accuracy * 100).toInt()}%", 0.15f)
                            TableCell(String.format("%.2f", r.f1Score), 0.15f)
                            TableCell(String.format("%.3f", r.avgLoss), 0.15f)
                            TableCell(String.format("%.1f", r.inferenceLatencyMs), 0.10f)
                            TableCell("${r.parameterCount / 1000}K", 0.10f)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModelResultCard(result: BenchmarkResult) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NeuroCard), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(result.modelName, color = NeuroOnBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("${(result.accuracy * 100).toInt()}%", color = NeuroAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Accuracy bar
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(NeuroSurfaceVariant)) {
                Box(
                    modifier = Modifier.fillMaxWidth(result.accuracy).height(6.dp).clip(RoundedCornerShape(3.dp))
                        .background(if (result.accuracy > 0.8f) NeuroAccent else if (result.accuracy > 0.65f) NeuroWarning else NeuroError)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MiniMetric("F1", String.format("%.2f", result.f1Score))
                MiniMetric("Loss", String.format("%.4f", result.avgLoss))
                MiniMetric("Latency", "${String.format("%.1f", result.inferenceLatencyMs)}ms")
                MiniMetric("Params", "${result.parameterCount / 1000}K")
            }
            // Convergence History (mini chart)
            if (result.convergenceHistory.size >= 2) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Convergence History:", color = NeuroSubtext, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                MiniLossChart(losses = result.convergenceHistory)
            }
        }
    }
}

@Composable
fun MiniLossChart(losses: List<Float>) {
    val maxLoss = losses.maxOrNull() ?: 1f
    val minLoss = losses.minOrNull() ?: 0f
    val range = (maxLoss - minLoss).coerceAtLeast(0.01f)

    Row(modifier = Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        losses.forEach { loss ->
            val h = ((loss - minLoss) / range)
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(h.coerceIn(0.05f, 1f))
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(NeuroPrimary.copy(alpha = 0.7f))
            )
        }
    }
}

@Composable
fun MiniMetric(label: String, value: String) {
    Column {
        Text(label, color = NeuroSubtext, fontSize = 10.sp)
        Text(value, color = NeuroOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun RowScope.TableHeader(text: String, weight: Float) {
    Text(text, color = NeuroSubtext, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(weight))
}

@Composable
fun RowScope.TableCell(text: String, weight: Float, color: Color = NeuroOnSurface) {
    Text(text, color = color, fontSize = 12.sp, modifier = Modifier.weight(weight), maxLines = 1)
}

@Composable
fun OptimizerTab() {
    var results by remember { mutableStateOf<List<OptimizerExperimentResult>>(emptyList()) }
    var isRunning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val colors = listOf(NeuroPrimary, NeuroAccent, NeuroWarning)

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NeuroSurfaceVariant), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("OPTIMIZER EXPERIMENTS", color = NeuroPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("Compare Mini-Batch SGD, Momentum (β=0.9), and RMSProp adaptive rates on gesture loss convergence", color = NeuroOnSurface, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (!isRunning) {
                    isRunning = true
                    scope.launch {
                        val r = withContext(Dispatchers.Default) { ModelBenchmarkRunner.runOptimizerExperiments() }
                        results = r
                        isRunning = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = NeuroAccent),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isRunning) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            else Text("RUN OPTIMIZER EXPERIMENTS", fontWeight = FontWeight.Bold)
        }
        if (results.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text("CONVERGENCE CURVES", color = NeuroSubtext, fontSize = 11.sp, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(12.dp))
            results.forEachIndexed { idx, res ->
                OptimizerResultCard(res, colors[idx % colors.size])
                Spacer(modifier = Modifier.height(10.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(colors = CardDefaults.cardColors(containerColor = NeuroSurfaceVariant), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("KEY OBSERVATIONS", color = NeuroSubtext, fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• RMSProp adapts per-parameter learning rates — fastest convergence", color = NeuroOnSurface, fontSize = 12.sp)
                    Text("• Momentum accumulates gradient direction — smoother than plain SGD", color = NeuroOnSurface, fontSize = 12.sp)
                    Text("• Mini-Batch SGD is most memory-efficient but slowest to converge", color = NeuroOnSurface, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun OptimizerResultCard(result: OptimizerExperimentResult, color: Color) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NeuroCard), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(result.optimizerName, color = NeuroOnBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("Final Loss: ${String.format("%.4f", result.finalLoss)}", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("LR: ${result.learningRate} · ${result.lossHistory.size} epochs", color = NeuroSubtext, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(8.dp))
            MiniLossChart2(losses = result.lossHistory, color = color)
        }
    }
}

@Composable
fun MiniLossChart2(losses: List<Float>, color: Color) {
    val maxLoss = losses.maxOrNull() ?: 1f
    val minLoss = losses.minOrNull() ?: 0f
    val range = (maxLoss - minLoss).coerceAtLeast(0.001f)
    Row(modifier = Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        losses.forEach { loss ->
            val h = ((loss - minLoss) / range).coerceIn(0.05f, 1f)
            Box(modifier = Modifier.weight(1f).fillMaxHeight(h).clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)).background(color.copy(alpha = 0.7f)))
        }
    }
}

@Composable
fun ActivationTab() {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("ACTIVATION FUNCTIONS", color = NeuroPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(16.dp))
        val activations = listOf(
            ActivationInfo("ReLU", "f(x) = max(0, x)", "Used in MLP hidden layers. Solves vanishing gradient. May cause dead neurons for x < 0.", NeuroAccent),
            ActivationInfo("Tanh", "f(x) = (e^x - e^-x) / (e^x + e^-x)", "Used in LSTM/GRU gates. Output ∈ (-1, 1). Better for sequence models than sigmoid.", NeuroSecondary),
            ActivationInfo("Sigmoid", "f(x) = 1 / (1 + e^-x)", "Used in LSTM/GRU input, forget, output gates. Output ∈ (0, 1). Controls memory flow.", NeuroWarning),
            ActivationInfo("Softmax", "f(x_i) = e^x_i / Σ e^x_j", "Used at output layer for multi-class classification. Converts logits to probability distribution summing to 1.", NeuroPrimary)
        )
        activations.forEach { info ->
            ActivationCard(info)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

data class ActivationInfo(val name: String, val formula: String, val explanation: String, val color: Color)

@Composable
fun ActivationCard(info: ActivationInfo) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NeuroCard), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(info.color, shape = androidx.compose.foundation.shape.CircleShape))
                Text(info.name, color = info.color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(color = NeuroSurfaceVariant, shape = RoundedCornerShape(8.dp)) {
                Text(info.formula, color = NeuroOnSurface, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(info.explanation, color = NeuroOnSurface, fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
fun RegularizationTab() {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("REGULARIZATION TECHNIQUES", color = NeuroPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(16.dp))
        listOf(
            Triple("Dropout", "Randomly zeros p% of neurons during training.\nReduces co-adaptation. Effective rate: p=0.2–0.5.", NeuroAccent),
            Triple("L2 Regularization", "Adds λ‖W‖² to loss. Penalizes large weights.\nBias-variance tradeoff: λ controls regularization strength.", NeuroWarning),
            Triple("Early Stopping", "Stop training when validation loss stops improving.\nPrevents overfitting by preserving best checkpoint.", NeuroPrimary),
            Triple("Batch Normalization", "Normalizes layer inputs to zero mean, unit variance.\nImproves gradient flow and training stability.", NeuroSecondary)
        ).forEach { (name, desc, color) ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), colors = CardDefaults.cardColors(containerColor = NeuroCard), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(name, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(desc, color = NeuroOnSurface, fontSize = 13.sp, lineHeight = 20.sp)
                }
            }
        }
    }
}