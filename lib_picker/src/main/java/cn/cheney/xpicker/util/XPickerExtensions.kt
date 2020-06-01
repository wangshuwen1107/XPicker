package cn.cheney.xpicker.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import java.lang.reflect.Field
import java.text.SimpleDateFormat
import kotlin.math.abs

fun XAngelUtil.getSensorAngle(x: Float, y: Float): Int {
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

fun Int.toDp(): Int = (this / Resources.getSystem().displayMetrics.density).toInt()

fun Int.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

fun Context.getStatusBarHeight(): Int {
    var c: Class<*>? = null
    var obj: Any? = null
    var field: Field? = null
    var x = 0
    var sbar = 38 //默认为38，貌似大部分是这样的
    try {
        c = Class.forName("com.android.internal.R\$dimen")
        obj = c.newInstance()
        field = c.getField("status_bar_height")
        x = field[obj].toString().toInt()
        sbar = this.resources.getDimensionPixelSize(x)
    } catch (e1: java.lang.Exception) {
        e1.printStackTrace()
    }
    return sbar
}


fun  Context.getNavigationBarHeight(): Int {
    val resources: Resources = this.resources
    val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    return resources.getDimensionPixelSize(resourceId)
}

@SuppressLint("SimpleDateFormat")
private val msFormat = SimpleDateFormat("mm:ss")

fun timeParse(duration: Long): String? {
    var time: String? = ""
    if (duration > 1000) {
        time = timeParseMinute(duration)
    } else {
        val minute = duration / 60000
        val seconds = duration % 60000
        val second = Math.round(seconds.toFloat() / 1000).toLong()
        if (minute < 10) {
            time += "0"
        }
        time += "$minute:"
        if (second < 10) {
            time += "0"
        }
        time += second
    }
    return time
}


fun timeParseMinute(duration: Long): String? {
    return try {
        msFormat.format(duration)
    } catch (e: Exception) {
        e.printStackTrace()
        "0:00"
    }
}

