package com.googlecode.gtalksms.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.tools.Tools;

public class NetworkConnectivityReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (MainService.running) {
            // is this notification telling us about a new network which is a 
            // 'failover' due to another network stopping? 
            boolean failover = intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false);
            // Are we in a 'no connectivity' state?
            boolean nocon = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            NetworkInfo network = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            // if no network, or if this is a "failover" notification 
            // (meaning the network we are connected to has stopped) 
            // and we are connected , we must disconnect.
            if (network == null || !network.isConnected() || failover) {
                Log.i(Tools.LOG_TAG, "notifying that the network is unavailable");
                Intent svcintent = MainService.newSvcIntent(context, ".GTalkSMS.NETWORK_CHANGED");
                svcintent.putExtra("available", false);
                context.startService(svcintent);
            }
            // connect if not already connected (eg, if we disconnected above) and we have connectivity
            if (!nocon) {
                Log.i(Tools.LOG_TAG, "notifying that a network is available...");
                Intent svcintent = MainService.newSvcIntent(context, ".GTalkSMS.NETWORK_CHANGED");
                svcintent.putExtra("available", true);
                context.startService(svcintent);
            }
        }
    }
}
