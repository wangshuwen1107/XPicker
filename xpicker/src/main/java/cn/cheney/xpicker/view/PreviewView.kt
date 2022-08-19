package cn.cheney.xpicker.view

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import cn.cheney.xpicker.core.Camera2Module
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

class PreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AutoTextureView(context, attrs, defStyle), LifecycleObserver {

    private var facingBack = true
    private var isPreviewIng = AtomicBoolean(false)

    private var lifecycleWk = WeakReference<Lifecycle?>(null)

    private var previewSurface: Surface? = null
    private var previewSurfaceTexture: SurfaceTexture? = null
    private var previewSurfaceSize: Size? = null

    private val cameraEngine = Camera2Module()


    init {
        cameraEngine.init(getContext())
        surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture?, width: Int, height: Int) {
                previewSurfaceTexture = surfaceTexture
                previewSurface = Surface(surfaceTexture)
                previewSurfaceSize = Size(width, height)
                startPreview()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                Log.i("PreviewView", "onSurfaceTextureDestroyed ")
                previewSurfaceTexture = null
                previewSurface = null
                previewSurfaceSize = null
                isPreviewIng.set(false)
                cameraEngine.stopPreview()
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
            }

        }
    }


    fun bindLifecycle(lifecycle: Lifecycle) {
        lifecycleWk = WeakReference(lifecycle)
        lifecycle.addObserver(this)
    }


    fun setFacingBack(facingBack: Boolean) {
        this.facingBack = facingBack
        startPreview()
    }

    fun takePhoto(callback: Camera2Module.TakePhotoCallback) {
        cameraEngine.takePhoto(context, callback)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun resume() {
        startPreview()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun destroy() {
        lifecycleWk.get()?.removeObserver(this)
    }


    private fun startPreview() {
        if (null == previewSurface || null == previewSurfaceSize || isPreviewIng.get()) {
            return
        }
        isPreviewIng.set(true)
        //设置预览大小
        cameraEngine.initCameraSize(facingBack, previewSurfaceSize!!)?.let { previewSize ->
            previewSurfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)
            setAspectRatio(previewSize.width, previewSize.height)
        }
        //开启预览
        cameraEngine.startPreview(facingBack, previewSurface!!) {
            isPreviewIng.set(false)
        }
    }

}