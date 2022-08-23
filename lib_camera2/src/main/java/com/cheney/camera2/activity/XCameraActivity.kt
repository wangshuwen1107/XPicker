package com.cheney.camera2.activity

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cheney.camera2.R
import com.cheney.camera2.callback.CameraSaveCallback
import com.cheney.camera2.callback.CaptureListener
import com.cheney.camera2.callback.TakePhotoCallback
import com.cheney.camera2.core.CameraThreadManager
import com.cheney.camera2.core.XMediaPlayer
import com.cheney.camera2.entity.CameraRequest
import com.cheney.camera2.entity.CaptureType
import com.cheney.camera2.util.FileUtil
import com.cheney.camera2.util.FileUtil.scanPhotoAlbum
import com.cheney.camera2.util.inRange
import com.cheney.camera2.view.PreviewView
import kotlinx.android.synthetic.main.ch_camera2_activity_camera.*
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class XCameraActivity : AppCompatActivity() {

    private var isBackCamera: Boolean = true
    private var videoTextureView: TextureView? = null
    private var hasPauseVideo = false
    private var videoSurface: Surface? = null

    private var videoUri: Uri? = null
    private var coverUri: Uri? = null
    private var duration: Int? = null
    private var videoFile: File? = null

    private var photoFile: File? = null
    private var cameraRequest: CameraRequest? = null

    private var recordTime: Long = 0


    private val xMediaPlayer: XMediaPlayer by lazy {
        XMediaPlayer(this)
    }

    private val goneFocusViewRunnable: Runnable = object : Runnable {
        override fun run() {
            if (isFinishing || isDestroyed) return
            camera_focus_iv.visibility = View.GONE
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ch_camera2_activity_camera)
        initConfig()
        initListener()
        camera_preview.bindLifecycle(lifecycle)
        camera_preview.setFacingBack(isBackCamera)
    }


    private fun initConfig() {
        cameraRequest = intent.getParcelableExtra(KEY_REQUEST)
        if (null == cameraRequest) {
            finish()
            return
        }
        isBackCamera = cameraRequest!!.backCamera
        camera_capture_layer.setMaxDuration(cameraRequest!!.maxRecordTime)
        camera_capture_layer.setMinDuration(cameraRequest!!.minRecordTime)
        camera_capture_layer.setCameraType(cameraRequest!!.captureMode)
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
        CameraThreadManager.mainHandler.removeCallbacks(goneFocusViewRunnable)
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
                showToast(getString(R.string.media_play_error))
                actionCancel()
            }
        })
        //聚焦
        camera_preview.listener = object : PreviewView.GestureListener {
            override fun onClick(x: Float, y: Float) {
                focus(x, y)
            }
        }
        //动作
        camera_capture_layer.setListener(object : CaptureListener() {
            override fun cancel() {
                delCacheFile()
                actionCancel()
            }

            override fun ok() {
                videoFile?.let {
                    //获取封面和时间
                    val coverAndDuration = FileUtil.getVideoAndDuration(it.absolutePath)
                    coverUri = if (coverAndDuration?.first == null) null else Uri.fromFile(coverAndDuration.first)
                    duration = coverAndDuration?.second
                    //添加到系统相册
                    scanPhotoAlbum(this@XCameraActivity, it)
                }
                photoFile?.let {
                    scanPhotoAlbum(this@XCameraActivity, it)
                }
                finish()
                callbackSuccess()
            }

            override fun takePictures() {
                this@XCameraActivity.takePhoto()
            }

            override fun recordShort(time: Long) {
                recordTime = time
                super.recordShort(time)
                showToast(getString(R.string.camera_recorder_too_short))
//                xpicker_camera_preview.stopRecording()
                camera_capture_layer.reset()
            }

            override fun recordEnd(time: Long) {
                super.recordEnd(time)
                recordTime = time
//                xpicker_camera_preview.stopRecording()
            }

        })
        //切换摄像头
        camera_switch_iv.setOnClickListener {
            toggleCamera()
        }
        //取消按钮
        camera_back_iv.setOnClickListener {
            finish()
            callbackFailed("USER_CANCEL")
        }
    }


    private fun callbackSuccess() {
        when (CaptureType.valueOf(cameraRequest!!.captureMode)) {
            CaptureType.ONLY_CAPTURE -> {
                if (null == photoFile) {
                    callbackFailed("PHOTO_FILE_EMPTY")
                } else {
                    cameraSaveCallback?.onTakePhotoSuccess(Uri.fromFile(photoFile!!))
                }
            }
            CaptureType.ONLY_RECORDER -> {
                if (null == videoUri) {
                    callbackFailed("VIDEO_FILE_EMPTY")
                } else {
                    cameraSaveCallback?.onVideoSuccess(coverUri, videoUri!!, duration)
                }
            }
            CaptureType.MIXED -> {
                if (null == photoFile && null == videoUri) {
                    callbackFailed("BOTH_FILE_EMPTY")
                } else if (null != photoFile) {
                    cameraSaveCallback?.onTakePhotoSuccess(Uri.fromFile(photoFile!!))
                } else {
                    cameraSaveCallback?.onVideoSuccess(coverUri, videoUri!!, duration)
                }

            }
        }
        cameraRequest = null
    }

    private fun callbackFailed(errorCode: String) {
        when (CaptureType.valueOf(cameraRequest!!.captureMode)) {
            CaptureType.ONLY_CAPTURE -> {
                cameraSaveCallback?.onTakePhotoFailed(errorCode)
            }
            CaptureType.ONLY_RECORDER -> {
                cameraSaveCallback?.onVideoFailed(errorCode)
            }
            CaptureType.MIXED -> {
                cameraSaveCallback?.onTakePhotoFailed(errorCode)
                cameraSaveCallback?.onVideoFailed(errorCode)
            }
        }
        cameraRequest = null
    }


    private fun delCacheFile() {
        videoFile?.deleteOnExit()
        videoFile = null
        photoFile?.deleteOnExit()
        photoFile = null
    }

    private fun actionCancel() {
        stopVideo()
        camera_back_iv.visibility = View.VISIBLE
        camera_switch_iv.visibility = View.VISIBLE
        camera_photo_preview_iv.visibility = View.GONE
        camera_capture_layer.reset()
    }

    private fun playVideo() {
        camera_switch_iv.visibility = View.GONE
        camera_video_layer.visibility = View.VISIBLE
        camera_back_iv.visibility = View.GONE

        videoTextureView = TextureView(this)
        val videoViewParam = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        videoViewParam.addRule(RelativeLayout.CENTER_IN_PARENT)
        camera_video_layer.addView(videoTextureView)
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
        camera_video_layer.removeAllViews()
        if (null != videoSurface) {
            videoSurface!!.release()
            videoSurface = null
        }
        camera_video_layer.visibility = View.GONE
        camera_switch_iv.visibility = View.VISIBLE
        camera_back_iv.visibility = View.VISIBLE
        xMediaPlayer.stop()
    }


    private fun toggleCamera() {
        isBackCamera = !isBackCamera
        camera_preview.setFacingBack(isBackCamera)
    }


    private fun takePhoto() {
        camera_preview.takePhoto(object : TakePhotoCallback() {
            override fun onSuccess(file: File) {
                safeUiThreadRun {
                    this@XCameraActivity.photoFile = file
                    camera_photo_preview_iv.visibility = View.VISIBLE
                    val photoBitmap = BitmapFactory.decodeFile(file.absolutePath)
                    camera_photo_preview_iv.setImageBitmap(photoBitmap)
                    camera_capture_layer.done()
                    camera_switch_iv.visibility = View.GONE
                    camera_back_iv.visibility = View.GONE
                }
            }

            override fun onFailed(errorCode: Int, errorMsg: String) {
                showToast(getString(R.string.camera_take_photo_error))
            }
        })
    }

    private fun focus(x: Float, y: Float) {
        val tranX = (x - camera_focus_iv.width / 2).inRange(
            0f,
            (camera_preview.width - camera_focus_iv.width).toFloat()
        )
        val tranY = (y - camera_focus_iv.height / 2).inRange(
            0f,
            (camera_preview.height - camera_focus_iv.height).toFloat()
        )
        camera_focus_iv.x = tranX
        camera_focus_iv.y = tranY
        camera_focus_iv.visibility = View.VISIBLE
        camera_preview.focus(x, y, camera_focus_iv.width) {
            CameraThreadManager.mainHandler.removeCallbacks(goneFocusViewRunnable)
            CameraThreadManager.mainHandler.postDelayed(goneFocusViewRunnable, 2000)
        }
    }

    private fun showToast(text: String) {
        safeUiThreadRun {
            Toast.makeText(this@XCameraActivity, text, Toast.LENGTH_SHORT).show()
        }
    }

    private fun safeUiThreadRun(block: () -> Unit) {
        if (isFinishing || isDestroyed) return
        block()
    }

    companion object {
        const val KEY_REQUEST = "cameraRequest"
        var cameraSaveCallback: CameraSaveCallback? = null
    }


}




