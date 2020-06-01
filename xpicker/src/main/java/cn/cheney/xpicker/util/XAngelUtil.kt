package cn.cheney.xpicker.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

object XAngelUtil {

    private var sensorManager: SensorManager? = null

     var sensorAngle: Int = 0


    private val sensorEventListener: SensorEventListener = object :
        SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (Sensor.TYPE_ACCELEROMETER != event.sensor.type) {
                return
            }
            val values = event.values
            sensorAngle = getSensorAngle(values[0], values[1])
            //Log.d(XPicker.TAG, "手机角度发生变化=$sensorAngle");
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    fun registerSensorManager(context: Context) {
        if (sensorManager == null) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        }
        sensorManager!!.registerListener(
            sensorEventListener,
            sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    fun unregisterSensorManager(context: Context) {
        if (sensorManager == null) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        }
        sensorManager!!.unregisterListener(
            sensorEventListener
        )
    }

}