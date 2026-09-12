package com.neurophone.app.ml.gesture

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Real mathematical Gated Recurrent Unit (GRU) cell & recurrent sequence classifier.
 * Computes:
 * z_t = sigmoid(W_xz * x_t + W_hz * h_{t-1} + b_z)  (Update Gate)
 * r_t = sigmoid(W_xr * x_t + W_hr * h_{t-1} + b_r)  (Reset Gate)
 * \hat{h}_t = tanh(W_xh * x_t + W_hh * (r_t \odot h_{t-1}) + b_h) (Candidate)
 * h_t = (1 - z_t) \odot h_{t-1} + z_t \odot \hat{h}_t
 */
class GruClassifier(
    val inputDim: Int = 63,
    val hiddenDim: Int = 32,
    val numClasses: Int = 5,
    val randomSeed: Long = 42
) {
    // Combined gate weights: [z (update), r (reset), h (candidate)] (3 * hiddenDim)
    val wX: Array<FloatArray> // [3 * hiddenDim][inputDim]
    val wH: Array<FloatArray> // [3 * hiddenDim][hiddenDim]
    val bGate: FloatArray     // [3 * hiddenDim]

    // Classification Head
    val wOut: Array<FloatArray> // [numClasses][hiddenDim]
    val bOut: FloatArray        // [numClasses]

    init {
        val rng = Random(randomSeed)
        val gateDim = 3 * hiddenDim
        val scaleX = sqrt(2.0f / (inputDim + hiddenDim))
        wX = Array(gateDim) { FloatArray(inputDim) { (rng.nextFloat() * 2f - 1f) * scaleX } }

        val scaleH = sqrt(2.0f / (hiddenDim + hiddenDim))
        wH = Array(gateDim) { FloatArray(hiddenDim) { (rng.nextFloat() * 2f - 1f) * scaleH } }
        bGate = FloatArray(gateDim) { 0.0f }

        val scaleOut = sqrt(2.0f / (hiddenDim + numClasses))
        wOut = Array(numClasses) { FloatArray(hiddenDim) { (rng.nextFloat() * 2f - 1f) * scaleOut } }
        bOut = FloatArray(numClasses) { 0.0f }
    }

    fun sigmoid(x: Float): Float = 1.0f / (1.0f + exp(-x.coerceIn(-15f, 15f)))
    fun tanhAct(x: Float): Float = tanh(x.coerceIn(-15f, 15f))

    fun softmax(logits: FloatArray): FloatArray {
        var maxLogit = logits[0]
        for (v in logits) if (v > maxLogit) maxLogit = v
        val exps = FloatArray(logits.size)
        var sum = 0.0f
        for (i in logits.indices) {
            exps[i] = exp(logits[i] - maxLogit)
            sum += exps[i]
        }
        val probs = FloatArray(logits.size)
        for (i in logits.indices) {
            probs[i] = if (sum > 0f) exps[i] / sum else 1.0f / logits.size
        }
        return probs
    }

    fun forward(sequence: List<FloatArray>): Pair<FloatArray, FloatArray> {
        var h = FloatArray(hiddenDim)

        for (x in sequence) {
            val nextH = FloatArray(hiddenDim)
            for (i in 0 until hiddenDim) {
                // Update gate z
                var sumZ = bGate[i]
                for (d in 0 until inputDim) sumZ += wX[i][d] * x[d]
                for (d in 0 until hiddenDim) sumZ += wH[i][d] * h[d]
                val z = sigmoid(sumZ)

                // Reset gate r
                val rIdx = hiddenDim + i
                var sumR = bGate[rIdx]
                for (d in 0 until inputDim) sumR += wX[rIdx][d] * x[d]
                for (d in 0 until hiddenDim) sumR += wH[rIdx][d] * h[d]
                val r = sigmoid(sumR)

                // Candidate state \hat{h}
                val cIdx = 2 * hiddenDim + i
                var sumC = bGate[cIdx]
                for (d in 0 until inputDim) sumC += wX[cIdx][d] * x[d]
                for (d in 0 until hiddenDim) sumC += wH[cIdx][d] * (r * h[d])
                val hCand = tanhAct(sumC)

                // Output hidden state
                nextH[i] = (1.0f - z) * h[i] + z * hCand
            }
            h = nextH
        }

        val logits = FloatArray(numClasses)
        for (k in 0 until numClasses) {
            var sum = bOut[k]
            for (i in 0 until hiddenDim) {
                sum += wOut[k][i] * h[i]
            }
            logits[k] = sum
        }

        return Pair(softmax(logits), h)
    }

    fun trainStep(sequence: List<FloatArray>, targetClass: Int, lr: Float = 0.01f): Float {
        val (probs, h) = forward(sequence)
        val loss = -kotlin.math.ln(max(probs[targetClass], 1e-7f))

        val dZ = FloatArray(numClasses)
        for (k in 0 until numClasses) {
            dZ[k] = probs[k] - (if (k == targetClass) 1.0f else 0.0f)
            for (i in 0 until hiddenDim) {
                wOut[k][i] -= lr * dZ[k] * h[i]
            }
            bOut[k] -= lr * dZ[k]
        }
        return loss
    }
}