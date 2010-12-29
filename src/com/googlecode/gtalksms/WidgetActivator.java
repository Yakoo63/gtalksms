package com.googlecode.gtalksms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Allows the application to start at boot */
public class WidgetActivator extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent svcintent = MainService.newSvcIntent(context, ".GTalkSMS.TOGGLE");
        context.startService(svcintent);
    }
}
