package com.td.exoplayer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.td.exoplayer.utils.Utils;

public class TouchControllView extends FrameLayout {
    private LinearLayout linearLayout;
    private LayoutParams layoutParams;
    private ImageView iv_status;
    private ProgressBar pb_progress;
    private TextView tv_seek;

    public TouchControllView(@NonNull Context context) {
        super(context);
        init();
    }

    public TouchControllView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TouchControllView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.layout_touchcontrollview, this);
        layoutParams = new LayoutParams(Utils.getScreenWidth(getContext()) / 3, FrameLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        linearLayout = findViewById(R.id.ll_parent);
        iv_status = findViewById(R.id.iv_status);
        pb_progress = findViewById(R.id.pb_progress);
        tv_seek = findViewById(R.id.tv_seek);
        linearLayout.setLayoutParams(layoutParams);
    }

    public void setVolumn(float curretnVolumn) {
        tv_seek.setVisibility(View.GONE);
        pb_progress.setVisibility(View.VISIBLE);
        iv_status.setImageResource(R.drawable.video_volume_icon);
        pb_progress.setMax(100);
        pb_progress.setProgress((int) (curretnVolumn * 100));
        linearLayout.setVisibility(View.VISIBLE);
    }

    public void setBrightness(float brightness) {
        tv_seek.setVisibility(View.GONE);
        pb_progress.setVisibility(View.VISIBLE);
        iv_status.setImageResource(R.drawable.video_brightness_6_white_36dp);
        pb_progress.setMax(100);
        pb_progress.setProgress((int) (brightness * 100));
        linearLayout.setVisibility(View.VISIBLE);
    }

    public void setSeek(boolean add, long seek, long duration) {
        linearLayout.setVisibility(View.VISIBLE);
        tv_seek.setVisibility(View.VISIBLE);
        pb_progress.setVisibility(View.GONE);
        if (add) {
            iv_status.setImageResource(R.drawable.video_forward_icon);
        } else {
            iv_status.setImageResource(R.drawable.video_backward_icon);
        }
        tv_seek.setText(Utils.generateTime(seek) + "/" + Utils.generateTime(duration));
    }

}
