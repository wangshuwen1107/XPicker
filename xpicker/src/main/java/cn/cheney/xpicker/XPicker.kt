package cn.cheney.xpicker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import cn.cheney.xpicker.activity.PickerActivity
import com.cheney.camera2.callback.CameraSaveCallback
import cn.cheney.xpicker.callback.CropCallback
import cn.cheney.xpicker.callback.SelectedCallback
import cn.cheney.xpicker.entity.ActionType
import cn.cheney.xpicker.entity.MineType
import cn.cheney.xpicker.entity.PickerRequest
import com.cheney.camera2.activity.XCameraActivity
import java.io.File

typealias ImageLoadListener = (file: File, iv: ImageView, mineType: String?) -> Unit

/**
 * @author Cheney
 * @since 2020.6.1
 * Builder class to ease Intent setup.
 */
class XPicker private constructor() {

    private lateinit var request: PickerRequest


    /**
     * set max record time （unit ms）
     */
    fun maxRecordTime(arg: Int): XPicker {
        request.maxRecordTime = arg
        return this
    }

    /**
     * set min record time （unit ms）
     */
    fun minRecordTime(arg: Int): XPicker {
        request.minRecordTime = arg
        return this
    }

    /**
     * set default camera id
     */
    fun defaultBackCamera(backCamera: Boolean): XPicker {
        request.backCamera = backCamera
        return this
    }

    /**
     * set a captureType to camera
     * @see CaptureType
     */
    fun captureMode(arg: com.cheney.camera2.entity.CaptureType): XPicker {
        request.captureMode = arg.type
        return this
    }


    /**
     * set load  media type
     * @see MineType
     */
    fun mineType(arg: MineType): XPicker {
        request.mineType = arg.type
        return this
    }

    /**
     * set max choose num to picker
     */
    fun maxPickerNum(arg: Int = 0): XPicker {
        request.maxPickerNum = arg
        return this
    }

    /**
     * whether they contain camera item in picker
     */
    fun haveCameraItem(arg: Boolean = false): XPicker {
        request.haveCameraItem = arg
        return this
    }


    /**
     * set the crop circle/square
     */
    fun circleCrop(arg: Boolean = false): XPicker {
        request.circleCrop = arg
        return this
    }

    fun start(
        context: Context,
        cameraSaveCallback: CameraSaveCallback? = null,
        selectedCallback: SelectedCallback? = null,
        cropCallback: CropCallback? = null
    ) {
        when (request.actionType) {
            ActionType.CAMERA.type -> {
                val intent = Intent(context, XCameraActivity::class.java)
                intent.putExtra(XCameraActivity.KEY_REQUEST, request.toCameraRequest())
                context.startActivity(intent)
                XCameraActivity.cameraSaveCallback = cameraSaveCallback
            }
            ActionType.PICKER.type -> {
                val intent = Intent(context, PickerActivity::class.java)
                intent.putExtra(XPickerConstant.REQUEST_KEY, request)
                context.startActivity(intent)
                PickerActivity.mediaSelectedCallback = selectedCallback
            }
            ActionType.CROP.type -> {
                val intent = Intent(context, PickerActivity::class.java)
                intent.putExtra(XPickerConstant.REQUEST_KEY, request)
                context.startActivity(intent)
                PickerActivity.cropCallback = cropCallback
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
            xPicker.request.mineType = MineType.TYPE_IMAGE_WITHOUT_GIF.type
            return xPicker
        }

        fun onImageLoad(file: File, iv: ImageView, mineType: String?) {
            imageLoadListener?.invoke(file, iv, mineType)
        }
    }

}