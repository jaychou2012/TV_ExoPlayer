package com.td.exoplayer;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.Timer;
import java.util.TimerTask;

import com.td.exoplayer.utils.Utils;

public class AdsView extends FrameLayout {
    private TextView textView;
    private ImageView imageView;
    private Timer timer;
    private TimerTask timerTask;
    private long duration = 0;
    private boolean hasAds = false;
    private String adsUrl = "";
    private long defaultDuration = 6 * 1000;
    private AdsState adsState;

    public AdsView(@NonNull Context context) {
        super(context);
        init();
    }

    public AdsView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AdsView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        textView = new TextView(getContext());
        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.RIGHT | Gravity.TOP;
        layoutParams.setMargins(Utils.dp2px(getContext(), 20), Utils.dp2px(getContext(), 20), Utils.dp2px(getContext(), 20), Utils.dp2px(getContext(), 20));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        textView.setPadding(Utils.dp2px(getContext(), 20), Utils.dp2px(getContext(), 10), Utils.dp2px(getContext(), 20), Utils.dp2px(getContext(), 10));
        textView.setTextColor(getResources().getColor(R.color.white));
        textView.setBackgroundResource(R.drawable.bg_tips);
        textView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));

        imageView = new ImageView(getContext());
        LayoutParams imgLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        addView(imageView, imgLayoutParams);
        addView(textView, layoutParams);
        textView.setVisibility(View.GONE);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
    }

    public TextView getTextTips() {
        return textView;
    }

    public void setHasAds(boolean hasAds, String adsUrl) {
        this.hasAds = hasAds;
        this.adsUrl = adsUrl;
        if (!hasAds) {
            textView.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }

    public void seeClick() {
        Toast.makeText(getContext(), "看广告", Toast.LENGTH_SHORT).show();
    }

    public void setDurationText(long duration) {
        if (!hasAds) {
            textView.setVisibility(View.GONE);
            return;
        }
        if (isImageAds(adsUrl)) {
            imageView.setVisibility(View.VISIBLE);
            Glide.with(getContext()).load(adsUrl).listener(new RequestListener() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target target, boolean isFirstResource) {
                    return false;
                }

                @Override
                public boolean onResourceReady(Object resource, Object model, Target target, DataSource dataSource, boolean isFirstResource) {
                    if (resource instanceof GifDrawable) {
                        ((GifDrawable) resource).setLoopCount(1);
                    }
                    return false;
                }

            }).into(imageView);
        }
        textView.setVisibility(View.VISIBLE);
        this.duration = duration;
        textView.setText((duration / 1000) + " 秒");
        cancelTimer();
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                if (duration >= 0) {
                    handler.obtainMessage(0).sendToTarget();
                } else {
                    handler.obtainMessage(-1).sendToTarget();
                }
            }
        };
        timer.schedule(timerTask, 0, 1000);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    setTextDuration();
                    break;
                case -1:
                    cancelTimer();
                    break;
            }
        }
    };

    private void setTextDuration() {
        duration = duration - 1000;
        if (duration >= 0) {
            textView.setText((duration / 1000) + " 秒");
        } else {
            textView.setText("0 秒");
            cancelTimer();
        }
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timerTask.cancel();
            timer = null;
            timerTask = null;
            textView.setVisibility(View.GONE);
            imageView.setVisibility(View.GONE);
            setVisibility(View.GONE);
            if (adsState != null) {
                adsState.adsComplete();
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    public boolean isImageAds(String adsUrl) {
        if (adsUrl.trim().length() == 0) {
            return false;
        }
        String type = adsUrl.substring(adsUrl.lastIndexOf("."), adsUrl.length()).toLowerCase();
        switch (type) {
            case ".png":
                return true;
            case ".jpg":
                return true;
            case ".jpeg":
                return true;
            case ".gif":
                return true;
            case ".bmp":
                return true;
            case ".svg":
                return true;
            case ".webp":
                return true;
            default:
                return false;
        }
    }

    public void setAdsStateListener(AdsState adsState) {
        this.adsState = adsState;
    }

    public interface AdsState {
        void adsComplete();
    }
}
