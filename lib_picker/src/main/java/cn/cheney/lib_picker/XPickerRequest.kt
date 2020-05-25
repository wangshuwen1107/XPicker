package cn.cheney.lib_picker

import android.content.Context
import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.StringDef
import cn.cheney.lib_picker.callback.CameraSaveCallback
import cn.cheney.lib_picker.camera.XCameraActivity

const val REQUEST_KEY = "xPicker_request"


const val ONLY_CAPTURE = "ONLY_CAPTURE"
const val ONLY_RECORDER = "ONLY_RECORDER"
const val MIXED = "MIXED"

@StringDef(ONLY_CAPTURE, MIXED, ONLY_RECORDER)
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
annotation class CameraType

const val CAMERA = "CAMERA"
const val PICKER = "PICKER"

@StringDef(PICKER, CAMERA)
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
annotation class ActionType

class XPickerRequest() : Parcelable {
    //摄像头类型
    @CameraType
    var captureMode: String = ONLY_CAPTURE

    //最小录制时间
    var minRecordTime = 2000

    //最大录制时间
    var maxRecordTime = 10000

    //默认摄像头 后置
    var defaultLensFacing: Int = 1

    //默认动作类型
    @ActionType
    var actionType = CAMERA

    constructor(parcel: Parcel) : this() {
        captureMode = parcel.readString().toString()
        minRecordTime = parcel.readInt()
        maxRecordTime = parcel.readInt()
        defaultLensFacing = parcel.readInt()
        actionType = parcel.readString().toString()
    }

    fun start(context: Context, callback: CameraSaveCallback? = null) {
        when (actionType) {
            CAMERA -> {
                val intent = Intent(context, XCameraActivity::class.java)
                intent.putExtra(REQUEST_KEY, this)
                context.startActivity(intent)
                XCameraActivity.cameraSaveCallback = callback
            }
            PICKER -> TODO()
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