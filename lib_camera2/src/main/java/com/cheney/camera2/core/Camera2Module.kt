package com.cheney.camera2.core

import android.content.Context
import android.graphics.*
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.text.TextUtils
import android.util.Log
import android.util.Size
import androidx.lifecycle.LifecycleObserver
import com.cheney.camera2.callback.TakePhotoCallback
import com.cheney.camera2.callback.VideoRecordCallback
import com.cheney.camera2.util.CoordinateTransformer
import com.cheney.camera2.util.getBestOutputSize


class Camera2Module : LifecycleObserver {

    private lateinit var cameraManager: CameraManager

    private lateinit var camera2Session: Camera2Session

    private var cameraDevice: CameraDevice? = null

    var cameraParamsHolder = CameraParamsHolder()

    companion object {
        private const val TAG = "Camera2Module"
    }

    fun init(context: Context) {
        cameraManager =
            context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        camera2Session = object : Camera2Session(context) {
            override fun getCameraDevice(): CameraDevice? {
                return cameraDevice
            }
        }
    }

    /**
     * 初始化相机参数配置
     */
    fun initCameraSize(facingBack: Boolean, surfaceSize: Size) {
        val cameraId = getCameraId(facingBack)
        if (TextUtils.isEmpty(cameraId)) {
            return
        }
        val characteristics = cameraManager.getCameraCharacteristics(cameraId!!)
        val streamConfigurationMap =
            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
        cameraParamsHolder.characteristics = characteristics
        val videoOutputSizes = streamConfigurationMap.getOutputSizes(MediaRecorder::class.java)
        val previewSizes = streamConfigurationMap.getOutputSizes(SurfaceTexture::class.java)
        val photoSizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG)
        cameraParamsHolder.previewSize = getBestOutputSize(previewSizes, surfaceSize)
        cameraParamsHolder.videoSize = getBestOutputSize(videoOutputSizes, surfaceSize)
        cameraParamsHolder.photoSize = getBestOutputSize(photoSizes, surfaceSize)
        cameraParamsHolder.surfaceSize = surfaceSize
        cameraParamsHolder.isFront = !facingBack
        Log.i(TAG, "surfaceSize =${surfaceSize} ")
        Log.i(TAG, "previewSize =${cameraParamsHolder.previewSize} ")
        Log.i(TAG, "photoSize =${cameraParamsHolder.photoSize} ")
    }

    /**
     * 开启预览
     */
    fun startPreview(facingBack: Boolean, surfaceTexture: SurfaceTexture, errorCallback: () -> Unit) {
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
        camera2Session.setPreviewSurface(surfaceTexture)
        closeDevice()
        cameraManager.openCamera(cameraId!!, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                camera2Session.startPreviewSession(photoSize)
            }

            override fun onDisconnected(camera: CameraDevice) {
                errorCallback()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                errorCallback()
            }
        }, CameraThreadManager.cameraHandler)
    }

    /**
     * 照相
     */
    fun takePhoto(orientation: Int, callback: TakePhotoCallback) {
        camera2Session.sendTakePhotoRequest(orientation, cameraParamsHolder.isFront, callback)
    }

    /**
     * 开启视频录制
     */
    fun startVideoRecorder(orientation: Int) {
        val videoSize = cameraParamsHolder.videoSize
        if (null == videoSize || null == cameraDevice) {
            Log.e(TAG, "startVideoRecorder but size not init please call initCameraSize")
            return
        }
        camera2Session.startVideoRecorder(videoSize, orientation)
    }


    fun stopVideoRecorder(callback: VideoRecordCallback?) {
        camera2Session.stopVideoRecorder(callback)
    }


    /**
     * 聚焦
     */
    fun focus(afRect: RectF, aeRect: RectF, callback: ((Boolean) -> Unit)?) {
        val surfaceSize = cameraParamsHolder.surfaceSize
        if (null == surfaceSize) {
            callback?.invoke(false)
            return
        }
        val previewRect = RectF(0f, 0f, surfaceSize.width.toFloat(), surfaceSize.height.toFloat())
        val transformer = CoordinateTransformer(cameraParamsHolder.characteristics!!, previewRect)
        val transformAFRect = transformer.toCameraSpace(afRect)
        val transformAERect = transformer.toCameraSpace(afRect)
        Log.i(TAG, "autoFocus 聚焦原始矩形=$afRect  曝光原始矩形=$aeRect")
        camera2Session.sendFocusRequest(transformAFRect, transformAERect, callback)
    }


    /**
     * 关闭相机
     */
    fun closeDevice() {
        cameraDevice?.close()
        cameraDevice = null
    }

    fun release() {
        closeDevice()
        camera2Session.release()
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


    class CameraParamsHolder {
        var surfaceSize: Size? = null
        var previewSize: Size? = null
        var photoSize: Size? = null
        var videoSize: Size? = null
        var characteristics: CameraCharacteristics? = null
        var isFront = true
    }

}