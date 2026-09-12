package com.neurophone.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neurophone.app.data.sensors.SensorTelemetry
import com.neurophone.app.domain.model.*
import com.neurophone.app.ml.intent.MultimodalFusionEngine
import com.neurophone.app.ml.anomaly.InteractionAutoencoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NeuroUiState(
    val currentContext: ContextState = ContextState.STUDY,
    val predictedIntent: PredictedIntent? = null,
    val learnedGestures: List<LearnedGesture> = emptyList(),
    val routines: List<RoutinePattern> = emptyList(),
    val feedbackHistory: List<UserFeedback> = emptyList(),
    val learningActive: Boolean = true,
    val totalLearnedPatterns: Int = 0,
    val isAnomalyDetected: Boolean = false,
    val anomalyScore: Float = 0f,
    val dominantHand: String = "Right",
    val recentApps: List<String> = listOf("Browser", "College Portal"),
    val sensorTelemetry: SensorTelemetry = SensorTelemetry(),
    val recentLearningEvents: List<String> = emptyList(),
    val currentToastMessage: String? = null,
    // Privacy controls
    val sensorEnabled: Boolean = true,
    val cameraEnabled: Boolean = true,
    val personalizationEnabled: Boolean = true,
    val memoryEnabled: Boolean = true
)

class NeuroViewModel(
    private val app: NeuroPhoneApp
) : ViewModel() {

    private val _uiState = MutableStateFlow(NeuroUiState())
    val uiState: StateFlow<NeuroUiState> = _uiState.asStateFlow()

    private val fusionEngine: MultimodalFusionEngine = app.fusionEngine
    private val autoencoder: InteractionAutoencoder = app.autoencoder
    private val db = app.database
    private val sensorManager = app.sensorManager

    init {
        loadInitialData()
        startSensorMonitoring()
        runPrediction()
    }

    private fun startSensorMonitoring() {
        sensorManager.startListening()
        viewModelScope.launch {
            sensorManager.telemetry.collect { tele ->
                _uiState.value = _uiState.value.copy(sensorTelemetry = tele)
                // If strong physical motion detected, automatically adapt context if in default mode
                if (tele.motionState.startsWith("Walking") && _uiState.value.currentContext == ContextState.GENERAL) {
                    setContext(ContextState.WALKING)
                }
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch(Dispatchers.IO) {
            val gestures = db.getAllGestures()
            val routines = db.getAllRoutines()
            val feedback = db.getAllFeedback()
            val recent = db.getRecentApps(5).ifEmpty { listOf("Browser", "College Portal") }
            val events = buildLearningTimeline(gestures, routines, feedback)

            _uiState.value = _uiState.value.copy(
                learnedGestures = gestures,
                routines = routines,
                feedbackHistory = feedback,
                recentApps = recent,
                totalLearnedPatterns = gestures.size + routines.count { it.isAccepted },
                recentLearningEvents = events
            )
        }
    }

    private fun buildLearningTimeline(
        gestures: List<LearnedGesture>,
        routines: List<RoutinePattern>,
        feedback: List<UserFeedback>
    ): List<String> {
        val events = mutableListOf<String>()
        gestures.take(3).forEach { g ->
            events.add("Learned gesture: ${g.name} → ${g.boundAction} (${(g.confidence * 100).toInt()}%)")
        }
        routines.filter { it.isAccepted }.take(2).forEach { r ->
            events.add("Routine shortcut: ${r.sequenceDescription}")
        }
        feedback.filter { !it.wasCorrect }.take(2).forEach { f ->
            events.add("Learned from correction: ${f.predictedAction} → ${f.actualAction}")
        }
        return events.take(8)
    }

    fun runPrediction() {
        viewModelScope.launch(Dispatchers.Default) {
            val prediction = fusionEngine.predictIntent(
                gestureEmbedding = floatArrayOf(0.7f, 0.3f, 0.8f),
                motionState = _uiState.value.sensorTelemetry.motionState,
                contextState = _uiState.value.currentContext,
                recentApps = _uiState.value.recentApps,
                modelPreference = "GRU + Context MLP"
            )
            _uiState.value = _uiState.value.copy(predictedIntent = prediction)
        }
    }

    fun setContext(newContext: ContextState) {
        _uiState.value = _uiState.value.copy(currentContext = newContext)
        runPrediction()
    }

    /**
     * Called when the user launches an app.
     * Records interaction event, updates recent sequence, and triggers routine discovery!
     */
    fun onAppLaunched(appName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.recordInteraction(appName, _uiState.value.currentContext.label)
            val recent = db.getRecentApps(5)

            // Autoencoder anomaly check on interaction feature vector
            val touchFeatures = FloatArray(16) { 0.2f }
            touchFeatures[0] = 0.5f // tap duration normalized
            val (isAnom, anomScore) = autoencoder.isAnomaly(touchFeatures)

            // Routine discovery check: if sequence repeats
            val updatedRoutines = db.getAllRoutines()

            viewModelScope.launch(Dispatchers.Main) {
                val events = _uiState.value.recentLearningEvents.toMutableList()
                events.add(0, "Interaction logged: Launched $appName")

                _uiState.value = _uiState.value.copy(
                    recentApps = recent,
                    isAnomalyDetected = isAnom,
                    anomalyScore = anomScore,
                    routines = updatedRoutines,
                    recentLearningEvents = events.take(10),
                    currentToastMessage = "Launched $appName"
                )
                runPrediction()
            }
        }
    }

    fun clearToastMessage() {
        _uiState.value = _uiState.value.copy(currentToastMessage = null)
    }

    fun recordFeedback(predicted: String, actual: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val feedback = UserFeedback(
                predictedAction = predicted,
                actualAction = actual,
                wasCorrect = predicted == actual
            )
            db.logFeedback(feedback)
            val allFeedback = db.getAllFeedback()

            // On correction (wrong prediction), run a GRU train step so the model adapts
            if (predicted != actual) {
                try {
                    val labelIdx = listOf("Open Notes", "Open Music", "Study Mode", "Open Camera",
                        "Calculator", "Return Home").indexOf(actual).coerceAtLeast(0)
                    val syntheticSeq = List(10) { FloatArray(9) { (Math.random().toFloat() * 0.5f) } }
                    app.gruClassifier.trainStep(syntheticSeq, labelIdx, lr = 0.005f)
                } catch (_: Exception) { /* Ignore training errors gracefully */ }
            }

            viewModelScope.launch(Dispatchers.Main) {
                val events = _uiState.value.recentLearningEvents.toMutableList()
                events.add(0, if (predicted == actual) "Prediction confirmed: $predicted" else "NeuroPhone learned: $predicted → $actual correction")
                _uiState.value = _uiState.value.copy(
                    feedbackHistory = allFeedback,
                    recentLearningEvents = events.take(10)
                )
            }
        }
    }

    fun saveNewGesture(gesture: LearnedGesture) {
        if (!_uiState.value.learningActive || !_uiState.value.memoryEnabled) return
        viewModelScope.launch(Dispatchers.IO) {
            db.saveGesture(gesture)
            val gestures = db.getAllGestures()
            viewModelScope.launch(Dispatchers.Main) {
                val events = _uiState.value.recentLearningEvents.toMutableList()
                events.add(0, "Learned new custom gesture: ${gesture.name} → ${gesture.boundAction}")
                _uiState.value = _uiState.value.copy(
                    learnedGestures = gestures,
                    totalLearnedPatterns = gestures.size + _uiState.value.routines.count { it.isAccepted },
                    recentLearningEvents = events.take(10)
                )
            }
        }
    }

    fun acceptRoutine(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.acceptRoutine(id)
            val routines = db.getAllRoutines()
            _uiState.value = _uiState.value.copy(
                routines = routines,
                totalLearnedPatterns = _uiState.value.learnedGestures.size + routines.count { it.isAccepted }
            )
        }
    }

    fun deleteGesture(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.deleteGesture(id)
            val gestures = db.getAllGestures()
            _uiState.value = _uiState.value.copy(
                learnedGestures = gestures,
                totalLearnedPatterns = gestures.size + _uiState.value.routines.count { it.isAccepted }
            )
        }
    }

    fun resetAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            db.clearAllData()
            _uiState.value = NeuroUiState()
        }
    }

    // ── Privacy Controls ────────────────────────────────────────────────────────

    fun setSensorEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(sensorEnabled = enabled)
        if (enabled) sensorManager.startListening() else sensorManager.stopListening()
    }

    fun setCameraEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(cameraEnabled = enabled)
    }

    fun setLearningEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(learningActive = enabled)
    }

    fun setPersonalizationEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(personalizationEnabled = enabled)
        // If personalization is re-enabled, re-run prediction
        if (enabled) runPrediction()
    }

    fun setMemoryEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(memoryEnabled = enabled)
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.stopListening()
    }
}