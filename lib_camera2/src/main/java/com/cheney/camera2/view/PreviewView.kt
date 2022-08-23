package com.cheney.camera2.view

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
import com.cheney.camera2.core.Camera2Module
import com.cheney.camera2.callback.TakePhotoCallback
import com.cheney.camera2.util.OrientationWatcher
import com.cheney.camera2.util.inRange
import com.cheney.camera2.util.isUsable
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class PreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AutoFitTextureView(context, attrs, defStyle), LifecycleObserver {

    private var facingBack = true
    private var isPreviewIng = AtomicBoolean(false)
    private var lifecycleWk = WeakReference<Lifecycle?>(null)
    private var mSurfaceTexture: SurfaceTexture? = null
    private var viewSize: Size? = null

    private val cameraEngine = Camera2Module()
    private var relativeOrientation: OrientationWatcher

    private var mDownTime: Long = 0
    private var mTouchTime: Long = 0
    private var mDownX = 0f
    private var mDownY = 0f
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
                this@PreviewView.mSurfaceTexture = surfaceTexture
                viewSize = Size(width, height)
                startPreview()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture?,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                mSurfaceTexture = null
                viewSize = null
                isPreviewIng.set(false)
                cameraEngine.closeDevice()
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
            }
        }
    }


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

    fun focus(focusPointX: Float, focusPointY: Float, focusViewSize: Int, callback: ((Boolean) -> Unit)?) {
        if (!viewSize.isUsable()) {
            callback?.invoke(false)
            return
        }
        if (width < 0 || height < 0) {
            callback?.invoke(false)
            return
        }
        //聚焦矩阵   设置比曝光小一点  整体是聚焦视图的1/3
        val afRectF = getFocusRect(focusPointX, focusPointY, focusViewSize)
        //曝光矩阵   整体是聚焦视图的1/2
        val aeRectF = getFocusRect(focusPointX, focusPointY, focusViewSize)
        cameraEngine.focus(afRectF, aeRectF, callback)
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


    private fun getFocusRect(focusPointX: Float, focusPointY: Float, areaSize: Int): RectF {
        val screenW = viewSize!!.width
        val screenH = viewSize!!.height
        val halfSize = areaSize / 2
        val left = (focusPointX - halfSize).inRange(0f, (screenW - areaSize).toFloat())
        val top = (focusPointY - halfSize).inRange(0f, (screenH - areaSize).toFloat())
        return RectF(left, top, left + halfSize, top + halfSize)

    }

    private fun startPreview() {
        if (null == mSurfaceTexture || null == viewSize || isPreviewIng.get()) {
            return
        }
        isPreviewIng.set(true)
        cameraEngine.initCameraSize(facingBack, viewSize!!)
        cameraEngine.cameraParamsHolder.previewSize?.let { previewSize ->
            mSurfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)
            setAspectRatio(previewSize.width, previewSize.height)
        }
        cameraEngine.startPreview(facingBack, mSurfaceTexture!!) {
            isPreviewIng.set(false)
        }
        cameraEngine.cameraParamsHolder.characteristics?.let {
            relativeOrientation.characteristics = it
        }
    }


}