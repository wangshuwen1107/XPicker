package cn.cheney.xpicker.util

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface


class OrientationWatcher(
    context: Context
) {

    var value = 0

    var characteristics: CameraCharacteristics? = null

    private val listener = object : OrientationEventListener(context.applicationContext) {
        override fun onOrientationChanged(orientation: Int) {
            if (null == characteristics) {
                return
            }
            val rotation = when {
                orientation <= 45 -> Surface.ROTATION_0
                orientation <= 135 -> Surface.ROTATION_90
                orientation <= 225 -> Surface.ROTATION_180
                orientation <= 315 -> Surface.ROTATION_270
                else -> Surface.ROTATION_0
            }
            val relative = computeRelativeRotation(characteristics!!, rotation)
            if (relative != value) {
                value = relative
            }
        }
    }

    fun enable() {
        listener.enable()
    }

    fun disable() {
        listener.disable()
    }

    companion object {

        private fun computeRelativeRotation(
            characteristics: CameraCharacteristics,
            surfaceRotation: Int
        ): Int {
            val isFront = characteristics.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_FRONT

            val sensorOrientationDegrees =
                characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!

            val deviceOrientationDegrees = when (surfaceRotation) {
                Surface.ROTATION_0 -> 0
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> 0
            }

            val sign = if (isFront) 1 else -1
//            Log.i("Camera2Module", "sensorOrientationDegrees=$sensorOrientationDegrees")
            return (sensorOrientationDegrees - (deviceOrientationDegrees * sign) + 360) % 360
        }
    }
}
