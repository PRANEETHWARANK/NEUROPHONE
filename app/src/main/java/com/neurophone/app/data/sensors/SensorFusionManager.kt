package com.neurophone.app.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

data class SensorTelemetry(
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 9.8f,
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val orientation: String = "Portrait",
    val motionMagnitude: Float = 0f,
    val motionState: String = "Stationary"
)

class SensorFusionManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val _telemetry = MutableStateFlow(SensorTelemetry())
    val telemetry: StateFlow<SensorTelemetry> = _telemetry.asStateFlow()

    private var currentAccel = FloatArray(3) { 0f }
    private var currentGyro = FloatArray(3) { 0f }

    fun startListening() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        gyroscope?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                currentAccel[0] = event.values[0]
                currentAccel[1] = event.values[1]
                currentAccel[2] = event.values[2]
                updateState()
            }
            Sensor.TYPE_GYROSCOPE -> {
                currentGyro[0] = event.values[0]
                currentGyro[1] = event.values[1]
                currentGyro[2] = event.values[2]
                updateState()
            }
        }
    }

    private fun updateState() {
        val mag = sqrt(
            (currentAccel[0] * currentAccel[0] +
             currentAccel[1] * currentAccel[1] +
             currentAccel[2] * currentAccel[2]).toDouble()
        ).toFloat()

        val motionState = when {
            kotlin.math.abs(mag - 9.8f) > 3.0f -> "Walking / Shaking"
            kotlin.math.abs(mag - 9.8f) > 0.8f -> "Handheld Active"
            else -> "Stationary Desk"
        }

        val orientation = if (kotlin.math.abs(currentAccel[0]) > kotlin.math.abs(currentAccel[1])) {
            "Landscape"
        } else {
            "Portrait"
        }

        _telemetry.value = SensorTelemetry(
            accelX = currentAccel[0],
            accelY = currentAccel[1],
            accelZ = currentAccel[2],
            gyroX = currentGyro[0],
            gyroY = currentGyro[1],
            gyroZ = currentGyro[2],
            orientation = orientation,
            motionMagnitude = mag,
            motionState = motionState
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}