package com.googlecode.gtalksms.cmd;

import android.content.Context;
import android.net.wifi.WifiManager;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class WifiCmd extends CommandHandlerBase {
	
	private static WifiManager sWifiManager;

    public WifiCmd(MainService mainService) {
        super(mainService, new String[] {"wifi", "wlan"}, CommandHandlerBase.TYPE_SYSTEM);
        // TODO if your command needs references, init them here
        if (sWifiManager == null) {
        	sWifiManager = (WifiManager) mainService.getSystemService(Context.WIFI_SERVICE);
        }
    }

    protected void execute(String cmd, String args) {
       // TODO Start here
        if (args.equals("on")) {
        	enableWifi();
        } else if (args.equals("off")) {
        	disableWifi();
        } else if (args.equals("status") || args.equals("")) {
        	sendStatus();
        } else {
            send("Unkown argument \"" + args + "\" for command \"" + cmd + "\"");
        }
    }
    private void enableWifi() {
    	send("Enabling Wifi");
    	boolean ret = sWifiManager.setWifiEnabled(true);
    	if (ret) {
    		send("Enabled Wifi");
    	} else {
    		send("Could not enable Wifi");
    	}
    }
    
    private void disableWifi() {
    	send("Disabling Wifi");
    	boolean ret = sWifiManager.setWifiEnabled(false);
    	if (ret) {
    		send("Disabled Wifi");
    	} else {
    		send("Could not disable Wifi");
    	}
    }
    
    private void sendStatus() {
    	int status = sWifiManager.getWifiState();
    	String statusStr;
    	switch (status) {
    	case WifiManager.WIFI_STATE_DISABLED:
    		statusStr = "disabled";
    		break;
    	case WifiManager.WIFI_STATE_DISABLING:
    		statusStr = "disabling";
    		break;
    	case WifiManager.WIFI_STATE_ENABLED:
    		statusStr = "enabled";
    		break;
    	case WifiManager.WIFI_STATE_ENABLING:
    		statusStr = "enabling";
    		break;
    	default:
    		statusStr = "unkown";
    		break;
    	}
    	boolean supplicant_alive = sWifiManager.pingSupplicant();
    	String supplicant_status;
    	if (supplicant_alive) {
    		supplicant_status = "WPA Supplicant is responding";
    	} else {
    		supplicant_status = "WPA Supplicant is NOT responding";
    	}
    	
    	XmppMsg res = new XmppMsg();
    	res.append("Wifi state is ");
    	res.appendBold(statusStr);
    	res.newLine();
    	res.appendLine(supplicant_status);
    	send(res);    	
    }
    
    @Override
    public String[] help() {
        return null;
    }
}
