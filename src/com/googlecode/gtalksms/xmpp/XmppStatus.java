package com.googlecode.gtalksms.xmpp;

import java.io.File;

import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.databases.KeyValueHelper;

import android.content.Context;

/**
 * This class provides an interface to the keyValue database
 * the last known state of the XMPP connection is saved. This helps
 * MainService to detect an unintentional restart of GTalkSMS and restore 
 * the last known state.
 *
 */
public class XmppStatus {
    
    private static final String STATEFILE_NAME = "xmppStatus";
    
    private static XmppStatus sXmppStatus;
    
    private final File mStatefile;
    private final KeyValueHelper mKeyValueHelper;
    
    
    private XmppStatus(Context ctx) {
        File filesDir = ctx.getFilesDir();
        mStatefile = new File(filesDir, STATEFILE_NAME);
        mKeyValueHelper = KeyValueHelper.getKeyValueHelper(ctx.getApplicationContext());
        // Delete the old statefile
        // TODO remove this check with a future release
        if (mStatefile.isFile()) {
            mStatefile.delete();
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
        String value = mKeyValueHelper.getValue(KeyValueHelper.KEY_XMPP_STATUS);
        if (value != null) {
            try {
                res = Integer.parseInt(value);
            } catch(NumberFormatException e) { 
                Log.e("XmppStatus unable to parse integer", e);
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
        mKeyValueHelper.addKey(KeyValueHelper.KEY_XMPP_STATUS, value);
    }
}
