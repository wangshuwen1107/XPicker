package cn.cheney.lib_picker

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import cn.cheney.lib_picker.callback.CameraSaveCallback
import cn.cheney.lib_picker.camera.XCameraActivity
import cn.cheney.lib_picker.picker.PickerActivity
import kotlinx.android.parcel.Parcelize

/**
 * @param captureMode           拍照模式
 * @param minRecordTime         录制最小时长（单位 毫秒）
 * @param maxRecordTime         录制最小时长（单位 毫秒）
 * @param defaultLensFacing     默认摄像头
 * @param actionType            拍照 XPickerConstant.CAMERA/浏览图片 XPickerConstant.PICKER
 * @param maxPickerNum          最大选择数量
 * @param browseType            浏览媒体类型 XPickerConstant.TYPE_IMAGE/XPickerConstant.TYPE_VIDEO
 *                              /XPickerConstant.TYPE_ALL
 *
 */
@Parcelize
data class XPickerRequest(
    var captureMode: String = XPickerConstant.ONLY_CAPTURE,
    var minRecordTime: Int = 2000,
    var maxRecordTime: Int = 10000,
    var defaultLensFacing: Int = 1,
    var actionType: String = XPickerConstant.CAMERA,
    var maxPickerNum: Int = 1,
    var browseType: Int = XPickerConstant.TYPE_IMAGE,
    var supportGif: Boolean = false
) : Parcelable {

    fun start(context: Context, callback: CameraSaveCallback? = null) {
        when (actionType) {
            XPickerConstant.CAMERA -> {
                val intent = Intent(context, XCameraActivity::class.java)
                intent.putExtra(XPickerConstant.REQUEST_KEY, this)
                context.startActivity(intent)
                XCameraActivity.cameraSaveCallback = callback
            }
            XPickerConstant.PICKER -> {
                val intent = Intent(context, PickerActivity::class.java)
                intent.putExtra(XPickerConstant.REQUEST_KEY, this)
                context.startActivity(intent)
            }
        }
    }


}