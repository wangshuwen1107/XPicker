package cn.cheney.lib_picker.camera

import android.content.Intent
import android.hardware.Camera
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import cn.cheney.lib_picker.XAngelManager
import cn.cheney.lib_picker.XPicker
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

    private var lensFacing: Int = 0


    /**
     * 初始化并且开启摄像头
     */
    fun initAndPreviewCamera(lensFacing: Int, view: PreviewView) {
        this.lensFacing = lensFacing
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            //size比列获取
            val metrics = DisplayMetrics().also { view.display.getRealMetrics(it) }
            val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
            val rotation = view.display.rotation
            //预览
            val preview = Preview.Builder()
                .setTargetRotation(rotation)
                .setTargetAspectRatio(screenAspectRatio)
                .build()
            //照相
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(XAngelManager.sensorAngle)
                .setTargetAspectRatio(screenAspectRatio)
                .build()
            cameraProvider.unbindAll()
            val camera = cameraProvider
                .bindToLifecycle(context, cameraSelector, preview, imageCapture)
            preview.setSurfaceProvider(view.createSurfaceProvider(camera.cameraInfo))
        }, ContextCompat.getMainExecutor(context))

        XAngelManager.registerSensorManager(context)
    }


    fun takePhoto(callback: TakePhotoCallback) {
        //照片输出配置
        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
        }
        val photoPath = createFile(context.externalMediaDirs.first(), FILENAME, PHOTO_EXTENSION)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoPath)
            .setMetadata(metadata)
            .build()
        when (XAngelManager.sensorAngle) {
            0 -> imageCapture?.targetRotation = Surface.ROTATION_0
            90 -> imageCapture?.targetRotation = Surface.ROTATION_270
            180 -> imageCapture?.targetRotation = Surface.ROTATION_180
            270 -> imageCapture?.targetRotation = Surface.ROTATION_90
        }
        imageCapture?.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(photoPath)
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

                override fun onError(exception: ImageCaptureException) {
                    context.runOnUiThread {
                        callback.invoke(null)
                    }
                }

            })
    }


    fun release() {
        cameraExecutor.shutdownNow()
        XAngelManager.unregisterSensorManager(context)
    }


    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }


    companion object {
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
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