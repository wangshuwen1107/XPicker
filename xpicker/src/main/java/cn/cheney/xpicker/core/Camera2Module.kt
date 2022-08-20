package cn.cheney.xpicker.core

import android.content.Context
import android.graphics.*
import android.hardware.camera2.*
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.lifecycle.LifecycleObserver
import cn.cheney.xpicker.entity.CameraError
import cn.cheney.xpicker.util.XFileUtil
import cn.cheney.xpicker.util.getBestOutputSize
import java.io.File


class Camera2Module : LifecycleObserver {


    private lateinit var cameraManager: CameraManager

    private val mainHandler = Handler(Looper.getMainLooper())

    private val cameraThread = HandlerThread("CameraThread").apply { start() }

    private val cameraHandler = Handler(cameraThread.looper)

    private var currentSession: CameraCaptureSession? = null

    private var currentDevice: CameraDevice? = null

    private var imageReader: ImageReader? = null

    private var previewRequest: CaptureRequest? = null

    var cameraParamsHolder = CameraParamsHolder()

    abstract class TakePhotoCallback {
        open fun onSuccess(file: File) {}
        open fun onFailed(errorCode: Int, errorMsg: String) {}
    }

    companion object {
        private val TAG = "Camera2Module"
    }

    fun init(context: Context) {
        cameraManager = context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

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
    }


    fun startPreview(facingBack: Boolean, surface: Surface, errorCallback: () -> Unit) {
        stopPreview()
        val cameraId = getCameraId(facingBack)
        if (TextUtils.isEmpty(cameraId)) {
            errorCallback()
            return
        }
        cameraManager.openCamera(cameraId!!, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                startSession(camera, surface)
            }

            override fun onDisconnected(camera: CameraDevice) {
                errorCallback()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                val msg = when (error) {
                    ERROR_CAMERA_DEVICE -> "Fatal (device)"
                    ERROR_CAMERA_DISABLED -> "Device policy"
                    ERROR_CAMERA_IN_USE -> "Camera in use"
                    ERROR_CAMERA_SERVICE -> "Fatal (service)"
                    ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                    else -> "Unknown"
                }
                errorCallback()
            }
        }, cameraHandler)
    }


    fun stopPreview() {
        currentDevice?.close()
        currentDevice = null
        currentSession?.close()
        currentSession = null
    }


    fun takePhoto(context: Context, orientation: Int, mirrored: Boolean, callback: TakePhotoCallback) {
        if (null == currentSession || null == currentDevice || null == imageReader) {
            mainHandler.post {
                callback.onFailed(
                    CameraError.TAKE_PHOTO_INIT_ERROR.errorCode,
                    CameraError.TAKE_PHOTO_INIT_ERROR.errorMsg
                )
            }
            return
        }
        val captureRequestBuilder = currentDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
        captureRequestBuilder.addTarget(imageReader!!.surface)
        captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, orientation)
        imageReader?.setOnImageAvailableListener({
            Log.i(TAG, "takePhoto setOnImageAvailableListener mirrored=$mirrored orientation=$orientation")
            val image = it.acquireNextImage()
            val outputFile = XFileUtil.createFile(
                context.externalMediaDirs.first(),
                XFileUtil.FILENAME, XFileUtil.PHOTO_EXTENSION
            )
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining()).apply { buffer.get(this) }
            var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val matrix = Matrix()
            matrix.postRotate(orientation * 1.0f, bitmap.width * 1.0f / 2, bitmap.height * 1.0f / 2)
            if (mirrored) {
                matrix.postScale(-1f, 1f)
            }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            val saveSuccess = XFileUtil.saveBitmapFile(bitmap, outputFile)
            image.close()
            if (saveSuccess) {
                mainHandler.post { callback.onSuccess(outputFile) }
            }
        }, cameraHandler)

        currentSession?.apply {
            stopRepeating()
            capture(captureRequestBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    previewRequest?.let {
                        session.setRepeatingRequest(it, null, cameraHandler)
                    }
                }

                override fun onCaptureFailed(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    failure: CaptureFailure
                ) {
                    super.onCaptureFailed(session, request, failure)
                    mainHandler.post {
                        callback.onFailed(
                            CameraError.TAKE_PHOTO_ERROR.errorCode,
                            CameraError.TAKE_PHOTO_ERROR.errorMsg
                        )
                    }
                }
            }, cameraHandler)
        }

    }


    private fun startSession(camera: CameraDevice, surface: Surface) {
        val captureRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequest.addTarget(surface)
        val targets = mutableListOf(surface)
        cameraParamsHolder.photoSize?.let {
            imageReader = ImageReader.newInstance(it.width, it.height, ImageFormat.JPEG, 2)
            targets.add(imageReader!!.surface)
        }
        camera.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {

            override fun onConfigured(session: CameraCaptureSession) {
                captureRequest.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
                previewRequest = captureRequest.build()
                session.setRepeatingRequest(previewRequest!!, null, cameraHandler)
                currentDevice = camera
                currentSession = session
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {

            }
        }, cameraHandler)
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
        var previewSize: Size? = null
        var photoSize: Size? = null
        var videoSize: Size? = null
        var characteristics: CameraCharacteristics? = null
    }

}