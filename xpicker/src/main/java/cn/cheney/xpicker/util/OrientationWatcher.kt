package cn.cheney.xpicker.util

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.view.OrientationEventListener
import android.view.Surface


class OrientationWatcher(
    context: Context
) {

    var value = 0

    var characteristics: CameraCharacteristics? = null
        set(value) {
            field = value
            update()
        }

    private var orientation = 0

    private val listener = object : OrientationEventListener(context.applicationContext) {
        override fun onOrientationChanged(orientation: Int) {
            this@OrientationWatcher.orientation = orientation
            update()
        }
    }

    private fun update() {
        if (null == characteristics) {
            return
        }
        val deviceRotation = when {
            orientation <= 45 -> 0
            orientation <= 135 -> 90
            orientation <= 225 -> 180
            orientation <= 315 -> 270
            else -> Surface.ROTATION_0
        }
        val isFront =
            characteristics?.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT

        val sensorOrientationDegrees =
            characteristics!!.get(CameraCharacteristics.SENSOR_ORIENTATION)!!

        value = if (isFront) {
            (sensorOrientationDegrees - deviceRotation + 360) % 360
        } else {
            (sensorOrientationDegrees + deviceRotation + 360) % 360
        }
//        Log.i(
//            "Camera2Module",
//            "camera=$sensorOrientationDegrees deviceRotation=$deviceRotation value=$value"
//        )
    }

    fun enable() {
        listener.enable()
    }

    fun disable() {
        listener.disable()
    }

}
