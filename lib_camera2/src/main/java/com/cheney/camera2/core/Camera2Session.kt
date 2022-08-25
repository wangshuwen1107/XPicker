package com.cheney.camera2.core

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.util.Size
import android.view.Surface
import com.cheney.camera2.callback.TakePhotoCallback
import com.cheney.camera2.callback.VideoRecordCallback
import com.cheney.camera2.entity.CameraError
import com.cheney.camera2.util.FileUtil
import java.io.File


abstract class Camera2Session(private var context: Context) : BaseSession(),
    ImageReader.OnImageAvailableListener {

    private var currentSession: CameraCaptureSession? = null

    private var mPreviewBuilder: CaptureRequest.Builder? = null
    private var previewSurface: Surface? = null

    private var imageReader: ImageReader? = null

    private var videoRecorderBuilder: CaptureRequest.Builder? = null
    private var videoRecorder = XVideoRecorder(context)

    private var takePhotoRequest: TakePhotoRequest? = null

    private var photoSize: Size? = null

    inner class TakePhotoRequest {
        var mirrored: Boolean = false
        var orientation: Int = 0
        var callback: TakePhotoCallback? = null
    }

    fun setPreviewSurface(surfaceTexture: SurfaceTexture) {
        previewSurface = Surface(surfaceTexture)
    }

    @Synchronized
    fun startPreviewSession(photoSize: Size) {
        this.photoSize = photoSize
        imageReader = ImageReader.newInstance(photoSize.width, photoSize.height, ImageFormat.JPEG, 2)
        imageReader!!.setOnImageAvailableListener(this, CameraThreadManager.cameraHandler)
        val targets = mutableListOf(previewSurface, imageReader!!.surface)
        stopPreview()
        getCameraDevice()?.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                synchronized(this@Camera2Session) {
                    currentSession = session
                    sendPreviewRequest()
                }
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {

            }
        }, CameraThreadManager.cameraHandler)
    }


    @Synchronized
    fun startVideoRecorder(videoSize: Size, rotation: Int) {
        CameraThreadManager.cameraHandler.post {
            stopPreview()
            videoRecorder.init(videoSize, rotation)
            videoRecorder.getRecorderSurface()
            val targets = mutableListOf(previewSurface, videoRecorder.getRecorderSurface())
            getCameraDevice()?.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    synchronized(this@Camera2Session) {
                        currentSession = session
                        sendVideoPreviewRequest()
                        videoRecorder.start()

                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {

                }
            }, CameraThreadManager.cameraHandler)
        }
    }


    fun stopVideoRecorder(callback: VideoRecordCallback?) {
        CameraThreadManager.cameraHandler.post {
            stopPreview()
            videoRecorder.stop(callback)
            photoSize?.let { startPreviewSession(it) }
        }
    }

    @Synchronized
    fun sendFocusRequest(afRect: Rect, aeRect: Rect, callback: ((Boolean) -> Unit)?) {
        if (null == currentSession) {
            callback?.invoke(false)
            return
        }
        var temp = videoRecorderBuilder
        if (null == temp) {
            temp = mPreviewBuilder
        }
        if (null == temp) {
            callback?.invoke(false)
            return
        }
        temp.apply {
            setFocusRequest(this, afRect, aeRect)
            setRepeatingPreview(this, currentSession!!)
            capture(this, currentSession!!) { success ->
                callback?.invoke(success)
            }
        }
    }


    @Synchronized
    fun sendTakePhotoRequest(orientation: Int, mirrored: Boolean, callback: TakePhotoCallback) {
        if (null == currentSession || null == imageReader) {
            callbackTakePhoto(null)
            return
        }
        currentSession!!.stopRepeating()
        currentSession!!.abortCaptures()
        takePhotoRequest = TakePhotoRequest().apply {
            this.mirrored = mirrored
            this.callback = callback
            this.orientation = orientation
        }
        val builder = createCaptureRequest(imageReader!!.surface, orientation)
        builder?.apply {
            capture(this, currentSession!!) {
                sendPreviewRequest()
                if (!it) {
                    callbackTakePhoto(null)
                }
            }
        }
    }


    fun release() {
        currentSession?.close()
        currentSession = null
        imageReader?.setOnImageAvailableListener(null, CameraThreadManager.cameraHandler)
        imageReader = null
        mPreviewBuilder = null
        videoRecorderBuilder = null
    }


    @Synchronized
    private fun stopPreview() {
        currentSession?.close()
        currentSession = null
        mPreviewBuilder = null
        videoRecorderBuilder = null
    }


    @Synchronized
    private fun sendPreviewRequest() {
        if (null == previewSurface || null == currentSession) return
        mPreviewBuilder = createPreviewRequest(previewSurface!!)
        mPreviewBuilder?.let {
            setRepeatingPreview(it, currentSession!!)
        }
    }


    @Synchronized
    private fun sendVideoPreviewRequest() {
        if (null == previewSurface || null == currentSession) return
        videoRecorderBuilder =
            createVideoRequest(previewSurface!!, videoRecorder.getRecorderSurface())
        videoRecorderBuilder?.apply {
            setRepeatingPreview(this, currentSession!!)
        }
    }

    override fun onImageAvailable(reader: ImageReader?) {
        if (null == takePhotoRequest || null == reader) {
            return
        }
        val outputFile = FileUtil.saveImage(
            context, reader.acquireLatestImage(),
            takePhotoRequest!!.orientation,
            takePhotoRequest!!.mirrored
        )
        callbackTakePhoto(outputFile)
    }


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


}