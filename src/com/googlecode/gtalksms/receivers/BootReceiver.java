package com.googlecode.gtalksms.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.googlecode.gtalksms.MainService;

/** Allows the application to start at boot */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("GTalkSMS", 0);
        boolean startAtBoot = prefs.getBoolean("startAtBoot", false);
        if (startAtBoot) {
            Intent serviceIntent = new Intent(MainService.ACTION_CONNECT);
            context.startService(serviceIntent);
        }
    }
}
