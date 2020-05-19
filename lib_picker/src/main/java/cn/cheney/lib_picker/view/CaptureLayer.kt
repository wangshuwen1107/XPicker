package cn.cheney.lib_picker.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import cn.cheney.lib_picker.R
import cn.cheney.lib_picker.callback.CaptureListener

class CaptureLayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var doneLayer: LinearLayout
    private lateinit var rootLayer: View
    private lateinit var doneIv: ImageView
    private lateinit var cancelIv: ImageView
    private lateinit var captureBtnLayer: ViewGroup
    private lateinit var captureBtn: CaptureButton
    private var listener: CaptureListener? = null

    init {
        initView()
    }

    private fun initView() {
        val rootView =
            View.inflate(context, R.layout.xpicker_layer_record_capture, null)
        rootLayer = rootView.findViewById(R.id.common_capture_root)
        captureBtnLayer = rootView.findViewById(R.id.capture_action_btn)
        doneLayer = rootView.findViewById(R.id.common_capture_done_layer)
        doneIv = rootView.findViewById(R.id.common_capture_done_iv)
        cancelIv = rootView.findViewById(R.id.common_capture_cancel_iv)
        captureBtn = rootView.findViewById(R.id.capture_action_btn)

        captureBtn.listener = (object : CaptureListener() {
            override fun takePictures() {
                listener?.takePictures()
            }

            override fun recordShort(time: Long) {
                captureBtn.resetSate()
                listener?.recordShort(time)
            }

            override fun recordTime(time: Long) {
                listener?.recordTime(time)
            }

            override fun recordStart() {
                listener?.recordStart()
                doneLayer.visibility = View.GONE
            }

            override fun recordEnd(time: Long) {
                listener?.recordEnd(time)
                done()
            }

            override fun recordZoom(zoom: Float) {
                listener?.recordZoom(zoom)
            }
        })
        doneIv.setOnClickListener(OnClickListener {
            listener?.ok()
        })
        cancelIv.setOnClickListener(OnClickListener { v: View? ->
            captureBtn.resetSate()
            listener?.cancel()
        })
        addView(rootView)
        reset()
    }


    fun reset() {
        captureBtn.resetSate()
        captureBtn.visibility = View.VISIBLE
        doneLayer.visibility = View.GONE
    }


    fun done() {
        captureBtn.visibility = View.GONE

        doneLayer.visibility = View.VISIBLE
        val cancelAnimation = TranslateAnimation(
            Animation.RELATIVE_TO_SELF,
            1f, Animation.RELATIVE_TO_SELF, 0f,
            0, 0f, 0, 0f
        )
        cancelAnimation.duration = 200
        cancelIv.animation = cancelAnimation
        cancelAnimation.start()
        val doneAnimation = TranslateAnimation(
            Animation.RELATIVE_TO_SELF,
            (-1).toFloat(), Animation.RELATIVE_TO_SELF, 0f,
            0, 0f, 0, 0f
        )
        doneAnimation.duration = 200
        doneIv.animation = doneAnimation
        doneAnimation.start()
    }

    fun setListener(listener: CaptureListener?) {
        this.listener = listener
    }


}