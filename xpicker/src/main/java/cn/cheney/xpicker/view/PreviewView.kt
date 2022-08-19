package cn.cheney.xpicker.view

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import cn.cheney.xpicker.core.Camera2Module
import cn.cheney.xpicker.util.OrientationWatcher
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

    private var relativeOrientation: OrientationWatcher


    init {
        cameraEngine.init(getContext())
        relativeOrientation = OrientationWatcher(getContext())
        relativeOrientation.enable()
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
        isPreviewIng.set(false)
        startPreview()
    }

    fun takePhoto(callback: Camera2Module.TakePhotoCallback) {
        val mirrored =
            cameraEngine.cameraParamsHolder.characteristics?.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_FRONT
        cameraEngine.takePhoto(context, relativeOrientation.value, mirrored, callback)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun resume() {
        startPreview()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun destroy() {
        relativeOrientation.disable()
        lifecycleWk.get()?.removeObserver(this)
    }


    private fun startPreview() {
        if (null == previewSurface || null == previewSurfaceSize || isPreviewIng.get()) {
            return
        }
        isPreviewIng.set(true)
        cameraEngine.initCameraSize(facingBack, previewSurfaceSize!!)
        cameraEngine.cameraParamsHolder.previewSize?.let { previewSize ->
            previewSurfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)
            setAspectRatio(previewSize.width, previewSize.height)
        }
        cameraEngine.startPreview(facingBack, previewSurface!!) {
            isPreviewIng.set(false)
        }
        cameraEngine.cameraParamsHolder.characteristics?.let {
            Log.i("Camera2Module", "startPreview back=${it.get(CameraCharacteristics.LENS_FACING) !=
                    CameraCharacteristics.LENS_FACING_FRONT}")
            relativeOrientation.characteristics = it
        }

    }

}