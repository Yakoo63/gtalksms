package com.googlecode.gtalksms.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.tools.Tools;

public class NetworkConnectivityReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(Tools.LOG_TAG, "NetworkConnectivityReceiver");
        // 'failover' due to another network stopping? 
        boolean failover = intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false);
        // Are we in a 'no connectivity' state?
        boolean connected = !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
        NetworkInfo network = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
        
        SharedPreferences prefs = context.getSharedPreferences("GTalkSMS", 0);
        if (prefs.getBoolean("startOnWifiConnected", false) && connected && network != null 
                && network.isConnected() && network.getTypeName().equals("WIFI")) {
            // Start GTalkSMS
            Log.i(Tools.LOG_TAG, "NetworkConnectivityReceiver: connected on wifi");
            context.startService(new Intent(MainService.ACTION_CONNECT));
        } else if (prefs.getBoolean("stopOnWifiDisconnected", false)
                && (!connected || network == null || !network.isConnected() || failover || !network.getTypeName().equals("WIFI"))) {
            // Stop GTalkSMS
            Log.i(Tools.LOG_TAG, "NetworkConnectivityReceiver: no wifi connection");
            Intent serviceIntent = new Intent(MainService.ACTION_CONNECT);
            serviceIntent.putExtra("force", true);
            serviceIntent.putExtra("disconnect", true);
            context.startService(serviceIntent);
        } else if (MainService.IsRunning && !prefs.getBoolean("stopOnPowerDisconnected", false)) {
            // if no network, or if this is a "failover" notification 
            // (meaning the network we are connected to has stopped) 
            // and we are connected , we must disconnect.
            if (network == null || !network.isConnected() || failover) {
                Log.i(Tools.LOG_TAG, "NetworkConnectivityReceiver: notifying that the network is unavailable");
                Intent svcintent = MainService.newSvcIntent(context, MainService.ACTION_NETWORK_CHANGED);
                svcintent.putExtra("available", false);
                context.startService(svcintent);
            }
            // connect if not already connected (eg, if we disconnected above) and we have connectivity
            if (connected) {
                Log.i(Tools.LOG_TAG, "NetworkConnectivityReceiver: notifying that a network is available...");
                Intent svcintent = MainService.newSvcIntent(context, MainService.ACTION_NETWORK_CHANGED);
                svcintent.putExtra("available", true);
                context.startService(svcintent);
            }
        }
    }
}
