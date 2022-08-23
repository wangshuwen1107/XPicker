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


    fun setAspectRatio(targetWidth: Int, targetHeight: Int) {
        post {
            // 相机选择的预览尺寸
            val cameraHeight: Int = targetWidth
            val cameraWidth: Int = targetHeight
            // 计算出将相机的尺寸 => View 的尺寸需要的缩放倍数
            val ratioPreview = cameraWidth.toFloat() / cameraHeight
            val ratioView = width.toFloat() / height
            val scaleX: Float
            val scaleY: Float
            if (ratioView < ratioPreview) {
                scaleX = ratioPreview / ratioView
                scaleY = 1f
            } else {
                scaleX = 1f
                scaleY = ratioView / ratioPreview
            }
            // 计算出 View 的偏移量
            val scaledWidth = width * scaleX
            val scaledHeight = height * scaleY
            val dx = (width - scaledWidth) / 2
            val dy = (height - scaledHeight) / 2
            val matrix = Matrix()
            matrix.postScale(scaleX, scaleY)
            matrix.postTranslate(dx, dy)
            setTransform(matrix)
//            Log.d("Camera2Module", "$width x $height")
        }

    }


}
