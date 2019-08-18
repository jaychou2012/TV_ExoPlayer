package com.td.exoplayerdemo;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;

import com.td.exoplayer.PlayerExo;
import com.td.exoplayer.PlayerView;
import com.td.exoplayerdemo.base.BaseApplication;

public class FullScreenActivity extends AppCompatActivity implements PlayerView.PlayState, PlayerExo.TryModeIml {
    private PlayerView playerView;
    private PlayerExo playerExo;
    private Uri[] uris = new Uri[1];
    private String adUrl = "";
    private Player player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen);
        initView();
    }

    private void initView() {
        playerView = findViewById(R.id.epv);
        playerExo = new PlayerExo(this, playerView);
        playerView.setPlayer(BaseApplication.player);
//        playerView.setScreenSize(256, 144);
        playerExo.setLoop(false);
        playerView.setPlayStateListener(this);
        playerView.setTVMode(false);
//        playerView = findViewById(R.id.epv);
//        playerExo = new PlayerExo(this, playerView);
        uris[0] = Uri.parse("http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4");//"http://218.70.82.218:9568/Data/2019-04-26/file_41f6245b-c53a-45e8-9a5c-8d08f39e30dc/playlist.m3u8"
//        uris[0] = Uri.parse(Environment.getExternalStorageDirectory().getPath() + "/big_buck_bunny.mp3");
//        playerView.setTVMode(true);
        initExoPlayer();
    }

    private void initExoPlayer() {
        adUrl = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4";
        playerExo.setStartPosition(C.INDEX_UNSET, C.TIME_UNSET);
        //http://event.android-studio.org/images/event/201605/google-io-2016/gizmodo-02-Google-Assistant-1.gif
        //https://source.android.google.cn/devices/tv/images/LiveChannels_Add_channel.png
        playerExo.initExoPlayer(uris, "");
        playerExo.setLoop(false);
        playerExo.setTryFreeMode(true);
        playerExo.setTryTime(10 * 1000);
        playerExo.setTryModeIml(this);
        playerView.setPlayStateListener(this);
    }

    @Override
    public void playComplete() {
//        initExoPlayer();
    }

    @Override
    public void onTryComplete() {
        Toast.makeText(this, "tryComplete", Toast.LENGTH_SHORT).show();
        System.out.println("tryComplete");
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
        super.onDestroy();
        playerExo.releaseAdsLoader();
        playerExo.releasePlayer();
    }
}
