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
        SharedPreferences prefs = context.getSharedPreferences("GTalkSMS", 0);
        boolean debugLog = prefs.getBoolean("debugLog", false);
        // 'failover' due to another network stopping? 
        boolean failover = intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false);
        // Are we in a 'no connectivity' state?
        boolean connected = !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
        if (debugLog) Log.d(Tools.LOG_TAG, "NetworkConnectivityReceiver: connected=" + connected + ", failover=" + failover);
        NetworkInfo network = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
        
        if (prefs.getBoolean("startOnWifiConnected", false) && connected && network != null 
                && network.isConnected() && network.getTypeName().equals("WIFI")) {
            // Start GTalkSMS
            Intent net_changed = new Intent(MainService.ACTION_NETWORK_CHANGED);
            net_changed.putExtra("available", true);
            if (debugLog) Log.d(Tools.LOG_TAG, "NetworkConnectivityReceiver: connected on wifi");
            context.startService(new Intent(MainService.ACTION_CONNECT));
            context.startService(net_changed);
        } else if (prefs.getBoolean("stopOnWifiDisconnected", false)
                && (!connected || network == null || !network.isConnected() || failover || !network.getTypeName().equals("WIFI"))) {
            // Stop GTalkSMS
            if (debugLog) Log.d(Tools.LOG_TAG, "NetworkConnectivityReceiver: no wifi connection");
            Intent serviceIntent = new Intent(MainService.ACTION_CONNECT);
            serviceIntent.putExtra("force", true);
            serviceIntent.putExtra("disconnect", true);
            context.startService(serviceIntent);
        } else if (MainService.IsRunning) {
            // if no network, or if this is a "failover" notification 
            // (meaning the network we are connected to has stopped) 
            // and we are connected , we must disconnect.
            if (network == null || !network.isConnected() || failover) {
                if (debugLog) Log.d(Tools.LOG_TAG, "NetworkConnectivityReceiver: notifying that the network is unavailable");
                Intent svcintent = new Intent(MainService.ACTION_NETWORK_CHANGED);
                svcintent.putExtra("available", false);
                context.startService(svcintent);
            }
            // connect if not already connected (eg, if we disconnected above) and we have connectivity
            if (connected) {
                if (debugLog) Log.d(Tools.LOG_TAG, "NetworkConnectivityReceiver: notifying that a network is available...");
                Intent svcintent = new Intent(MainService.ACTION_NETWORK_CHANGED);
                if(network.getTypeName().equals("WIFI")) {
                    svcintent.putExtra("WIFI", true);
                } else {
                    svcintent.putExtra("WIFI", false);                    
                }
                svcintent.putExtra("available", true);
                context.startService(svcintent);
            }
        }
    }
}
