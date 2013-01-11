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

    private static String lastActiveNetworkName = null;

    @SuppressWarnings("deprecation")
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences(Tools.APP_NAME, 0);
        boolean debugLog = prefs.getBoolean("debugLog", false);
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            Log.e(Tools.LOG_TAG, "NetworkConnectivityReceiver: Connectivity Manager is null!");
            return;
        }
        
        if (debugLog) {
            for (NetworkInfo network : cm.getAllNetworkInfo()) {
                Log.d(Tools.LOG_TAG, "NetworkConnectivityReceiver: "
                        + " available=" + (network.isAvailable()?1:0)
                        + ", connected=" + (network.isConnected()?1:0)
                        + ", connectedOrConnecting=" + (network.isConnectedOrConnecting()?1:0)
                        + ", failover=" + (network.isFailover()?1:0)
                        + ", roaming=" + (network.isRoaming()?1:0)
                        + ", networkName=" + network.getTypeName());
            } 
        }

		NetworkInfo network = cm.getActiveNetworkInfo();
		if (network != null && MainService.IsRunning) {
			String networkName = network.getTypeName();
			boolean networkChanged = false;
			boolean connectedOrConnecting = network.isConnectedOrConnecting();
			boolean connected = network.isConnected();
			if (!networkName.equals(lastActiveNetworkName)) {
				lastActiveNetworkName = networkName;
				networkChanged = true;
			}
			Log.d(Tools.LOG_TAG, "NetworkConnectivityReceiver: " + MainService.ACTION_NETWORK_STATUS_CHANGED + " name="
			        + network.getTypeName() + " changed=" + networkChanged + " connected=" + connected
			        + " connectedOrConnecting="
			        + connectedOrConnecting);

			Intent svcintent = new Intent(MainService.ACTION_NETWORK_STATUS_CHANGED);
			svcintent.putExtra("networkChanged", networkChanged);
			svcintent.putExtra("connectedOrConnecting", connectedOrConnecting);
			svcintent.putExtra("connected", connected);
			context.startService(svcintent);
		}

        network = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
        if (network.getTypeName().equals("WIFI") && network.isConnected() && prefs.getBoolean("startOnWifiConnected", false)) {
            // Start GTalkSMS
            if (debugLog) {
                Log.d(Tools.LOG_TAG, "NetworkConnectivityReceiver: startOnWifiConnected enabled, wifi connected, sending intent");
            }
            context.startService(new Intent(MainService.ACTION_CONNECT));
        } else if (network.getTypeName().equals("WIFI") && !network.isConnected() && prefs.getBoolean("stopOnWifiDisconnected", false)) {            
            // Stop GTalkSMS
            if (debugLog) {
                Log.d(Tools.LOG_TAG, "NetworkConnectivityReceiver: stopOnWifiDisconnected enabled, wifi disconnected, sending intent");
            }
            context.startService(new Intent(MainService.ACTION_DISCONNECT));
        }
    }

	public static void setLastActiveNetworkName(Context ctx) {
		ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo network = cm.getActiveNetworkInfo();
		if (network != null) {
			lastActiveNetworkName = network.getTypeName();
		}
	}
}
