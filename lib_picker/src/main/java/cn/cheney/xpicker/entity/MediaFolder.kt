package cn.cheney.xpicker.entity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MediaFolder(
    var name: String = "",
    var path: String = "",
    var imageNum: Int = 0,
    var isCheck: Boolean = false,
    var firstImagePath: String = "",
    var mediaList: ArrayList<MediaEntity> = arrayListOf()
) : Parcelable
