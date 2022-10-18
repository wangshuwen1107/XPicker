package com.cheney.camera2.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.cheney.camera2.callback.CaptureUIListener;
import com.cheney.camera2.entity.CaptureType;
import com.cheney.camera2.util.Logger;


public class CaptureButton extends View {

    private int state;              //当前按钮状态
    private String button_state;       //按钮可执行的功能状态（拍照,录制,两者）

    public static final int STATE_IDLE = 0x001;        //空闲状态
    public static final int STATE_PRESS = 0x002;       //按下状态
    public static final int STATE_LONG_PRESS = 0x003;  //长按状态
    public static final int STATE_RECORDERING = 0x004; //录制状态
    public static final int STATE_BAN = 0x005;         //禁止状态

    private static final int progress_color = 0xFF2D93FA;            //进度条颜色
    private static final int outside_color = 0xE6D8D8D8;             //外圆背景色
    private static final int inside_color = 0xFFFFFFFF;              //内圆背景色

    private float event_Y;  //Touch_Event_Down时候记录的Y值
    private Paint mPaint;
    private float strokeWidth;          //进度条宽度

    //中心坐标
    private float center_X;
    private float center_Y;

    private float button_idle_out_radius;
    private float button_idle_in_radius;

    private float button_ing_out_radius;
    private float button_ing_in_radius;


    private float button_out_radius;
    private float button_in_radius;


    private float progress;         //录制视频的进度
    private int duration;           //录制视频最大时间长度
    private int min_duration;       //最短录制时间限制
    private int recorded_time;      //记录当前录制的时间

    private RectF rectF;

    private LongPressRunnable longPressRunnable;    //长按后处理的逻辑Runnable
    private CaptureUIListener captureListener;        //按钮回调接口
    private RecordCountDownTimer timer;             //计时器

    public CaptureButton(Context context) {
        super(context);
        init();
    }

    public CaptureButton(Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CaptureButton(Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    public void init() {
        progress = 0;
        longPressRunnable = new LongPressRunnable();
        //初始化为空闲状态
        state = STATE_IDLE;
        //初始化按钮为可录制可拍照
        button_state = CaptureType.ONLY_CAPTURE.getType();
        setMinDuration(1000);
        setDuration(15000);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        post(new Runnable() {
            @Override
            public void run() {
                initSize();
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void initSize() {
        int button_size = getWidth();
        center_X = (button_size) / 2F;
        center_Y = (button_size) / 2F;

        button_idle_out_radius = button_size * 0.8f * 0.5f;
        button_idle_in_radius = button_size * 0.7f * 0.5f;

        button_out_radius = button_idle_out_radius;
        button_in_radius = button_idle_in_radius;

        //RECORDING -  外边
        button_ing_out_radius = button_size * 0.5f;
        //RECORDING -  内边
        button_ing_in_radius = button_size * 0.9f * 0.5f;
        //录制边线
        strokeWidth = button_size * 0.5f * 0.1f;

        Logger.Companion.i("size=" + button_size
                + " 普通状态外圆=" + button_idle_out_radius
                + " 普通状态内圆=" + button_idle_in_radius
        );
        float recordingStrokeRadius = (button_size / 2f - strokeWidth / 2);
        rectF = new RectF(
                center_X - recordingStrokeRadius,
                center_Y - recordingStrokeRadius,
                center_X + recordingStrokeRadius,
                center_Y + recordingStrokeRadius);

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        try {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(outside_color); //外圆（半透明灰色）
            canvas.drawCircle(center_X, center_Y, button_out_radius, mPaint);
            mPaint.setColor(inside_color);  //内圆（白色）
            canvas.drawCircle(center_X, center_Y, button_in_radius, mPaint);
            //如果状态为录制状态，则绘制录制进度条
            if (state == STATE_RECORDERING) {
                mPaint.setColor(progress_color);
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setStrokeWidth(strokeWidth);
                canvas.drawArc(rectF, -90, progress, false, mPaint);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (event.getPointerCount() > 1 || state != STATE_IDLE)
                        break;
                    event_Y = event.getY();     //记录Y值
                    state = STATE_PRESS;        //修改当前状态为点击按下

                    //判断按钮状态是否为可录制状态
                    if ((button_state.equals(CaptureType.ONLY_RECORDER.getType())
                            || button_state.equals(CaptureType.MIXED.getType())))
                        postDelayed(longPressRunnable, 500);    //同时延长500启动长按后处理的逻辑Runnable
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (captureListener != null
                            && state == STATE_RECORDERING
                            && (button_state.equals(CaptureType.ONLY_RECORDER.getType())
                            || button_state.equals(CaptureType.MIXED.getType()))) {
                        //记录当前Y值与按下时候Y值的差值，调用缩放回调接口
                        captureListener.recordZoom(event_Y - event.getY());
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    //根据当前按钮的状态进行相应的处理
                    handlerUnpressByState();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    //当手指松开按钮时候处理的逻辑
    private void handlerUnpressByState() {
        try {
            removeCallbacks(longPressRunnable); //移除长按逻辑的Runnable
            //根据当前状态处理
            switch (state) {
                //当前是点击按下
                case STATE_PRESS:
                    if (captureListener != null && (button_state.equals(CaptureType.ONLY_CAPTURE.getType())
                            || button_state.equals(CaptureType.MIXED.getType()))) {
                        //回调拍照接口
                        captureListener.takePictures();
                        state = STATE_BAN;
                    } else {
                        state = STATE_IDLE;
                    }
                    break;
                //当前是长按状态
                case STATE_RECORDERING:
                    timer.cancel(); //停止计时器
                    recordEnd();    //录制结束
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //录制结束
    private void recordEnd() {

        try {
            resetRecordAnim();  //重制按钮状态
            if (captureListener != null) {
                if (recorded_time < min_duration)
                    captureListener.recordShort(recorded_time);//回调录制时间过短
                else
                    captureListener.recordEnd(recorded_time);  //回调录制结束
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //重制状态
    private void resetRecordAnim() {
        try {
            state = STATE_BAN;
            progress = 0;       //重制进度
            invalidate();
            //还原按钮初始状态动画
            startRecordAnimation(
                    button_ing_out_radius,
                    button_idle_out_radius,
                    button_ing_in_radius,
                    button_idle_in_radius
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //内外圆动画
    private void startRecordAnimation(float outside_start,
                                      float outside_end,
                                      float inside_start,
                                      float inside_end) {
        try {
            ValueAnimator outside_anim = ValueAnimator.ofFloat(outside_start, outside_end);
            ValueAnimator inside_anim = ValueAnimator.ofFloat(inside_start, inside_end);
            //外圆动画监听
            outside_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    button_out_radius = (float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            //内圆动画监听
            inside_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    button_in_radius = (float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            AnimatorSet set = new AnimatorSet();
            //当动画结束后启动录像Runnable并且回调录像开始接口
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    //设置为录制状态
                    if (state == STATE_LONG_PRESS) {
                        if (captureListener != null)
                            captureListener.recordStart();
                        state = STATE_RECORDERING;
                        timer.start();
                    }
                }
            });
            set.playTogether(outside_anim, inside_anim);
            set.setDuration(100);
            set.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //更新进度条
    private void updateProgress(long millisUntilFinished) {
        try {
            recorded_time = (int) (duration - millisUntilFinished);
            if (captureListener != null) {
                captureListener.recordTime(recorded_time);
            }
            progress = 360f - millisUntilFinished / (float) duration * 360f;
//            Logger.Companion.d("updateProgress 还剩="+millisUntilFinished
//                    + " 录制时间="+recorded_time
//                    + " progress="+progress
//            );
            invalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //录制视频计时器
    private class RecordCountDownTimer extends CountDownTimer {
        RecordCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            updateProgress(millisUntilFinished);
        }

        @Override
        public void onFinish() {
            updateProgress(0);
            recordEnd();
        }
    }

    //长按线程
    private class LongPressRunnable implements Runnable {
        @Override
        public void run() {
            try {
                state = STATE_LONG_PRESS;   //如果按下后经过500毫秒则会修改当前状态为长按状态
                //启动按钮动画，外圆变大，内圆缩小
                startRecordAnimation(
                        button_idle_out_radius,
                        button_ing_out_radius,
                        button_idle_in_radius,
                        button_ing_in_radius
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**************************************************
     * 对外提供的API                     *
     **************************************************/

    //设置最长录制时间
    public void setDuration(int duration) {
        this.duration = duration;
        timer = new RecordCountDownTimer(duration, duration / 360);    //录制定时器
    }

    //设置最短录制时间
    public void setMinDuration(int duration) {
        this.min_duration = duration;
    }

    //设置回调接口
    public void setCaptureListener(CaptureUIListener captureListener) {
        this.captureListener = captureListener;
    }

    //设置按钮功能（拍照和录像）
    public void setButtonFeatures(String state) {
        this.button_state = state;
    }

    //是否空闲状态
    public boolean isIdle() {
        return state == STATE_IDLE;
    }

    //设置状态
    public void resetState() {
        state = STATE_IDLE;
    }

    public String getType() {
        return button_state;
    }

    public int getDuration() {
        return duration;
    }

    public int getMin_duration() {
        return min_duration;
    }
}
