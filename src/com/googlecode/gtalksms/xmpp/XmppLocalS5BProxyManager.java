package com.googlecode.gtalksms.xmpp;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamManager;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Proxy;

import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.tools.Tools;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class XmppLocalS5BProxyManager {
    
    private static XmppLocalS5BProxyManager sS5BManager;
    
    private final WifiManager mWifiManager;
    private final Socks5Proxy mProxy;
    
    
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
                disableStreamHostPrioritization(connection);
            }            
        };
        xmppMgr.registerConnectionChangeListener(listener);
    }
    
    private static void disableStreamHostPrioritization(XMPPConnection connection) {
        Socks5BytestreamManager s5bsm = Socks5BytestreamManager.getBytestreamManager(connection);
        s5bsm.setProxyPrioritizationEnabled(false);
        s5bsm.setTargetResponseTimeout(30000);
    }
    
    private void maybeUpdateLocalIP() {
        WifiInfo info = mWifiManager.getConnectionInfo();
        List<String> addresses = new ArrayList<String>();

        if (info != null) {
            // There is an active Wifi connection
            String ip = Tools.ipIntToString(info.getIpAddress());
            // Sometimes "0.0.0.0" gets returned
            if (!ip.equals("0.0.0.0")) {
                addresses.add(ip);
            }

        } 
        // set an ip in case there is a Wifi Connection
        // otherwise addresses will be empty and local S5B proxy
        // will not be used
        mProxy.replaceLocalAddresses(addresses);
    }
}
