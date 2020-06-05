package cn.cheney.xpicker.entity

import android.os.Parcelable
import cn.cheney.xpicker.XPickerConstant
import kotlinx.android.parcel.Parcelize

/**
 * @param captureMode           拍照模式
 * @param minRecordTime         录制最小时长（单位 毫秒）
 * @param maxRecordTime         录制最小时长（单位 毫秒）
 * @param defaultLensFacing     默认摄像头
 * @param actionType            拍照
 * @param maxPickerNum          最大选择数量
 * @param mineType              浏览媒体类型
 *
 */
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
    var spanCount: Int = 4
) : Parcelable