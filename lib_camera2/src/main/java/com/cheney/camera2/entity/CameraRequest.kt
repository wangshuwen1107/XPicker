package com.cheney.camera2.entity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CameraRequest(
    var captureMode: String = CaptureType.ONLY_CAPTURE.type,
    var minRecordTime: Int = 2000,
    var maxRecordTime: Int = 10000,
    var backCamera: Boolean = true,
) : Parcelable