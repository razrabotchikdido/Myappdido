package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

data class CompassData(
    val azimuthDegrees: Float = 0f,
    val qiblaBearingDegrees: Float = 0f,
    val relativeQiblaAngle: Float = 0f, // Angle to rotate the arrow: (qibla - azimuth)
    val isAlignedWithQibla: Boolean = false,
    val sensorAccuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val hasCompassSensor: Boolean = true
)

class CompassSensorManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magneticField: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _compassData = MutableStateFlow(CompassData())
    val compassData: StateFlow<CompassData> = _compassData.asStateFlow()

    private var targetQiblaBearing: Float = 0f
    private var lastVibratedAligned = false
    private var lastAzimuth = 0f

    // Fallback data arrays
    private val gravityValues = FloatArray(3)
    private val geomagneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    fun setTargetQiblaBearing(bearing: Float) {
        targetQiblaBearing = bearing
        updateCompassData(lastAzimuth, _compassData.value.sensorAccuracy)
    }

    fun startListening() {
        val hasSensor = rotationVectorSensor != null || (accelerometer != null && magneticField != null)
        if (!hasSensor) {
            _compassData.value = _compassData.value.copy(hasCompassSensor = false)
            return
        }

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            magneticField?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val azimuthRad = orientationAngles[0]
                val azimuthDeg = (Math.toDegrees(azimuthRad.toDouble()).toFloat() + 360f) % 360f
                processNewAzimuth(azimuthDeg, event.accuracy)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, gravityValues, 0, 3)
                hasGravity = true
                tryCalculateOrientation(event.accuracy)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, geomagneticValues, 0, 3)
                hasGeomagnetic = true
                tryCalculateOrientation(event.accuracy)
            }
        }
    }

    private fun tryCalculateOrientation(accuracy: Int) {
        if (hasGravity && hasGeomagnetic) {
            val r = FloatArray(9)
            val i = FloatArray(9)
            if (SensorManager.getRotationMatrix(r, i, gravityValues, geomagneticValues)) {
                SensorManager.getOrientation(r, orientationAngles)
                val azimuthRad = orientationAngles[0]
                val azimuthDeg = (Math.toDegrees(azimuthRad.toDouble()).toFloat() + 360f) % 360f
                processNewAzimuth(azimuthDeg, accuracy)
            }
        }
    }

    private fun processNewAzimuth(rawAzimuth: Float, accuracy: Int) {
        // Low-pass exponential smoothing
        val alpha = 0.15f
        var diff = rawAzimuth - lastAzimuth
        while (diff < -180f) diff += 360f
        while (diff > 180f) diff -= 360f
        val smoothed = (lastAzimuth + alpha * diff + 360f) % 360f
        lastAzimuth = smoothed

        updateCompassData(smoothed, accuracy)
    }

    private fun updateCompassData(azimuth: Float, accuracy: Int) {
        // Relative angle to Qibla (where arrow points relative to current phone heading)
        val relative = (targetQiblaBearing - azimuth + 360f) % 360f
        // When within 3 degrees of Kaaba
        val angleDiff = if (relative > 180f) 360f - relative else relative
        val isAligned = angleDiff <= 3.5f

        if (isAligned && !lastVibratedAligned) {
            triggerVibration()
        }
        lastVibratedAligned = isAligned

        _compassData.value = CompassData(
            azimuthDegrees = azimuth,
            qiblaBearingDegrees = targetQiblaBearing,
            relativeQiblaAngle = relative,
            isAlignedWithQibla = isAligned,
            sensorAccuracy = accuracy,
            hasCompassSensor = true
        )
    }

    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        _compassData.value = _compassData.value.copy(sensorAccuracy = accuracy)
    }
}
