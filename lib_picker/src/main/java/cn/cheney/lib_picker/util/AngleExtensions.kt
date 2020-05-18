package cn.cheney.lib_picker.util

import android.util.Size
import cn.cheney.lib_picker.XAngelManager
import cn.cheney.lib_picker.camera.CameraEngine
import kotlin.math.abs


fun XAngelManager.getSensorAngle(x: Float, y: Float): Int {
    return if (abs(x) > abs(y)) {
        /**
         * 横屏倾斜角度比较大
         */
        if (x > 4) {
            /**
             * 左边倾斜
             */
            270
        } else if (x < -4) {
            /**
             * 右边倾斜
             */
            90
        } else {
            /**
             * 倾斜角度不够大
             */
            0
        }
    } else {
        if (y > 7) {
            /**
             * 左边倾斜
             */
            0
        } else if (y < -7) {
            /**
             * 右边倾斜
             */
            180
        } else {
            /**
             * 倾斜角度不够大
             */
            0
        }
    }
}

fun CameraEngine.getPerfectSize(sizeList: List<Size>, surfaceWidth: Int, surfaceHeight: Int): Size {
    val reqTmpWidth: Int = surfaceHeight
    val reqTmpHeight: Int = surfaceWidth
    //先查找preview中是否存在与surfaceView相同宽高的尺寸
    for (size in sizeList) {
        if (size.width == reqTmpWidth && size.height == reqTmpHeight) {
            return Size(size.width, size.height)
        }
    }
    // 得到与传入的宽高比最接近的size
    val reqRatio = reqTmpWidth.toFloat() / reqTmpHeight
    var curRatio: Float
    var deltaRatio: Float
    var deltaRatioMin = Float.MAX_VALUE
    var retSize: Size? = null
    for (size in sizeList) {
        curRatio = size.width.toFloat() / size.height
        deltaRatio = Math.abs(reqRatio - curRatio)
        if (deltaRatio < deltaRatioMin) {
            deltaRatioMin = deltaRatio
            retSize = size
        }
    }
    return Size(retSize!!.width, retSize.height)

}