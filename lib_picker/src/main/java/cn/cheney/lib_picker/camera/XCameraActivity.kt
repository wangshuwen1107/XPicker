package cn.cheney.lib_picker.camera

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.*
import android.view.TextureView.SurfaceTextureListener
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraX
import cn.cheney.lib_picker.*
import cn.cheney.lib_picker.callback.CaptureListener
import cn.cheney.lib_picker.media.XMediaPlayer
import kotlinx.android.synthetic.main.xpicker_activity_camera.*

class XCameraActivity : AppCompatActivity() {

    private lateinit var lensFacing: CameraX.LensFacing
    private var displayId: Int? = null
    private var videoUri: Uri? = null
    private var coverUri: Uri? = null
    private var duration: Int? = null
    private var videoTextureView: TextureView? = null
    private var hasPauseVideo = false
    private var videoSurface: Surface? = null

    private val xMediaPlayer: XMediaPlayer by lazy {
        XMediaPlayer(this)
    }

    private val cameraEngine: CameraEngine by lazy {
        CameraEngine(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.xpicker_activity_camera)
        xpicker_camera_preview.post {
            displayId = xpicker_camera_preview.display.displayId
            cameraEngine.initAndPreviewCamera(lensFacing, xpicker_camera_preview)
        }
        initConfig()
        initListener()
    }


    private fun initConfig() {
        val xPickerRequest = intent.getParcelableExtra<XPickerRequest>(REQUEST_KEY)
        if (null == xPickerRequest) {
            finish()
            return
        }
        lensFacing = when (xPickerRequest.defaultLensFacing) {
            FRONT -> {
                CameraX.LensFacing.FRONT
            }
            else -> {
                CameraX.LensFacing.BACK
            }
        }
        xpicker_camera_capture_layer.setMaxDuration(xPickerRequest.maxRecordTime)
        xpicker_camera_capture_layer.setMinDuration(xPickerRequest.minRecordTime)
        xpicker_camera_capture_layer.setCameraType(xPickerRequest.cameraType)
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
        cameraEngine.release()
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
                actionCancel()
            }

            override fun ok() {
                super.ok()
                TODO()
            }

            override fun takePictures() {
                super.takePictures()
                xpicker_camera_switch_iv.visibility = View.GONE
                xpicker_camera_back_iv.visibility = View.GONE

                cameraEngine.takePhoto {
                    if (it == null) {
                        Toast.makeText(
                            this@XCameraActivity,
                            getString(R.string.xpicker_take_photo_error),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        xpicker_camera_capture_layer.reset()
                    } else {
                        xpicker_camera_photo_preview_iv.visibility = View.VISIBLE
                        XPicker.onImageLoad(it, xpicker_camera_photo_preview_iv)
                        xpicker_camera_capture_layer.done()
                    }
                }
            }

            override fun recordStart() {
                super.recordStart()
                cameraEngine.startRecord { videoUri, coverUri, duration ->
                    this@XCameraActivity.videoUri = videoUri
                    this@XCameraActivity.coverUri = coverUri
                    this@XCameraActivity.duration = duration
                    if (null != videoUri) {
                        xpicker_camera_capture_layer.done()
                        playVideo()
                    } else {
                        xpicker_camera_capture_layer.reset()
                    }
                }
            }

            override fun recordShort(time: Long) {
                super.recordShort(time)
                Toast.makeText(
                    this@XCameraActivity,
                    getString(R.string.xpicker_recorder_too_short), Toast.LENGTH_SHORT
                )
                    .show()
                xpicker_camera_capture_layer.reset()
            }

            override fun recordEnd(time: Long) {
                super.recordEnd(time)
                cameraEngine.stopRecord()
            }

        })
        //切换摄像头
        xpicker_camera_switch_iv.setOnClickListener {
            lensFacing = if (lensFacing == CameraX.LensFacing.BACK) {
                CameraX.LensFacing.FRONT
            } else {
                CameraX.LensFacing.BACK
            }
            cameraEngine.initAndPreviewCamera(lensFacing, xpicker_camera_preview)
        }


        xpicker_camera_back_iv.setOnClickListener {
            finish()
        }

        xpicker_camera_preview.setOnTouchListener { v, event ->
            if (event.action != MotionEvent.ACTION_UP) {
                return@setOnTouchListener false
            }
            return@setOnTouchListener true
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


}



