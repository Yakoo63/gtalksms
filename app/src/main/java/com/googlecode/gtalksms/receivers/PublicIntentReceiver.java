package com.googlecode.gtalksms.receivers;

import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.SettingsManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class PublicIntentReceiver extends BroadcastReceiver {
    private static PublicIntentReceiver sPublicIntentReceiver;
    private static final IntentFilter sIntentFilter;
    private static Context sContext;
    
    private static SettingsManager sSettings;

    static {
        sIntentFilter = new IntentFilter();
        // ACTION_CONNECT and TOGGLE is received by filter within the Apps Manifest
        // sIntentFilter.addAction(MainService.ACTION_CONNECT);
        // sIntentFilter.addAction(MainService.ACTION_DISCONNECT);
        // sIntentFilter.addAction(MainService.ACTION_TOGGLE);
        sIntentFilter.addAction(MainService.ACTION_COMMAND);
        sIntentFilter.addAction(MainService.ACTION_SEND);
    }

    /**
     * A nice empty constructor stub, so that Android can
     * instantiate this class
     */
    public PublicIntentReceiver() {
        // Does nothing
    }

    private PublicIntentReceiver(Context context) {
        sSettings = SettingsManager.getSettingsManager(context);
        sContext = context;
    }
    
    public static void initReceiver(Context ctx) {
        getReceiver(ctx);
    }

    private static PublicIntentReceiver getReceiver(Context ctx) {
        if (sPublicIntentReceiver == null) {
            sPublicIntentReceiver = new PublicIntentReceiver(ctx);
            ctx.registerReceiver(sPublicIntentReceiver, sIntentFilter);
        }
        return sPublicIntentReceiver;
    }

    public static void onServiceStop() {
        if (sPublicIntentReceiver != null) {
            try {
                sContext.unregisterReceiver(sPublicIntentReceiver);
            } catch (Exception e) {
                Log.e("Failed to unregistrer public receiver", e);
            }
            sPublicIntentReceiver = null;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("PublicIntentReceiver got intent: " + intent.getAction());

        // Init the static class to have access to the SettingsManager
        if (sPublicIntentReceiver == null) {
            sPublicIntentReceiver = new PublicIntentReceiver(context);
        }

        if (sSettings.publicIntentsEnabled) {
            if (sSettings.publicIntentTokenRequired) {
                String token = intent.getStringExtra("token");
                if (token == null || !sSettings.publicIntentToken.equals(token)) {
                    // token required but no token set or it doesn't match
                    Log.w("Public intent without correct security token received");
                    return;
                }
            }
            intent.setClass(context, MainService.class);
            context.startService(intent);
        } else {
            Log.w("Received public intent but public intents are disabled");
        }
    }
}