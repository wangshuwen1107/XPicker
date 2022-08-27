package com.cheney.camera2.view

import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView
import kotlin.math.roundToInt

open class AutoFitTextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : TextureView(context, attrs, defStyle) {

    private var aspectRatio = 0f

    fun setAspectRatio(targetWidth: Int, targetHeight: Int) {
        aspectRatio = targetWidth.toFloat() / targetHeight
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        if (aspectRatio == 0f) {
            setMeasuredDimension(width, height)
        } else {
            val newWidth: Int
            val newHeight: Int
            val myViewRatio = width / height
            //说明目标的高度小-> 高度全屏 宽度缩放
            newWidth = width
            newHeight = (width / aspectRatio).roundToInt()
            setMeasuredDimension(newWidth, newHeight)
        }
    }


}
