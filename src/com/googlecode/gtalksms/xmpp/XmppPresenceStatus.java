package com.googlecode.gtalksms.xmpp;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;

import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.tools.Tools;

import android.content.Context;

public class XmppPresenceStatus {
    
    private static XMPPConnection sConnection;
    private static XmppBuddies sXmppBuddies;
    private static XmppPresenceStatus sXmppPresenceStatus;
    
    private String mBatteryPercentage;
    private String mPowerSource;
    
    private XmppPresenceStatus(Context ctx) {
        sXmppBuddies = XmppBuddies.getInstance(ctx);
    }
    
    public static XmppPresenceStatus getInstance(Context ctx) {
        if (sXmppPresenceStatus == null) {
            sXmppPresenceStatus = new XmppPresenceStatus(ctx);            
        }
        return sXmppPresenceStatus;
    }
    
    public void registerListener(XmppManager xmppMgr) {
        XmppConnectionChangeListener listener = new XmppConnectionChangeListener() {
            public void newConnection(XMPPConnection connection) {
                sConnection = connection;
                setStatus(true);
            }
        };
        xmppMgr.registerConnectionChangeListener(listener);
    }
    
    /**
     * For the BatteryCmd, should only be called if one parameter
     * has changed
     * 
     * @param percentage
     * @param source
     */
    public void setPowerInfo(String percentage, String source) {
        mBatteryPercentage = percentage;
        mPowerSource = source;
        newStatusInformationAvailable();
    }
    
    private String composePresenceStatus() {
        // start with the App name
        String res = Tools.APP_NAME;
        // and add battery information
        if (mBatteryPercentage != null) {
            res += " - " + mBatteryPercentage;
        }
        if (mPowerSource != null) {
            res += " - " + mPowerSource;
        }
        return res;
    }    
    
    /**
     * Sets the XMPP presence status, but only if
     * - we are connected
     * - the notification address is online
     * 
     * @param force - don't check if the notification address is online
     * @return true if the presence was set
     */
    private boolean setStatus(boolean force) {
        if ((sXmppBuddies.isNotificationAddressAvailable() || force) 
                && (sConnection != null && sConnection.isAuthenticated())) {
            Presence presence = new Presence(Presence.Type.available);
            presence.setStatus(composePresenceStatus());
            presence.setPriority(24);
            sConnection.sendPacket(presence);
            return true;
        } else {
            return false;
        }
    }   
    
    private void newStatusInformationAvailable() {
        setStatus(false);
    }
}
