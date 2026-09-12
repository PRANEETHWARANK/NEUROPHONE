package com.neurophone.app

import com.neurophone.app.domain.model.HandFrame
import com.neurophone.app.domain.model.HandLandmark
import com.neurophone.app.domain.model.GestureSequence
import com.neurophone.app.ml.anomaly.InteractionAutoencoder
import com.neurophone.app.ml.gesture.GruClassifier
import com.neurophone.app.ml.gesture.LstmClassifier
import com.neurophone.app.ml.gesture.MlpClassifier
import com.neurophone.app.ml.gesture.TransformerClassifier
import com.neurophone.app.ml.intent.MultimodalFusionEngine
import org.junit.Assert.*
import org.junit.Test

class NeuroPhoneUnitTests {

    @Test
    fun testGestureSequenceNormalization() {
        val landmarks = (0 until 21).map { id ->
            HandLandmark(id = id, x = id * 0.1f + 1.0f, y = id * 0.2f + 2.0f, z = 0.5f)
        }
        val frames = (0 until 15).map { t -> HandFrame(timestampMs = t * 33L, landmarks = landmarks) }
        val seq = GestureSequence(frames)

        val normalized = seq.normalize()
        // Must be resampled to exactly SEQUENCE_LENGTH (30)
        assertEquals(GestureSequence.SEQUENCE_LENGTH, normalized.frames.size)

        // Wrist (landmark 0) should be origin centered: x=0, y=0, z=0
        val wrist = normalized.frames[0].landmarks[0]
        assertEquals(0.0f, wrist.x, 0.001f)
        assertEquals(0.0f, wrist.y, 0.001f)
        assertEquals(0.0f, wrist.z, 0.001f)

        val flat = seq.toFlattenedArray()
        assertEquals(30 * 63, flat.size)
    }

    @Test
    fun testMlpForwardAndTraining() {
        val mlp = MlpClassifier(inputSize = 10, hiddenSize = 8, numClasses = 3, randomSeed = 101)
        val input = FloatArray(10) { 0.5f }

        val (probsBefore, _) = mlp.forward(input)
        assertEquals(3, probsBefore.size)
        assertEquals(1.0f, probsBefore.sum(), 0.001f)

        // Train 5 steps for class 1
        var loss = 0f
        for (i in 0 until 5) {
            loss = mlp.trainStep(input, targetClass = 1, learningRate = 0.1f)
        }
        val (probsAfter, _) = mlp.forward(input)
        assertTrue("Class 1 probability should increase after training", probsAfter[1] > probsBefore[1])
        assertTrue("Loss should be finite positive", loss > 0f)
    }

    @Test
    fun testLstmRecurrentForwardAndLoss() {
        val lstm = LstmClassifier(inputDim = 8, hiddenDim = 12, numClasses = 4, randomSeed = 202)
        val sequence = (0 until 10).map { FloatArray(8) { 0.2f } }

        val (probs, hidden) = lstm.forward(sequence)
        assertEquals(4, probs.size)
        assertEquals(12, hidden.size)
        assertEquals(1.0f, probs.sum(), 0.001f)

        val loss = lstm.trainStep(sequence, targetClass = 2, lr = 0.05f)
        assertTrue(loss > 0f)
    }

    @Test
    fun testGruForwardPass() {
        val gru = GruClassifier(inputDim = 8, hiddenDim = 12, numClasses = 4, randomSeed = 303)
        val sequence = (0 until 10).map { FloatArray(8) { 0.3f } }

        val (probs, hidden) = gru.forward(sequence)
        assertEquals(4, probs.size)
        assertEquals(12, hidden.size)
        assertEquals(1.0f, probs.sum(), 0.001f)
    }

    @Test
    fun testTransformerMultiHeadAttentionForward() {
        val transformer = TransformerClassifier(
            seqLength = 10,
            inputDim = 8,
            dModel = 16,
            numHeads = 2,
            numClasses = 3,
            randomSeed = 404
        )
        val sequence = (0 until 10).map { FloatArray(8) { 0.4f } }

        val (probs, pooled) = transformer.forward(sequence)
        assertEquals(3, probs.size)
        assertEquals(16, pooled.size)
        assertEquals(1.0f, probs.sum(), 0.001f)
    }

    @Test
    fun testAutoencoderAnomalyDetection() {
        val autoencoder = InteractionAutoencoder(featureDim = 8, latentDim = 2, randomSeed = 505)
        val normalPattern = FloatArray(8) { 0.5f }

        // Train on normal pattern
        for (i in 0 until 15) {
            autoencoder.trainStep(normalPattern, lr = 0.05f)
        }

        val (_, _, normalError) = autoencoder.forward(normalPattern)

        // Test with anomalous pattern
        val anomalousPattern = FloatArray(8) { 5.0f }
        val (_, _, anomalousError) = autoencoder.forward(anomalousPattern)

        assertTrue("Anomalous error should be significantly higher than normal reconstruction error",
            anomalousError > normalError * 3f)
    }

    @Test
    fun testMultimodalFusionEngine() {
        val engine = MultimodalFusionEngine()
        val intent = engine.predictIntent(
            gestureEmbedding = floatArrayOf(0.8f, 0.4f, 0.9f),
            motionState = "Stationary Desk",
            contextState = com.neurophone.app.domain.model.ContextState.STUDY,
            recentApps = listOf("Browser", "College Portal")
        )

        assertNotNull(intent.action)
        assertTrue("Confidence must be in (0, 1]", intent.confidence in 0.5f..1.0f)
        assertTrue("Latency must be recorded", intent.latencyMs >= 0)
        assertTrue("Signals must be populated", intent.contributingSignals.isNotEmpty())
    }
}