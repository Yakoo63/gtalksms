package com.googlecode.gtalksms.xmpp;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Proxy;

import com.googlecode.gtalksms.XmppManager;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class XmppLocalS5BProxyManager {
    
    private static XmppLocalS5BProxyManager sS5BManager;
    
    private WifiManager mWifiManager;
    private Socks5Proxy mProxy;
    
    
    private XmppLocalS5BProxyManager(Context ctx) {
        mWifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        mProxy = Socks5Proxy.getSocks5Proxy();

    }
    
    public static XmppLocalS5BProxyManager getInstance(Context ctx) {
        if (sS5BManager == null) {
            sS5BManager = new XmppLocalS5BProxyManager(ctx);
        }        
        return sS5BManager; 
    }

    public void registerListener(XmppManager xmppMgr) {
        XmppConnectionChangeListener listener = new XmppConnectionChangeListener() {
            public void newConnection(XMPPConnection connection) {
                maybeUpdateLocalIP();
            }            
        };
        xmppMgr.registerConnectionChangeListener(listener);
    }
    
    private void maybeUpdateLocalIP() {
        WifiInfo info = mWifiManager.getConnectionInfo();
        List<String> addresses = new ArrayList<String>();

        if (info != null) {
            // There is an active Wifi connection
            String ip = ipIntToString(info.getIpAddress());
            addresses.add(ip);

        } 
        // set an ip in case there is a Wifi Connection
        // otherwise addresses will be empty and local S5B proxy
        // will not be used
        mProxy.replaceLocalAddresses(addresses);
    }
    
    private static String ipIntToString(int ip) {
        return String.format("%d.%d.%d.%d", 
        (ip & 0xff), 
        (ip >> 8 & 0xff),
        (ip >> 16 & 0xff),
        (ip >> 24 & 0xff));
    }

}
