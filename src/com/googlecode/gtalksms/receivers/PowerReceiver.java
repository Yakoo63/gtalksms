package com.googlecode.gtalksms.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class PowerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // handle the preference for "start when charging" and "stop when not charging"
        SharedPreferences prefs = context.getSharedPreferences("GTalkSMS", 0);
        boolean connected = intent.getAction().equals("android.intent.action.ACTION_POWER_CONNECTED");
        String prefName = connected ? "startOnPowerConnected" : "stopOnPowerDisconnected";
        if (prefs.getBoolean(prefName, false)) {
            Intent serviceIntent = new Intent(".GTalkSMS.ACTION");
            if (!connected) {
                serviceIntent.putExtra("disconnect", true);
            }
            context.startService(serviceIntent);
        }
    }
}
