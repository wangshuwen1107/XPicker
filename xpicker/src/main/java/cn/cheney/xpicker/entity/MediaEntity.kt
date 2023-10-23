package cn.cheney.xpicker.entity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class MediaEntity(
    var localPath: String,
    var fileType: Int? = null,
    var mineType: String? = null,
    var duration: Long = 0,
    var compressPath: String? = null,
    var latitude: String? = null,
    var longitude: String? = null,
    var height: Int = 0,
    var width: Int = 0,
    var selected: Boolean = false,
    var selectedNum: Int = 0,
) : Parcelable {


    companion object {
        //FileType
        const val FILE_TYPE_VIDEO = 1
        const val FILE_TYPE_IMAGE = 2

        //RealMineType
        const val GIF = "image/gif"
        const val JPEG = "image/jpeg"
        const val PNG = "image/png"
        const val WEBP = "image/webp"
        const val MP4 = "video/mp4"

    }


}