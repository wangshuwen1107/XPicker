package com.cheney.camera2.activity

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cheney.camera2.R
import com.cheney.camera2.callback.CameraSaveCallback
import com.cheney.camera2.callback.CaptureUIListener
import com.cheney.camera2.callback.TakePhotoCallback
import com.cheney.camera2.callback.VideoRecordCallback
import com.cheney.camera2.core.CameraThreadManager
import com.cheney.camera2.entity.CameraRequest
import com.cheney.camera2.entity.CaptureType
import com.cheney.camera2.util.FileUtil
import com.cheney.camera2.util.FileUtil.scanPhotoAlbum
import com.cheney.camera2.util.inRange
import com.cheney.camera2.view.PreviewView
import com.cheney.camera2.view.VideoPlayView
import com.gyf.immersionbar.ImmersionBar
import kotlinx.android.synthetic.main.ch_camera2_activity_camera.*
import java.io.File
import java.util.*


class XCameraActivity : AppCompatActivity() {

    private var isBackCamera: Boolean = true

    private var videoUri: Uri? = null
    private var coverUri: Uri? = null
    private var duration: Int? = null
    private var videoFile: File? = null

    private var photoFile: File? = null
    private var cameraRequest: CameraRequest? = null

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
        ImmersionBar.with(this)
            .transparentStatusBar()
            .fitsSystemWindows(false)
            .transparentNavigationBar()
            .init()
        initConfig()
        initListener()
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

        camera_preview.bindLifecycle(lifecycle)
        camera_preview.setFacingBack(isBackCamera)
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
        //聚焦
        camera_preview.listener = object : PreviewView.PreviewUiListener {
            override fun onClick(x: Float, y: Float) {
                focus(x, y)
            }
        }
        //切换摄像头
        camera_switch_iv.setOnClickListener {
            toggleCamera()
        }
        //取消按钮
        camera_back_iv.setOnClickListener {
            finish()
            callbackFailed("USER_CANCEL")
        }
        //动作
        camera_capture_layer.setListener(object : CaptureUIListener() {
            override fun cancel() {
                delCacheFile()
                stopVideo()
                actionCancel()
            }

            override fun ok() {
                saveFileToSystemPhoto()
                finish()
                callbackSuccess()
            }

            override fun takePictures() {
                takePhoto()
            }

            override fun recordStart() {
                startRecorderVideo()
            }

            override fun recordShort(time: Long) {
                camera_capture_layer.reset()
                showToast(getString(R.string.camera_recorder_too_short))
                stopRecorderVideo(false)
            }

            override fun recordEnd(time: Long) {
                stopRecorderVideo(true)
            }

        })
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
        camera_back_iv.visibility = View.VISIBLE
        camera_switch_iv.visibility = View.VISIBLE
        camera_photo_preview_iv.visibility = View.GONE
        camera_capture_layer.reset()
    }


    private fun toggleCamera() {
        isBackCamera = !isBackCamera
        camera_preview.setFacingBack(isBackCamera)
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

    private fun takePhoto() {
        camera_preview.takePhoto(object : TakePhotoCallback() {
            override fun onSuccess(file: File) {
                safeUiThreadRun {
                    this@XCameraActivity.photoFile = file
                    val photoBitmap = BitmapFactory.decodeFile(file.absolutePath)
                    camera_photo_preview_iv.setImageBitmap(photoBitmap)
                    camera_photo_preview_iv.visibility = View.VISIBLE

                    camera_capture_layer.done()
                    camera_switch_iv.visibility = View.GONE
                    camera_back_iv.visibility = View.GONE
                }
            }

            override fun onFailed(errorCode: Int, errorMsg: String) {
                safeUiThreadRun { showToast(getString(R.string.camera_take_photo_error)) }
            }
        })
    }

    private fun startRecorderVideo() {
        camera_switch_iv.visibility = View.GONE
        camera_back_iv.visibility = View.GONE
        camera_preview.startVideoRecorder()
    }

    private fun stopRecorderVideo(needCallback: Boolean) {
        //提前打开预览模式
        camera_video_layer.visibility = View.VISIBLE
        camera_preview.stopVideoRecorder(if (needCallback) object : VideoRecordCallback {
            override fun onSuccess(file: File) {
                safeUiThreadRun {
                    videoFile = file
                    videoUri = Uri.fromFile(file)
                    camera_capture_layer.done()
                    playVideo()
                }
            }

            override fun onFailed() {
                safeUiThreadRun {
                    camera_capture_layer.reset()
                    showToast(getString(R.string.camera_recorder_error))
                }
            }
        } else null)
    }

    private fun playVideo() {
        camera_switch_iv.visibility = View.GONE
        camera_back_iv.visibility = View.GONE
        camera_video_layer.visibility = View.VISIBLE

        val videoView = VideoPlayView(this)
        camera_video_layer.addView(
            videoView, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        videoView.bindLifecycle(lifecycle)
        videoView.playVideo(videoUri!!)
        videoView.playErrorListener = {
            showToast(getString(R.string.media_play_error))
            stopVideo()
            actionCancel()
        }
    }

    private fun stopVideo() {
        camera_switch_iv.visibility = View.VISIBLE
        camera_back_iv.visibility = View.VISIBLE
        camera_video_layer.visibility = View.GONE
        camera_video_layer.removeAllViews()
    }

    private fun saveFileToSystemPhoto() {
        videoFile?.let {
            //获取封面和时间
            val coverAndDuration = FileUtil.getVideoAndDuration(it.absolutePath)
            coverUri =
                if (coverAndDuration?.first == null) null else Uri.fromFile(coverAndDuration.first)
            duration = coverAndDuration?.second
            //添加到系统相册
            scanPhotoAlbum(this@XCameraActivity, it)
        }
        photoFile?.let {
            scanPhotoAlbum(this@XCameraActivity, it)
        }
    }


    private fun showToast(text: String) {
        Toast.makeText(this@XCameraActivity, text, Toast.LENGTH_SHORT).show()
    }

    private fun safeUiThreadRun(block: () -> Unit) {
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            block()
        }

    }

    companion object {
        const val KEY_REQUEST = "cameraRequest"
        var cameraSaveCallback: CameraSaveCallback? = null
    }


}




