package cn.cheney.xpicker.entity

import android.os.Parcelable
import cn.cheney.xpicker.XPickerConstant
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PickerRequest(
    var captureMode: String = CaptureType.ONLY_CAPTURE.type,
    var minRecordTime: Int = 2000,
    var maxRecordTime: Int = 10000,
    var defaultLensFacing: Int = 1,
    var actionType: String,
    var maxPickerNum: Int = 1,
    var mineType: Int = 2,
    var supportGif: Boolean = false,
    var haveCameraItem: Boolean = false,
    var circleCrop: Boolean = false,
    var spanCount: Int = 4
) : Parcelable