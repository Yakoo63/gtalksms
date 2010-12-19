package com.googlecode.gtalksms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.googlecode.gtalksms.tools.Tools;

/** Allows the application to start at boot */
public class WidgetActivator extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(".GTalkSMS.ACTION");
      
        if (MainService.getInstance() == null) {
            context.startService(serviceIntent);
            Log.i(Tools.LOG_TAG, "WidgetActivator startService");
        } else {
            context.stopService(serviceIntent);
            Log.i(Tools.LOG_TAG, "WidgetActivator stopService");
        }
    }
}
