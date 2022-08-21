package cn.cheney.xpicker.view

import android.content.Context
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import cn.cheney.xpicker.camera.Camera2Module
import cn.cheney.xpicker.camera.CameraThreadManager
import cn.cheney.xpicker.camera.TakePhotoCallback
import cn.cheney.xpicker.util.OrientationWatcher
import cn.cheney.xpicker.util.toDp
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class PreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AutoTextureView(context, attrs, defStyle), LifecycleObserver {

    private var facingBack = true
    private var isPreviewIng = AtomicBoolean(false)
    private var lifecycleWk = WeakReference<Lifecycle?>(null)
    private var previewSurfaceTexture: SurfaceTexture? = null
    private var previewSurfaceSize: Size? = null

    private val cameraEngine = Camera2Module()
    private var relativeOrientation: OrientationWatcher


    var listener: GestureListener? = null

    interface GestureListener {
        fun onClick(x: Float, y: Float)
    }

    init {
        cameraEngine.init(getContext())
        relativeOrientation = OrientationWatcher(getContext())
        relativeOrientation.enable()
        surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surfaceTexture: SurfaceTexture?,
                width: Int,
                height: Int
            ) {
                previewSurfaceTexture = surfaceTexture
                previewSurfaceSize = Size(width, height)
                startPreview()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture?,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                Log.i("PreviewView", "onSurfaceTextureDestroyed ")
                previewSurfaceTexture = null
                previewSurfaceSize = null
                isPreviewIng.set(false)
                cameraEngine.closeDevice()
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
            }
        }
    }

    private var mDownTime: Long = 0
    private var mTouchTime: Long = 0
    private var mDownX = 0f
    private var mDownY = 0f

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event!!.action) {
            MotionEvent.ACTION_DOWN -> {
                mDownTime = System.currentTimeMillis()
                mDownX = event.x
                mDownY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
            }
            MotionEvent.ACTION_UP -> {
                mTouchTime = System.currentTimeMillis() - mDownTime
                val distanceX: Float = event.x - mDownX
                val distanceY: Float = event.y - mDownY
                if (abs(distanceX) < 54 && Math.abs(distanceY) < 54 && mTouchTime < 200) {
                    listener?.onClick(event.x, event.y)
                }
            }
        }
        return true
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

    fun takePhoto(callback: TakePhotoCallback) {
        cameraEngine.takePhoto(relativeOrientation.value, callback)
    }

    fun autoFocus(x: Float, y: Float) {
        if (null == previewSurfaceSize) {
            return
        }
        val screenW = previewSurfaceSize!!.width
        val screenH = previewSurfaceSize!!.height
        val focusWidth = 50.toDp().toFloat()
        val focusHeight = 50.toDp().toFloat()

        val focusRect = RectF(
            x - focusWidth,
            y - focusHeight,
            x + focusWidth,
            y + focusHeight
        )
        cameraEngine.focus(focusRect)
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
        if (null == previewSurfaceTexture || null == previewSurfaceSize || isPreviewIng.get()) {
            return
        }
        isPreviewIng.set(true)
        cameraEngine.initCameraSize(facingBack, previewSurfaceSize!!)
        cameraEngine.cameraParamsHolder.previewSize?.let { previewSize ->
            previewSurfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)
        }
        cameraEngine.startPreview(facingBack, previewSurfaceTexture!!) {
            isPreviewIng.set(false)
        }
        cameraEngine.cameraParamsHolder.characteristics?.let {
            relativeOrientation.characteristics = it
        }
    }


}