package com.cheney.camera2.view

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.util.AttributeSet
import android.view.Surface
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.cheney.camera2.core.XMediaPlayer
import java.lang.ref.WeakReference

class VideoPlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AutoFitTextureView(context, attrs, defStyle), LifecycleObserver {

    private var hasPauseVideo = false

    private var videoSurface: Surface? = null

    private var lifecycleWk = WeakReference<Lifecycle?>(null)

    private val xMediaPlayer: XMediaPlayer = XMediaPlayer(getContext())

    private var videoUri: Uri? = null

    var playErrorListener: (() -> Unit)? = null

    init {
        surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surfaceTexture: SurfaceTexture?,
                width: Int,
                height: Int
            ) {
                videoSurface = Surface(surfaceTexture)
                playInner()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture?,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                videoSurface = null
                stopVideo()
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
            }
        }
        xMediaPlayer.setMediaListener(object : XMediaPlayer.MediaListener {
            override fun onPrepared(mediaPlayer: MediaPlayer?) {
                if (null == mediaPlayer) {
                    return
                }
                setAspectRatio(mediaPlayer.videoWidth, mediaPlayer.videoHeight)
                visibility = View.VISIBLE
            }

            override fun onCompleted() {

            }

            override fun onError() {
                playErrorListener?.invoke()
            }
        })

    }

    fun bindLifecycle(lifecycle: Lifecycle) {
        lifecycleWk = WeakReference(lifecycle)
        lifecycle.addObserver(this)
    }

    fun playVideo(uri: Uri) {
        this.videoUri = uri
        playInner()
    }


    fun stopVideo() {
        if (xMediaPlayer.isPlaying()) {
            hasPauseVideo = true
            xMediaPlayer.stop()
        }
        visibility = View.GONE
    }

    private fun playInner() {
        if (null != videoUri && null != videoSurface) {
            xMediaPlayer.play(videoSurface, videoUri)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun resume() {
        if (hasPauseVideo) {
            playInner()
            hasPauseVideo = false
        }
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private fun pause() {
        stopVideo()
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun destroy() {
        lifecycleWk.get()?.removeObserver(this)
    }


}