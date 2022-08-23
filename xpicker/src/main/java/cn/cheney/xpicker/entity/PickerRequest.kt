package cn.cheney.xpicker.entity

import android.os.Parcelable
import com.cheney.camera2.entity.CameraRequest
import com.cheney.camera2.entity.CaptureType
import kotlinx.android.parcel.Parcelize

@Parcelize
class PickerRequest(var actionType: String = ActionType.PICKER.type) : Parcelable {
    var captureMode: String = CaptureType.ONLY_CAPTURE.type
    var minRecordTime: Int = 2000
    var maxRecordTime: Int = 10000
    var backCamera: Boolean = true
    var maxPickerNum: Int = 1
    var mineType: Int = 2
    var supportGif: Boolean = false
    var haveCameraItem: Boolean = false
    var circleCrop: Boolean = false
    var spanCount: Int = 4



    fun toCameraRequest():CameraRequest{
        val cameraRequest = CameraRequest()
        cameraRequest.backCamera = backCamera
        cameraRequest.captureMode = captureMode
        cameraRequest.maxRecordTime = maxRecordTime
        cameraRequest.minRecordTime = minRecordTime
        return cameraRequest
    }
}