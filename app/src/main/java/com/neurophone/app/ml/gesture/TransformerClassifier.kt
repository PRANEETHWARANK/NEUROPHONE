package com.neurophone.app.ml.gesture

import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Real mathematical Lightweight Transformer Encoder for sequence gesture & intent prediction.
 * Architecture:
 * Input Sequence (T x D_in) -> Linear Projection + Sinusoidal Positional Encoding ->
 * Multi-Head Self Attention (Q, K, V with Scaled Dot-Product) -> Residual + LayerNorm ->
 * Feed-Forward Network (MLP) -> Residual + LayerNorm -> Mean Pooling -> Classification Head.
 */
class TransformerClassifier(
    val seqLength: Int = 30,
    val inputDim: Int = 63,
    val dModel: Int = 32,
    val numHeads: Int = 2,
    val numClasses: Int = 5,
    val randomSeed: Long = 42
) {
    val dHead = dModel / numHeads

    // Linear Input Projection: [dModel][inputDim]
    val wProj: Array<FloatArray>
    val bProj: FloatArray

    // Positional Encodings [seqLength][dModel]
    val posEncoding: Array<FloatArray>

    // Multi-Head Attention Weights: W_Q, W_K, W_V, W_O [dModel][dModel]
    val wQ: Array<FloatArray>
    val wK: Array<FloatArray>
    val wV: Array<FloatArray>
    val wO: Array<FloatArray>

    // FFN Weights
    val wFfn1: Array<FloatArray> // [2 * dModel][dModel]
    val bFfn1: FloatArray
    val wFfn2: Array<FloatArray> // [dModel][2 * dModel]
    val bFfn2: FloatArray

    // Output Head
    val wCls: Array<FloatArray> // [numClasses][dModel]
    val bCls: FloatArray

    init {
        val rng = Random(randomSeed)
        val scaleProj = sqrt(2.0f / (inputDim + dModel))
        wProj = Array(dModel) { FloatArray(inputDim) { (rng.nextFloat() * 2f - 1f) * scaleProj } }
        bProj = FloatArray(dModel) { 0f }

        // Standard Sinusoidal Positional Encodings
        posEncoding = Array(seqLength) { pos ->
            FloatArray(dModel) { i ->
                val exponent = (2 * (i / 2)).toDouble() / dModel
                val freq = (1.0 / Math.pow(10000.0, exponent)).toFloat()
                if (i % 2 == 0) sin(pos.toFloat() * freq) else cos(pos.toFloat() * freq)
            }
        }

        val scaleAttn = sqrt(2.0f / (dModel + dModel))
        wQ = Array(dModel) { FloatArray(dModel) { (rng.nextFloat() * 2f - 1f) * scaleAttn } }
        wK = Array(dModel) { FloatArray(dModel) { (rng.nextFloat() * 2f - 1f) * scaleAttn } }
        wV = Array(dModel) { FloatArray(dModel) { (rng.nextFloat() * 2f - 1f) * scaleAttn } }
        wO = Array(dModel) { FloatArray(dModel) { (rng.nextFloat() * 2f - 1f) * scaleAttn } }

        val dFfn = 2 * dModel
        val scaleFfn = sqrt(2.0f / (dModel + dFfn))
        wFfn1 = Array(dFfn) { FloatArray(dModel) { (rng.nextFloat() * 2f - 1f) * scaleFfn } }
        bFfn1 = FloatArray(dFfn) { 0f }
        wFfn2 = Array(dModel) { FloatArray(dFfn) { (rng.nextFloat() * 2f - 1f) * scaleFfn } }
        bFfn2 = FloatArray(dModel) { 0f }

        val scaleCls = sqrt(2.0f / (dModel + numClasses))
        wCls = Array(numClasses) { FloatArray(dModel) { (rng.nextFloat() * 2f - 1f) * scaleCls } }
        bCls = FloatArray(numClasses) { 0f }
    }

    private fun layerNorm(x: FloatArray): FloatArray {
        var mean = 0f
        for (v in x) mean += v
        mean /= x.size
        var variance = 0f
        for (v in x) variance += (v - mean) * (v - mean)
        variance /= x.size
        val std = sqrt(variance + 1e-5f)
        return FloatArray(x.size) { (x[it] - mean) / std }
    }

    private fun relu(x: Float): Float = max(0f, x)

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
        val T = minOf(sequence.size, seqLength)
        // 1. Projection + Positional Encoding
        val xEmbedded = Array(T) { t ->
            val frame = sequence[t]
            val emb = FloatArray(dModel)
            for (i in 0 until dModel) {
                var sum = bProj[i]
                for (j in 0 until inputDim) sum += wProj[i][j] * frame[j]
                emb[i] = sum + posEncoding[t][i]
            }
            emb
        }

        // 2. Multi-Head Scaled Dot-Product Self-Attention
        val Q = Array(T) { t -> FloatArray(dModel) { i -> wQ[i].indices.sumOf { (wQ[i][it] * xEmbedded[t][it]).toDouble() }.toFloat() } }
        val K = Array(T) { t -> FloatArray(dModel) { i -> wK[i].indices.sumOf { (wK[i][it] * xEmbedded[t][it]).toDouble() }.toFloat() } }
        val V = Array(T) { t -> FloatArray(dModel) { i -> wV[i].indices.sumOf { (wV[i][it] * xEmbedded[t][it]).toDouble() }.toFloat() } }

        val scale = 1.0f / sqrt(dHead.toFloat())
        val attnOutput = Array(T) { FloatArray(dModel) }

        for (h in 0 until numHeads) {
            val headOffset = h * dHead
            for (i in 0 until T) {
                val scores = FloatArray(T)
                for (j in 0 until T) {
                    var dot = 0f
                    for (d in 0 until dHead) {
                        dot += Q[i][headOffset + d] * K[j][headOffset + d]
                    }
                    scores[j] = dot * scale
                }
                val weights = softmax(scores)
                for (d in 0 until dHead) {
                    var sumV = 0f
                    for (j in 0 until T) {
                        sumV += weights[j] * V[j][headOffset + d]
                    }
                    attnOutput[i][headOffset + d] += sumV
                }
            }
        }

        // Residual + LayerNorm
        val xNorm1 = Array(T) { t ->
            val res = FloatArray(dModel) { d -> xEmbedded[t][d] + attnOutput[t][d] }
            layerNorm(res)
        }

        // FFN + Residual + LayerNorm
        val xFfn = Array(T) { t ->
            val intermediate = FloatArray(2 * dModel) { idx ->
                var s = bFfn1[idx]
                for (d in 0 until dModel) s += wFfn1[idx][d] * xNorm1[t][d]
                relu(s)
            }
            val ffnOut = FloatArray(dModel) { idx ->
                var s = bFfn2[idx]
                for (d in 0 until 2 * dModel) s += wFfn2[idx][d] * intermediate[d]
                s
            }
            val res = FloatArray(dModel) { d -> xNorm1[t][d] + ffnOut[d] }
            layerNorm(res)
        }

        // Mean Pooling across sequence dimension
        val pooled = FloatArray(dModel)
        for (t in 0 until T) {
            for (d in 0 until dModel) {
                pooled[d] += xFfn[t][d]
            }
        }
        for (d in 0 until dModel) {
            pooled[d] = pooled[d] / T.toFloat()
        }

        // Classification Head
        val logits = FloatArray(numClasses)
        for (k in 0 until numClasses) {
            var sum = bCls[k]
            for (d in 0 until dModel) {
                sum += wCls[k][d] * pooled[d]
            }
            logits[k] = sum
        }

        return Pair(softmax(logits), pooled)
    }

    fun trainStep(sequence: List<FloatArray>, targetClass: Int, lr: Float = 0.01f): Float {
        val (probs, pooled) = forward(sequence)
        val loss = -kotlin.math.ln(max(probs[targetClass], 1e-7f))

        val dZ = FloatArray(numClasses)
        for (k in 0 until numClasses) {
            dZ[k] = probs[k] - (if (k == targetClass) 1.0f else 0.0f)
            for (d in 0 until dModel) {
                wCls[k][d] -= lr * dZ[k] * pooled[d]
            }
            bCls[k] -= lr * dZ[k]
        }
        return loss
    }
}