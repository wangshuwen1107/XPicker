package cn.cheney.xpicker.entity

import android.net.Uri
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MediaEntity(
    var fileType: Int? = null,
    var mineType: String? = null,
    var duration: Int = 0,
    var localPath: String? = null,
    var compressLocalUri: Uri? = null,
    var cropPath: String? = null,
    var latitude: String? = null,
    var longitude: String? = null,
    var height: Int = 0,
    var width: Int = 0,
    var selected: Boolean = false,
    var selectedNum: Int = 0
) : Parcelable {
    companion object {
        //FileType
        const val FILE_TYPE_VIDEO = 1
        const val FILE_TYPE_IMAGE = 2
    }
}