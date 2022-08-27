package com.cheney.camera2.util

import android.graphics.Point
import android.util.Size
import android.view.Display
import kotlin.math.max
import kotlin.math.min

class SmartSize(width: Int, height: Int) {
    var size = Size(width, height)
    var long = max(size.width, size.height)
    var short = min(size.width, size.height)

    override fun toString() = "SmartSize(${long}x${short})"
}

fun getDisplaySmartSize(display: Display): SmartSize {
    val outPoint = Point()
    display.getRealSize(outPoint)
    return SmartSize(outPoint.x, outPoint.y)
}

fun getBestOutputSize(allSizes: Array<Size>, surfaceSize: Size, ratioBlock: (Int, Int) -> Boolean): Size {
    val surfaceSmartSize = SmartSize(surfaceSize.width, surfaceSize.height)
    val smartAllSize = allSizes.sortedWith(compareBy { it.height * it.width })
        .map { SmartSize(it.width, it.height) }
        .reversed()

    var ratioAllSize = smartAllSize.filter { ratioBlock(it.short, it.long) }
    //没有对应尺寸的
    if (ratioAllSize.isNullOrEmpty()) {
        ratioAllSize = smartAllSize
    }
    val beatSize =
        ratioAllSize.firstOrNull { it.long <= surfaceSmartSize.long && it.short <= surfaceSmartSize.short }
            ?: return ratioAllSize[0].size
    return beatSize.size
}

fun Size?.isUsable(): Boolean {
    return null != this && width > 0 && height > 0
}

fun Float.inRange(min: Float, max: Float): Float {
    if (this < min) {
        return min
    }
    if (this > max) {
        return max
    }
    return this
}