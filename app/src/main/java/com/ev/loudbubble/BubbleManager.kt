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

    private var zeroPitch: Float = 0f
    private var zeroRoll: Float = 0f

    private var lastPitch: Float = 0f
    private var lastRoll: Float = 0f

    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val sensorListener = object : SensorEventListener {
        fun toDegrees(radians: Float): Float {
            return radians * 180 / Math.PI.toFloat()
        }

        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {

                Sensor.TYPE_ROTATION_VECTOR -> {
                    val lastRotationVector = event.values

                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(
                        rotationMatrix,
                        lastRotationVector
                    )

                    // Remap the coordinate system to have Z pointing to the sky and X pointing along the device's short side
                    val remappedRotationMatrix = FloatArray(9)
                    SensorManager.remapCoordinateSystem(
                        rotationMatrix,
                        SensorManager.AXIS_X,
                        SensorManager.AXIS_Z,
                        remappedRotationMatrix
                    )

                    // Get the orientation angles (pitch, roll, azimuth) from the remapped rotation matrix
                    val orientationAngles = FloatArray(3)
                    SensorManager.getOrientation(remappedRotationMatrix, orientationAngles)

                    // Extract pitch and roll values
                    lastPitch = orientationAngles[1]
                    lastRoll = orientationAngles[2]

                    val pitch = lastPitch - zeroPitch
                    val roll = lastRoll - zeroRoll

                    bubleStateListener(toDegrees(pitch), toDegrees(roll))
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // oh well..
        }
    }

    fun zero() {
        zeroPitch = lastPitch
        zeroRoll = lastRoll

        sharedPreferences.edit(commit = true) {
            putFloat("zeroPitch", zeroPitch)
            putFloat("zeroRoll", zeroRoll)
        }
    }

    fun init() {
        zeroPitch = sharedPreferences.getFloat("zeroPitch", 0f)
        zeroRoll = sharedPreferences.getFloat("zeroRoll", 0f)
        resume()
    }

    fun resume() {
        sensorManager.registerListener(
            sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI
        )
        sensorManager.registerListener(
            sensorListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI
        )
    }

    fun pause() {
        sensorManager.unregisterListener(sensorListener)
    }

}