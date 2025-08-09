package com.googlecode.gtalksms.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.SettingsManager;

public class PowerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Handle the preference for "start when charging" and "stop when not charging"        
        SettingsManager settings = SettingsManager.getSettingsManager(context);
        boolean connected = intent.getAction().equals("android.intent.action.ACTION_POWER_CONNECTED");

        Handler DisconnectHandler = MainService.getDelayedDisconnectHandler();
        if (DisconnectHandler != null) {
            DisconnectHandler.removeCallbacksAndMessages(null);
        }
        
        if (connected && settings.startOnPowerConnected) {
            // Prepare and send a connect intent, which will also start the service
            Intent serviceIntent = new Intent(MainService.ACTION_CONNECT);
            context.startService(serviceIntent);
        } else if (DisconnectHandler != null && settings.stopOnPowerDisconnected) {
            long delayMillis = settings.stopOnPowerDelay * 60 * 1000;
            Log.d("Posting delayed disconnect in " + settings.stopOnPowerDelay + " minutes");
            Runnable r = new DisconnectDelayed(context);
            DisconnectHandler.postDelayed(r, delayMillis);            
        }
    }
    
    class DisconnectDelayed implements Runnable {
        private final Context mContext;
        
        public DisconnectDelayed(Context ctx) {
            this.mContext = ctx;
        }

        @Override
        public void run() {
            Intent serviceIntent = new Intent(MainService.ACTION_CONNECT);
            serviceIntent.putExtra("disconnect", true);
            Log.d("Issueing disconnect intent because of delayed disconnect");
            mContext.startService(serviceIntent);
        }
        
    }
}
