package cn.cheney.lib_picker

import android.net.Uri
import android.widget.ImageView

typealias ImageLoadListener = (fileUrl: Uri, iv: ImageView) -> Unit

object XPicker {

    val TAG: String = XPicker.javaClass.simpleName

    var imageLoadListener: ImageLoadListener? = null


    fun onImageLoad(fileUrl: Uri, iv: ImageView) {
        imageLoadListener?.invoke(fileUrl, iv)
    }




}