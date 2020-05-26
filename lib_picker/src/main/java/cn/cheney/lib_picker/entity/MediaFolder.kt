package cn.cheney.lib_picker.entity

import android.os.Parcel
import android.os.Parcelable

data class MediaFolder(
    val name: String?,
    val path: String?,
    val imageNum: Int,
    val isCheck: Boolean,
    var mediaList: List<MediaEntity>
)