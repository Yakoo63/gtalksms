package com.googlecode.gtalksms.cmd;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class WifiCmd extends CommandHandlerBase {
    
    private static final int RSSI_LEVEL = 5;
	
	private static WifiManager sWifiManager;

    public WifiCmd(MainService mainService) {
        super(mainService, new String[] {"wifi", "wlan"}, CommandHandlerBase.TYPE_SYSTEM);
        if (sWifiManager == null) {
        	sWifiManager = (WifiManager) mainService.getSystemService(Context.WIFI_SERVICE);
        }
    }

    protected void execute(String cmd, String args) {
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
    
    public void sendStatus() {
        send(getStatus());
    }
    
    private static XmppMsg getStatus() {
        XmppMsg res = new XmppMsg();
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
    	res.append("Wifi state is ");
    	res.appendBold(statusStr);
    	res.newLine();
    	
        boolean supplicant_alive = sWifiManager.pingSupplicant();
        String supplicant_status;
        if (supplicant_alive) {
            supplicant_status = "WPA Supplicant is responding";
        } else {
            supplicant_status = "WPA Supplicant is NOT responding";
        }
    	res.appendLine(supplicant_status);
    	
    	WifiInfo info = sWifiManager.getConnectionInfo();
    	if (info != null) {
    	    res.newLine();
    	    String bssid = info.getBSSID();
    	    String ip = Tools.ipIntToString(info.getIpAddress());
    	    String ssid = info.getSSID();
    	    int rssi = info.getRssi();
    	    
    	    // bssid
    	    res.appendBold("BSSID: ");
    	    res.appendLine(bssid);
    	    // ssid
    	    res.appendBold("SSID: ");
    	    res.appendLine(ssid);
    	    // ip
    	    res.appendBold("IP: ");
    	    res.appendLine(ip);
    	    // link speed
    	    res.appendBold("Current link speed: ");
    	    res.append(info.getLinkSpeed());
    	    res.appendLine(WifiInfo.LINK_SPEED_UNITS);
    	    // rssi
    	    res.appendBold("Received signal strength indicator: ");
    	    res.appendLine(rssi);
    	    // rssi - level
    	    res.appendBold("RSSI on a scale from 1 to " + RSSI_LEVEL + ": ");
    	    res.appendLine(Integer.toString(WifiManager.calculateSignalLevel(rssi, RSSI_LEVEL)));
    	}
    	
    	DhcpInfo dhcpInfo = sWifiManager.getDhcpInfo();
    	if (dhcpInfo != null) {    	  
    	    res.newLine();
    	    res.appendBoldLine("DHCP Info");
    	    res.appendBold("DNS1: ");
    	    res.appendLine(Tools.ipIntToString(dhcpInfo.dns1));
    	    res.appendBold("DNS2: ");
            res.appendLine(Tools.ipIntToString(dhcpInfo.dns2));
            res.appendBold("Gateway: ");
            res.appendLine(Tools.ipIntToString(dhcpInfo.gateway));
            res.appendBold("IP: ");
            res.appendLine(Tools.ipIntToString(dhcpInfo.ipAddress));
            res.appendBold("Netmask: ");
            res.appendLine(Tools.ipIntToString(dhcpInfo.netmask));
            res.appendBold("Lease Duraction: ");
            res.appendLine(dhcpInfo.leaseDuration + "s");
            res.appendBold("DHCP Server IP: ");
            res.appendLine(Tools.ipIntToString(dhcpInfo.serverAddress));
            
    	}
    	
    	return res;    	
    }
    
    @Override
    public String[] help() {
        String[] s = { 
                getString(R.string.chat_help_wifi_on, makeBold("\"wifi:on\""), makeBold("\"wlan:on\"")),
                getString(R.string.chat_help_wifi_off, makeBold("\"wifi:off\""), makeBold("\"wlan:off\"")),
                getString(R.string.chat_help_wifi_state, makeBold("\"wifi:state\""), makeBold("\"wlan:state\"")),
        };
        return s;
    }
}
