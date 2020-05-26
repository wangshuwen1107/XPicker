package cn.cheney.lib_picker.entity

import android.os.Parcel
import android.os.Parcelable

data class MediaEntity(
    val fileType: Int,
    val name: String,
    val localPath: String,
    val localThumbnailPath: String?,
    val latitude: String?,
    val longitude: String?

) : Parcelable {
    constructor(source: Parcel) : this(
        source.readInt(),
        source.readString().toString(),
        source.readString().toString(),
        source.readString(),
        source.readString(),
        source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeInt(fileType)
        writeString(name)
        writeString(localPath)
        writeString(localThumbnailPath)
        writeString(latitude)
        writeString(longitude)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<MediaEntity> = object : Parcelable.Creator<MediaEntity> {
            override fun createFromParcel(source: Parcel): MediaEntity = MediaEntity(source)
            override fun newArray(size: Int): Array<MediaEntity?> = arrayOfNulls(size)
        }
    }
}