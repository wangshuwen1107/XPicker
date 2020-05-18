package cn.cheney.lib_picker.util

import android.content.Context
import android.content.res.Resources
import android.os.Build
import cn.cheney.lib_picker.XAngelManager
import java.util.concurrent.Executor
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
fun Context.mainExecutor(): Executor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    mainExecutor
} else {
    MainExecutor()
}

fun Int.toDp(): Int = (this / Resources.getSystem().displayMetrics.density).toInt()

fun Int.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()