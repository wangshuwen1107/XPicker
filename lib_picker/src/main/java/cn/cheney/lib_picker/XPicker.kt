package cn.cheney.lib_picker

import android.net.Uri
import android.widget.ImageView

typealias ImageLoadListener = (fileUrl: Uri, iv: ImageView) -> Unit

object XPicker {

    val TAG: String = XPicker.javaClass.simpleName

    const val ONLY_CAPTURE = 0x101 //只能拍照
    const val ONLY_RECORDER = 0x102 //只能录像
    const val CAPTURE_AND_RECORDER = 0x103 //两者都可以

    var imageLoadListener: ImageLoadListener? = null

    fun onImageLoad(fileUrl: Uri, iv: ImageView) {
        imageLoadListener?.invoke(fileUrl, iv)
    }




}