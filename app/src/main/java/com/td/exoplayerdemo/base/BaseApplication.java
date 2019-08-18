package com.td.exoplayerdemo.base;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.google.android.exoplayer2.Player;

import com.td.exoplayerdemo.utils.CrashHandler;

public class BaseApplication extends Application {
    public static Player player;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initcrash();
    }

    private void initcrash() {
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(getApplicationContext());
    }
}
