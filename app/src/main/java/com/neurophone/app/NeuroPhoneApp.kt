package com.neurophone.app

import android.app.Application
import android.content.Context
import com.neurophone.app.data.database.NeuroDatabaseHelper
import com.neurophone.app.data.sensors.SensorFusionManager
import com.neurophone.app.ml.anomaly.InteractionAutoencoder
import com.neurophone.app.ml.gesture.GruClassifier
import com.neurophone.app.ml.gesture.LstmClassifier
import com.neurophone.app.ml.gesture.MlpClassifier
import com.neurophone.app.ml.gesture.TransformerClassifier
import com.neurophone.app.ml.intent.MultimodalFusionEngine

/**
 * Application-level singleton providing access to shared resources.
 */
class NeuroPhoneApp : Application() {

    companion object {
        lateinit var instance: NeuroPhoneApp
            private set

        fun appContext(): Context = instance.applicationContext
    }

    // Shared ML instances
    val mlpClassifier by lazy { MlpClassifier() }
    val lstmClassifier by lazy { LstmClassifier() }
    val gruClassifier by lazy { GruClassifier() }
    val transformerClassifier by lazy { TransformerClassifier() }
    val autoencoder by lazy { InteractionAutoencoder() }
    val fusionEngine by lazy { MultimodalFusionEngine() }
    val database by lazy { NeuroDatabaseHelper(this) }
    val sensorManager by lazy { SensorFusionManager(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}