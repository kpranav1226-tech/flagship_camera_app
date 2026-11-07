package com.clearshot.ai.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.round
import kotlin.math.sqrt

/**
 * High-precision device orientation manager.
 * Tracks physical horizon tilt angle using Android Rotation Vector sensor,
 * with automatic fallback to Gravity and Accelerometer sensors.
 *
 * Implements omnidirectional roll calculation that operates seamlessly
 * across portrait, landscape, and reverse landscape camera orientations.
 * Optimizes battery consumption by registering sensor listeners only when actively needed.
 */
class DeviceOrientationManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    // 1. Primary: Rotation Vector (optimal hardware sensor fusion)
    private val rotationVectorSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    // 2. Fallback A: Hardware Gravity sensor
    private val gravitySensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
    // 3. Fallback B: Accelerometer sensor
    private val accelerometerSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val activeSensor: Sensor? = rotationVectorSensor ?: gravitySensor ?: accelerometerSensor

    private val _rollAngle = MutableStateFlow(0f)
    val rollAngle: StateFlow<Float> = _rollAngle.asStateFlow()

    private val _isLevel = MutableStateFlow(false)
    val isLevel: StateFlow<Boolean> = _isLevel.asStateFlow()

    var toleranceDeg: Float = 1.0f
        set(value) {
            field = value.coerceIn(0.2f, 5.0f)
            _isLevel.value = abs(_rollAngle.value) <= field
        }

    private val rotationMatrix = FloatArray(9)
    private var isRunning = false

    fun start() {
        if (isRunning) return
        activeSensor?.let { sensor ->
            sensorManager?.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_UI
            )
            isRunning = true
            Log.d(TAG, "Orientation sensor active: ${sensor.name} (type ${sensor.type})")
        } ?: Log.w(TAG, "No suitable orientation sensor found on device")
    }

    fun stop() {
        if (!isRunning) return
        sensorManager?.unregisterListener(this)
        isRunning = false
        Log.d(TAG, "Orientation sensor stopped to preserve battery")
    }

    override fun onSensorChanged(event: SensorEvent) {
        val (gx, gy) = when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                // Device-frame gravity vector components from rotation matrix: R[6], R[7]
                Pair(rotationMatrix[6], rotationMatrix[7])
            }
            Sensor.TYPE_GRAVITY, Sensor.TYPE_ACCELEROMETER -> {
                Pair(event.values[0], event.values[1])
            }
            else -> return
        }

        val magnitude = sqrt((gx * gx + gy * gy).toDouble())
        // When device is lying flat on a table (pointing directly up or down), horizon is undefined
        if (magnitude < 0.15) return

        // Compute angle against device axes (-180° to +180°)
        val rawAngle = Math.toDegrees(atan2(gx.toDouble(), gy.toDouble())).toFloat()

        // Calculate roll relative to nearest cardinal horizon (0° portrait, 90° landscape, -90° rev-landscape, 180°)
        val nearestCardinal = 90f * round(rawAngle / 90f)
        val relativeRoll = rawAngle - nearestCardinal

        // Low-pass filter for smooth 60fps indicator motion without sensor jitter
        val current = _rollAngle.value
        val smoothed = current * 0.75f + relativeRoll * 0.25f
        _rollAngle.value = smoothed
        _isLevel.value = abs(smoothed) <= toleranceDeg
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val TAG = "OrientationManager"
    }
}
