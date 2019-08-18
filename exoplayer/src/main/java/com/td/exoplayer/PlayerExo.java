package com.td.exoplayer;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.widget.Toast;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackPreparer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.offline.FilteringManifestParser;
import com.google.android.exoplayer2.offline.StreamKey;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.ads.AdsLoader;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.manifest.DashManifestParser;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.hls.playlist.DefaultHlsPlaylistParserFactory;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.manifest.SsManifestParser;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.util.ErrorMessageProvider;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.Util;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.List;

import com.td.exoplayer.utils.C;
import com.td.exoplayer.utils.SettingIml;
import com.td.exoplayer.utils.SettingReceiver;

import static com.google.android.exoplayer2.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON;
import static com.google.android.exoplayer2.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES;
import static com.google.android.exoplayer2.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS;

public class PlayerExo implements PlaybackPreparer, SettingIml, AdsView.AdsState {
    private Context context;
    private PlayerView playerView;
    private static final CookieManager DEFAULT_COOKIE_MANAGER;

    static {
        DEFAULT_COOKIE_MANAGER = new CookieManager();
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    private DataSource.Factory dataSourceFactory;

    private SimpleExoPlayer player;
    private FrameworkMediaDrm mediaDrm;
    private MediaSource mediaSource;
    private DefaultTrackSelector trackSelector;
    private DefaultTrackSelector.Parameters trackSelectorParameters;
    private TrackGroupArray lastSeenTrackGroupArray;

    private boolean startAutoPlay = true;
    private int startWindow;
    private long startPosition;
    private boolean loop = false;
    private int freeTime = 5 * 60 * 1000;
    private boolean tryFree = false;
    private TryModeIml tryModeIml;

    private String adTagUriString;
    private Uri[] uri = new Uri[1];//Uri.parse("http://218.70.82.218:9568/Data/2019-04-26/file_41f6245b-c53a-45e8-9a5c-8d08f39e30dc/playlist.m3u8")

    private AdsLoader adsLoader;
    private Uri loadedAdTagUri;
    private String title = "";
    private SettingReceiver receiver;
    private boolean init = false;
    private BaseFactory baseFactory;

    public PlayerExo(Context context, PlayerView playerView) {
        this.context = context;
        this.playerView = playerView;
        receiver = new SettingReceiver(this);
        baseFactory = BaseFactory.getInstance(context);
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(receiver, intentFilter);
    }

    public void setUris(Uri[] uris) {
        this.uri = uris;
    }

    public void setAdsUrl(String adsUrl) {
        this.adTagUriString = adsUrl;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setStartAutoPlay(boolean autoPlay) {
        this.startAutoPlay = autoPlay;
    }

    public void setStartPosition(int startWindow, long position) {
        this.startWindow = startWindow;
        this.startPosition = position;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public void setTryFreeMode(boolean tryFree) {
        this.tryFree = tryFree;
        playerView.setTryFreeMode(tryFree);
    }

    public void setTryTime(int tryTime) {
        this.freeTime = tryTime;
    }

    public void setTryModeIml(TryModeIml tryModeIml) {
        this.tryModeIml = tryModeIml;
    }

    public void setPlayStatus(boolean play) {
        if (player != null) {
            player.setPlayWhenReady(play);
        }
    }

    public void initExoPlayer(Uri[] uris, String adsUrl) {
        this.uri = uris;
        this.adTagUriString = adsUrl;
        playerView.setAdsUrl(adsUrl.trim().length() == 0 ? false : true, adsUrl);
        dataSourceFactory = buildDataSourceFactory();
        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
        }
        trackSelectorParameters = new DefaultTrackSelector.ParametersBuilder().build();
        TrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory();

        DefaultRenderersFactory renderersFactory =
                new DefaultRenderersFactory(context);
        renderersFactory.setExtensionRendererMode(EXTENSION_RENDERER_MODE_ON);

        trackSelector = new DefaultTrackSelector(trackSelectionFactory);
        trackSelector.setParameters(trackSelectorParameters);
        playerView.setErrorMessageProvider(new PlayerErrorMessageProvider());
        lastSeenTrackGroupArray = null;
//        playerView.requestFocus();
        playerView.getController().requestFocus();
        player = ExoPlayerFactory.newSimpleInstance(context, renderersFactory, trackSelector);
        player.addListener(new PlayerEventListener());
        player.setPlayWhenReady(startAutoPlay);
        player.addAnalyticsListener(new EventLogger(trackSelector));
        playerView.setPlayer(player);
        playerView.setPlaybackPreparer(this);

        MediaSource[] mediaSources = new MediaSource[uris.length];
        for (int i = 0; i < uris.length; i++) {
            if (uris[i] == null) {
                uri[i] = Uri.parse("");
            }
            mediaSources[i] = buildMediaSource(uris[i], null);
        }
        if (loop) {
            mediaSource = new LoopingMediaSource(buildMediaSource(uris[0], null));
        } else {
            mediaSource = new ConcatenatingMediaSource(mediaSources);
        }
//        mediaSource =
//                mediaSources.length == 1 ? mediaSources[0] : new ConcatenatingMediaSource(mediaSources);
        if (adTagUriString.length() > 0) {
            Uri adTagUri = Uri.parse(adTagUriString);
            if (!adTagUri.equals(loadedAdTagUri)) {
                releaseAdsLoader();
                loadedAdTagUri = adTagUri;
                playerView.setAdsUrl(true, adsUrl);
                if (isImageAds(adsUrl)) {
                    playerView.getAdOverlayFrameLayout().setDurationText(6 * 1000);
                    playerView.setAdsStateListener(this);
                    return;
                } else {
                    if (loop) {
                        LoopingMediaSource loopMediaSource = new LoopingMediaSource(buildMediaSource(uris[0], null));
                        ConcatenatingMediaSource adMediaSource = new ConcatenatingMediaSource(buildMediaSource(Uri.parse(adTagUriString)));
                        mediaSource =
                                new ConcatenatingMediaSource(adMediaSource, loopMediaSource);
                    } else {
                        ((ConcatenatingMediaSource) mediaSource).addMediaSource(0, buildMediaSource(Uri.parse(adTagUriString)));
                    }
                }
            }
        } else {
            releaseAdsLoader();
            playerView.setAdsUrl(false, "");
        }
        boolean haveStartPosition = startWindow != C.INDEX_UNSET;
        if (haveStartPosition) {
            player.seekTo(startWindow, startPosition);
        }
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS);
        playerView.setName(title);
        player.prepare(mediaSource, !haveStartPosition, false);
    }

    @Override
    public void preparePlayback() {

    }

    @Override
    public void onAction(Intent intent) {
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                if (NetworkInfo.State.CONNECTED == mNetworkInfo.getState() && mNetworkInfo.isAvailable()) {
                    if (mNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI || mNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE || mNetworkInfo.getType() == ConnectivityManager.TYPE_ETHERNET) {
                        if (init) {
                            initExoPlayer(uri, adTagUriString);
                        }
                        init = true;
                    }
                } else {

                }
            } else {

            }
        }
    }

    @Override
    public void adsComplete() {
        if (player != null && mediaSource != null) {
            boolean haveStartPosition = startWindow != C.INDEX_UNSET;
            if (haveStartPosition) {
                player.seekTo(startWindow, startPosition);
            }
            playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS);
            playerView.setName(title);
            player.prepare(mediaSource, !haveStartPosition, false);
            playerView.maybeShowController(true);
        }
    }

    private class PlayerEventListener implements Player.EventListener {

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            if (playbackState == Player.STATE_READY && !playerView.isPlayingAd()) {
                handler.postDelayed(runnable, 1000);
            }
        }

        @Override
        public void onPositionDiscontinuity(@Player.DiscontinuityReason int reason) {
            if (player.getPlaybackError() != null) {
                updateStartPosition();
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException e) {
            if (isBehindLiveWindow(e)) {
                clearStartPosition();
                initExoPlayer(uri, adTagUriString);
            } else {
                updateStartPosition();
            }
        }

        @Override
        @SuppressWarnings("ReferenceEquality")
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            if (trackGroups != lastSeenTrackGroupArray) {
                MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
                if (mappedTrackInfo != null) {
                    if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_VIDEO)
                            == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                        showToast("Media includes video tracks, but none are playable by this device");
                    }
                    if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_AUDIO)
                            == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                        showToast("Media includes audio tracks, but none are playable by this device");
                    }
                }
                lastSeenTrackGroupArray = trackGroups;
            }
        }
    }

    private static boolean isBehindLiveWindow(ExoPlaybackException e) {
        if (e.type != ExoPlaybackException.TYPE_SOURCE) {
            return false;
        }
        Throwable cause = e.getSourceException();
        while (cause != null) {
            if (cause instanceof BehindLiveWindowException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    public void showToast(String text) {
        Toast.makeText(context, "" + text, Toast.LENGTH_SHORT).show();
    }

    public SimpleExoPlayer getPlayer() {
        return player;
    }

    public void releaseAdsLoader() {
        if (adsLoader != null) {
            adsLoader.release();
            adsLoader = null;
            loadedAdTagUri = null;
            playerView.getOverlayFrameLayout().removeAllViews();
        }
    }

    public void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
            mediaSource = null;
            trackSelector = null;
            playerView.setPlayer(null);
            playerView.unregistVoiceSetting();
        }
        if (adsLoader != null) {
            adsLoader.setPlayer(null);
        }
        if (receiver != null) {
            context.unregisterReceiver(receiver);
        }
        releaseMediaDrm();
    }

    public void releaseMediaDrm() {
        if (mediaDrm != null) {
            mediaDrm.release();
            mediaDrm = null;
        }
    }

    /**
     * Returns an ads media source, reusing the ads loader if one exists.
     */
    private @Nullable
    void createAdsMediaSource(MediaSource mediaSource, Uri adTagUri) {
        ((ConcatenatingMediaSource) this.mediaSource).addMediaSource(0, mediaSource);
    }

    private MediaSource buildMediaSource(Uri uri) {
        return buildMediaSource(uri, null);
    }

    private List<StreamKey> getOfflineStreamKeys(Uri uri) {
        return baseFactory.getDownloadTracker().getOfflineStreamKeys(uri);
    }

    private MediaSource buildMediaSource(Uri uri, @Nullable String overrideExtension) {
        @C.ContentType int type = Util.inferContentType(uri, overrideExtension);
        switch (type) {
            case C.TYPE_DASH:
                return new DashMediaSource.Factory(dataSourceFactory)
                        .setManifestParser(
                                new FilteringManifestParser<>(new DashManifestParser(), getOfflineStreamKeys(uri)))
                        .createMediaSource(uri);
            case C.TYPE_SS:
                return new SsMediaSource.Factory(dataSourceFactory)
                        .setManifestParser(
                                new FilteringManifestParser<>(new SsManifestParser(), getOfflineStreamKeys(uri)))
                        .createMediaSource(uri);
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(dataSourceFactory)
                        .setPlaylistParserFactory(
                                new DefaultHlsPlaylistParserFactory(getOfflineStreamKeys(uri)))
                        .createMediaSource(uri);
            case C.TYPE_TS:
                DefaultExtractorsFactory defaultExtractorsFactory = new DefaultExtractorsFactory();
                defaultExtractorsFactory.setTsExtractorFlags(FLAG_DETECT_ACCESS_UNITS | FLAG_ALLOW_NON_IDR_KEYFRAMES);
                return new ExtractorMediaSource.Factory(dataSourceFactory).setExtractorsFactory(defaultExtractorsFactory).createMediaSource(uri);
            case C.TYPE_OTHER:
                return new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    /**
     * Returns a new DataSource factory.
     */
    private DataSource.Factory buildDataSourceFactory() {
        return baseFactory.buildDataSourceFactory();
    }

    private class PlayerErrorMessageProvider implements ErrorMessageProvider<ExoPlaybackException> {

        @Override
        public Pair<Integer, String> getErrorMessage(ExoPlaybackException e) {
            String errorString = context.getString(R.string.error_generic);
            if (e.type == ExoPlaybackException.TYPE_RENDERER) {
                Exception cause = e.getRendererException();
                if (cause instanceof MediaCodecRenderer.DecoderInitializationException) {
                    // Special case for decoder initialization failures.
                    MediaCodecRenderer.DecoderInitializationException decoderInitializationException =
                            (MediaCodecRenderer.DecoderInitializationException) cause;
                    if (decoderInitializationException.decoderName == null) {
                        if (decoderInitializationException.getCause() instanceof MediaCodecUtil.DecoderQueryException) {
                            errorString = context.getString(R.string.error_querying_decoders);
                        } else if (decoderInitializationException.secureDecoderRequired) {
                            errorString =
                                    context.getString(
                                            R.string.error_no_secure_decoder, decoderInitializationException.mimeType);
                        } else {
                            errorString =
                                    context.getString(R.string.error_no_decoder, decoderInitializationException.mimeType);
                        }
                    } else {
                        errorString =
                                context.getString(
                                        R.string.error_instantiating_decoder,
                                        decoderInitializationException.decoderName);
                    }
                }
            }
            return Pair.create(0, errorString);
        }
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

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case -1:
                    handler.removeCallbacks(runnable);
                    playerView.onPause();
                    player.setPlayWhenReady(false);
                    if (tryModeIml != null) {
                        tryModeIml.onTryComplete();
                    }
                    break;
            }
        }
    };

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if(player!=null){
                if (player.getContentPosition() >= freeTime) {
                    handler.obtainMessage(-1).sendToTarget();
                } else {
                    handler.postDelayed(this, 1000);
                }
            }
        }
    };

    private void updateStartPosition() {
        if (player != null) {
            startAutoPlay = player.getPlayWhenReady();
            startWindow = player.getCurrentWindowIndex();
            startPosition = Math.max(0, player.getContentPosition());
        }
    }

    private void clearStartPosition() {
        startAutoPlay = true;
        startWindow = C.INDEX_UNSET;
        startPosition = C.TIME_UNSET;
    }

    public interface TryModeIml {
        void onTryComplete();
    }

}
