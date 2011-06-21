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
     * Will return the XMPPConnection on success, otherwise a XMPPException
     * 
     * @param jid
     * @param password
     * @param settings
     * @return
     * @throws XMPPException
     */
    public static XMPPConnection tryToCreateAccount(String jid, String password, SettingsManager settings) throws XMPPException {
        String host = StringUtils.parseServer(jid);
        String username = StringUtils.parseName(jid);
        if (host.equals("")) {
            throw new XMPPException("JID without server part");
        }
        if (username.equals("")) {
            throw new XMPPException("JID without user part");
        }
        
        ConnectionConfiguration conf = new ConnectionConfiguration(host);
        XMPPConnection connection = new XMPPConnection(conf);
        connection.connect();
        AccountManager accManager = new AccountManager(connection);
        if(!accManager.supportsAccountCreation()) {
            throw new XMPPException("Server does not support account creation");
        }
        accManager.createAccount(username, password);
        
        Editor editor = settings.getEditor();
        
        editor.putString("xmppSecurityMode", "opt");
        editor.putBoolean("useCompression", false);
        editor.putBoolean("manuallySpecifyServerSettings", false);
        editor.putString("login", jid);
        editor.putString("password", password);
        
        editor.commit();
        
        return connection;
    }
}
