package cn.cheney.xpicker.activity

import android.annotation.SuppressLint
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
import cn.cheney.xpicker.R
import cn.cheney.xpicker.XPicker
import cn.cheney.xpicker.XPickerConstant
import cn.cheney.xpicker.XPickerConstant.Companion.REQUEST_KEY
import cn.cheney.xpicker.entity.PickerRequest
import cn.cheney.xpicker.callback.CameraSaveCallback
import cn.cheney.xpicker.callback.CaptureListener
import cn.cheney.xpicker.camera.Camera2Module
import cn.cheney.xpicker.camera.CameraThreadManager
import cn.cheney.xpicker.camera.TakePhotoCallback
import cn.cheney.xpicker.core.XMediaPlayer
import cn.cheney.xpicker.entity.CaptureType
import cn.cheney.xpicker.util.XFileUtil
import cn.cheney.xpicker.util.XFileUtil.scanPhotoAlbum
import cn.cheney.xpicker.view.PreviewView
import kotlinx.android.synthetic.main.xpicker_activity_camera.*
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
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var xPickerRequest: PickerRequest? = null

    private var recordTime: Long = 0


    private val xMediaPlayer: XMediaPlayer by lazy {
        XMediaPlayer(this)
    }


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.xpicker_activity_camera)
        initConfig()
        initListener()
        camera_preview.bindLifecycle(lifecycle)
        camera_preview.setFacingBack(isBackCamera)
    }


    private fun initConfig() {
        val bundle = intent.getBundleExtra(XPickerConstant.REQUEST_BUNDLE_KEY)
        xPickerRequest = bundle?.getParcelable(REQUEST_KEY)
        if (null == xPickerRequest) {
            finish()
            return
        }
        isBackCamera = xPickerRequest!!.backCamera
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
                showToast(getString(R.string.media_play_error))
                actionCancel()
            }
        })
        camera_preview.listener = object : PreviewView.GestureListener {
            override fun onClick(x: Float, y: Float) {
                camera_focus_iv.x = x - camera_focus_iv.width / 2
                camera_focus_iv.y = y - camera_focus_iv.height / 2
                camera_focus_iv.visibility = View.VISIBLE
                camera_preview.autoFocus(x, y)
                CameraThreadManager.mainHandler.postDelayed({
                    camera_focus_iv.visibility = View.GONE
                }, 2000)
            }


        }
        //动作
        xpicker_camera_capture_layer.setListener(object : CaptureListener() {
            override fun cancel() {
                delCacheFile()
                actionCancel()
            }

            override fun ok() {
                videoFile?.let {
                    //获取封面和时间
                    val coverAndDuration = XFileUtil.getVideoAndDuration(it.absolutePath)
                    this@XCameraActivity.coverUri =
                        if (coverAndDuration?.first == null) null else Uri.fromFile(coverAndDuration.first)
                    this@XCameraActivity.duration = coverAndDuration?.second
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
                xpicker_camera_capture_layer.reset()
            }

            override fun recordEnd(time: Long) {
                super.recordEnd(time)
                recordTime = time
//                xpicker_camera_preview.stopRecording()
            }

        })
        //切换摄像头
        xpicker_camera_switch_iv.setOnClickListener {
            toggleCamera()
        }
        //取消按钮
        xpicker_camera_back_iv.setOnClickListener {
            finish()
            callbackFailed("USER_CANCEL")
        }
    }


    private fun callbackSuccess() {
        when (CaptureType.valueOf(xPickerRequest!!.captureMode)) {
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
        xPickerRequest = null
    }

    private fun callbackFailed(errorCode: String) {
        when (CaptureType.valueOf(xPickerRequest!!.captureMode)) {
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
        xPickerRequest = null
    }


    private fun delCacheFile() {
        videoFile?.deleteOnExit()
        videoFile = null
        photoFile?.deleteOnExit()
        photoFile = null
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


    private fun toggleCamera() {
        isBackCamera = !isBackCamera
        camera_preview.setFacingBack(isBackCamera)
    }


    private fun takePhoto() {
        camera_preview.takePhoto(object : TakePhotoCallback() {
            override fun onSuccess(file: File) {
                safeUiThreadRun {
                    this@XCameraActivity.photoFile = file
                    xpicker_camera_photo_preview_iv.visibility = View.VISIBLE
                    XPicker.onImageLoad(
                        Uri.fromFile(file),
                        xpicker_camera_photo_preview_iv,
                        XPickerConstant.JPEG
                    )
                    xpicker_camera_capture_layer.done()
                    xpicker_camera_switch_iv.visibility = View.GONE
                    xpicker_camera_back_iv.visibility = View.GONE

                }
            }

            override fun onFailed(errorCode: Int, errorMsg: String) {
                showToast(getString(R.string.camera_take_photo_error))
            }
        })
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
        var cameraSaveCallback: CameraSaveCallback? = null
    }


}




