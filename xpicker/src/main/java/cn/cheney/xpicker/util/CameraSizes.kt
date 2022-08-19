package cn.cheney.xpicker.util

import android.graphics.Point
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Size
import android.view.Display
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class SmartSize(width: Int, height: Int) {
    var size = Size(width, height)
    var long = max(size.width, size.height)
    var short = min(size.width, size.height)
    override fun toString() = "SmartSize(${long}x${short})"
}

val SIZE_1080P: SmartSize = SmartSize(1920, 1080)

fun getDisplaySmartSize(display: Display): SmartSize {
    val outPoint = Point()
    display.getRealSize(outPoint)
    return SmartSize(outPoint.x, outPoint.y)
}

fun getBestOutputSize(allSizes: Array<Size>?, surfaceSize: Size): Size {
    if (allSizes.isNullOrEmpty()){
        return SIZE_1080P.size
    }
    var diff = Int.MAX_VALUE
    var diffMinIndex = 0
    val surfaceSmartSize = SmartSize(surfaceSize.width, surfaceSize.height)
    val validSizes = allSizes.map { SmartSize(it.width, it.height) }
    validSizes.forEachIndexed { index, it ->
        val currentDiff = abs(it.long - surfaceSmartSize.long) + abs(it.short - surfaceSmartSize.short)
        if (currentDiff == 0) {
            diffMinIndex = index
            return@forEachIndexed
        }
        if (currentDiff <= diff) {
            diff = currentDiff
            diffMinIndex = index
        }
    }
    return validSizes[diffMinIndex].size
}