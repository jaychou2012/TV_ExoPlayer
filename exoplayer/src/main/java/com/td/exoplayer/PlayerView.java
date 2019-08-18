/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.td.exoplayer;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ControlDispatcher;
import com.google.android.exoplayer2.DefaultControlDispatcher;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackPreparer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Player.DiscontinuityReason;
import com.google.android.exoplayer2.Player.VideoComponent;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.id3.ApicFrame;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.ads.AdsLoader;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.spherical.SingleTapListener;
import com.google.android.exoplayer2.ui.spherical.SphericalSurfaceView;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.ErrorMessageProvider;
import com.google.android.exoplayer2.util.RepeatModeUtil;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoListener;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import com.td.exoplayer.AspectRatioFrameLayout.ResizeMode;
import com.td.exoplayer.utils.Utils;

public class PlayerView extends FrameLayout implements AdsLoader.AdViewProvider, PlayerControlView.PlayOrPauseStatus, PlayerControlView.VisibilityListener, View.OnTouchListener {
    @Override
    public void playOrPause(boolean pause) {
        if (max) {
            iv_playOrPause.setVisibility(View.VISIBLE);
            iv_playOrPause.setImageResource(pause ? R.drawable.icon_play : R.drawable.icon_pause);
        }
    }

    @Override
    public void onVisibilityChange(int visibility) {
        if (visibility == View.GONE || visibility == View.INVISIBLE && iv_playOrPause.getVisibility() == View.VISIBLE) {
            if (controller != null) {
                if (controller.isPlayPlaying()) {
                    iv_playOrPause.setVisibility(GONE);
                }
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                onTouch = true;
                if (seek == 0) {
                    seek = player.getCurrentPosition();
                }
                x = event.getX();
                y = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                dx = event.getX() - x;
                dy = event.getY() - y;
                break;
            case MotionEvent.ACTION_UP:
                onTouch = false;
                seek = 0;
                touchControllView.setVisibility(View.GONE);
                break;
        }
        if (isTVMode) {
            return false;
        }
        return gestureDetector.onTouchEvent(event);
    }

    // LINT.IfChange

    /**
     * Determines when the buffering view is shown. One of {@link #SHOW_BUFFERING_NEVER}, {@link
     * #SHOW_BUFFERING_WHEN_PLAYING} or {@link #SHOW_BUFFERING_ALWAYS}.
     */
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SHOW_BUFFERING_NEVER, SHOW_BUFFERING_WHEN_PLAYING, SHOW_BUFFERING_ALWAYS})
    public @interface ShowBuffering {
    }

    /**
     * The buffering view is never shown.
     */
    public static final int SHOW_BUFFERING_NEVER = 0;
    /**
     * The buffering view is shown when the player is in the {@link Player#STATE_BUFFERING buffering}
     * state and {@link Player#getPlayWhenReady() playWhenReady} is {@code true}.
     */
    public static final int SHOW_BUFFERING_WHEN_PLAYING = 1;
    /**
     * The buffering view is always shown when the player is in the {@link Player#STATE_BUFFERING
     * buffering} state.
     */
    public static final int SHOW_BUFFERING_ALWAYS = 2;
    // LINT.ThenChange(../../../../../../res/values/attrs.xml)

    // LINT.IfChange
    private static final int SURFACE_TYPE_NONE = 0;
    private static final int SURFACE_TYPE_SURFACE_VIEW = 1;
    private static final int SURFACE_TYPE_TEXTURE_VIEW = 2;
    private static final int SURFACE_TYPE_MONO360_VIEW = 3;
    // LINT.ThenChange(../../../../../../res/values/attrs.xml)

    @Nullable
    private final AspectRatioFrameLayout contentFrame;
    private final View shutterView;
    @Nullable
    private final View surfaceView;
    private final ImageView artworkView;
    private final SubtitleView subtitleView;
    private ImageView iv_playOrPause;
    @Nullable
    private final View bufferingView;
    private LottieAnimationView lav;
    @Nullable
    private final TextView errorMessageView;
    @Nullable
    public final PlayerControlView controller;
    private final ComponentListener componentListener;
    @Nullable
    private final AdsView adOverlayFrameLayout;
    @Nullable
    private final FrameLayout overlayFrameLayout;

    private Player player;
    private boolean useController;
    private boolean useArtwork;
    @Nullable
    private Drawable defaultArtwork;
    private @ShowBuffering
    int showBuffering;
    private boolean keepContentOnPlayerReset;
    @Nullable
    private ErrorMessageProvider<? super ExoPlaybackException> errorMessageProvider;
    @Nullable
    private CharSequence customErrorMessage;
    private int controllerShowTimeoutMs;
    private boolean controllerAutoShow;
    private boolean controllerHideDuringAds;
    private boolean controllerHideOnTouch;
    private boolean isPlayingAd = false;
    private boolean hasAds = false;
    private String adsUrl = "";
    private int textureViewRotation;
    private PlayState playState;
    private GestureDetector gestureDetector;
    private SettingVoiceObserver settingVoiceObserver;
    private boolean tryFree = false;

    private TouchControllView touchControllView;
    private boolean isTVMode = true;
    private float x = 0, y = 0;
    private float dx = 0, dy = 0;
    private int limitX = 80, limitY = 100;
    private int FLAG_CONTROL = 0;
    private boolean onTouch = false;
    public boolean max = true;
    private long duration = 0;
    private int seekCount = 15 * 1000;
    private long seek = 0;
    private int[] videoSize = new int[2];

    public PlayerView(Context context) {
        this(context, null);
    }

    public PlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (isInEditMode()) {
            contentFrame = null;
            shutterView = null;
            surfaceView = null;
            iv_playOrPause = null;
            artworkView = null;
            subtitleView = null;
            bufferingView = null;
            errorMessageView = null;
            controller = null;
            componentListener = null;
            adOverlayFrameLayout = null;
            overlayFrameLayout = null;
            ImageView logo = new ImageView(context);
            if (Util.SDK_INT >= 23) {
                configureEditModeLogoV23(getResources(), logo);
            } else {
                configureEditModeLogo(getResources(), logo);
            }
            addView(logo);
            return;
        }

        boolean shutterColorSet = false;
        int shutterColor = 0;
        int playerLayoutId = R.layout.exo_player_view;
        boolean useArtwork = true;
        int defaultArtworkId = 0;
        boolean useController = true;
        int surfaceType = SURFACE_TYPE_SURFACE_VIEW;
        int resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
        int controllerShowTimeoutMs = PlayerControlView.DEFAULT_SHOW_TIMEOUT_MS;
        boolean controllerHideOnTouch = true;
        boolean controllerAutoShow = true;
        boolean controllerHideDuringAds = true;
        int showBuffering = SHOW_BUFFERING_NEVER;
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, com.google.android.exoplayer2.ui.R.styleable.PlayerView, 0, 0);
            try {
                shutterColorSet = a.hasValue(com.google.android.exoplayer2.ui.R.styleable.PlayerView_shutter_background_color);
                shutterColor = a.getColor(com.google.android.exoplayer2.ui.R.styleable.PlayerView_shutter_background_color, shutterColor);
                playerLayoutId = a.getResourceId(com.google.android.exoplayer2.ui.R.styleable.PlayerView_player_layout_id, playerLayoutId);
                useArtwork = a.getBoolean(com.google.android.exoplayer2.ui.R.styleable.PlayerView_use_artwork, useArtwork);
                defaultArtworkId =
                        a.getResourceId(com.google.android.exoplayer2.ui.R.styleable.PlayerView_default_artwork, defaultArtworkId);
                useController = a.getBoolean(com.google.android.exoplayer2.ui.R.styleable.PlayerView_use_controller, useController);
                surfaceType = a.getInt(com.google.android.exoplayer2.ui.R.styleable.PlayerView_surface_type, surfaceType);
                resizeMode = a.getInt(com.google.android.exoplayer2.ui.R.styleable.PlayerView_resize_mode, resizeMode);
                controllerShowTimeoutMs =
                        a.getInt(com.google.android.exoplayer2.ui.R.styleable.PlayerView_show_timeout, controllerShowTimeoutMs);
                controllerHideOnTouch =
                        a.getBoolean(com.google.android.exoplayer2.ui.R.styleable.PlayerView_hide_on_touch, controllerHideOnTouch);
                controllerAutoShow = a.getBoolean(com.google.android.exoplayer2.ui.R.styleable.PlayerView_auto_show, controllerAutoShow);
                showBuffering = a.getInteger(com.google.android.exoplayer2.ui.R.styleable.PlayerView_show_buffering, showBuffering);
                keepContentOnPlayerReset =
                        a.getBoolean(
                                com.google.android.exoplayer2.ui.R.styleable.PlayerView_keep_content_on_player_reset, keepContentOnPlayerReset);
                controllerHideDuringAds =
                        a.getBoolean(com.google.android.exoplayer2.ui.R.styleable.PlayerView_hide_during_ads, controllerHideDuringAds);
            } finally {
                a.recycle();
            }
        }
        LayoutInflater.from(context).inflate(playerLayoutId, this);
        componentListener = new ComponentListener();
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        // Content frame.
        contentFrame = findViewById(R.id.exo_content_frame);
        if (contentFrame != null) {
            setResizeModeRaw(contentFrame, resizeMode);
        }
        settingVoiceObserver = new SettingVoiceObserver(new Handler());
        context.getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, settingVoiceObserver);
        // Shutter view.
        shutterView = findViewById(R.id.exo_shutter);
        if (shutterView != null && shutterColorSet) {
            shutterView.setBackgroundColor(shutterColor);
        }
        iv_playOrPause = findViewById(R.id.iv_playOrPause);
        // Create a surface view and insert it into the content frame, if there is one.
        if (contentFrame != null && surfaceType != SURFACE_TYPE_NONE) {
            ViewGroup.LayoutParams params =
                    new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            switch (surfaceType) {
                case SURFACE_TYPE_TEXTURE_VIEW:
                    surfaceView = new TextureView(context);
                    break;
                case SURFACE_TYPE_MONO360_VIEW:
                    Assertions.checkState(Util.SDK_INT >= 15);
                    SphericalSurfaceView sphericalSurfaceView = new SphericalSurfaceView(context);
                    sphericalSurfaceView.setSurfaceListener(componentListener);
                    sphericalSurfaceView.setSingleTapListener(componentListener);
                    surfaceView = sphericalSurfaceView;
                    break;
                default:
                    surfaceView = new SurfaceView(context);
                    break;
            }
            surfaceView.setLayoutParams(params);
            contentFrame.addView(surfaceView, 0);
        } else {
            surfaceView = null;
        }

        // Ad overlay frame layout.
        adOverlayFrameLayout = findViewById(R.id.exo_ad_overlay);

        // Overlay frame layout.
        overlayFrameLayout = findViewById(R.id.exo_overlay);

        // Artwork view.
        artworkView = findViewById(R.id.exo_artwork);
        this.useArtwork = useArtwork && artworkView != null;
        if (defaultArtworkId != 0) {
            defaultArtwork = ContextCompat.getDrawable(getContext(), defaultArtworkId);
        }

        // Subtitle view.
        subtitleView = findViewById(R.id.exo_subtitles);
        if (subtitleView != null) {
            subtitleView.setUserDefaultStyle();
            subtitleView.setUserDefaultTextSize();
        }

        // Buffering view.
        bufferingView = findViewById(R.id.exo_buffering);
        if (bufferingView != null) {
            bufferingView.setVisibility(View.GONE);
        }
        this.showBuffering = showBuffering;
        lav = findViewById(R.id.lav);
        if (lav != null) {
            lav.setVisibility(View.GONE);
        }

        // Error message view.
        errorMessageView = findViewById(R.id.exo_error_message);
        if (errorMessageView != null) {
            errorMessageView.setVisibility(View.GONE);
        }

        // Playback control view.
        PlayerControlView customController = findViewById(R.id.exo_controller);
        View controllerPlaceholder = findViewById(R.id.exo_controller_placeholder);
        if (customController != null) {
            this.controller = customController;
        } else if (controllerPlaceholder != null) {
            // Propagate attrs as playbackAttrs so that PlayerControlView's custom attributes are
            // transferred, but standard FrameLayout attributes (e.g. background) are not.
            this.controller = new PlayerControlView(context, null, 0, attrs);
            controller.setLayoutParams(controllerPlaceholder.getLayoutParams());
            ViewGroup parent = ((ViewGroup) controllerPlaceholder.getParent());
            int controllerIndex = parent.indexOfChild(controllerPlaceholder);
            parent.removeView(controllerPlaceholder);
            parent.addView(controller, controllerIndex);
        } else {
            this.controller = null;
        }
        this.controllerShowTimeoutMs = controller != null ? controllerShowTimeoutMs : 0;
        this.controllerHideOnTouch = controllerHideOnTouch;
        this.controllerAutoShow = controllerAutoShow;
        this.controllerHideDuringAds = controllerHideDuringAds;
        this.useController = useController && controller != null;
        this.controller.setPlayOrPauseStatusListener(this);
        this.controller.setVisibilityListener(this);

        touchControllView = findViewById(R.id.tcv);
        setOnTouchListener(this);
        gestureDetector = new GestureDetector(context, new GestureListener());
        gestureDetector.setIsLongpressEnabled(false);

        hideController();
    }

    public AdsView getAdOverlayFrameLayout() {
        return adOverlayFrameLayout;
    }

    public void setScreenSize(int width, int height) {
        LayoutParams params =
                new LayoutParams(
                        width, height);//640 360
        surfaceView.setLayoutParams(params);
        setLayoutParams(params);
        max = false;
        hideController();
    }

    public void setMax(boolean isMax) {
        this.max = isMax;
        if (!isMax) {
            iv_playOrPause.setVisibility(View.GONE);
            hideController();
        }
    }

    public void setFullScreen() {
        LayoutParams params =
                new LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        surfaceView.setLayoutParams(params);
        setLayoutParams(params);
        max = true;
    }

    public void setTryFreeMode(boolean tryFree) {
        this.tryFree = tryFree;
    }

    /**
     * Switches the view targeted by a given {@link Player}.
     *
     * @param player        The player whose target view is being switched.
     * @param oldPlayerView The old view to detach from the player.
     * @param newPlayerView The new view to attach to the player.
     */
    public static void switchTargetView(
            Player player, @Nullable PlayerView oldPlayerView, @Nullable PlayerView newPlayerView) {
        if (oldPlayerView == newPlayerView) {
            return;
        }
        // We attach the new view before detaching the old one because this ordering allows the player
        // to swap directly from one surface to another, without transitioning through a state where no
        // surface is attached. This is significantly more efficient and achieves a more seamless
        // transition when using platform provided video decoders.
        if (newPlayerView != null) {
            newPlayerView.setPlayer(player);
        }
        if (oldPlayerView != null) {
            oldPlayerView.setPlayer(null);
        }
    }

    /**
     * Returns the player currently set on this view, or null if no player is set.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Set the {@link Player} to use.
     *
     * <p>To transition a {@link Player} from targeting one view to another, it's recommended to use
     * {@link #switchTargetView(Player, PlayerView, PlayerView)} rather than this method. If you do
     * wish to use this method directly, be sure to attach the player to the new view <em>before</em>
     * calling {@code setPlayer(null)} to detach it from the old one. This ordering is significantly
     * more efficient and may allow for more seamless transitions.
     *
     * @param player The {@link Player} to use, or {@code null} to detach the current player. Only
     *               players which are accessed on the main thread are supported ({@code
     *               player.getApplicationLooper() == Looper.getMainLooper()}).
     */
    public void setPlayer(@Nullable Player player) {
        Assertions.checkState(Looper.myLooper() == Looper.getMainLooper());
        Assertions.checkArgument(
                player == null || player.getApplicationLooper() == Looper.getMainLooper());
        if (this.player == player) {
            return;
        }
        if (this.player != null) {
            this.player.removeListener(componentListener);
            VideoComponent oldVideoComponent = this.player.getVideoComponent();
            if (oldVideoComponent != null) {
                oldVideoComponent.removeVideoListener(componentListener);
                if (surfaceView instanceof TextureView) {
                    oldVideoComponent.clearVideoTextureView((TextureView) surfaceView);
                } else if (surfaceView instanceof SphericalSurfaceView) {
                    ((SphericalSurfaceView) surfaceView).setVideoComponent(null);
                } else if (surfaceView instanceof SurfaceView) {
                    oldVideoComponent.clearVideoSurfaceView((SurfaceView) surfaceView);
                }
            }
            Player.TextComponent oldTextComponent = this.player.getTextComponent();
            if (oldTextComponent != null) {
                oldTextComponent.removeTextOutput(componentListener);
            }
        }
        this.player = player;
        if (useController) {
            controller.setPlayer(player);
        }
        if (subtitleView != null) {
            subtitleView.setCues(null);
        }
        updateBuffering();
        updateErrorMessage();
        updateForCurrentTrackSelections(/* isNewPlayer= */ true);
        if (player != null) {
            VideoComponent newVideoComponent = player.getVideoComponent();
            if (newVideoComponent != null) {
                if (surfaceView instanceof TextureView) {
                    newVideoComponent.setVideoTextureView((TextureView) surfaceView);
                } else if (surfaceView instanceof SphericalSurfaceView) {
                    ((SphericalSurfaceView) surfaceView).setVideoComponent(newVideoComponent);
                } else if (surfaceView instanceof SurfaceView) {
                    newVideoComponent.setVideoSurfaceView((SurfaceView) surfaceView);
                }
                newVideoComponent.addVideoListener(componentListener);
            }
            Player.TextComponent newTextComponent = player.getTextComponent();
            if (newTextComponent != null) {
                newTextComponent.addTextOutput(componentListener);
            }
            player.addListener(componentListener);
            maybeShowController(false);
        } else {
            hideController();
        }
        iv_playOrPause.setImageResource(R.drawable.icon_pause);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (surfaceView instanceof SurfaceView) {
            // Work around https://github.com/google/ExoPlayer/issues/3160.
            surfaceView.setVisibility(visibility);
        }
    }

    /**
     * Sets the {@link ResizeMode}.
     *
     * @param resizeMode The {@link ResizeMode}.
     */
    public void setResizeMode(@ResizeMode int resizeMode) {
        Assertions.checkState(contentFrame != null);
        contentFrame.setResizeMode(resizeMode);
    }

    /**
     * Returns the {@link ResizeMode}.
     */
    public @ResizeMode
    int getResizeMode() {
        Assertions.checkState(contentFrame != null);
        return contentFrame.getResizeMode();
    }

    /**
     * Returns whether artwork is displayed if present in the media.
     */
    public boolean getUseArtwork() {
        return useArtwork;
    }

    /**
     * Sets whether artwork is displayed if present in the media.
     *
     * @param useArtwork Whether artwork is displayed.
     */
    public void setUseArtwork(boolean useArtwork) {
        Assertions.checkState(!useArtwork || artworkView != null);
        if (this.useArtwork != useArtwork) {
            this.useArtwork = useArtwork;
            updateForCurrentTrackSelections(/* isNewPlayer= */ false);
        }
    }

    /**
     * Returns the default artwork to display.
     */
    public @Nullable
    Drawable getDefaultArtwork() {
        return defaultArtwork;
    }

    /**
     * Sets the default artwork to display if {@code useArtwork} is {@code true} and no artwork is
     * present in the media.
     *
     * @param defaultArtwork the default artwork to display.
     * @deprecated use (@link {@link #setDefaultArtwork(Drawable)} instead.
     */
    @Deprecated
    public void setDefaultArtwork(@Nullable Bitmap defaultArtwork) {
        setDefaultArtwork(
                defaultArtwork == null ? null : new BitmapDrawable(getResources(), defaultArtwork));
    }

    /**
     * Sets the default artwork to display if {@code useArtwork} is {@code true} and no artwork is
     * present in the media.
     *
     * @param defaultArtwork the default artwork to display
     */
    public void setDefaultArtwork(@Nullable Drawable defaultArtwork) {
        if (this.defaultArtwork != defaultArtwork) {
            this.defaultArtwork = defaultArtwork;
            updateForCurrentTrackSelections(/* isNewPlayer= */ false);
        }
    }

    /**
     * Returns whether the playback controls can be shown.
     */
    public boolean getUseController() {
        return useController;
    }

    public PlayerControlView getController() {
        return controller;
    }

    /**
     * Sets whether the playback controls can be shown. If set to {@code false} the playback controls
     * are never visible and are disconnected from the player.
     *
     * @param useController Whether the playback controls can be shown.
     */
    public void setUseController(boolean useController) {
        Assertions.checkState(!useController || controller != null);
        if (this.useController == useController) {
            return;
        }
        this.useController = useController;
        if (useController) {
            controller.setPlayer(player);
        } else if (controller != null) {
            controller.hide();
            controller.setPlayer(null);
        }
    }

    /**
     * Sets the background color of the {@code exo_shutter} view.
     *
     * @param color The background color.
     */
    public void setShutterBackgroundColor(int color) {
        if (shutterView != null) {
            shutterView.setBackgroundColor(color);
        }
    }

    /**
     * Sets whether the currently displayed video frame or media artwork is kept visible when the
     * player is reset. A player reset is defined to mean the player being re-prepared with different
     * media, the player transitioning to unprepared media, {@link Player#stop(boolean)} being called
     * with {@code reset=true}, or the player being replaced or cleared by calling {@link
     * #setPlayer(Player)}.
     *
     * <p>If enabled, the currently displayed video frame or media artwork will be kept visible until
     * the player set on the view has been successfully prepared with new media and loaded enough of
     * it to have determined the available tracks. Hence enabling this option allows transitioning
     * from playing one piece of media to another, or from using one player instance to another,
     * without clearing the view's content.
     *
     * <p>If disabled, the currently displayed video frame or media artwork will be hidden as soon as
     * the player is reset. Note that the video frame is hidden by making {@code exo_shutter} visible.
     * Hence the video frame will not be hidden if using a custom layout that omits this view.
     *
     * @param keepContentOnPlayerReset Whether the currently displayed video frame or media artwork is
     *                                 kept visible when the player is reset.
     */
    public void setKeepContentOnPlayerReset(boolean keepContentOnPlayerReset) {
        if (this.keepContentOnPlayerReset != keepContentOnPlayerReset) {
            this.keepContentOnPlayerReset = keepContentOnPlayerReset;
            updateForCurrentTrackSelections(/* isNewPlayer= */ false);
        }
    }

    /**
     * Sets whether a buffering spinner is displayed when the player is in the buffering state. The
     * buffering spinner is not displayed by default.
     *
     * @param showBuffering Whether the buffering icon is displayed
     * @deprecated Use {@link #setShowBuffering(int)}
     */
    @Deprecated
    public void setShowBuffering(boolean showBuffering) {
        setShowBuffering(showBuffering ? SHOW_BUFFERING_WHEN_PLAYING : SHOW_BUFFERING_NEVER);
    }

    /**
     * Sets whether a buffering spinner is displayed when the player is in the buffering state. The
     * buffering spinner is not displayed by default.
     *
     * @param showBuffering The mode that defines when the buffering spinner is displayed. One of
     *                      {@link #SHOW_BUFFERING_NEVER}, {@link #SHOW_BUFFERING_WHEN_PLAYING} and
     *                      {@link #SHOW_BUFFERING_ALWAYS}.
     */
    public void setShowBuffering(@ShowBuffering int showBuffering) {
        if (this.showBuffering != showBuffering) {
            this.showBuffering = showBuffering;
            updateBuffering();
        }
    }

    public void setName(String name) {
        if (controller != null) {
            controller.setName(name);
        }
    }

    public void setAdsUrl(boolean hasAds, String adsUrl) {
        this.hasAds = hasAds;
        this.adsUrl = hasAds ? adsUrl : "";
        adOverlayFrameLayout.setHasAds(hasAds, adsUrl);
        if (hasAds) {
            iv_playOrPause.setVisibility(View.GONE);
        }
    }

    /**
     * Sets the optional {@link ErrorMessageProvider}.
     *
     * @param errorMessageProvider The error message provider.
     */
    public void setErrorMessageProvider(
            @Nullable ErrorMessageProvider<? super ExoPlaybackException> errorMessageProvider) {
        if (this.errorMessageProvider != errorMessageProvider) {
            this.errorMessageProvider = errorMessageProvider;
            updateErrorMessage();
        }
    }

    /**
     * Sets a custom error message to be displayed by the view. The error message will be displayed
     * permanently, unless it is cleared by passing {@code null} to this method.
     *
     * @param message The message to display, or {@code null} to clear a previously set message.
     */
    public void setCustomErrorMessage(@Nullable CharSequence message) {
        Assertions.checkState(errorMessageView != null);
        customErrorMessage = message;
        updateErrorMessage();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (player != null && isPlayingAd() && adOverlayFrameLayout != null) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                return false;
            }
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER && event.getAction() == KeyEvent.ACTION_UP) {
                adOverlayFrameLayout.seeClick();
            }
            return true;
        }
        if (tryFree && (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT
                || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER)) {
            return true;
        }
        if (!max && event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER) {
            return false;
        }
        boolean isDpadWhenControlHidden =
                isDpadKey(event.getKeyCode()) && useController && !controller.isVisible();
        boolean handled =
                isDpadWhenControlHidden || dispatchMediaKeyEvent(event) || super.dispatchKeyEvent(event);

        if (handled && max) {
            maybeShowController(true);
        }
        return handled;
    }

    /**
     * Called to process media key events. Any {@link KeyEvent} can be passed but only media key
     * events will be handled. Does nothing if playback controls are disabled.
     *
     * @param event A key event.
     * @return Whether the key event was handled.
     */
    public boolean dispatchMediaKeyEvent(KeyEvent event) {
        return useController && controller.dispatchMediaKeyEvent(event);
    }

    /**
     * Returns whether the controller is currently visible.
     */
    public boolean isControllerVisible() {
        return controller != null && controller.isVisible();
    }

    /**
     * Shows the playback controls. Does nothing if playback controls are disabled.
     *
     * <p>The playback controls are automatically hidden during playback after {{@link
     * #getControllerShowTimeoutMs()}}. They are shown indefinitely when playback has not started yet,
     * is paused, has ended or failed.
     */
    public void showController() {
        showController(shouldShowControllerIndefinitely());
    }

    /**
     * Hides the playback controls. Does nothing if playback controls are disabled.
     */
    public void hideController() {
        if (controller != null) {
            controller.hide();
        }
    }

    /**
     * Returns the playback controls timeout. The playback controls are automatically hidden after
     * this duration of time has elapsed without user input and with playback or buffering in
     * progress.
     *
     * @return The timeout in milliseconds. A non-positive value will cause the controller to remain
     * visible indefinitely.
     */
    public int getControllerShowTimeoutMs() {
        return controllerShowTimeoutMs;
    }

    /**
     * Sets the playback controls timeout. The playback controls are automatically hidden after this
     * duration of time has elapsed without user input and with playback or buffering in progress.
     *
     * @param controllerShowTimeoutMs The timeout in milliseconds. A non-positive value will cause the
     *                                controller to remain visible indefinitely.
     */
    public void setControllerShowTimeoutMs(int controllerShowTimeoutMs) {
        Assertions.checkState(controller != null);
        this.controllerShowTimeoutMs = controllerShowTimeoutMs;
        if (controller.isVisible()) {
            // Update the controller's timeout if necessary.
            showController();
        }
    }

    /**
     * Returns whether the playback controls are hidden by touch events.
     */
    public boolean getControllerHideOnTouch() {
        return controllerHideOnTouch;
    }

    /**
     * Sets whether the playback controls are hidden by touch events.
     *
     * @param controllerHideOnTouch Whether the playback controls are hidden by touch events.
     */
    public void setControllerHideOnTouch(boolean controllerHideOnTouch) {
        Assertions.checkState(controller != null);
        this.controllerHideOnTouch = controllerHideOnTouch;
    }

    /**
     * Returns whether the playback controls are automatically shown when playback starts, pauses,
     * ends, or fails. If set to false, the playback controls can be manually operated with {@link
     * #showController()} and {@link #hideController()}.
     */
    public boolean getControllerAutoShow() {
        return controllerAutoShow;
    }

    /**
     * Sets whether the playback controls are automatically shown when playback starts, pauses, ends,
     * or fails. If set to false, the playback controls can be manually operated with {@link
     * #showController()} and {@link #hideController()}.
     *
     * @param controllerAutoShow Whether the playback controls are allowed to show automatically.
     */
    public void setControllerAutoShow(boolean controllerAutoShow) {
        this.controllerAutoShow = controllerAutoShow;
    }

    /**
     * Sets whether the playback controls are hidden when ads are playing. Controls are always shown
     * during ads if they are enabled and the player is paused.
     *
     * @param controllerHideDuringAds Whether the playback controls are hidden when ads are playing.
     */
    public void setControllerHideDuringAds(boolean controllerHideDuringAds) {
        this.controllerHideDuringAds = controllerHideDuringAds;
    }

    /**
     * Set the {@link PlayerControlView.VisibilityListener}.
     *
     * @param listener The listener to be notified about visibility changes.
     */
    public void setControllerVisibilityListener(PlayerControlView.VisibilityListener listener) {
        Assertions.checkState(controller != null);
        controller.setVisibilityListener(listener);
    }

    /**
     * Sets the {@link PlaybackPreparer}.
     *
     * @param playbackPreparer The {@link PlaybackPreparer}.
     */
    public void setPlaybackPreparer(@Nullable PlaybackPreparer playbackPreparer) {
        Assertions.checkState(controller != null);
        controller.setPlaybackPreparer(playbackPreparer);
    }

    /**
     * Sets the {@link ControlDispatcher}.
     *
     * @param controlDispatcher The {@link ControlDispatcher}, or null to use {@link
     *                          DefaultControlDispatcher}.
     */
    public void setControlDispatcher(@Nullable ControlDispatcher controlDispatcher) {
        Assertions.checkState(controller != null);
        controller.setControlDispatcher(controlDispatcher);
    }

    /**
     * Sets the rewind increment in milliseconds.
     *
     * @param rewindMs The rewind increment in milliseconds. A non-positive value will cause the
     *                 rewind button to be disabled.
     */
    public void setRewindIncrementMs(int rewindMs) {
        Assertions.checkState(controller != null);
        controller.setRewindIncrementMs(rewindMs);
    }

    /**
     * Sets the fast forward increment in milliseconds.
     *
     * @param fastForwardMs The fast forward increment in milliseconds. A non-positive value will
     *                      cause the fast forward button to be disabled.
     */
    public void setFastForwardIncrementMs(int fastForwardMs) {
        Assertions.checkState(controller != null);
        controller.setFastForwardIncrementMs(fastForwardMs);
    }

    /**
     * Sets which repeat toggle modes are enabled.
     *
     * @param repeatToggleModes A set of {@link RepeatModeUtil.RepeatToggleModes}.
     */
    public void setRepeatToggleModes(@RepeatModeUtil.RepeatToggleModes int repeatToggleModes) {
        Assertions.checkState(controller != null);
        controller.setRepeatToggleModes(repeatToggleModes);
    }

    /**
     * Sets whether the shuffle button is shown.
     *
     * @param showShuffleButton Whether the shuffle button is shown.
     */
    public void setShowShuffleButton(boolean showShuffleButton) {
        Assertions.checkState(controller != null);
        controller.setShowShuffleButton(showShuffleButton);
    }

    /**
     * Sets whether the time bar should show all windows, as opposed to just the current one.
     *
     * @param showMultiWindowTimeBar Whether to show all windows.
     */
    public void setShowMultiWindowTimeBar(boolean showMultiWindowTimeBar) {
        Assertions.checkState(controller != null);
        controller.setShowMultiWindowTimeBar(showMultiWindowTimeBar);
    }

    /**
     * Sets the millisecond positions of extra ad markers relative to the start of the window (or
     * timeline, if in multi-window mode) and whether each extra ad has been played or not. The
     * markers are shown in addition to any ad markers for ads in the player's timeline.
     *
     * @param extraAdGroupTimesMs The millisecond timestamps of the extra ad markers to show, or
     *                            {@code null} to show no extra ad markers.
     * @param extraPlayedAdGroups Whether each ad has been played, or {@code null} to show no extra ad
     *                            markers.
     */
    public void setExtraAdGroupMarkers(
            @Nullable long[] extraAdGroupTimesMs, @Nullable boolean[] extraPlayedAdGroups) {
        Assertions.checkState(controller != null);
        controller.setExtraAdGroupMarkers(extraAdGroupTimesMs, extraPlayedAdGroups);
    }

    /**
     * Set the {@link AspectRatioFrameLayout.AspectRatioListener}.
     *
     * @param listener The listener to be notified about aspect ratios changes of the video content or
     *                 the content frame.
     */
    public void setAspectRatioListener(AspectRatioFrameLayout.AspectRatioListener listener) {
        Assertions.checkState(contentFrame != null);
        contentFrame.setAspectRatioListener(listener);
    }

    /**
     * Gets the view onto which video is rendered. This is a:
     *
     * <ul>
     * <li>{@link SurfaceView} by default, or if the {@code surface_type} attribute is set to {@code
     * surface_view}.
     * <li>{@link TextureView} if {@code surface_type} is {@code texture_view}.
     * <li>{@link SphericalSurfaceView} if {@code surface_type} is {@code spherical_view}.
     * <li>{@code null} if {@code surface_type} is {@code none}.
     * </ul>
     *
     * @return The {@link SurfaceView}, {@link TextureView}, {@link SphericalSurfaceView} or {@code
     * null}.
     */
    public View getVideoSurfaceView() {
        return surfaceView;
    }

    /**
     * Gets the overlay {@link FrameLayout}, which can be populated with UI elements to show on top of
     * the player.
     *
     * @return The overlay {@link FrameLayout}, or {@code null} if the layout has been customized and
     * the overlay is not present.
     */
    @Nullable
    public FrameLayout getOverlayFrameLayout() {
        return overlayFrameLayout;
    }

    /**
     * Gets the {@link SubtitleView}.
     *
     * @return The {@link SubtitleView}, or {@code null} if the layout has been customized and the
     * subtitle view is not present.
     */
    public SubtitleView getSubtitleView() {
        return subtitleView;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() != MotionEvent.ACTION_DOWN) {
            return false;
        }
        return performClick();
    }

    public void setTVMode(boolean isTVMode) {
        this.isTVMode = isTVMode;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return toggleControllerVisibility();
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        if (!useController || player == null) {
            return false;
        }
        maybeShowController(true);
        return true;
    }

    public void onResume() {
        if (surfaceView instanceof SphericalSurfaceView) {
            ((SphericalSurfaceView) surfaceView).onResume();
        }
    }

    public void onPause() {
        if (surfaceView instanceof SphericalSurfaceView) {
            ((SphericalSurfaceView) surfaceView).onPause();
        }
    }

    /**
     * Called when there's a change in the aspect ratio of the content being displayed. The default
     * implementation sets the aspect ratio of the content frame to that of the content, unless the
     * content view is a {@link SphericalSurfaceView} in which case the frame's aspect ratio is
     * cleared.
     *
     * @param contentAspectRatio The aspect ratio of the content.
     * @param contentFrame       The content frame, or {@code null}.
     * @param contentView        The view that holds the content being displayed, or {@code null}.
     */
    protected void onContentAspectRatioChanged(
            float contentAspectRatio,
            @Nullable AspectRatioFrameLayout contentFrame,
            @Nullable View contentView) {
        if (contentFrame != null) {
            contentFrame.setAspectRatio(
                    contentView instanceof SphericalSurfaceView ? 0 : contentAspectRatio);

        }
    }

    // AdsLoader.AdViewProvider implementation.

    @Override
    public ViewGroup getAdViewGroup() {
        return Assertions.checkNotNull(
                adOverlayFrameLayout, "exo_ad_overlay must be present for ad playback");
    }

    @Override
    public View[] getAdOverlayViews() {
        ArrayList<View> overlayViews = new ArrayList<>();
        if (overlayFrameLayout != null) {
            overlayViews.add(overlayFrameLayout);
        }
        if (controller != null) {
            overlayViews.add(controller);
        }
        return overlayViews.toArray(new View[0]);
    }

    // Internal methods.

    private boolean toggleControllerVisibility() {
        if (!useController || player == null) {
            return false;
        }
        if (!controller.isVisible()) {
            maybeShowController(true);
        } else if (controllerHideOnTouch) {
            controller.hide();
        }
        return true;
    }

    /**
     * Shows the playback controls, but only if forced or shown indefinitely.
     */
    public void maybeShowController(boolean isForced) {
        if (isPlayingAd() && controllerHideDuringAds) {
            return;
        }
        if (useController && max) {
            boolean wasShowingIndefinitely = controller.isVisible() && controller.getShowTimeoutMs() <= 0;
            boolean shouldShowIndefinitely = shouldShowControllerIndefinitely();
            if (isForced || wasShowingIndefinitely || shouldShowIndefinitely) {
                showController(shouldShowIndefinitely);
            }
        }
    }

    private boolean shouldShowControllerIndefinitely() {
        if (player == null) {
            return true;
        }
        int playbackState = player.getPlaybackState();
        return controllerAutoShow
                && (playbackState == Player.STATE_IDLE
                || playbackState == Player.STATE_ENDED
                || !player.getPlayWhenReady());
    }

    private void showController(boolean showIndefinitely) {
        if (!useController) {
            return;
        }
        controller.setShowTimeoutMs(showIndefinitely ? 0 : controllerShowTimeoutMs);
        controller.show();
        controller.requestFocus();
    }

    public boolean isPlayingAd() {//player.isPlayingAd()&&
        if (isImageAds(adsUrl)) {
            return player != null && player.getPlayWhenReady() && player.getPlaybackState() == Player.STATE_IDLE;
        } else {
            return player != null && playingAd() && player.getPlayWhenReady();
        }
    }

    private boolean playingAd() {
        if (player.getCurrentPeriodIndex() == 0 && hasAds) {
            return true;
        }
        return false;
    }

    public void setAdsStateListener(AdsView.AdsState adsState) {
        adOverlayFrameLayout.setAdsStateListener(adsState);
    }

    private void updateForCurrentTrackSelections(boolean isNewPlayer) {
        if (player == null || player.getCurrentTrackGroups().isEmpty()) {
            if (!keepContentOnPlayerReset) {
                hideArtwork();
                closeShutter();
            }
            return;
        }

        if (isNewPlayer && !keepContentOnPlayerReset) {
            // Hide any video from the previous player.
            closeShutter();
        }

        TrackSelectionArray selections = player.getCurrentTrackSelections();
        for (int i = 0; i < selections.length; i++) {
            if (player.getRendererType(i) == C.TRACK_TYPE_VIDEO && selections.get(i) != null) {
                // Video enabled so artwork must be hidden. If the shutter is closed, it will be opened in
                // onRenderedFirstFrame().
                hideArtwork();
                return;
            }
        }

        // Video disabled so the shutter must be closed.
        closeShutter();
        // Display artwork if enabled and available, else hide it.
        if (useArtwork) {
            for (int i = 0; i < selections.length; i++) {
                TrackSelection selection = selections.get(i);
                if (selection != null) {
                    for (int j = 0; j < selection.length(); j++) {
                        Metadata metadata = selection.getFormat(j).metadata;
                        if (metadata != null && setArtworkFromMetadata(metadata)) {
                            return;
                        }
                    }
                }
            }
            if (setDrawableArtwork(defaultArtwork)) {
                return;
            }
        }
        // Artwork disabled or unavailable.
        hideArtwork();
    }

    private boolean setArtworkFromMetadata(Metadata metadata) {
        for (int i = 0; i < metadata.length(); i++) {
            Metadata.Entry metadataEntry = metadata.get(i);
            if (metadataEntry instanceof ApicFrame) {
                byte[] bitmapData = ((ApicFrame) metadataEntry).pictureData;
                Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
                return setDrawableArtwork(new BitmapDrawable(getResources(), bitmap));
            }
        }
        return false;
    }

    private boolean setDrawableArtwork(@Nullable Drawable drawable) {
        if (drawable != null) {
            int drawableWidth = drawable.getIntrinsicWidth();
            int drawableHeight = drawable.getIntrinsicHeight();
            if (drawableWidth > 0 && drawableHeight > 0) {
                float artworkAspectRatio = (float) drawableWidth / drawableHeight;
                onContentAspectRatioChanged(artworkAspectRatio, contentFrame, artworkView);
                artworkView.setImageDrawable(drawable);
                artworkView.setVisibility(VISIBLE);
                return true;
            }
        }
        return false;
    }

    private void hideArtwork() {
        if (artworkView != null) {
            artworkView.setImageResource(android.R.color.transparent); // Clears any bitmap reference.
            artworkView.setVisibility(INVISIBLE);
        }
    }

    private void closeShutter() {
        if (shutterView != null) {
            shutterView.setVisibility(View.VISIBLE);
        }
    }

    private void updateBuffering() {
        if (lav != null) {
            boolean showBufferingSpinner =
                    player != null
                            && player.getPlaybackState() == Player.STATE_BUFFERING
                            && (showBuffering == SHOW_BUFFERING_ALWAYS
                            || (showBuffering == SHOW_BUFFERING_WHEN_PLAYING && player.getPlayWhenReady()));
            lav.setVisibility(showBufferingSpinner ? View.VISIBLE : View.GONE);
        }
    }

    private void updateErrorMessage() {
        if (errorMessageView != null) {
            if (customErrorMessage != null) {
                errorMessageView.setText(customErrorMessage);
                errorMessageView.setVisibility(View.VISIBLE);
                return;
            }
            ExoPlaybackException error = null;
            if (player != null
                    && player.getPlaybackState() == Player.STATE_IDLE
                    && errorMessageProvider != null) {
                error = player.getPlaybackError();
            }
            if (error != null) {
                CharSequence errorMessage = errorMessageProvider.getErrorMessage(error).second;
                errorMessageView.setText(errorMessage);
                errorMessageView.setVisibility(View.VISIBLE);
            } else {
                errorMessageView.setVisibility(View.GONE);
            }
        }
    }

    @TargetApi(23)
    private static void configureEditModeLogoV23(Resources resources, ImageView logo) {
        logo.setImageDrawable(resources.getDrawable(com.google.android.exoplayer2.ui.R.drawable.exo_edit_mode_logo, null));
        logo.setBackgroundColor(resources.getColor(com.google.android.exoplayer2.ui.R.color.exo_edit_mode_background_color, null));
    }

    @SuppressWarnings("deprecation")
    private static void configureEditModeLogo(Resources resources, ImageView logo) {
        logo.setImageDrawable(resources.getDrawable(com.google.android.exoplayer2.ui.R.drawable.exo_edit_mode_logo));
        logo.setBackgroundColor(resources.getColor(com.google.android.exoplayer2.ui.R.color.exo_edit_mode_background_color));
    }

    @SuppressWarnings("ResourceType")
    private static void setResizeModeRaw(AspectRatioFrameLayout aspectRatioFrame, int resizeMode) {
        aspectRatioFrame.setResizeMode(resizeMode);
    }

    /**
     * Applies a texture rotation to a {@link TextureView}.
     */
    private static void applyTextureViewRotation(TextureView textureView, int textureViewRotation) {
        float textureViewWidth = textureView.getWidth();
        float textureViewHeight = textureView.getHeight();
        if (textureViewWidth == 0 || textureViewHeight == 0 || textureViewRotation == 0) {
            textureView.setTransform(null);
        } else {
            Matrix transformMatrix = new Matrix();
            float pivotX = textureViewWidth / 2;
            float pivotY = textureViewHeight / 2;
            transformMatrix.postRotate(textureViewRotation, pivotX, pivotY);

            // After rotation, scale the rotated texture to fit the TextureView size.
            RectF originalTextureRect = new RectF(0, 0, textureViewWidth, textureViewHeight);
            RectF rotatedTextureRect = new RectF();
            transformMatrix.mapRect(rotatedTextureRect, originalTextureRect);
            transformMatrix.postScale(
                    textureViewWidth / rotatedTextureRect.width(),
                    textureViewHeight / rotatedTextureRect.height(),
                    pivotX,
                    pivotY);
            textureView.setTransform(transformMatrix);
        }
    }

    @SuppressLint("InlinedApi")
    private boolean isDpadKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_UP
                || keyCode == KeyEvent.KEYCODE_DPAD_UP_RIGHT
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                || keyCode == KeyEvent.KEYCODE_DPAD_DOWN_RIGHT
                || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                || keyCode == KeyEvent.KEYCODE_DPAD_DOWN_LEFT
                || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KeyEvent.KEYCODE_DPAD_UP_LEFT
                || keyCode == KeyEvent.KEYCODE_DPAD_CENTER;
    }

    private final class ComponentListener
            implements Player.EventListener,
            TextOutput,
            VideoListener,
            OnLayoutChangeListener,
            SphericalSurfaceView.SurfaceListener,
            SingleTapListener {

        // TextOutput implementation

        @Override
        public void onCues(List<Cue> cues) {
            if (subtitleView != null) {
                subtitleView.onCues(cues);
            }
        }

        // VideoListener implementation

        @Override
        public void onVideoSizeChanged(
                int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
            float videoAspectRatio =
                    (height == 0 || width == 0) ? 1 : (width * pixelWidthHeightRatio) / height;
            if (surfaceView instanceof TextureView) {
                // Try to apply rotation transformation when our surface is a TextureView.
                if (unappliedRotationDegrees == 90 || unappliedRotationDegrees == 270) {
                    // We will apply a rotation 90/270 degree to the output texture of the TextureView.
                    // In this case, the output video's width and height will be swapped.
                    videoAspectRatio = 1 / videoAspectRatio;
                }
                if (textureViewRotation != 0) {
                    surfaceView.removeOnLayoutChangeListener(this);
                }
                textureViewRotation = unappliedRotationDegrees;
                if (textureViewRotation != 0) {
                    // The texture view's dimensions might be changed after layout step.
                    // So add an OnLayoutChangeListener to apply rotation after layout step.
                    surfaceView.addOnLayoutChangeListener(this);
                }
                applyTextureViewRotation((TextureView) surfaceView, textureViewRotation);
            }
            videoSize[0] = width;
            videoSize[1] = height;
            onContentAspectRatioChanged(videoAspectRatio, contentFrame, surfaceView);
        }

        @Override
        public void onRenderedFirstFrame() {
            if (shutterView != null) {
                shutterView.setVisibility(INVISIBLE);
            }
        }

        @Override
        public void onTracksChanged(TrackGroupArray tracks, TrackSelectionArray selections) {
            updateForCurrentTrackSelections(/* isNewPlayer= */ false);
        }

        // Player.EventListener implementation

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            updateBuffering();
            updateErrorMessage();
            if (isPlayingAd() && controllerHideDuringAds) {
                hideController();
            } else {
                if (max) {
                    maybeShowController(false);
                }
            }
            if (playbackState == Player.STATE_ENDED) {
                if (max) {
                    iv_playOrPause.setVisibility(View.VISIBLE);
                    iv_playOrPause.setImageResource(R.drawable.icon_play);
                }
                if (playState != null) {
                    playState.playComplete();
                }
            }
            if (playbackState == Player.STATE_READY) {
                if (isPlayingAd()) {
                    adOverlayFrameLayout.setDurationText(player.getDuration());
                }
            }
        }

        @Override
        public void onPositionDiscontinuity(@DiscontinuityReason int reason) {
            if (isPlayingAd() && controllerHideDuringAds) {
                hideController();
            }
        }

        // OnLayoutChangeListener implementation

        @Override
        public void onLayoutChange(
                View view,
                int left,
                int top,
                int right,
                int bottom,
                int oldLeft,
                int oldTop,
                int oldRight,
                int oldBottom) {
            applyTextureViewRotation((TextureView) view, textureViewRotation);
        }

        // SphericalSurfaceView.SurfaceTextureListener implementation

        @Override
        public void surfaceChanged(@Nullable Surface surface) {
            if (player != null) {
                VideoComponent videoComponent = player.getVideoComponent();
                if (videoComponent != null) {
                    videoComponent.setVideoSurface(surface);
                }
            }
        }

        // SingleTapListener implementation

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return toggleControllerVisibility();
        }
    }

    class SettingVoiceObserver extends ContentObserver {

        /**
         * Creates a content observer.
         *
         * @param handler The handler to run {@link #onChange} on, or null if none.
         */
        public SettingVoiceObserver(Handler handler) {
            super(handler);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            setVolume(Utils.getCurrentSystemVolume(getContext()));
        }
    }

    public void setVolume(float volume) {
        if (player != null) {
            ((SimpleExoPlayer) player).setVolume(volume);
        }
    }

    public void unregistVoiceSetting() {
        getContext().getContentResolver().unregisterContentObserver(settingVoiceObserver);
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

    public void setPlayStateListener(PlayState playState) {
        this.playState = playState;
    }

    public interface PlayState {
        void playComplete();
    }

    class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (onTouch) {
                if (x < Utils.getScreenWidth(getContext()) / 3 && Math.abs(dx) < limitX && distanceY > 0) {
                    FLAG_CONTROL = 0;
                    onTouch = false;
                } else if (x < Utils.getScreenWidth(getContext()) / 3 && Math.abs(dx) < limitX && distanceY < 0) {
                    FLAG_CONTROL = 0;
                    onTouch = false;
                }
                if (x > (Utils.getScreenWidth(getContext()) / 3) * 2 && Math.abs(dx) < limitX && distanceY > 0) {
                    FLAG_CONTROL = 1;
                    onTouch = false;
                } else if (x > (Utils.getScreenWidth(getContext()) / 3) * 2 && Math.abs(dx) < limitX && distanceY < 0) {
                    FLAG_CONTROL = 1;
                    onTouch = false;
                }
                if (Math.abs(distanceX) > Math.abs(distanceY)) {
                    if (Math.abs(dy) < limitX && distanceX > 0) {
                        FLAG_CONTROL = 2;
                        onTouch = false;
                    } else if (Math.abs(dy) < limitX && distanceX < 0) {
                        FLAG_CONTROL = 2;
                        onTouch = false;
                    }
                }
            }
            if (x < Utils.getScreenWidth(getContext()) / 3 && Math.abs(dx) < limitX && distanceY > 0 && FLAG_CONTROL == 0) {
                setBrightness(true);
            } else if (x < Utils.getScreenWidth(getContext()) / 3 && Math.abs(dx) < limitX && distanceY < 0 && FLAG_CONTROL == 0) {
                setBrightness(false);
            }
            if (x > (Utils.getScreenWidth(getContext()) / 3) * 2 && Math.abs(dx) < limitX && distanceY > 0 && FLAG_CONTROL == 1) {
                setVolumn(true);
            } else if (x > (Utils.getScreenWidth(getContext()) / 3) * 2 && Math.abs(dx) < limitX && distanceY < 0 && FLAG_CONTROL == 1) {
                setVolumn(false);
            }
            if (Math.abs(distanceX) > Math.abs(distanceY)) {
                if (Math.abs(dy) < limitX && distanceX > 0 && FLAG_CONTROL == 2 && max) {
                    setSeek(false);
                } else if (Math.abs(dy) < limitX && distanceX < 0 && FLAG_CONTROL == 2 && max) {
                    setSeek(true);
                }
            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    }

    private float curretnVolumn = 0.5f;

    public void setVolumn(boolean add) {
        Toast.makeText(getContext(), "setVolumn", Toast.LENGTH_SHORT).show();
        AudioManager manager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        if (curretnVolumn < 0.01f) {
            curretnVolumn = 0.00f;
        }
        if (add) {
            curretnVolumn = curretnVolumn + 0.01f;
        } else {
            curretnVolumn = curretnVolumn - 0.01f;
        }
        if (curretnVolumn > 1.0f) {
            curretnVolumn = 1.0f;
        } else if (curretnVolumn < 0.01f) {
            curretnVolumn = 0.0f;
        }
        manager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) (curretnVolumn * 15),
                0);
        touchControllView.setVisibility(View.VISIBLE);
        touchControllView.setVolumn(curretnVolumn);
    }

    public void setBrightness(boolean add) {
        Toast.makeText(getContext(), "setBrightness", Toast.LENGTH_SHORT).show();
        float brightness = ((Activity) getContext()).getWindow().getAttributes().screenBrightness;
        if (brightness <= 0.00f) {
            brightness = 0.50f;
        }
        if (brightness < 0.01f) {
            brightness = 0.01f;
        }
        if (add) {
            brightness = brightness + 0.01f;
        } else {
            brightness = brightness - 0.01f;
        }
        if (brightness > 1.0f) {
            brightness = 1.0f;
        } else if (brightness < 0.01f) {
            brightness = 0.01f;
        }
        Window window = ((Activity) getContext()).getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.screenBrightness = brightness;
        window.setAttributes(lp);
        touchControllView.setVisibility(View.VISIBLE);
        touchControllView.setBrightness(brightness);
    }

    public void setSeek(boolean add) {
        Toast.makeText(getContext(), "setSeek", Toast.LENGTH_SHORT).show();
        touchControllView.setVisibility(View.VISIBLE);
        if (duration == 0) {
            duration = player.getDuration();
        }
        seekCount = (int) (duration / 400);
        if (add) {
            seek = seek + 500;
        } else {
            seek = seek - 500;
        }
        if (seek <= 0) {
            seek = 0;
        }
        if (seek > duration) {
            seek = duration;
        }
        player.seekTo(seek);
        if (duration != 0) {
            touchControllView.setSeek(add, seek, duration);
            controller.getTimeBar().setPosition(seek);
        }
    }
}
