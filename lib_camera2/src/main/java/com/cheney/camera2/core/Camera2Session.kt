package com.cheney.camera2.core

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.MeteringRectangle
import android.media.ImageReader
import android.util.Size
import android.view.Surface
import com.cheney.camera2.callback.TakePhotoCallback
import com.cheney.camera2.entity.CameraError
import com.cheney.camera2.util.FileUtil
import java.io.File


class Camera2Session(private var context: Context) : BaseSession(), ImageReader.OnImageAvailableListener {

    private var currentDevice: CameraDevice? = null
    private var imageReader: ImageReader? = null

    private var mPreviewBuilder: CaptureRequest.Builder? = null
    private var mCaptureBuilder: CaptureRequest.Builder? = null

    private var currentSession: CameraCaptureSession? = null
    private var surface: Surface? = null

    private var takePhotoRequest: TakePhotoRequest? = null

    inner class TakePhotoRequest {
        var mirrored: Boolean = false
        var orientation: Int = 0
        var callback: TakePhotoCallback? = null
    }

    fun startPreviewSession(camera: CameraDevice, surfaceTexture: SurfaceTexture, photoSize: Size) {
        this.currentDevice = camera
        this.surface = Surface(surfaceTexture)
        val targets = mutableListOf(surface)
        createImageReader(photoSize)
        targets.add(imageReader!!.surface)
        camera.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                currentSession = session
                sendPreviewRequest()
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {

            }
        }, CameraThreadManager.cameraHandler)
    }


    fun stopPreviewSession() {
        currentDevice?.close()
        currentDevice = null
        currentSession?.close()
        currentSession = null

        imageReader?.setOnImageAvailableListener(null, CameraThreadManager.cameraHandler)
        imageReader = null

        mPreviewBuilder = null
        mCaptureBuilder = null
    }

    fun sendFocusRequest(afRect: Rect, aeRect: Rect, callback: ((Boolean) -> Unit)?) {
        if (null == mPreviewBuilder || null == currentSession) {
            callback?.invoke(false)
            return
        }
        mPreviewBuilder?.apply {
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
            set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START)
            //聚焦区域
            set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(MeteringRectangle(afRect, 1000)))
            //曝光区域
            set(CaptureRequest.CONTROL_AE_REGIONS, arrayOf(MeteringRectangle(aeRect, 1000)))
            currentSession?.let {
                setRepeatingPreview(this, it)
                capture(this, it) { success ->
                    callback?.invoke(success)
                }
            }
        }
    }

    fun sendTakePhotoRequest(orientation: Int, mirrored: Boolean, callback: TakePhotoCallback) {
        if (null == currentSession) {
            callbackTakePhoto(null)
            return
        }
        currentSession!!.stopRepeating()
        currentSession!!.abortCaptures()

        this.takePhotoRequest = TakePhotoRequest().apply {
            this.mirrored = mirrored
            this.callback = callback
            this.orientation = orientation
        }
        //构建拍照的request
        val builder = getCaptureRequestBuilder()
        builder?.apply {
            set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
            set(CaptureRequest.JPEG_ORIENTATION, orientation)
            imageReader?.surface?.let {
                addTarget(it)
            }
            capture(this, currentSession!!) {
                if (it) {
                    sendPreviewRequest()
                } else {
                    callbackTakePhoto(null)
                }
            }
        }
    }


    override fun onImageAvailable(reader: ImageReader?) {
        if (null == takePhotoRequest || null == reader) {
            return
        }
        val outputFile =
            FileUtil.saveImage(
                context, reader.acquireLatestImage(),
                takePhotoRequest!!.orientation,
                takePhotoRequest!!.mirrored
            )
        callbackTakePhoto(outputFile)
    }


    /**
     * 回调拍照后的文件
     */
    private fun callbackTakePhoto(outputFile: File?) {
        CameraThreadManager.mainHandler.post {
            val callback = takePhotoRequest!!.callback
            takePhotoRequest = if (null != outputFile) {
                callback?.onSuccess(outputFile)
                null
            } else {
                callback?.onFailed(
                    CameraError.TAKE_PHOTO_ERROR.errorCode,
                    CameraError.TAKE_PHOTO_ERROR.errorMsg
                )
                null
            }
        }
    }

    /**
     * 发送预览请求
     */
    private fun sendPreviewRequest() {
        val builder = getPreviewRequestBuilder()
        builder?.apply {
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE)
            set(
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE
            )
            set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            surface?.let {
                addTarget(it)
            }
            currentSession?.let {
                setRepeatingPreview(builder, it)
            }
        }
    }


    private fun createImageReader(photoSize: Size) {
        if (null == imageReader) {
            this.imageReader = ImageReader.newInstance(
                photoSize.width,
                photoSize.height, ImageFormat.JPEG, 2
            )
            imageReader?.setOnImageAvailableListener(this, CameraThreadManager.cameraHandler)
        }
    }

    private fun getCaptureRequestBuilder(): CaptureRequest.Builder? {
        if (null == mCaptureBuilder) {
            mCaptureBuilder = currentDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        }
        return mCaptureBuilder
    }

    private fun getPreviewRequestBuilder(): CaptureRequest.Builder? {
        if (null == mPreviewBuilder) {
            mPreviewBuilder = currentDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        }
        return mPreviewBuilder
    }


}