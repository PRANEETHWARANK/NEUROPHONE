package com.neurophone.app.ml.anomaly

import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Real mathematical Autoencoder for interaction sequence anomaly detection.
 * Learns normal user interaction profile (Bottleneck representation).
 * Computes:
 * z = relu(W_enc * x + b_enc)  (Encoder: dimension D -> Latent bottleneck)
 * \hat{x} = W_dec * z + b_dec   (Decoder: Latent -> D)
 * Reconstruction Error = 1/D \sum (x_i - \hat{x}_i)^2
 */
class InteractionAutoencoder(
    val featureDim: Int = 16,
    val latentDim: Int = 4,
    val randomSeed: Long = 42
) {
    val wEnc: Array<FloatArray> // [latentDim][featureDim]
    val bEnc: FloatArray

    val wDec: Array<FloatArray> // [featureDim][latentDim]
    val bDec: FloatArray

    // Baseline running average threshold
    var anomalyThreshold: Float = 0.12f

    init {
        val rng = Random(randomSeed)
        val scaleEnc = sqrt(2.0f / (featureDim + latentDim))
        wEnc = Array(latentDim) { FloatArray(featureDim) { (rng.nextFloat() * 2f - 1f) * scaleEnc } }
        bEnc = FloatArray(latentDim) { 0f }

        val scaleDec = sqrt(2.0f / (latentDim + featureDim))
        wDec = Array(featureDim) { FloatArray(latentDim) { (rng.nextFloat() * 2f - 1f) * scaleDec } }
        bDec = FloatArray(featureDim) { 0f }
    }

    private fun relu(x: Float): Float = max(0f, x)
    private fun reluDeriv(x: Float): Float = if (x > 0f) 1.0f else 0.0f

    /**
     * Reconstruct features and calculate Mean Squared Reconstruction Error
     */
    fun forward(features: FloatArray): Triple<FloatArray, FloatArray, Float> {
        val latent = FloatArray(latentDim)
        for (i in 0 until latentDim) {
            var sum = bEnc[i]
            for (j in 0 until featureDim) {
                sum += wEnc[i][j] * features[j]
            }
            latent[i] = relu(sum)
        }

        val reconstructed = FloatArray(featureDim)
        var mse = 0f
        for (j in 0 until featureDim) {
            var sum = bDec[j]
            for (i in 0 until latentDim) {
                sum += wDec[j][i] * latent[i]
            }
            reconstructed[j] = sum
            val diff = features[j] - reconstructed[j]
            mse += diff * diff
        }
        mse /= featureDim

        return Triple(latent, reconstructed, mse)
    }

    /**
     * Unsupervised online training on normal user interaction vectors
     */
    fun trainStep(features: FloatArray, lr: Float = 0.01f): Float {
        val (latent, reconstructed, mse) = forward(features)

        // dL / dRec = -2/D * (x - \hat{x})
        val dRec = FloatArray(featureDim) { j ->
            -2.0f / featureDim * (features[j] - reconstructed[j])
        }

        // Backprop through decoder
        val dLatent = FloatArray(latentDim)
        for (j in 0 until featureDim) {
            val dr = dRec[j]
            for (i in 0 until latentDim) {
                dLatent[i] += dr * wDec[j][i]
                wDec[j][i] -= lr * dr * latent[i]
            }
            bDec[j] -= lr * dr
        }

        // Backprop through encoder
        for (i in 0 until latentDim) {
            val dZ = dLatent[i] * reluDeriv(latent[i])
            for (j in 0 until featureDim) {
                wEnc[i][j] -= lr * dZ * features[j]
            }
            bEnc[i] -= lr * dZ
        }

        return mse
    }

    fun isAnomaly(features: FloatArray): Pair<Boolean, Float> {
        val (_, _, mse) = forward(features)
        return Pair(mse > anomalyThreshold, mse)
    }
}