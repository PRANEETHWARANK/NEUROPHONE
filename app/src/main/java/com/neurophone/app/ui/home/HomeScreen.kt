package com.neurophone.app.ui.home

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurophone.app.NeuroUiState
import com.neurophone.app.domain.model.ContextState
import com.neurophone.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    uiState: NeuroUiState,
    onAppClick: (String) -> Unit = {},
    onNavigateToDashboard: () -> Unit,
    onNavigateToTeach: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onNavigateToLab: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val currentTime = remember { mutableStateOf("") }
    val currentDate = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFmt = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault())
        currentTime.value = timeFmt.format(Date())
        currentDate.value = dateFmt.format(Date())
    }

    // Show toast feedback when an app is launched or action performed
    LaunchedEffect(uiState.currentToastMessage) {
        uiState.currentToastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NeuroBackground, Color(0xFF0D0D1A), NeuroBackground)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Bar Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentTime.value.ifEmpty { "10:32 AM" },
                    color = NeuroOnBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = uiState.sensorTelemetry.motionState,
                        color = NeuroSubtext,
                        fontSize = 11.sp
                    )
                    Icon(Icons.Default.Wifi, contentDescription = "WiFi", tint = NeuroOnSurface, modifier = Modifier.size(16.dp))
                    Icon(Icons.Default.BatteryFull, contentDescription = "Battery", tint = NeuroAccent, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Time & Date Display
            Text(
                text = currentTime.value.ifEmpty { "10:32 AM" },
                color = NeuroOnBackground,
                fontSize = 54.sp,
                fontWeight = FontWeight.Thin,
                letterSpacing = (-2).sp
            )
            Text(
                text = currentDate.value.ifEmpty { "Saturday, 13 September" },
                color = NeuroSubtext,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Good ${getGreeting()}",
                color = NeuroOnBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Light
            )

            Spacer(modifier = Modifier.height(24.dp))

            // AI Intent Card
            AiCard(uiState = uiState, onTap = onNavigateToDashboard, onExecuteAction = onAppClick)

            Spacer(modifier = Modifier.height(24.dp))

            // Context-adaptive App Grid
            AppDock(
                context = uiState.currentContext,
                onAppClick = onAppClick
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Learning Status Card
            LearningStatusCard(
                uiState = uiState,
                onTeach = onNavigateToTeach,
                onMemory = onNavigateToMemory
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation Row
            NavigationRow(
                onDashboard = onNavigateToDashboard,
                onTeach = onNavigateToTeach,
                onMemory = onNavigateToMemory,
                onLab = onNavigateToLab,
                onPrivacy = onNavigateToPrivacy
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun AiCard(uiState: NeuroUiState, onTap: () -> Unit, onExecuteAction: (String) -> Unit = {}) {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NeuroCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🧠", fontSize = 22.sp)
                    Column {
                        Text("NEUROPHONE AI", color = NeuroPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Text("Context: ${uiState.currentContext.label}", color = NeuroSubtext, fontSize = 12.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(NeuroAccent)
                        .alpha(alpha)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Predicted Next Action", color = NeuroSubtext, fontSize = 11.sp, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(4.dp))

            val intent = uiState.predictedIntent
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = intent?.action ?: "Analyzing...",
                    color = NeuroOnBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (intent != null) {
                    Button(
                        onClick = { onExecuteAction(intent.action) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeuroPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Open", fontSize = 12.sp)
                    }
                }
            }

            if (intent != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ConfidencePill(confidence = intent.confidence)
                    Text(
                        text = "${intent.latencyMs}ms · ${intent.modelType}",
                        color = NeuroSubtext, fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("Signals contributing to this prediction:", color = NeuroSubtext, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                intent.contributingSignals.take(3).forEach { signal ->
                    Text(text = signal, color = NeuroOnSurface, fontSize = 12.sp, modifier = Modifier.padding(vertical = 1.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "● Learning Active · ${uiState.totalLearnedPatterns} patterns learned",
                color = NeuroAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ConfidencePill(confidence: Float) {
    val color = when {
        confidence >= 0.85f -> NeuroAccent
        confidence >= 0.65f -> NeuroWarning
        else -> NeuroError
    }
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.18f)) {
        Text(
            text = "${(confidence * 100).toInt()}% confidence",
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun AppDock(context: ContextState, onAppClick: (String) -> Unit = {}) {
    val apps = when (context) {
        ContextState.STUDY -> listOf(
            "📚" to "Notes", "🌐" to "Browser", "🧮" to "Calculator",
            "⏱" to "Timer", "📄" to "PDF", "📅" to "Calendar"
        )
        ContextState.ENTERTAINMENT -> listOf(
            "🎵" to "Music", "📺" to "Video", "🎮" to "Games",
            "🖼" to "Gallery", "📻" to "Radio", "🎬" to "Movies"
        )
        ContextState.TRAVEL, ContextState.WALKING -> listOf(
            "🗺" to "Maps", "📞" to "Phone", "🎵" to "Music",
            "🧭" to "Navigate", "☁" to "Weather", "🏨" to "Hotels"
        )
        else -> listOf(
            "📞" to "Phone", "📷" to "Camera", "📧" to "Mail",
            "🌐" to "Browser", "🎵" to "Music", "📚" to "Notes"
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Adaptive App Dock · ${context.label} Mode", color = NeuroSubtext, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(apps) { (icon, label) ->
                AppIcon(icon = icon, label = label, onClick = { onAppClick(label) })
            }
        }
    }
}

@Composable
fun AppIcon(icon: String, label: String, onClick: () -> Unit = {}) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(62.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(NeuroSurfaceVariant)
                .clickable { onClick() }
        ) {
            Text(text = icon, fontSize = 28.sp, textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = NeuroOnSurface, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
fun LearningStatusCard(uiState: NeuroUiState, onTeach: () -> Unit, onMemory: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NeuroSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("● NEUROPHONE LEARNING PROFILE", color = NeuroAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                StatItem(value = "${uiState.learnedGestures.size}", label = "Gestures")
                StatItem(value = "${uiState.routines.count { it.isAccepted }}", label = "Shortcuts")
                StatItem(value = "${uiState.feedbackHistory.size}", label = "Corrections")
                StatItem(value = "${uiState.totalLearnedPatterns}", label = "Patterns")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onTeach,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = NeuroPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Teach Gesture", fontSize = 12.sp) }
                OutlinedButton(
                    onClick = onMemory,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NeuroPrimary)
                ) { Text("Neural Memory", color = NeuroPrimary, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = NeuroPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = NeuroSubtext, fontSize = 11.sp)
    }
}

@Composable
fun NavigationRow(
    onDashboard: () -> Unit,
    onTeach: () -> Unit,
    onMemory: () -> Unit,
    onLab: () -> Unit,
    onPrivacy: () -> Unit
) {
    val navItems = listOf(
        Triple(Icons.Default.Dashboard, "AI", onDashboard),
        Triple(Icons.Default.TouchApp, "Teach", onTeach),
        Triple(Icons.Default.Memory, "Memory", onMemory),
        Triple(Icons.Default.Science, "Lab", onLab),
        Triple(Icons.Default.Security, "Privacy", onPrivacy)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NeuroCard)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        navItems.forEach { (icon, label, action) ->
            NavItem(icon = icon, label = label, onClick = action)
        }
    }
}

@Composable
fun NavItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = NeuroOnSurface, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, color = NeuroSubtext, fontSize = 10.sp)
    }
}

fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Morning"
        hour < 17 -> "Afternoon"
        else -> "Evening"
    }
}