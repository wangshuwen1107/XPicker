package cn.cheney.xpicker.camera

import android.content.Context
import android.graphics.*
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.text.TextUtils
import android.util.Log
import android.util.Size
import androidx.lifecycle.LifecycleObserver
import cn.cheney.xpicker.util.CoordinateTransformer
import cn.cheney.xpicker.util.getBestOutputSize
import cn.cheney.xpicker.util.toDp


class Camera2Module : LifecycleObserver {

    private lateinit var cameraManager: CameraManager

    private lateinit var camera2Session: Camera2Session

    var cameraParamsHolder = CameraParamsHolder()

    companion object {
        private const val TAG = "Camera2Module"
    }

    fun init(context: Context) {
        cameraManager =
            context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        camera2Session = Camera2Session(context)
    }

    fun initCameraSize(facingBack: Boolean, surfaceSize: Size) {
        val cameraId = getCameraId(facingBack)
        if (TextUtils.isEmpty(cameraId)) {
            return
        }
        val characteristics = cameraManager.getCameraCharacteristics(cameraId!!)
        val streamConfigurationMap =
            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        cameraParamsHolder.characteristics = characteristics
        val videoOutputSizes = streamConfigurationMap?.getOutputSizes(MediaRecorder::class.java)
        val previewSizes = streamConfigurationMap?.getOutputSizes(SurfaceTexture::class.java)
        val photoSizes = streamConfigurationMap?.getOutputSizes(ImageFormat.JPEG)
        cameraParamsHolder.previewSize = getBestOutputSize(previewSizes, surfaceSize)
        cameraParamsHolder.videoSize = getBestOutputSize(videoOutputSizes, surfaceSize)
        cameraParamsHolder.photoSize = getBestOutputSize(photoSizes, surfaceSize)
        cameraParamsHolder.surfaceSize = surfaceSize
        cameraParamsHolder.isFront = !facingBack
    }


    fun startPreview(
        facingBack: Boolean,
        surfaceTexture: SurfaceTexture,
        errorCallback: () -> Unit
    ) {
        camera2Session.stopPreviewSession()
        val cameraId = getCameraId(facingBack)
        if (TextUtils.isEmpty(cameraId)) {
            errorCallback()
            return
        }
        val photoSize = cameraParamsHolder.photoSize
        if (null == photoSize) {
            errorCallback()
            Log.e(TAG, "startPreview but size not init please call initCameraSize")
            return
        }
        cameraManager.openCamera(cameraId!!, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                camera2Session.startPreviewSession(camera, surfaceTexture, photoSize)
            }

            override fun onDisconnected(camera: CameraDevice) {
                errorCallback()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                errorCallback()
            }
        }, CameraThreadManager.cameraHandler)
    }


    fun closeDevice() {
        camera2Session.stopPreviewSession()
    }

    fun takePhoto(orientation: Int, callback: TakePhotoCallback) {
        camera2Session.sendTakePhotoRequest(orientation, cameraParamsHolder.isFront, callback)
    }

    fun focus(afRect: RectF){
        getFocusRect(afRect)?.let {
            Log.i("Camera2Module", "autoFocus focusRect=$afRect cameraFocusRect=$it")
            camera2Session.sendFocusRequest(it)
        }
    }

    private fun getCameraId(facingBack: Boolean): String? {
        return cameraManager.cameraIdList.firstOrNull {
            val targetCharacteristics = if (facingBack) {
                CameraCharacteristics.LENS_FACING_BACK
            } else {
                CameraCharacteristics.LENS_FACING_FRONT
            }
            cameraManager.getCameraCharacteristics(it)
                .get(CameraCharacteristics.LENS_FACING) == targetCharacteristics
        }
    }



    private fun getFocusRect(focusRect:RectF): Rect? {
        val surfaceSize = cameraParamsHolder.surfaceSize ?: return null
        val previewRect = RectF(0f, 0f, surfaceSize.width.toFloat(), surfaceSize.height.toFloat())
        val result = CoordinateTransformer(cameraParamsHolder.characteristics!!, previewRect).toCameraSpace(
                focusRect
            )
        return result
    }


    class CameraParamsHolder {
        var surfaceSize: Size? = null
        var previewSize: Size? = null
        var photoSize: Size? = null
        var videoSize: Size? = null
        var characteristics: CameraCharacteristics? = null
        var isFront = true
    }

}