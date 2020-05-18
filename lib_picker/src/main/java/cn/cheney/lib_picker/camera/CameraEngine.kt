package cn.cheney.lib_picker.camera

import android.annotation.SuppressLint
import android.content.Intent
import android.hardware.Camera
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.net.toFile
import cn.cheney.lib_picker.XAngelManager
import cn.cheney.lib_picker.XPicker
import cn.cheney.lib_picker.view.AutoFitPreviewBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

typealias TakePhotoCallback = (fileUrl: Uri?) -> Unit

class CameraEngine(private var context: AppCompatActivity) {

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var imageCapture: ImageCapture? = null

    private var videoCapture: VideoCapture? = null

    private var cameraId: CameraX.LensFacing? = null

    private val defaultRecordSize = Size(720, 480)

    private var surfaceView: View? = null

    /**
     * 初始化并且开启摄像头
     */
    @SuppressLint("RestrictedApi")
    fun initAndPreviewCamera(lensFacing: CameraX.LensFacing, view: TextureView) {
        this.cameraId = lensFacing
        this.surfaceView = view

        //size比列获取
        val metrics = DisplayMetrics().also { view.display.getRealMetrics(it) }
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        val rotation = view.display.rotation
        //预览
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetAspectRatio(screenAspectRatio)
            setLensFacing(lensFacing)
            setTargetRotation(rotation)
        }.build()
        val preview = AutoFitPreviewBuilder.build(previewConfig, view)

        //照相
        val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setTargetAspectRatio(screenAspectRatio)
            setLensFacing(lensFacing)
            setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
            setTargetRotation(view.display.rotation)
        }.build()

        imageCapture = ImageCapture(imageCaptureConfig)

        //录制
        val videoCaptureConfig = VideoCaptureConfig.Builder().apply {
            setTargetAspectRatio(screenAspectRatio)
            setLensFacing(lensFacing)
            setVideoFrameRate(24)
            setTargetRotation(view.display.rotation)
        }.build()
        videoCapture = VideoCapture(videoCaptureConfig)

        CameraX.unbindAll()
        CameraX.bindToLifecycle(context, preview, imageCapture, videoCapture)
        XAngelManager.registerSensorManager(context)
    }

    /**
     * 拍照
     */
    fun takePhoto(callback: TakePhotoCallback) {
        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = cameraId == CameraX.LensFacing.FRONT
        }
        val photoPath = createFile(context.externalMediaDirs.first(), FILENAME, PHOTO_EXTENSION)
        when (XAngelManager.sensorAngle) {
            0 -> imageCapture?.setTargetRotation(Surface.ROTATION_0)
            90 -> imageCapture?.setTargetRotation(Surface.ROTATION_270)
            180 -> imageCapture?.setTargetRotation(Surface.ROTATION_180)
            270 -> imageCapture?.setTargetRotation(Surface.ROTATION_90)
        }
        imageCapture?.takePicture(
            photoPath,
            metadata,
            cameraExecutor, object : ImageCapture.OnImageSavedListener {
                override fun onImageSaved(file: File) {
                    val savedUri = Uri.fromFile(file)
                    //添加到系统相册
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                        context.sendBroadcast(
                            Intent(Camera.ACTION_NEW_PICTURE, savedUri)
                        )
                    }
                    val mimeType = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(savedUri.toFile().extension)
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(savedUri.toFile().absolutePath),
                        arrayOf(mimeType)
                    ) { _, uri ->
                        Log.d(XPicker.TAG, "Image capture scanned into media store: $uri")
                    }
                    context.runOnUiThread {
                        callback.invoke(savedUri)
                    }
                }

                override fun onError(
                    imageCaptureError: ImageCapture.ImageCaptureError,
                    message: String,
                    cause: Throwable?
                ) {
                    context.runOnUiThread {
                        callback.invoke(null)
                    }
                }

            })
    }


    fun startRecord(): Boolean {

//        if (null == cameraId || null == surfaceView) {
//            return false
//        }
//        val characteristics = cameraManager.getCameraCharacteristics("${cameraId!!}")
//        val cameraConfig = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
//        var outPutSize: Size? = null
//        cameraConfig.getOutputSizes(MediaRecorder::class.java).forEach {
//            if (it.width == defaultRecordSize.width
//                && it.height == it.height
//            ) {
//                outPutSize = this@CameraEngine.defaultRecordSize
//                return@forEach
//            }
//        }
//        if (null == outPutSize) {
//            outPutSize = getPerfectSize(
//                cameraConfig.getOutputSizes(MediaRecorder::class.java).toList(),
//                surfaceView!!.width, surfaceView!!.height
//            )
//        }
//        Log.i(XPicker.TAG, "outPutSize =${outPutSize}")
//
//        recorder.apply {
//            reset()
//            val outPutFile =
//                createFile(context.externalMediaDirs.first(), FILENAME, VIDEO_EXTENSION)
//            setAudioSource(MediaRecorder.AudioSource.MIC)
//            setVideoSource(MediaRecorder.VideoSource.SURFACE)
//            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
//            setOutputFile(outPutFile.absolutePath)
//            setVideoEncodingBitRate(4 * 1024 * 1024)
//            setVideoSize(outPutSize!!.width, outPutSize!!.height)
//            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
//            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
//            prepare()
//            start()
//        }
        return true
    }


    fun release() {
        cameraExecutor.shutdownNow()
        XAngelManager.unregisterSensorManager(context)
    }


    private fun aspectRatio(width: Int, height: Int): AspectRatio {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }


    companion object {
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val VIDEO_EXTENSION = ".mp4"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
        /** Helper function used to create a timestamped file */
        private fun createFile(baseFolder: File, format: String, extension: String) =
            File(
                baseFolder, SimpleDateFormat(format, Locale.CHINA)
                    .format(System.currentTimeMillis()) + extension
            )
    }

}