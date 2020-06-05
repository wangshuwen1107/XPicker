package cn.cheney.xpicker.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import cn.cheney.xpicker.R
import cn.cheney.xpicker.XPickerConstant
import cn.cheney.xpicker.callback.CaptureListener
import cn.cheney.xpicker.entity.CaptureType

class CaptureLayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var doneLayer: LinearLayout
    private lateinit var captureBtnLayer: FrameLayout
    private lateinit var doneIv: ImageView
    private lateinit var cancelIv: ImageView
    private lateinit var captureBtn: CaptureButton
    private lateinit var tipTxt: TextView
    private var listener: CaptureListener? = null

    init {
        initView()
    }

    private fun initView() {
        val rootView =
            View.inflate(context, R.layout.xpicker_layer_record_capture, null)
        captureBtnLayer = rootView.findViewById(R.id.capture_btn_layer)
        doneLayer = rootView.findViewById(R.id.xpicker_capture_done_layer)
        doneIv = rootView.findViewById(R.id.xpicker_capture_done_iv)
        cancelIv = rootView.findViewById(R.id.xpicker_capture_cancel_iv)
        tipTxt = rootView.findViewById(R.id.capture_tip_txt)
        addCaptureBtn()
        doneIv.setOnClickListener(OnClickListener {
            listener?.ok()
        })
        cancelIv.setOnClickListener(OnClickListener { v: View? ->
            listener?.cancel()
        })
        addView(rootView)
        reset()
    }

    @SuppressLint("SetTextI18n")
    private fun addCaptureBtn() {
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        captureBtn = CaptureButton(context, (screenWidth / 4.5f).toInt())
        captureBtn.setCaptureLisenter(object : CaptureListener() {
            override fun takePictures() {
                listener?.takePictures()
            }

            override fun recordShort(time: Long) {
                listener?.recordShort(time)
            }

            override fun recordTime(time: Long) {
                listener?.recordTime(time)
                tipTxt.text = "${time / 1000}s / ${captureBtn.duration / 1000}s "
            }

            override fun recordStart() {
                listener?.recordStart()
                doneLayer.visibility = View.GONE
            }

            override fun recordEnd(time: Long) {
                listener?.recordEnd(time)
                tipTxt.text = "${time / 1000}s"
            }

            override fun recordZoom(zoom: Float) {
                listener?.recordZoom(zoom)
            }
        })
        captureBtnLayer.addView(captureBtn)
    }


    fun reset() {
        captureBtn.resetState()
        captureBtn.visibility = View.VISIBLE
        tipTxt.visibility = View.VISIBLE
        setTipByType()
        doneLayer.visibility = View.GONE
    }


    fun done() {
        captureBtn.visibility = View.GONE
        tipTxt.visibility = View.GONE

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

    fun setMaxDuration(duration: Int) {
        if (duration > 1000) {
            captureBtn.duration = duration
        }

    }

    fun setMinDuration(duration: Int) {
        if (duration > 1000) {
            captureBtn.setMinDuration(duration)
        }
    }


    fun setCameraType(type: String) {
        captureBtn.setButtonFeatures(type)
        setTipByType()
    }


    private fun setTipByType() {
        when (CaptureType.valueOf(captureBtn.type)) {
            CaptureType.MIXED -> {
                tipTxt.text = context.getText(R.string.camera_mixed_tip)
            }
            CaptureType.ONLY_CAPTURE -> {
                tipTxt.text = context.getText(R.string.camera_capture_tip)
            }
            CaptureType.ONLY_RECORDER -> {
                tipTxt.text = context.getText(R.string.camera_recorder_tip)
            }
        }
    }
}
