package cn.cheney.lib_picker.view

import android.content.Context
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import cn.cheney.lib_picker.R
import cn.cheney.lib_picker.XPicker
import cn.cheney.lib_picker.callback.CaptureListener
import kotlinx.android.synthetic.main.xpicker_view_capture.view.*


class CaptureButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var captureState = State.IDLE
    private var timer: RecordCountDownTimer? = null
    private var recordDuration: Long = 0
    private val mHandler: Handler by lazy {
        Handler(Looper.getMainLooper())
    }

    var maxDuration: Long = 10000
    var minDuration: Long = 2000
    var listener: CaptureListener? = null

    enum class State {
        IDLE, RECODING, TAKE_PHOTO
    }


    private val longPressRunnable = Runnable {
        captureState = State.RECODING
        timer!!.start()
        xpicker_camera_capture_recording_dot.visibility = View.VISIBLE
        xpicker_camera_capture_normal_dot.visibility = View.GONE
        listener?.recordStart()
    }


    init {
        val rootView = View.inflate(getContext(), R.layout.xpicker_view_capture, null)
        addView(rootView)

        timer = RecordCountDownTimer(maxDuration, maxDuration / 360)

        xpicker_camera_capture_pb.maxProgress = maxDuration.toInt()
        xpicker_camera_capture_pb.progress = 0

    }

    inner class RecordCountDownTimer internal constructor(
        millisInFuture: Long,
        countDownInterval: Long
    ) :
        CountDownTimer(millisInFuture, countDownInterval) {
        override fun onTick(millisUntilFinished: Long) {
            updateProgress(millisUntilFinished)
        }


        override fun onFinish() {
            updateProgress(0)
            recordEnd()
        }
    }


    private fun updateProgress(millisUntilFinished: Long) {
        recordDuration = maxDuration - millisUntilFinished
        xpicker_camera_capture_pb.progress = recordDuration.toInt()
    }


    private fun recordEnd() {
        timer?.cancel()
        if (recordDuration < minDuration) {
            listener?.recordShort(recordDuration)
        } else {
            listener?.recordEnd(recordDuration)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        //Log.i(XPicker.TAG, "onTouchEvent= ${event?.action}")
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> mHandler.postDelayed(longPressRunnable, 500)
            MotionEvent.ACTION_UP -> handleUp()
        }
        return true
    }


    private fun handleUp() {
        when (captureState) {
            State.IDLE -> {
                Log.i(XPicker.TAG, "current state= ${State.TAKE_PHOTO}")
                captureState = State.TAKE_PHOTO
                mHandler.removeCallbacks(longPressRunnable)
                xpicker_camera_capture_recording_dot.visibility = View.GONE
                xpicker_camera_capture_normal_dot.visibility = View.VISIBLE
                listener?.takePictures()
            }
            //录制结束
            State.RECODING -> recordEnd()
        }
    }


    fun resetSate() {
        xpicker_camera_capture_pb.maxProgress = maxDuration.toInt()
        xpicker_camera_capture_pb.progress = 0

        captureState = State.IDLE
        xpicker_camera_capture_recording_dot.visibility = View.GONE
        xpicker_camera_capture_normal_dot.visibility = View.VISIBLE
    }


}