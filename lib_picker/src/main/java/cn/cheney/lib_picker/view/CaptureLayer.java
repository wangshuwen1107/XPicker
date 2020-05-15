package cn.cheney.lib_picker.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cn.cheney.lib_picker.R;


public class CaptureLayer extends FrameLayout {

    private LinearLayout doneLayer;

    private View rootLayer;
    private ImageView doneIv;
    private ImageView cancelIv;
    private ImageView redCircleIv;
    private ImageView redSquareIv;
    private CircleProgressView progressView;

    private CaptureListener listener;


    public interface CaptureListener {
        void onClick();

        void onLongClick();

        void onBackClick();

        void onDoneClick();
    }


    public CaptureLayer(@NonNull Context context) {
        this(context, null);
    }

    public CaptureLayer(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CaptureLayer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        View rootView = View.inflate(getContext(), R.layout.xpicker_layter_record_capture, null);
        rootLayer = rootView.findViewById(R.id.common_capture_root);
        progressView = rootView.findViewById(R.id.common_capture_action_iv);
        doneLayer = rootView.findViewById(R.id.common_capture_done_layer);
        doneIv = rootView.findViewById(R.id.common_capture_done_iv);
        cancelIv = rootView.findViewById(R.id.common_capture_cancel_iv);
        redCircleIv = rootView.findViewById(R.id.media_record_red_circle);
        redSquareIv = rootView.findViewById(R.id.media_record_red_square);

        progressView.setOnClickListener(v -> {
            if (null != listener) {
                listener.onClick();
            }
        });
        progressView.setOnLongClickListener(v -> {
            if (null != listener) {
                listener.onLongClick();
            }
            return false;
        });

        doneIv.setOnClickListener(v -> {
            if (null != listener) {
                listener.onDoneClick();
            }
        });
        cancelIv.setOnClickListener(v -> {
            if (null != listener) {
                listener.onBackClick();
            }
        });
        addView(rootView);
        normal();
    }


    public void setMaxLength(int maxLength) {
        progressView.setMaxProgress(maxLength);
    }


    public void setProgress(int progress) {
        progressView.setProgress(progress);
    }


    public void normal() {
        progressView.setVisibility(VISIBLE);
        progressView.setProgress(0);
        doneLayer.setVisibility(GONE);
        redCircleIv.setVisibility(VISIBLE);
        redSquareIv.setVisibility(GONE);
    }


    public void recording() {
        progressView.setVisibility(VISIBLE);
        doneLayer.setVisibility(GONE);
        redCircleIv.setVisibility(GONE);
        redSquareIv.setVisibility(VISIBLE);
    }


    public void done() {
        doneLayer.setVisibility(VISIBLE);

        TranslateAnimation cancelAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
                1, Animation.RELATIVE_TO_SELF, 0,
                0, 0, 0, 0);
        cancelAnimation.setDuration(200);
        cancelIv.setAnimation(cancelAnimation);
        cancelAnimation.start();

        TranslateAnimation doneAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
                -1, Animation.RELATIVE_TO_SELF, 0,
                0, 0, 0, 0);
        doneAnimation.setDuration(200);
        doneIv.setAnimation(doneAnimation);
        doneAnimation.start();

        progressView.setVisibility(GONE);
        redCircleIv.setVisibility(GONE);
        redSquareIv.setVisibility(GONE);
    }


    public void setListener(CaptureListener listener) {
        this.listener = listener;
    }
}
