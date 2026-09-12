package com.neurophone.app.ui.memory

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurophone.app.NeuroUiState
import com.neurophone.app.NeuroViewModel
import com.neurophone.app.domain.model.LearnedGesture
import com.neurophone.app.domain.model.RoutinePattern
import com.neurophone.app.domain.model.UserFeedback
import com.neurophone.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NeuralMemoryScreen(uiState: NeuroUiState, viewModel: NeuroViewModel, onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Gestures", "Routines", "Corrections", "Timeline")

    Column(modifier = Modifier.fillMaxSize().background(NeuroBackground)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeuroOnBackground)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("NEURAL MEMORY", color = NeuroPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("What NeuroPhone has learned about you", color = NeuroSubtext, fontSize = 12.sp)
            }
        }

        // Summary Stats Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            MemoryStat("${uiState.learnedGestures.size}", "Gestures", NeuroPrimary)
            MemoryStat("${uiState.routines.count { it.isAccepted }}", "Shortcuts", NeuroAccent)
            MemoryStat("${uiState.feedbackHistory.size}", "Learnings", NeuroWarning)
            MemoryStat(
                "${uiState.feedbackHistory.count { it.wasCorrect } * 100 / uiState.feedbackHistory.size.coerceAtLeast(1)}%",
                "Accuracy", NeuroSecondary
            )
        }

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = NeuroSurface,
            contentColor = NeuroPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = NeuroPrimary,
                    height = 2.dp
                )
            }
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = idx == selectedTab,
                    onClick = { selectedTab = idx },
                    text = {
                        Text(title, fontSize = 12.sp, fontWeight = if (idx == selectedTab) FontWeight.Bold else FontWeight.Normal,
                            color = if (idx == selectedTab) NeuroPrimary else NeuroSubtext)
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> GesturesList(uiState.learnedGestures, viewModel)
            1 -> RoutinesList(uiState.routines, viewModel)
            2 -> CorrectionsList(uiState.feedbackHistory)
            3 -> LearningTimeline(uiState.recentLearningEvents)
        }
    }
}

@Composable
fun MemoryStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(label, color = NeuroSubtext, fontSize = 11.sp)
    }
}

@Composable
fun GesturesList(gestures: List<LearnedGesture>, viewModel: NeuroViewModel) {
    if (gestures.isEmpty()) {
        EmptyState("No gestures learned yet.\nTeach a gesture to get started!")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(gestures, key = { it.id }) { gesture ->
            GestureMemoryCard(gesture = gesture, onDelete = { viewModel.deleteGesture(gesture.id) })
        }
    }
}

@Composable
fun GestureMemoryCard(gesture: LearnedGesture, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NeuroCard)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("✋", fontSize = 18.sp)
                    Text(gesture.name, color = NeuroOnBackground, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                Text("→  ${gesture.boundAction}", color = NeuroPrimary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MemoryChip("${(gesture.confidence * 100).toInt()}%", NeuroAccent)
                    MemoryChip("${gesture.usageCount} uses", NeuroSubtext)
                    MemoryChip("${gesture.sampleCount} samples", NeuroSubtext)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NeuroError, modifier = Modifier.size(18.dp))
                }
            }
        }
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Gesture?") },
                text = { Text("Remove '${gesture.name}' from Neural Memory?") },
                confirmButton = { TextButton(onClick = { onDelete(); showDeleteConfirm = false }) { Text("Delete", color = NeuroError) } },
                dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
                containerColor = NeuroCard
            )
        }
    }
}

@Composable
fun MemoryChip(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.12f)) {
        Text(text, color = color, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
fun RoutinesList(routines: List<RoutinePattern>, viewModel: NeuroViewModel) {
    if (routines.isEmpty()) {
        EmptyState("No routines detected yet.\nUse your phone regularly to discover patterns!")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(routines, key = { it.id }) { routine ->
            RoutineCard(routine = routine, onAccept = { viewModel.acceptRoutine(routine.id) })
        }
    }
}

@Composable
fun RoutineCard(routine: RoutinePattern, onAccept: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = NeuroCard)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Detected Workflow", color = NeuroSubtext, fontSize = 11.sp, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(routine.sequenceDescription, color = NeuroOnBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MemoryChip("${routine.occurrenceCount}× detected", NeuroWarning)
                MemoryChip("${(routine.confidence * 100).toInt()}% confidence", NeuroAccent)
            }
            if (!routine.isAccepted) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onAccept, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NeuroPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Create Shortcut", fontSize = 12.sp) }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text("✓ Shortcut: ${routine.suggestedShortcut}", color = NeuroAccent, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun CorrectionsList(feedback: List<UserFeedback>) {
    if (feedback.isEmpty()) {
        EmptyState("No corrections recorded yet.\nNeuroPhone learns when it makes mistakes!")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(feedback.take(20)) { fb ->
            FeedbackCard(fb)
        }
    }
}

@Composable
fun FeedbackCard(feedback: UserFeedback) {
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    val time = fmt.format(Date(feedback.timestamp))
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = NeuroCard)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(time, color = NeuroSubtext, fontSize = 11.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("AI: ${feedback.predictedAction}", color = if (feedback.wasCorrect) NeuroAccent else NeuroSubtext.copy(alpha = 0.7f), fontSize = 13.sp)
                    if (!feedback.wasCorrect) {
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = NeuroError, modifier = Modifier.size(14.dp))
                        Text("Actual: ${feedback.actualAction}", color = NeuroOnBackground, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Icon(
                if (feedback.wasCorrect) Icons.Default.CheckCircle else Icons.Default.School,
                contentDescription = null,
                tint = if (feedback.wasCorrect) NeuroAccent else NeuroPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun LearningTimeline(events: List<String>) {
    if (events.isEmpty()) {
        EmptyState("Learning timeline is empty.\nStart interacting with NeuroPhone to see events!")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(events.take(12)) { event ->
            TimelineEvent(event)
        }
    }
}

@Composable
fun TimelineEvent(event: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.padding(top = 4.dp)) {
            Box(modifier = Modifier.size(8.dp).background(NeuroPrimary, shape = androidx.compose.foundation.shape.CircleShape))
        }
        Text(event, color = NeuroOnSurface, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = NeuroSubtext, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(32.dp))
    }
}
