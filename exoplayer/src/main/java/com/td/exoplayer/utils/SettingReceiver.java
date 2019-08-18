package com.td.exoplayer.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

/**
 * Created by office on 2018/5/9.
 */

public class SettingReceiver extends BroadcastReceiver {
    private static final String ACTION_BOOT = "BOOT_COMPLETE_ACTION";//Intent.ACTION_BOOT_COMPLETED
    private static final String ACTION_CONNECT_CHANGE = ConnectivityManager.CONNECTIVITY_ACTION;
    private SettingIml settingIml;

    public SettingReceiver(SettingIml iml) {
        this.settingIml = iml;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {

        } else if (intent.getAction().equals(ACTION_CONNECT_CHANGE)) {

        }
        if (settingIml != null) {
            settingIml.onAction(intent);
        }
    }
}
