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
import com.cheney.camera2.callback.VideoRecordCallback
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

    private val camera2Module = Camera2Module()
    private var relativeOrientation: OrientationWatcher

    private var mDownTime: Long = 0
    private var mTouchTime: Long = 0
    private var mDownX = 0f
    private var mDownY = 0f
    var listener: PreviewUiListener? = null

    interface PreviewUiListener {
        fun onClick(x: Float, y: Float)
    }

    init {
        camera2Module.init(getContext())
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
                camera2Module.release()
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

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun resume() {
        startPreview()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun destroy() {
        camera2Module.release()
        relativeOrientation.disable()
        lifecycleWk.get()?.removeObserver(this)
    }


    fun setFacingBack(facingBack: Boolean) {
        this.facingBack = facingBack
        isPreviewIng.set(false)
        startPreview()
    }

    fun takePhoto(callback: TakePhotoCallback) {
        camera2Module.takePhoto(relativeOrientation.value, callback)
    }

    fun startVideoRecorder() {
        camera2Module.startVideoRecorder(relativeOrientation.value)
    }

    fun stopVideoRecorder(callback: VideoRecordCallback?) {
        camera2Module.stopVideoRecorder(callback)
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
        //聚焦矩阵
        val afRectF = getFocusRect(focusPointX, focusPointY, focusViewSize)
        //曝光矩阵
        val aeRectF = getFocusRect(focusPointX, focusPointY, focusViewSize)
        camera2Module.focus(afRectF, aeRectF, callback)
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
        camera2Module.initCameraSize(facingBack, viewSize!!)
        camera2Module.cameraParamsHolder.previewSize?.let { previewSize ->
            mSurfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)
            setAspectRatio(previewSize.height, previewSize.width)
        }
        camera2Module.startPreview(facingBack, mSurfaceTexture!!) {
            isPreviewIng.set(false)
        }
        camera2Module.cameraParamsHolder.characteristics?.let {
            relativeOrientation.characteristics = it
        }
    }


}