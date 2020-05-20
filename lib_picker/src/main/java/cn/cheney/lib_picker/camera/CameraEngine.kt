package cn.cheney.lib_picker.camera

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.Camera
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.util.Pair
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.net.toFile
import cn.cheney.lib_picker.XPicker
import cn.cheney.lib_picker.util.XAngelManager
import cn.cheney.lib_picker.view.AutoFitPreviewBuilder
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

typealias TakePhotoCallback = (fileUrl: Uri?) -> Unit
typealias RecordCallback = (videoUrl: Uri?, coverUrl: Uri?, duration: Int?) -> Unit

@SuppressLint("RestrictedApi")
class CameraEngine(private var context: AppCompatActivity) {

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var imageCapture: ImageCapture? = null

    private var videoCapture: VideoCapture? = null

    private var lensFacing: CameraX.LensFacing? = null

    private var surfaceView: View? = null

    private var isRecording = false


    /**
     * 初始化并且开启摄像头
     */
    fun initAndPreviewCamera(lensFacing: CameraX.LensFacing, view: TextureView) {
        this.lensFacing = lensFacing
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
            setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
            setTargetRotation(rotation)
        }.build()

        imageCapture = ImageCapture(imageCaptureConfig)

        //录制
        val videoCaptureConfig = VideoCaptureConfig.Builder().apply {
            setTargetAspectRatio(screenAspectRatio)
            setLensFacing(lensFacing)
            setVideoFrameRate(24)
            setTargetRotation(rotation)
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
            isReversedHorizontal = lensFacing == CameraX.LensFacing.FRONT
        }
        val photoPath = createFile(context.externalMediaDirs.first(), FILENAME, PHOTO_EXTENSION)
        when (XAngelManager.sensorAngle) {
            0 -> imageCapture?.setTargetRotation(Surface.ROTATION_0)
            90 -> imageCapture?.setTargetRotation(Surface.ROTATION_270)
            180 -> imageCapture?.setTargetRotation(Surface.ROTATION_180)
            270 -> imageCapture?.setTargetRotation(Surface.ROTATION_90)
        }
        Log.d(XPicker.TAG, "takePhoto is called ")
        imageCapture?.takePicture(
            photoPath,
            metadata,
            cameraExecutor, object : ImageCapture.OnImageSavedListener {
                override fun onImageSaved(file: File) {
                    val savedUri = Uri.fromFile(file)
                    context.runOnUiThread {
                        callback.invoke(savedUri)
                        Log.d(XPicker.TAG, "onImageSaved: $savedUri")
                    }

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

    /**
     * 录制视频
     */
    fun startRecord(callback: RecordCallback? = null): Boolean {
        if (isRecording) {
            return false
        }
        val outPutFile =
            createFile(context.externalMediaDirs.first(), FILENAME, VIDEO_EXTENSION)
        isRecording = true
        videoCapture?.startRecording(outPutFile, cameraExecutor,
            object : VideoCapture.OnVideoSavedListener {
                override fun onVideoSaved(file: File) {
                    val coverFile = getVideoAndDuration(file.absolutePath)?.first
                    var coverUrl: Uri? = null
                    if (null != coverFile) {
                        coverUrl = Uri.fromFile(coverFile)
                    }
                    context.runOnUiThread {
                        callback?.invoke(
                            Uri.fromFile(file),
                            coverUrl,
                            getVideoAndDuration(file.absolutePath)?.second
                        )
                    }
                }

                override fun onError(
                    videoCaptureError: VideoCapture.VideoCaptureError, message: String,
                    cause: Throwable?
                ) {
                    context.runOnUiThread {
                        callback?.invoke(null, null, null)
                    }
                }
            })
        return true
    }

    /**
     * 停止录制视频
     */
    fun stopRecord() {
        videoCapture?.stopRecording()
        isRecording = false
    }



    fun focus(){
    }

    fun release() {
        cameraExecutor.shutdownNow()
        XAngelManager.unregisterSensorManager(context)
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


        private fun aspectRatio(width: Int, height: Int): AspectRatio {
            val previewRatio = max(width, height).toDouble() / min(width, height)
            if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
                return AspectRatio.RATIO_4_3
            }
            return AspectRatio.RATIO_16_9
        }



        private fun getVideoAndDuration(videoPath: String): Pair<File, Int>? {
            if (TextUtils.isEmpty(videoPath)) {
                return null
            }
            if (!File(videoPath).exists()) {
                return null
            }
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(videoPath)
            val duration =
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val bitmap = mmr.getFrameAtTime(1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            val replace = videoPath.replace(".mp4", ".jpg")
            if (null == bitmap) {
                Log.e(XPicker.TAG, "firstFrame get Failed -------")
                return Pair.create(null, duration.toInt())
            }
            saveBitmapFile(bitmap, File(replace))
            return Pair.create(File(replace), duration.toInt())
        }

        private fun saveBitmapFile(bitmap: Bitmap, file: File) {
            try {
                val bos =
                    BufferedOutputStream(FileOutputStream(file))
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
                bos.flush()
                bos.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

    }

}