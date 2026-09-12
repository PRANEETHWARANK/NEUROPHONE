package com.neurophone.app.ml.gesture

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Real mathematical Long Short-Term Memory (LSTM) cell & recurrent sequence classifier.
 * Computes:
 * i_t = sigmoid(W_xi * x_t + W_hi * h_{t-1} + b_i)
 * f_t = sigmoid(W_xf * x_t + W_hf * h_{t-1} + b_f)
 * g_t = tanh(W_xg * x_t + W_hg * h_{t-1} + b_g)
 * o_t = sigmoid(W_xo * x_t + W_ho * h_{t-1} + b_o)
 * c_t = f_t * c_{t-1} + i_t * g_t
 * h_t = o_t * tanh(c_t)
 */
class LstmClassifier(
    val inputDim: Int = 63, // Features per hand landmark frame
    val hiddenDim: Int = 32,
    val numClasses: Int = 5,
    val randomSeed: Long = 42
) {
    // Combined gates weights: [i, f, g, o] (size 4 * hiddenDim)
    val wX: Array<FloatArray> // [4 * hiddenDim][inputDim]
    val wH: Array<FloatArray> // [4 * hiddenDim][hiddenDim]
    val bGate: FloatArray     // [4 * hiddenDim]

    // Dense classification head
    val wOut: Array<FloatArray> // [numClasses][hiddenDim]
    val bOut: FloatArray        // [numClasses]

    init {
        val rng = Random(randomSeed)
        val gateDim = 4 * hiddenDim
        val scaleX = sqrt(2.0f / (inputDim + hiddenDim))
        wX = Array(gateDim) { FloatArray(inputDim) { (rng.nextFloat() * 2f - 1f) * scaleX } }

        val scaleH = sqrt(2.0f / (hiddenDim + hiddenDim))
        wH = Array(gateDim) { FloatArray(hiddenDim) { (rng.nextFloat() * 2f - 1f) * scaleH } }
        bGate = FloatArray(gateDim) { idx ->
            // Initialize forget gate bias to 1.0f for stable gradient flow
            if (idx in hiddenDim until 2 * hiddenDim) 1.0f else 0.0f
        }

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

    /**
     * Process temporal sequence: [T frames, inputDim features]
     * Returns: Pair(probabilities [numClasses], finalHiddenState [hiddenDim])
     */
    fun forward(sequence: List<FloatArray>): Pair<FloatArray, FloatArray> {
        var h = FloatArray(hiddenDim)
        var c = FloatArray(hiddenDim)

        for (x in sequence) {
            val nextH = FloatArray(hiddenDim)
            val nextC = FloatArray(hiddenDim)

            for (i in 0 until hiddenDim) {
                // Input gate
                var sumI = bGate[i]
                for (d in 0 until inputDim) sumI += wX[i][d] * x[d]
                for (d in 0 until hiddenDim) sumI += wH[i][d] * h[d]
                val gateI = sigmoid(sumI)

                // Forget gate
                val fIdx = hiddenDim + i
                var sumF = bGate[fIdx]
                for (d in 0 until inputDim) sumF += wX[fIdx][d] * x[d]
                for (d in 0 until hiddenDim) sumF += wH[fIdx][d] * h[d]
                val gateF = sigmoid(sumF)

                // Candidate gate
                val gIdx = 2 * hiddenDim + i
                var sumG = bGate[gIdx]
                for (d in 0 until inputDim) sumG += wX[gIdx][d] * x[d]
                for (d in 0 until hiddenDim) sumG += wH[gIdx][d] * h[d]
                val gateG = tanhAct(sumG)

                // Output gate
                val oIdx = 3 * hiddenDim + i
                var sumO = bGate[oIdx]
                for (d in 0 until inputDim) sumO += wX[oIdx][d] * x[d]
                for (d in 0 until hiddenDim) sumO += wH[oIdx][d] * h[d]
                val gateO = sigmoid(sumO)

                nextC[i] = gateF * c[i] + gateI * gateG
                nextH[i] = gateO * tanhAct(nextC[i])
            }
            h = nextH
            c = nextC
        }

        // Dense classification output
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

        // Backprop through final classification head
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