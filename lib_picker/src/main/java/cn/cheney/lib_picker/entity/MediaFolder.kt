package cn.cheney.lib_picker.entity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MediaFolder(
    var name: String = "",
    var path: String = "",
    var imageNum: Int = 0,
    var isCheck: Boolean = false,
    var firstImagePath: String = "",
    var mediaList: MutableList<MediaEntity> = mutableListOf()
) : Parcelable
