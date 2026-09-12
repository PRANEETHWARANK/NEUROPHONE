package com.neurophone.app.ml.gesture

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Real mathematical Multi-Layer Perceptron (MLP) implementation.
 * Supports forward pass, softmax, cross-entropy, and gradient descent backpropagation.
 */
class MlpClassifier(
    val inputSize: Int = 1890, // 30 frames * 63 features
    val hiddenSize: Int = 64,
    val numClasses: Int = 5,
    val randomSeed: Long = 42
) {
    // Weights: W1 [hiddenSize][inputSize], b1 [hiddenSize]
    val w1: Array<FloatArray>
    val b1: FloatArray

    // Weights: W2 [numClasses][hiddenSize], b2 [numClasses]
    val w2: Array<FloatArray>
    val b2: FloatArray

    init {
        val rng = Random(randomSeed)
        // He / Xavier Initialization
        val scale1 = sqrt(2.0f / inputSize)
        w1 = Array(hiddenSize) { FloatArray(inputSize) { (rng.nextFloat() * 2f - 1f) * scale1 } }
        b1 = FloatArray(hiddenSize) { 0.0f }

        val scale2 = sqrt(2.0f / hiddenSize)
        w2 = Array(numClasses) { FloatArray(hiddenSize) { (rng.nextFloat() * 2f - 1f) * scale2 } }
        b2 = FloatArray(numClasses) { 0.0f }
    }

    fun relu(x: Float): Float = max(0f, x)
    fun reluDeriv(x: Float): Float = if (x > 0f) 1.0f else 0.0f

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
     * Forward pass returns pair of (probabilities, hidden_activations)
     */
    fun forward(input: FloatArray): Pair<FloatArray, FloatArray> {
        val h = FloatArray(hiddenSize)
        for (i in 0 until hiddenSize) {
            var sum = b1[i]
            val row = w1[i]
            for (j in 0 until inputSize) {
                sum += row[j] * input[j]
            }
            h[i] = relu(sum)
        }

        val logits = FloatArray(numClasses)
        for (k in 0 until numClasses) {
            var sum = b2[k]
            val row = w2[k]
            for (i in 0 until hiddenSize) {
                sum += row[i] * h[i]
            }
            logits[k] = sum
        }

        return Pair(softmax(logits), h)
    }

    /**
     * Real On-Device Backpropagation training step
     */
    fun trainStep(
        input: FloatArray,
        targetClass: Int,
        learningRate: Float = 0.01f,
        weightDecay: Float = 0.0001f
    ): Float {
        val (probs, h) = forward(input)
        val loss = -kotlin.math.ln(max(probs[targetClass], 1e-7f))

        // Gradient of Cross-Entropy w.r.t logits: dL/dz2 = p - y
        val dZ2 = FloatArray(numClasses)
        for (k in 0 until numClasses) {
            dZ2[k] = probs[k] - (if (k == targetClass) 1.0f else 0.0f)
        }

        // Gradient w.r.t W2, b2
        val dH = FloatArray(hiddenSize)
        for (k in 0 until numClasses) {
            val dz = dZ2[k]
            val w2k = w2[k]
            for (i in 0 until hiddenSize) {
                dH[i] += dz * w2k[i]
                w2k[i] -= learningRate * (dz * h[i] + weightDecay * w2k[i])
            }
            b2[k] -= learningRate * dz
        }

        // Gradient w.r.t hidden pre-activation: dZ1 = dH * relu'(Z1)
        val dZ1 = FloatArray(hiddenSize)
        for (i in 0 until hiddenSize) {
            dZ1[i] = dH[i] * reluDeriv(h[i])
        }

        // Gradient w.r.t W1, b1
        for (i in 0 until hiddenSize) {
            val dz = dZ1[i]
            val w1i = w1[i]
            for (j in 0 until inputSize) {
                w1i[j] -= learningRate * (dz * input[j] + weightDecay * w1i[j])
            }
            b1[i] -= learningRate * dz
        }

        return loss
    }
}