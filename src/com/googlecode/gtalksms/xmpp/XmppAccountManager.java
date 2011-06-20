package com.googlecode.gtalksms.xmpp;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;

import android.content.SharedPreferences.Editor;

import com.googlecode.gtalksms.SettingsManager;

public class XmppAccountManager {
    
    /**
     * Tries to create a new account and if successful 
     * will write the account data to the SharedPreferences.
     * Will return null on success, otherwise a string with the
     * error message or a XMPPException
     * 
     * @param jid
     * @param password
     * @param settings
     * @return
     * @throws XMPPException
     */
    public static String tryToCreateAccount(String jid, String password, SettingsManager settings) throws XMPPException {
        String host = StringUtils.parseServer(jid);
        String username = StringUtils.parseName(jid);
        if (host.equals("")) {
            return "JID without server part";
        }
        if (username.equals("")) {
            return "JID without user part";
        }
        
        ConnectionConfiguration conf = new ConnectionConfiguration(host);
        XMPPConnection connection = new XMPPConnection(conf);
        connection.connect();
        AccountManager accManager = new AccountManager(connection);
        if(!accManager.supportsAccountCreation()) {
            return "Server does not support account creation";
        }
        accManager.createAccount(username, password);
        
        Editor editor = settings.getEditor();
        
        editor.putString("xmppSecurityMode", "opt");
        editor.putBoolean("useCompression", false);
        editor.putBoolean("manuallySpecifyServerSettings", false);
        editor.putString("login", jid);
        editor.putString("password", password);
        
        editor.commit();
        
        return null;
    }
}
