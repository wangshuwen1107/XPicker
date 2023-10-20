package cn.cheney.xpicker.entity

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize class MediaFolder : Parcelable {
    var firstImagePath: String = ""
    var name: String = ""
    var path: String = ""
    var imageNum: Int = 0
    var isCheck: Boolean = false
    var firstVideoThumbnailBitmap: Bitmap? = null
    var firstImageMineType: String? = null
    var mediaList: ArrayList<MediaEntity> = arrayListOf()
}
