package com.googlecode.gtalksms.xmpp;

import java.io.File;

import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.databases.KeyValueHelper;
import com.googlecode.gtalksms.tools.GoogleAnalyticsHelper;

import android.content.Context;

public class XmppStatus {
    
    private static final String STATEFILE_NAME = "xmppStatus";
    
    private static XmppStatus sXmppStatus;
    private static File sStatefile;
    private static KeyValueHelper sKeyValueHelper;
    
    
    private XmppStatus(Context ctx) {
        File filesDir = ctx.getFilesDir();
        sStatefile = new File(filesDir, STATEFILE_NAME);
        sKeyValueHelper = KeyValueHelper.getKeyValueHelper(ctx);
        // Delete the old statefile
        // TODO remove this check with a future release
        if (sStatefile.isFile()) {
            sStatefile.delete();
        }
    }
    
    public static XmppStatus getInstance(Context ctx) {
        if (sXmppStatus == null) {
            sXmppStatus = new XmppStatus(ctx);            
        }
        return sXmppStatus;
    }
    
    /**
     * Gets the last known XMPP status from the statefile
     * if there is no statefile the status for DISCONNECTED is returned
     * 
     * @return integer representing the XMPP status as defined in XmppManager
     */
    public int getLastKnowState() {
        int res = XmppManager.DISCONNECTED;
        String value = sKeyValueHelper.getValue(KeyValueHelper.KEY_XMPP_STATUS);
        if (value != null) {
            try {
                res = Integer.parseInt(value);
            } catch(NumberFormatException e) { 
                GoogleAnalyticsHelper.trackAndLogError("XmppStatus unable to parse integer", e);
            }
        }
        return res;        
    }
    
    /**
     * Writes the current status int into the statefile
     * 
     * @param status
     */
    public void setState(int status) {
        String value = Integer.toString(status);
        sKeyValueHelper.addKey(KeyValueHelper.KEY_XMPP_STATUS, value);
    }
}
