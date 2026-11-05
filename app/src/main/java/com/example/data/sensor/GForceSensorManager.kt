package com.example.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.model.LiveGForce
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * Reads phone accelerometer sensors to calculate real-time
 * lateral cornering G-force and braking/acceleration G-force.
 */
class GForceSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _liveGForce = MutableStateFlow(LiveGForce())
    val liveGForce: StateFlow<LiveGForce> = _liveGForce.asStateFlow()

    private var filterLat = 0f
    private var filterLong = 0f
    private val alpha = 0.15f // Low-pass filter smoothing coefficient

    private var peakLeft = 0f
    private var peakRight = 0f
    private var peakBrake = 0f
    private var peakAccel = 0f

    private var isListening = false

    fun startListening() {
        if (isListening || sensorManager == null || accelerometer == null) return
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        isListening = true
    }

    fun stopListening() {
        if (!isListening) return
        sensorManager?.unregisterListener(this)
        isListening = false
    }

    fun resetPeaks() {
        peakLeft = 0f
        peakRight = 0f
        peakBrake = 0f
        peakAccel = 0f
        _liveGForce.value = _liveGForce.value.copy(
            peakLeftG = 0f,
            peakRightG = 0f,
            peakBrakingG = 0f,
            peakAccelG = 0f
        )
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        // 1G is ~9.80665 m/s²
        // In phone vertical portrait mount in car:
        // X-axis: lateral acceleration (- Left, + Right)
        // Y-axis: vertical (or longitudinal if angled)
        // Z-axis: forward / backward
        val rawLat = event.values[0] / 9.80665f
        val rawLong = (if (event.values.size > 2) -event.values[2] else event.values[1]) / 9.80665f

        // Apply low pass filter
        filterLat = filterLat + alpha * (rawLat - filterLat)
        filterLong = filterLong + alpha * (rawLong - filterLong)

        val clampedLat = filterLat.coerceIn(-2.5f, 2.5f)
        val clampedLong = filterLong.coerceIn(-2.5f, 2.5f)

        if (clampedLat < peakLeft) peakLeft = clampedLat
        if (clampedLat > peakRight) peakRight = clampedLat
        if (clampedLong < peakBrake) peakBrake = clampedLong
        if (clampedLong > peakAccel) peakAccel = clampedLong

        _liveGForce.value = LiveGForce(
            latG = clampedLat,
            longG = clampedLong,
            peakLeftG = peakLeft,
            peakRightG = peakRight,
            peakBrakingG = peakBrake,
            peakAccelG = peakAccel
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
