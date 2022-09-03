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
import com.cheney.camera2.util.Logger
import java.io.File


abstract class Camera2Session(private var context: Context) : BaseSession(),
    ImageReader.OnImageAvailableListener {

    private var currentSession: CameraCaptureSession? = null

    private var previewBuilder: CaptureRequest.Builder? = null

    private var videoRecorderBuilder: CaptureRequest.Builder? = null

    private var previewSurfaceTexture: SurfaceTexture? = null

    private var imageReader: ImageReader? = null

    private var videoRecorder = XVideoRecorder(context)

    private var takePhotoRequest: TakePhotoRequest? = null

    private var photoSize: Size? = null

    inner class TakePhotoRequest {
        var mirrored: Boolean = false
        var orientation: Int = 0
        var callback: TakePhotoCallback? = null
    }

    fun setPreviewSurface(surfaceTexture: SurfaceTexture) {
        previewSurfaceTexture = surfaceTexture
    }

    @Synchronized
    fun startPreviewSession(photoSize: Size, callback: ((Boolean) -> Unit)? = null) {
        val cameraDevice = getCameraDevice()
        if (null == cameraDevice || null == previewSurfaceTexture) {
            Logger.e(
                "startPreviewSession cameraDevice=$cameraDevice " +
                        "previewSurfaceTexture=$previewSurfaceTexture "
            )
            callback?.invoke(false)
            return
        }
        this.photoSize = photoSize
        imageReader = ImageReader.newInstance(
            photoSize.width,
            photoSize.height,
            ImageFormat.JPEG,
            2
        )
        imageReader!!.setOnImageAvailableListener(this, CameraThreadManager.cameraHandler)
        val targets = mutableListOf(Surface(previewSurfaceTexture), imageReader!!.surface)
        stopPreview()
        cameraDevice.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                synchronized(this@Camera2Session) {
                    currentSession = session
                    val success = sendPreviewRequest()
                    CameraThreadManager.mainHandler.post { callback?.invoke(success) }

                }
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Logger.e("startPreviewSession create session failed ")
                CameraThreadManager.mainHandler.post { callback?.invoke(false) }
            }
        }, CameraThreadManager.cameraHandler)
    }


    @Synchronized
    fun startVideoRecorder(videoSize: Size, rotation: Int, callback: ((Boolean) -> Unit)? = null) {
        val cameraDevice = getCameraDevice()
        if (null == cameraDevice || null == previewSurfaceTexture) {
            Logger.e(
                "startVideoRecorder cameraDevice=$cameraDevice " +
                        "previewSurfaceTexture=$previewSurfaceTexture "
            )
            callback?.invoke(false)
            return
        }
        videoRecorder.setUp(videoSize, rotation)
        val recorderSurface = videoRecorder.getRecorderSurface()
        if (null == recorderSurface) {
            Logger.e(
                "startVideoRecorder videoRecorder getSurface NULL"
            )
            callback?.invoke(false)
            return
        }
        stopPreview()
        previewSurfaceTexture!!.setDefaultBufferSize(videoSize.width, videoSize.height)
        val targets = mutableListOf(Surface(previewSurfaceTexture), recorderSurface)
        cameraDevice.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    synchronized(this@Camera2Session) {
                        currentSession = session
                        val previewSuccess = sendVideoPreviewRequest(recorderSurface)
                        if (!previewSuccess) {
                            CameraThreadManager.mainHandler.post { callback?.invoke(false) }
                            return
                        }
                        videoRecorder.start()
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Logger.e("startVideoRecorder create session failed ")
                    CameraThreadManager.mainHandler.post { callback?.invoke(false) }
                }
            }, CameraThreadManager.cameraHandler
        )
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
            temp = previewBuilder
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
        previewBuilder = null
        videoRecorderBuilder = null
        previewSurfaceTexture = null
    }


    @Synchronized
    private fun stopPreview() {
        currentSession?.close()
        currentSession = null
        previewBuilder = null
        videoRecorderBuilder = null
    }


    @Synchronized
    private fun sendPreviewRequest(): Boolean {
        if (null == previewSurfaceTexture || null == currentSession) return false
        previewBuilder = createPreviewRequest(Surface(previewSurfaceTexture))
        return setRepeatingPreview(previewBuilder!!, currentSession!!)
    }


    @Synchronized
    private fun sendVideoPreviewRequest(videoSurface: Surface): Boolean {
        if (null == previewSurfaceTexture || null == currentSession) return false
        videoRecorderBuilder =
            createVideoRequest(Surface(previewSurfaceTexture), videoSurface)
        return setRepeatingPreview(videoRecorderBuilder!!, currentSession!!)
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