package cn.cheney.lib_picker.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.VideoCapture
import androidx.camera.view.CameraView
import cn.cheney.lib_picker.R
import cn.cheney.lib_picker.XPicker
import cn.cheney.lib_picker.XPickerConstant
import cn.cheney.lib_picker.XPickerConstant.Companion.REQUEST_KEY
import cn.cheney.lib_picker.XPickerRequest
import cn.cheney.lib_picker.callback.CameraSaveCallback
import cn.cheney.lib_picker.callback.CaptureListener
import cn.cheney.lib_picker.media.XMediaPlayer
import kotlinx.android.synthetic.main.xpicker_activity_camera.*
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class XCameraActivity : AppCompatActivity() {

    private var lensFacing: Int = 1
    private var videoTextureView: TextureView? = null
    private var hasPauseVideo = false
    private var videoSurface: Surface? = null

    private var videoUri: Uri? = null
    private var coverUri: Uri? = null
    private var duration: Int? = null
    private var videoFile: File? = null

    private var photoFile: File? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var xPickerRequest: XPickerRequest? = null

    private var recordTime: Long = 0

    private val xMediaPlayer: XMediaPlayer by lazy {
        XMediaPlayer(this)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.xpicker_activity_camera)
        xpicker_camera_preview.captureMode = CameraView.CaptureMode.MIXED
        xpicker_camera_preview.isPinchToZoomEnabled = false
        xpicker_camera_preview.bindToLifecycle(this)
        initConfig()
        initListener()
        xpicker_camera_preview.captureMode
    }


    private fun initConfig() {
        xPickerRequest = intent.getParcelableExtra<XPickerRequest>(REQUEST_KEY)
        if (null == xPickerRequest) {
            finish()
            return
        }
        lensFacing = xPickerRequest!!.defaultLensFacing
        xpicker_camera_capture_layer.setMaxDuration(xPickerRequest!!.maxRecordTime)
        xpicker_camera_capture_layer.setMinDuration(xPickerRequest!!.minRecordTime)
        xpicker_camera_capture_layer.setCameraType(xPickerRequest!!.captureMode)
    }

    override fun onResume() {
        super.onResume()
        if (hasPauseVideo) {
            playVideo()
            hasPauseVideo = false
        }
    }


    override fun onPause() {
        super.onPause()
        if (xMediaPlayer.isPlaying()) {
            hasPauseVideo = true
        }
        stopVideo()
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdownNow()
    }


    override fun onBackPressed() {
        super.onBackPressed()
        callbackFailed("USER_CANCEL")
    }

    private fun initListener() {
        xMediaPlayer.setMediaListener(object : XMediaPlayer.MediaListener {
            override fun onPrepared(mediaPlayer: MediaPlayer?) {
                val displayMetrics = DisplayMetrics().also {
                    windowManager.defaultDisplay.getMetrics(it)
                }
                val width = displayMetrics.widthPixels

                val videoWidth = mediaPlayer!!.videoWidth
                val videoHeight = mediaPlayer.videoHeight

                val videoViewParam =
                    videoTextureView!!.layoutParams as RelativeLayout.LayoutParams
                videoViewParam.height = (videoHeight * 1.0f / videoWidth * width).toInt()
                videoViewParam.width = width

                videoViewParam.addRule(RelativeLayout.CENTER_IN_PARENT)

                videoTextureView!!.layoutParams = videoViewParam
            }

            override fun onCompleted() {

            }

            override fun onError() {
                runOnUiThread {
                    Toast.makeText(
                        this@XCameraActivity,
                        getString(R.string.xpicker_play_error), Toast.LENGTH_SHORT
                    ).show()
                    actionCancel()
                }
            }

        })
        //动作
        xpicker_camera_capture_layer.setListener(object : CaptureListener() {
            override fun cancel() {
                super.cancel()
                delCacheFile()
                actionCancel()
            }

            override fun ok() {
                super.ok()
                if (null != videoFile) {
                    //获取封面和时间
                    val coverAndDuration = getVideoAndDuration(videoFile!!.absolutePath)
                    this@XCameraActivity.coverUri =
                        if (coverAndDuration?.first == null) null else Uri.fromFile(
                            coverAndDuration.first
                        )
                    this@XCameraActivity.duration = coverAndDuration?.second
                    //添加到系统相册
                    scanPhotoAlbum(this@XCameraActivity, videoFile)
                }
                if (null != photoFile) {
                    scanPhotoAlbum(this@XCameraActivity, photoFile)
                }
                finish()
                callbackSuccess()
            }

            override fun takePictures() {
                super.takePictures()
                val photoFile = createFile(externalMediaDirs.first(), FILENAME, PHOTO_EXTENSION)
                xpicker_camera_preview.takePicture(photoFile, cameraExecutor, object :
                    ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        runOnUiThread {
                            if (!photoFile.exists()) {
                                Toast.makeText(
                                    this@XCameraActivity,
                                    getString(R.string.xpicker_take_photo_error),
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@runOnUiThread
                            }
                            this@XCameraActivity.photoFile = photoFile
                            xpicker_camera_photo_preview_iv.visibility = View.VISIBLE
                            XPicker.onImageLoad(
                                Uri.fromFile(photoFile),
                                xpicker_camera_photo_preview_iv
                            )
                            xpicker_camera_capture_layer.done()
                            xpicker_camera_switch_iv.visibility = View.GONE
                            xpicker_camera_back_iv.visibility = View.GONE
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        runOnUiThread {
                            Toast.makeText(
                                this@XCameraActivity,
                                getString(R.string.xpicker_take_photo_error),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                })
            }

            override fun recordStart() {
                super.recordStart()
                val videoFile = createFile(externalMediaDirs.first(), FILENAME, VIDEO_EXTENSION)
                xpicker_camera_preview.startRecording(videoFile, cameraExecutor,
                    object : VideoCapture.OnVideoSavedCallback {
                        override fun onVideoSaved(file: File) {
                            runOnUiThread {
                                if (!videoFile.exists() || recordTime < xPickerRequest!!.minRecordTime) {
                                    Log.e(XPicker.TAG, "record onVideoSaved too short del")
                                    videoFile.deleteOnExit()
                                    xpicker_camera_capture_layer.reset()
                                    return@runOnUiThread
                                }
                                Log.i(
                                    XPicker.TAG,
                                    "record onVideoSaved path=${videoFile.absolutePath}"
                                )
                                this@XCameraActivity.videoFile = videoFile
                                this@XCameraActivity.videoUri = Uri.fromFile(videoFile)
                                xpicker_camera_capture_layer.done()
                                playVideo()
                            }
                        }

                        override fun onError(
                            videoCaptureError: Int, message: String,
                            cause: Throwable?
                        ) {
                            Log.e(
                                XPicker.TAG,
                                "record onError videoCaptureError=${videoCaptureError} ,message=${message}"
                            )
                            runOnUiThread {
                                xpicker_camera_capture_layer.reset()
                            }

                        }

                    })
            }

            override fun recordShort(time: Long) {
                recordTime = time
                Log.e(XPicker.TAG, "record  recordShort time=${time} ")
                super.recordShort(time)
                Toast.makeText(
                    this@XCameraActivity,
                    getString(R.string.xpicker_recorder_too_short), Toast.LENGTH_SHORT
                ).show()
                xpicker_camera_preview.stopRecording()
                xpicker_camera_capture_layer.reset()
            }

            override fun recordEnd(time: Long) {
                super.recordEnd(time)
                recordTime = time
                xpicker_camera_preview.stopRecording()
            }

        })
        //切换摄像头
        xpicker_camera_switch_iv.setOnClickListener {
            xpicker_camera_preview.toggleCamera()
        }
        xpicker_camera_back_iv.setOnClickListener {
            finish()
            callbackFailed("USER_CANCEL")
        }
    }


    private fun callbackSuccess() {
        when (xPickerRequest!!.captureMode) {
            XPickerConstant.ONLY_CAPTURE -> {
                if (null == photoFile) {
                    callbackFailed("PHOTO_FILE_EMPTY")
                } else {
                    cameraSaveCallback?.onTakePhotoSuccess(Uri.fromFile(photoFile!!))
                }
            }
            XPickerConstant.ONLY_RECORDER -> {
                if (null == videoUri) {
                    callbackFailed("VIDEO_FILE_EMPTY")
                } else {
                    cameraSaveCallback?.onVideoSuccess(coverUri, videoUri!!, duration)
                }
            }
            XPickerConstant.MIXED -> {
                if (null == photoFile && null == videoUri) {
                    callbackFailed("BOTH_FILE_EMPTY")
                } else if (null != photoFile) {
                    cameraSaveCallback?.onTakePhotoSuccess(Uri.fromFile(photoFile!!))
                } else {
                    cameraSaveCallback?.onVideoSuccess(coverUri, videoUri!!, duration)
                }

            }
        }
        xPickerRequest = null
    }

    private fun callbackFailed(errorCode: String) {
        when (xPickerRequest!!.captureMode) {
            XPickerConstant.ONLY_CAPTURE -> {
                cameraSaveCallback?.onTakePhotoFailed(errorCode)
            }
            XPickerConstant.ONLY_RECORDER -> {
                cameraSaveCallback?.onVideoFailed(errorCode)
            }
            XPickerConstant.MIXED -> {
                cameraSaveCallback?.onTakePhotoFailed(errorCode)
                cameraSaveCallback?.onVideoFailed(errorCode)
            }
        }
        xPickerRequest = null
    }


    private fun delCacheFile() {
        if (null != videoFile) {
            videoFile!!.deleteOnExit()
            videoFile = null
        }
        if (null != photoFile) {
            photoFile!!.deleteOnExit()
            photoFile = null
        }
    }

    private fun actionCancel() {
        stopVideo()
        xpicker_camera_back_iv.visibility = View.VISIBLE
        xpicker_camera_switch_iv.visibility = View.VISIBLE
        xpicker_camera_photo_preview_iv.visibility = View.GONE
        xpicker_camera_capture_layer.reset()
    }

    private fun playVideo() {
        xpicker_camera_switch_iv.visibility = View.GONE
        xpicker_camera_video_layer.visibility = View.VISIBLE
        xpicker_camera_back_iv.visibility = View.GONE

        videoTextureView = TextureView(this)
        val videoViewParam = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        videoViewParam.addRule(RelativeLayout.CENTER_IN_PARENT)
        xpicker_camera_video_layer.addView(videoTextureView)
        videoTextureView!!.surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                videoSurface = Surface(surface)
                if (null != videoUri) {
                    xMediaPlayer.play(videoSurface, videoUri)
                }
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                videoSurface = null
                stopVideo()
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
        if (null != videoSurface) {
            xMediaPlayer.play(videoSurface, videoUri)
        }
    }

    private fun stopVideo() {
        xpicker_camera_video_layer.removeAllViews()
        if (null != videoSurface) {
            videoSurface!!.release()
            videoSurface = null
        }
        xpicker_camera_video_layer.visibility = View.GONE
        xpicker_camera_switch_iv.visibility = View.VISIBLE
        xpicker_camera_back_iv.visibility = View.VISIBLE
        xMediaPlayer.stop()
    }

    companion object {
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val VIDEO_EXTENSION = ".mp4"

        /** Helper function used to create a timestamped file */
        private fun createFile(baseFolder: File, format: String, extension: String) =
            File(
                baseFolder, SimpleDateFormat(format, Locale.CHINA)
                    .format(System.currentTimeMillis()) + extension
            )

        private fun getVideoAndDuration(videoPath: String): Pair<File?, Int>? {
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
                return Pair(null, duration.toInt())
            }
            saveBitmapFile(bitmap, File(replace))
            return Pair(File(replace), duration.toInt())
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

        private fun scanPhotoAlbum(context: Context, dataFile: File?) {
            if (dataFile == null) {
                return
            }
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                dataFile.absolutePath.substring(
                    dataFile.absolutePath.lastIndexOf(".") + 1
                )
            )
            MediaScannerConnection.scanFile(
                context,
                arrayOf(dataFile.absolutePath),
                arrayOf(mimeType),
                null
            )
        }

        var cameraSaveCallback: CameraSaveCallback? = null
    }


}




