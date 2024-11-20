package com.ev.loudbubble

import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.edit
import kotlin.math.atan2
import kotlin.math.sqrt

class BubbleManager(
    val sensorManager: SensorManager,
    val sharedPreferences: SharedPreferences
) {

    var bubleStateListener: (Float, Float) -> Unit = { _, _ -> }

    private var zeroX: Float = 0f
    private var zeroY: Float = 0f
    private var zeroZ: Float = 0f

    private var lastAccelValues: FloatArray = FloatArray(3)

    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            lastAccelValues = event.values
            val x = event.values[0].toDouble() - zeroX
            val y = event.values[1].toDouble() - zeroY
            val z = event.values[2].toDouble() - zeroZ

            val pitch = Math.toDegrees(
                atan2(y, sqrt(x * x + z * z))
            )

            val roll = Math.toDegrees(atan2(-x, z))

            bubleStateListener(pitch.toFloat(), roll.toFloat())
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // oh well..
        }
    }

    fun zero() {
        zeroX = lastAccelValues[0]
        zeroY = lastAccelValues[1]
        zeroZ = lastAccelValues[2]

        sharedPreferences.edit(commit = true) {
            putFloat("zeroX", zeroX)
            putFloat("zeroY", zeroY)
            putFloat("zeroZ", zeroZ)
        }
    }

    fun init() {
        zeroX = sharedPreferences.getFloat("zeroX", 0f)
        zeroY = sharedPreferences.getFloat("zeroY", 0f)
        zeroZ = sharedPreferences.getFloat("zeroZ", 0f)
        resume()
    }

    fun resume() {
        sensorManager.registerListener(
            sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI
        )
    }

    fun pause() {
        sensorManager.unregisterListener(sensorListener)
    }

}