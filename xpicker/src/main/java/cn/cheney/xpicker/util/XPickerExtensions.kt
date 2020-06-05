package cn.cheney.xpicker.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import cn.cheney.xpicker.XPickerConstant
import java.io.File
import java.text.SimpleDateFormat


fun Int.toDp(): Int = (this / Resources.getSystem().displayMetrics.density).toInt()

fun Int.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()


fun File.getUir(context: Context): Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
    FileProvider.getUriForFile(context, XPickerConstant.FILE_PROVIDER, this)
} else {
    Uri.fromFile(this)
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

