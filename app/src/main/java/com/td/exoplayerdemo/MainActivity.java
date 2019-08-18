package com.td.exoplayerdemo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v17.leanback.widget.HorizontalGridView;
import android.support.v17.leanback.widget.OnChildViewHolderSelectedListener;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.exoplayer2.offline.DownloadService;
import com.google.connectlib.ConnectService;
import com.whatjay.recyclerview.adapter.BaseSmartAdapter;

import java.util.ArrayList;
import java.util.List;

import com.td.exoplayer.BaseFactory;
import com.td.exoplayer.DownloadTracker;
import com.td.exoplayer.PlayerExo;
import com.td.exoplayer.PlayerView;
import com.td.exoplayer.utils.CINDEX;
import com.td.exoplayer.utils.ExoDownloadService;
import com.td.exoplayer.utils.Utils;
import com.td.exoplayerdemo.adapter.TVListAdapter;

public class MainActivity extends AppCompatActivity implements PlayerView.PlayState, BaseSmartAdapter.OnRecyclerViewItemClickListener, DownloadTracker.Listener, ConnectService.MessageListener {
    private PlayerView playerView;
    private LinearLayout ll_tv_list;
    private HorizontalGridView hgv;
    private String mediaName = "电影";
    private int position;
    private List<String> seriesItemList;
    private String type = "";
    private boolean showList = false;
    private TVListAdapter listAdapter;
    private CountDownTimer countDownTimerList;
    private long time = 0;
    private String episodeId = "0";
    private PlayerExo playerExo;
    private long startPosition = CINDEX.TIME_UNSET;
    private int startWindow = CINDEX.INDEX_UNSET;
    private String adUrl = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dlinear&correlator=";
    private Uri[] uris = new Uri[1];
    private DownloadTracker downloadTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        downloadTracker = BaseFactory.getInstance(this).getDownloadTracker();
        startService(
                new Intent(this, ExoDownloadService.class).setAction(DownloadService.ACTION_INIT));
        playerView = findViewById(R.id.epv);
        ll_tv_list = findViewById(R.id.ll_tv_list);
        hgv = findViewById(R.id.hgv);
        seriesItemList = new ArrayList<>();
        playerExo = new PlayerExo(this, playerView);
        uris[0] = Uri.parse("http://218.70.82.218:9568/Data/2019-04-26/file_41f6245b-c53a-45e8-9a5c-8d08f39e30dc/playlist.m3u8");
//        uris[0] = Uri.parse(Environment.getExternalStorageDirectory().getPath() + "/big_buck_bunny.mp3");
        playerView.setTVMode(true);
        initList();
        initExoPlayer();
    }

    private void initList() {
        if (startPosition > 0) {
            startWindow = 0;
        }
        listAdapter = new TVListAdapter(this, R.layout.item_tvprogram, seriesItemList, type);
        hgv.setAdapter(listAdapter);
        hgv.setNumRows(1);
        hgv.setRowHeight(Utils.dp2px(this, 100));
        listAdapter.setOnItemClickListener(this);
        hgv.setSelectedPosition(position);
        hgv.setGravity(Gravity.CENTER);
        listAdapter.setPosition(position);
        listAdapter.setCurrentFocusPosition(position);
        hgv.setOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(RecyclerView parent, RecyclerView.ViewHolder child, int position, int subposition) {
                super.onChildViewHolderSelected(parent, child, position, subposition);
                if (time <= 2000) {
                    showTVList();
                }
            }
        });
        initService();
    }

    private void initService() {
        Intent intent = new Intent(this, ConnectService.class);
        startService(intent);
        ConnectService.setMessageListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        downloadTracker.addListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (playerView != null) {
            playerView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (playerView != null) {
            playerView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        downloadTracker.removeListener(this);
        super.onDestroy();
        if (playerExo.getPlayer() != null) {
            playerExo.releasePlayer();
            playerExo.releaseAdsLoader();
            startWindow = CINDEX.INDEX_UNSET;
            startPosition = CINDEX.POSITION_UNSET;
        }
        cancelTimer();
    }

    private void cancelTimer() {
        if (countDownTimerList != null) {
            countDownTimerList.cancel();
            countDownTimerList = null;
        }
    }

    private void download() {
        downloadTracker.toggleDownload(this, mediaName, uris[0], "");
    }

    private void initExoPlayer() {
        initTitle();
        adUrl = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4";
        playerExo.setStartPosition(startWindow, startPosition);
        //http://event.android-studio.org/images/event/201605/google-io-2016/gizmodo-02-Google-Assistant-1.gif
        //https://source.android.google.cn/devices/tv/images/LiveChannels_Add_channel.png
        playerExo.initExoPlayer(uris, "");
        playerView.setPlayStateListener(this);
        download();
    }

    private void initTitle() {
        if (seriesItemList.size() > 0) {
            playerExo.setTitle(mediaName + "  " + (position + 1));
        } else {
            playerExo.setTitle(mediaName);
        }
        if (startPosition > 0) {
            startWindow = 0;
        }
    }

    @Override
    public void playComplete() {

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (showList) {
                    hideTVList();
                    return true;
                }
                if (Utils.isQuickClick()) {
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                hideTVList();
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (!showList) {
                    showTVList();
                }
                break;
            case KeyEvent.KEYCODE_VOLUME_UP:
                Utils.setVolume(this, true, false);
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                Utils.setVolume(this, false, false);
                return true;
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                Utils.setVolume(this, false, true);
                return true;
            case KeyEvent.KEYCODE_MENU:
                Intent intent = new Intent(this, WebActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void hideTVList() {
        if (!showList) {
            return;
        }
        cancelTimer();
        Animation animation = Utils.SlideToBottom(null);
        ll_tv_list.startAnimation(animation);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                ll_tv_list.setVisibility(View.GONE);
                showList = false;
                if (hgv.getSelectedPosition() != position) {
                    listAdapter.setCurrentFocusPosition(hgv.getSelectedPosition());
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void showTVList() {
        if (ll_tv_list.getVisibility() != View.VISIBLE) {
            ll_tv_list.setVisibility(View.VISIBLE);
            Utils.getSlideFromBottom(ll_tv_list, null);
            listAdapter.notifyDataSetChanged();
        }
        showList = true;
        cancelTimer();
        countDownTimerList = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                time = millisUntilFinished;
            }

            @Override
            public void onFinish() {
                Animation animation = Utils.SlideToBottom(null);
                ll_tv_list.startAnimation(animation);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        ll_tv_list.setVisibility(View.GONE);
                        showList = false;
                        if (hgv.getSelectedPosition() != position) {
                            listAdapter.setCurrentFocusPosition(hgv.getSelectedPosition());
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
            }
        };
        countDownTimerList.start();
    }

    @Override
    public void onItemClick(View view, int position) {
        this.position = position;
        startWindow = CINDEX.INDEX_UNSET;
        startPosition = CINDEX.POSITION_UNSET;
        listAdapter.setPosition(position);
        listAdapter.setCurrentFocusPosition(position);
        if (Utils.isQuickClick()) {
            return;
        }
        initExoPlayer();
        initList();
    }

    @Override
    public void onDownloadsChanged() {

    }

    @Override
    public void onMessage(String message) {
        switch (message) {
            case "left": {
                Toast.makeText(getBaseContext(), "" + message, Toast.LENGTH_SHORT).show();
                KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT);
                dispatchKeyEvent(keyEvent);
            }
            break;
            case "right": {
                Toast.makeText(getBaseContext(), "" + message, Toast.LENGTH_SHORT).show();
                KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT);
                dispatchKeyEvent(keyEvent);
            }
            break;
            case "up": {
                Toast.makeText(getBaseContext(), "" + message, Toast.LENGTH_SHORT).show();
                KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP);
                dispatchKeyEvent(keyEvent);
            }
            break;
            case "down": {
                Toast.makeText(getBaseContext(), "" + message, Toast.LENGTH_SHORT).show();
                KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN);
                dispatchKeyEvent(keyEvent);
            }
            break;
            case "enter": {
                Toast.makeText(getBaseContext(), "" + message, Toast.LENGTH_SHORT).show();
                KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER);
                dispatchKeyEvent(keyEvent);
            }
            break;
            case "back": {
                Toast.makeText(getBaseContext(), "" + message, Toast.LENGTH_SHORT).show();
                KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK);
                dispatchKeyEvent(keyEvent);
            }
            break;
            default:
                Toast.makeText(getBaseContext(), "default：" + message, Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
