package info.cemu.cemu.input

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import info.cemu.cemu.nativeinterface.NativeInput.onMotion
import info.cemu.cemu.nativeinterface.NativeInput.setMotionEnabled

class SensorManager(context: Context) : SensorEventListener {
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope =
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var deviceRotationProvider = { Surface.ROTATION_0 }
    private val hasMotionData = accelerometer != null && gyroscope != null
    private var gyroX = 0f
    private var gyroY = 0f
    private var gyroZ = 0f
    private var isListening = false

    fun startListening() {
        if (!hasMotionData || isListening) {
            return
        }
        isListening = true
        setMotionEnabled(true)
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

    fun setDeviceRotationProvider(deviceRotationProvider: () -> Int) {
        this.deviceRotationProvider = deviceRotationProvider
    }

    fun pauseListening() {
        if (!hasMotionData || !isListening) {
            return
        }
        isListening = false
        setMotionEnabled(false)
        sensorManager.unregisterListener(this)
    }


    override fun onSensorChanged(event: SensorEvent) {
        val values = event.values
        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            val gyroValues = getSensorEventValues(values)
            gyroX = gyroValues.first
            gyroY = gyroValues.second
            gyroZ = gyroValues.third
            return
        }
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) {
            return
        }
        val (accelX, accelY, accelZ) = getSensorEventValues(values)
        onMotion(event.timestamp, gyroX, gyroY, gyroZ, accelX, accelZ, -accelY)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    }

    private fun getSensorEventValues(values: FloatArray): Triple<Float, Float, Float> {
        val x: Float
        val y: Float
        val z = values[2]
        val deviceRotation = deviceRotationProvider()
        when (deviceRotation) {
            Surface.ROTATION_90 -> {
                x = -values[1]
                y = values[0]
            }

            Surface.ROTATION_180 -> {
                x = -values[0]
                y = -values[1]
            }

            Surface.ROTATION_270 -> {
                x = values[1]
                y = -values[0]
            }

            else /*Surface.ROTATION_0*/ -> {
                x = values[0]
                y = values[1]
            }
        }
        return Triple(x, y, z)
    }
}
