package com.neurophone.app.domain.model

data class HandLandmark(
    val id: Int,
    val x: Float,
    val y: Float,
    val z: Float,
    val visibility: Float = 1.0f
)

data class HandFrame(
    val timestampMs: Long,
    val landmarks: List<HandLandmark>
) {
    fun toFeatureArray(): FloatArray {
        val features = FloatArray(63) // 21 landmarks * 3 (x,y,z)
        landmarks.take(21).forEachIndexed { idx, lm ->
            features[idx * 3] = lm.x
            features[idx * 3 + 1] = lm.y
            features[idx * 3 + 2] = lm.z
        }
        return features
    }
}

data class GestureSequence(
    val frames: List<HandFrame> = emptyList()
) {
    companion object {
        const val SEQUENCE_LENGTH = 30 // 30 frames normalized
        const val FEATURES_PER_FRAME = 63 // 21 * 3
    }

    /**
     * Normalizes sequence coordinates relative to wrist (landmark 0) and scales uniformly.
     * Fixed length resampling to exactly SEQUENCE_LENGTH frames.
     */
    fun normalize(): GestureSequence {
        if (frames.isEmpty()) return this

        val resampled = if (frames.size == SEQUENCE_LENGTH) {
            frames
        } else {
            // Linear interpolation / uniform resampling
            val result = mutableListOf<HandFrame>()
            val step = (frames.size - 1).toFloat() / (SEQUENCE_LENGTH - 1)
            for (i in 0 until SEQUENCE_LENGTH) {
                val srcIdx = (i * step).toInt().coerceIn(0, frames.size - 1)
                result.add(frames[srcIdx])
            }
            result
        }

        val normalizedFrames = resampled.map { frame ->
            if (frame.landmarks.isEmpty()) return@map frame
            val wrist = frame.landmarks[0]
            var maxDist = 0.001f
            frame.landmarks.forEach { lm ->
                val dist = kotlin.math.sqrt(
                    (lm.x - wrist.x) * (lm.x - wrist.x) +
                    (lm.y - wrist.y) * (lm.y - wrist.y) +
                    (lm.z - wrist.z) * (lm.z - wrist.z)
                )
                if (dist > maxDist) maxDist = dist
            }

            val centeredLandmarks = frame.landmarks.map { lm ->
                lm.copy(
                    x = (lm.x - wrist.x) / maxDist,
                    y = (lm.y - wrist.y) / maxDist,
                    z = (lm.z - wrist.z) / maxDist
                )
            }
            frame.copy(landmarks = centeredLandmarks)
        }

        return GestureSequence(normalizedFrames)
    }

    fun toFlattenedArray(): FloatArray {
        val norm = normalize()
        val arr = FloatArray(SEQUENCE_LENGTH * FEATURES_PER_FRAME)
        norm.frames.forEachIndexed { fIdx, frame ->
            val feat = frame.toFeatureArray()
            System.arraycopy(feat, 0, arr, fIdx * FEATURES_PER_FRAME, feat.size)
        }
        return arr
    }
}

enum class ContextState(val label: String, val description: String) {
    STUDY("STUDY", "Focused study or reading session"),
    ENTERTAINMENT("ENTERTAINMENT", "Media, music, and streaming"),
    TRAVEL("TRAVEL", "Transit, transit navigation, maps"),
    WALKING("WALKING", "Active walking and motion"),
    GAMING("GAMING", "Immersive landscape gaming"),
    MEDIA("MEDIA", "Video viewing / photography"),
    GENERAL("GENERAL", "Daily casual smartphone interaction")
}

data class LearnedGesture(
    val id: String,
    val name: String,
    val boundAction: String,
    val sampleCount: Int,
    val confidence: Float,
    val usageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class PredictedIntent(
    val action: String,
    val confidence: Float,
    val context: ContextState,
    val modelType: String,
    val contributingSignals: List<String>,
    val latencyMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

data class InteractionEvent(
    val id: Long = 0,
    val actionType: String,
    val targetApp: String,
    val touchX: Float,
    val touchY: Float,
    val tapDurationMs: Long,
    val swipeVelocity: Float,
    val context: ContextState,
    val timestamp: Long = System.currentTimeMillis()
)

data class RoutinePattern(
    val id: String,
    val sequenceDescription: String,
    val suggestedShortcut: String,
    val occurrenceCount: Int,
    val confidence: Float,
    val isAccepted: Boolean = false
)

data class UserFeedback(
    val id: Long = 0,
    val predictedAction: String,
    val actualAction: String,
    val wasCorrect: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)