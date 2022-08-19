package cn.cheney.xpicker.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView
import kotlin.math.roundToInt

open class AutoTextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : TextureView(context, attrs, defStyle) {

    private var aspectRatio = 1f

    fun setAspectRatio(targetWidth: Int, targetHeight: Int) {
        aspectRatio = if (targetWidth <= targetHeight) {
            targetWidth * 1.0f / targetHeight
        } else {
            targetHeight * 1.0f / targetWidth
        }
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        if (aspectRatio == 1.0f) {
            setMeasuredDimension(width, height)
        } else {
            val newWidth: Int
            val newHeight: Int
            if (width < height * aspectRatio) {
                newHeight = height
                newWidth = (height * aspectRatio).roundToInt()
            } else {
                newWidth = width
                newHeight = (width / aspectRatio).roundToInt()
            }
            Log.d("AutoTextureView", "aspectRatio=$aspectRatio $newWidth x $newHeight")
            setMeasuredDimension(newWidth, newHeight)
        }

    }

}
