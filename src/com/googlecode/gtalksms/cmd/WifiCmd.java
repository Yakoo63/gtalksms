package com.googlecode.gtalksms.cmd;

import java.util.List;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiConfiguration;
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
        super(mainService, CommandHandlerBase.TYPE_SYSTEM, "WiFi", new Cmd("wifi", "wlan"));
    }
    
    public void activate() {
        super.activate();
        sWifiManager = (WifiManager) sMainService.getSystemService(Context.WIFI_SERVICE);
    }
    
    public void deactivate() {
        super.activate();
        sWifiManager = null;
    }

    protected void execute(String cmd, String args) {
        String[] sArgs = splitArgs(args);
        
        if (sArgs[0].equals("on")) {
            enableWifi();
        } else if (sArgs[0].equals("off")) {
            disableWifi();
        } else if (sArgs[0].equals("state") || args.equals("")) {
            sendStatus();
        } else if (sArgs[0].equals("list")) {
            listNetworks();
        } else if (sArgs[0].equals("enable")) {
            enableNetwork(sArgs);
        } else if (sArgs[0].equals("disable")) {
            disableNetwork(sArgs);
        } else {
            send("Unkown argument \"" + args + "\" for command \"" + cmd + "\"");
        }
    }
    private void listNetworks() {
        XmppMsg msg = new XmppMsg();
        List<WifiConfiguration> networks = sWifiManager.getConfiguredNetworks();
        for (WifiConfiguration w : networks) {
            msg.appendBold("ID: ");
            msg.append(w.networkId);
            msg.appendBold(" Name: ");
            msg.append(w.SSID);
            msg.appendBold(" Status: ");
            String status;
            switch(w.status) {
            case WifiConfiguration.Status.CURRENT:
                status = "Current connected network";
                break;
            case WifiConfiguration.Status.ENABLED:
                status = "Enabled";
                break;
            case WifiConfiguration.Status.DISABLED:
                status = "Disabled";
                break;
            default:
                status = "Unkown";
                break;
            }
            msg.append(status);
            msg.newLine();
        }
        send(msg);
    }
    
    private void enableNetwork(String[] args) {
        if (args.length == 2) {
            int id = Integer.parseInt(args[1]);
            if (sWifiManager.enableNetwork(id, false)) {
                send("Successfully enabled network with ID " + id);
            } else {
                send("Could not enable network with ID" + id);
            }
        } else {
            send("Error enabling network");
        }
    }
    
    private void disableNetwork(String[] args) {
        if (args.length == 2) {
            int id = Integer.parseInt(args[1]);
            if (sWifiManager.disableNetwork(id)) {
                send("Successfully disabled network with ID " + id);
            } else {
                send("Could not disable network with ID " + id);
            }
        } else {
            send("Error disabling network");
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
    
    void sendStatus() {
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
    protected void initializeSubCommands() {
        Cmd wifi = mCommandMap.get("wifi");
        wifi.setHelp(R.string.chat_help_wifi_state, null);   
        wifi.AddSubCmd("on", R.string.chat_help_wifi_on, null);   
        wifi.AddSubCmd("off", R.string.chat_help_wifi_off, null);   
        wifi.AddSubCmd("state", R.string.chat_help_wifi_state, null);   
        wifi.AddSubCmd("list", R.string.chat_help_wifi_list, null);   
        wifi.AddSubCmd("enable", R.string.chat_help_wifi_enable, "#id#");   
        wifi.AddSubCmd("disable", R.string.chat_help_wifi_disable, "#id#");   
    }
}
