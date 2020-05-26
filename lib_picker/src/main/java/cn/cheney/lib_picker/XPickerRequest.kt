package cn.cheney.lib_picker

import android.content.Context
import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import cn.cheney.lib_picker.callback.CameraSaveCallback
import cn.cheney.lib_picker.camera.XCameraActivity
import cn.cheney.lib_picker.picker.PickerActivity

class XPickerRequest() : Parcelable {
    //摄像头类型
    var captureMode: String = XPickerConstant.ONLY_CAPTURE

    //最小录制时间
    var minRecordTime = 2000

    //最大录制时间
    var maxRecordTime = 10000

    //默认摄像头 后置
    var defaultLensFacing: Int = 1

    //默认动作类型
    var actionType = XPickerConstant.CAMERA

    constructor(parcel: Parcel) : this() {
        captureMode = parcel.readString().toString()
        minRecordTime = parcel.readInt()
        maxRecordTime = parcel.readInt()
        defaultLensFacing = parcel.readInt()
        actionType = parcel.readString().toString()
    }

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

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(captureMode)
        parcel.writeInt(minRecordTime)
        parcel.writeInt(maxRecordTime)
        parcel.writeInt(defaultLensFacing)
        parcel.writeString(actionType)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<XPickerRequest> {
        override fun createFromParcel(parcel: Parcel): XPickerRequest {
            return XPickerRequest(parcel)
        }

        override fun newArray(size: Int): Array<XPickerRequest?> {
            return arrayOfNulls(size)
        }
    }

}