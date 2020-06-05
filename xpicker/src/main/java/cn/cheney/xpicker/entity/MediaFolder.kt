package cn.cheney.xpicker.entity

import android.net.Uri
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MediaFolder(
    var name: String = "",
    var path: String = "",
    var imageNum: Int = 0,
    var isCheck: Boolean = false,
    var firstImageUri: Uri?=null,
    var mediaList: ArrayList<MediaEntity> = arrayListOf()
) : Parcelable
