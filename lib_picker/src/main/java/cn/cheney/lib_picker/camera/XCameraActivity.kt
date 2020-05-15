package cn.cheney.lib_picker.camera

import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import cn.cheney.lib_picker.R
import cn.cheney.lib_picker.XAngelManager
import cn.cheney.lib_picker.XPicker
import cn.cheney.lib_picker.view.CaptureLayer
import kotlinx.android.synthetic.main.xpicker_activity_camera.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class XCameraActivity : AppCompatActivity() {

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var imageCapture: ImageCapture? = null

    private var displayId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.xpicker_activity_camera)
        xpicker_camera_preview.post {
            displayId = xpicker_camera_preview.display.displayId
            initCamera()
        }
        initListener()
        XAngelManager.registerSensorManager(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        XAngelManager.unregisterSensorManager(this)
        cameraExecutor.shutdownNow()
    }

    private fun initListener() {
        xpicker_camera_capture_layer.setListener(object : CaptureLayer.CaptureListener {

            override fun onBackClick() {
                xpicker_camera_switch_iv.visibility = View.VISIBLE
                xpicker_camera_photo_show_iv.visibility = View.GONE
                xpicker_camera_capture_layer.normal()
            }

            override fun onDoneClick() {

            }

            override fun onLongClick() {
            }

            override fun onClick() {
                //隐藏切换按钮
                xpicker_camera_switch_iv.visibility = View.GONE
                takePhoto()
            }

        })
        xpicker_camera_switch_iv.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            initCamera()
        }
    }


    private fun initCamera() {
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            //size比列获取
            val metrics =
                DisplayMetrics().also { xpicker_camera_preview.display.getRealMetrics(it) }
            val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
            val rotation = xpicker_camera_preview.display.rotation

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
                .bindToLifecycle(this, cameraSelector, preview, imageCapture)

            preview.setSurfaceProvider(xpicker_camera_preview.createSurfaceProvider(camera.cameraInfo))

        }, ContextCompat.getMainExecutor(this))


    }


    private fun takePhoto() {
        //照片输出配置
        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
        }
        val photoPath = createFile(externalMediaDirs.first(), FILENAME, PHOTO_EXTENSION)
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
                        sendBroadcast(
                            Intent(android.hardware.Camera.ACTION_NEW_PICTURE, savedUri)
                        )
                    }
                    val mimeType = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(savedUri.toFile().extension)
                    MediaScannerConnection.scanFile(
                        this@XCameraActivity,
                        arrayOf(savedUri.toFile().absolutePath),
                        arrayOf(mimeType)
                    ) { _, uri ->
                        Log.d(TAG, "Image capture scanned into media store: $uri")
                    }

                    runOnUiThread {
                        xpicker_camera_photo_show_iv.visibility = View.VISIBLE
                        XPicker.onImageLoad(savedUri, xpicker_camera_photo_show_iv)
                        xpicker_camera_capture_layer.done()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    runOnUiThread {
                        Toast.makeText(this@XCameraActivity, exception.message, Toast.LENGTH_SHORT)
                            .show()
                        xpicker_camera_capture_layer.normal()
                    }
                }

            })
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    companion object {
        private const val TAG = "CameraXBasic"
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


