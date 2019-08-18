package com.td.exoplayer.utils;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.exoplayer2.util.Util;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import com.td.exoplayer.R;

public class Utils {
    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
    private static Toast toast;

    public static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }

    public static int getScreenHeight(Context context) {
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.heightPixels;
    }

    public static int px2dp(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static int pxTodp(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale);
    }

    public static int dp2px(Context context, float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dpVal, context.getResources().getDisplayMetrics());
    }

    public static int sp2px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    public static void setVolume(Context context, boolean add, boolean mute) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (add) {
            if (current >= max) {
                return;
            }
            current++;
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, current, 0);
        } else {
            if (current <= 0) {
                return;
            }
            current--;
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, current, 0);
        }
        if (mute) {
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC, mute);
        }
        showVolumeToast(context, current, max, mute);
    }

    private static final int MIN_CLICK_DELAY_TIME = 200;
    private static long lastClickTime;

    public static boolean isQuickClick() {
        boolean flag = true;
        long curClickTime = System.currentTimeMillis();
        if ((curClickTime - lastClickTime) >= MIN_CLICK_DELAY_TIME) {
            flag = false;
        }
        lastClickTime = curClickTime;
        return flag;
    }

    private static LinearLayout.LayoutParams layoutParams;
    private static long currentTime = 0;
    private static boolean isShow = false;

    public static void showVolumeToast(Context context, int volume, int max, boolean mute) {
        currentTime = System.currentTimeMillis();
        if (!isShow) {
            toast = new Toast(context);
            toast.setView(View.inflate(context, R.layout.layout_volumn_toast, null));
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 50);
            SeekBar seekBar = (SeekBar) toast.getView().findViewById(R.id.seekBar);
            ImageView iv_volume = (ImageView) toast.getView().findViewById(R.id.iv_img);
            layoutParams = new LinearLayout.LayoutParams((Utils.getScreenWidth(context) / 4), LinearLayout.LayoutParams.WRAP_CONTENT);
            seekBar.setLayoutParams(layoutParams);
            seekBar.setMax(max);
            if (!mute) {
                seekBar.setProgress(volume);
                iv_volume.setImageResource(R.drawable.icon_volume);
            } else {
                seekBar.setProgress(0);
                iv_volume.setImageResource(R.drawable.icon_mute);
            }
            final Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    toast.show();
                    isShow = true;
                    if (System.currentTimeMillis() - currentTime > 2 * 1000) {
                        toast.cancel();
                        timer.cancel();
                        isShow = false;
                    }
                }
            }, 0, 3000);
        } else {
            SeekBar seekBar = (SeekBar) toast.getView().findViewById(R.id.seekBar);
            ImageView iv_volume = (ImageView) toast.getView().findViewById(R.id.iv_img);
            layoutParams = new LinearLayout.LayoutParams((getScreenWidth(context) / 4), LinearLayout.LayoutParams.WRAP_CONTENT);
            seekBar.setLayoutParams(layoutParams);
            seekBar.setMax(max);
            if (!mute) {
                seekBar.setProgress(volume);
                iv_volume.setImageResource(R.drawable.icon_volume);
            } else {
                seekBar.setProgress(0);
                iv_volume.setImageResource(R.drawable.icon_mute);
            }
        }
    }

    public static float getCurrentSystemVolume(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        return ((float) current / (float) max);
    }

    public static void setStreamMute(Context context, boolean mute) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC, mute);
        }
    }

    public static void setStreamVolume(Context context, int volume) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        }
    }

    public static Animation SlideToBottom(Interpolator inter) {
        TranslateAnimation transAnim = new TranslateAnimation(2, 0.0F, 2, 0.0F, 2, 0.0F, 2, 1.0F);
        transAnim.setFillAfter(true);
        transAnim.setDuration(600);
        if (inter != null) {
            transAnim.setInterpolator(inter);
        }
        return transAnim;
    }

    public static Animation SlideToTop(Interpolator inter) {
        TranslateAnimation transAnim = new TranslateAnimation(2, 0.0F, 2, 0.0F, 2, 0.0F, 2, -1.0F);
        transAnim.setFillAfter(true);
        transAnim.setDuration(600);
        if (inter != null) {
            transAnim.setInterpolator(inter);
        }
        return transAnim;
    }

    public static void getSlideFromBottom(View view, Interpolator interpolator) {
        Animation anim = SlideFromBottom(interpolator);
        view.setAnimation(anim);
        view.startAnimation(anim);
    }

    public static Animation SlideFromBottom(Interpolator inter) {
        TranslateAnimation transAnim = new TranslateAnimation(2, 0.0F, 2, 0.0F, 2, 1.0F, 2, 0.0F);
        transAnim.setFillAfter(true);
        transAnim.setDuration(600);
        if (inter != null) {
            transAnim.setInterpolator(inter);
        }
        return transAnim;
    }

    public static void getSlideFromTop(View view, Interpolator interpolator) {
        Animation anim = SlideFromTop(interpolator);
        view.setAnimation(anim);
        view.startAnimation(anim);
    }

    public static Animation SlideFromTop(Interpolator inter) {
        TranslateAnimation transAnim = new TranslateAnimation(2, 0.0F, 2, 0.0F, 2, -1.0F, 2, 0.0F);
        transAnim.setFillAfter(true);
        transAnim.setDuration(600);
        if (inter != null) {
            transAnim.setInterpolator(inter);
        }
        return transAnim;
    }

    public static String generateTime(long position) {
        int totalSeconds = (int) (position / 1000);
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        if (hours > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes,
                    seconds).toString();
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds)
                    .toString();
        }
    }

    /**
     * Makes a best guess to infer the type from a {@link Uri}.
     *
     * @param uri The {@link Uri}.
     * @param overrideExtension If not null, used to infer the type.
     * @return The content type.
     */
    @C.ContentType
    public static int inferContentType(Uri uri, String overrideExtension) {
        return TextUtils.isEmpty(overrideExtension)
                ? inferContentType(uri)
                : inferContentType("." + overrideExtension);
    }

    /**
     * Makes a best guess to infer the type from a {@link Uri}.
     *
     * @param uri The {@link Uri}.
     * @return The content type.
     */
    @C.ContentType
    public static int inferContentType(Uri uri) {
        String path = uri.getPath();
        return path == null ? C.TYPE_OTHER : inferContentType(path);
    }

    /**
     * Makes a best guess to infer the type from a file name.
     *
     * @param fileName Name of the file. It can include the path of the file.
     * @return The content type.
     */
    @C.ContentType
    public static int inferContentType(String fileName) {
        fileName = Util.toLowerInvariant(fileName);
        if (fileName.endsWith(".mpd")) {
            return C.TYPE_DASH;
        } else if (fileName.endsWith(".m3u8")) {
            return C.TYPE_HLS;
        } else if (fileName.matches(".*\\.ism(l)?(/manifest(\\(.+\\))?)?")) {
            return C.TYPE_SS;
        }else if (fileName.endsWith(".ts")) {
            return C.TYPE_TS;
        }  else {
            return C.TYPE_OTHER;
        }
    }

}
