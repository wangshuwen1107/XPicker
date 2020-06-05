package cn.cheney.xpicker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import cn.cheney.xpicker.activity.PickerActivity
import cn.cheney.xpicker.activity.XCameraActivity
import cn.cheney.xpicker.callback.CameraSaveCallback
import cn.cheney.xpicker.callback.SelectedCallback
import cn.cheney.xpicker.entity.ActionType
import cn.cheney.xpicker.entity.CaptureType
import cn.cheney.xpicker.entity.MineType
import cn.cheney.xpicker.entity.PickerRequest

typealias ImageLoadListener = (fileUrl: Uri, iv: ImageView) -> Unit

class XPicker private constructor() {

    private lateinit var request: PickerRequest


    fun maxRecordTime(arg: Int): XPicker {
        request.maxRecordTime = arg
        return this
    }

    fun minRecordTime(arg: Int): XPicker {
        request.minRecordTime = arg
        return this
    }

    fun defaultLensFacing(arg: Int): XPicker {
        request.defaultLensFacing = arg
        return this
    }


    fun captureMode(arg: CaptureType): XPicker {
        request.captureMode = arg.type
        return this
    }


    fun mineType(arg: MineType): XPicker {
        request.mineType = arg.type
        return this
    }


    fun maxPickerNum(arg: Int = 0): XPicker {
        request.maxPickerNum = arg
        return this
    }

    fun haveCameraItem(arg: Boolean = false): XPicker {
        request.haveCameraItem = arg
        return this
    }

    fun start(
        context: Context,
        cameraSaveCallback: CameraSaveCallback? = null,
        mediaSelectedCallback: SelectedCallback? = null
    ) {
        when (request.actionType) {
            ActionType.CAMERA.type -> {
                val intent = Intent(context, XCameraActivity::class.java)
                intent.putExtra(XPickerConstant.REQUEST_KEY, request)
                context.startActivity(intent)
                XCameraActivity.cameraSaveCallback = cameraSaveCallback
            }
            ActionType.PICKER.type-> {
                val intent = Intent(context, PickerActivity::class.java)
                intent.putExtra(XPickerConstant.REQUEST_KEY, request)
                context.startActivity(intent)
                PickerActivity.mediaSelectedCallback = mediaSelectedCallback
            }
            ActionType.CROP.type -> {
                val intent = Intent(context, PickerActivity::class.java)
                val bundle = Bundle()
                bundle.putParcelable(XPickerConstant.REQUEST_KEY, request)
                intent.putExtra("123",bundle)
                context.startActivity(intent)
                PickerActivity.mediaSelectedCallback = mediaSelectedCallback
            }
        }
    }


    companion object {
        var imageLoadListener: ImageLoadListener? = null

        fun ofPicker(): XPicker {
            val xPicker = XPicker()
            xPicker.request = PickerRequest(actionType = ActionType.PICKER.type)
            return xPicker
        }

        fun ofCamera(): XPicker {
            val xPicker = XPicker()
            xPicker.request = PickerRequest(actionType = ActionType.CAMERA.type)
            return xPicker
        }

        fun ofCrop(): XPicker {
            val xPicker = XPicker()
            xPicker.request = PickerRequest(actionType = ActionType.CROP.type)
            return xPicker
        }

        fun onImageLoad(fileUrl: Uri, iv: ImageView) {
            imageLoadListener?.invoke(fileUrl, iv)
        }
    }

}