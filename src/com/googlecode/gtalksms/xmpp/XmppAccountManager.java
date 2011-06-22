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
     * returns the XMPPConnection on success, otherwise throws an XMPPException
     * 
     * @param jid
     * @param password
     * @return
     * @throws XMPPException
     */
    public static XMPPConnection tryToCreateAccount(String jid, String password) throws XMPPException {
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

        return connection;
    }
    
    /**
     * Writes the given minimal settings to the shared preferences.
     * The jid needs to be in the form of user@server.tld
     * because smack will do automatic DNS SRV lookups on server.tld
     * to find the right XMPP server
     * 
     * @param jid
     * @param password
     * @param settings
     */
    public static void savePreferences(String jid, String password, String notifiedAddress, SettingsManager settings) {
        Editor editor = settings.getEditor();
        
        editor.putString("notifiedAddress", notifiedAddress);
        editor.putString("xmppSecurityMode", "opt");
        editor.putBoolean("useCompression", false);
        editor.putBoolean("manuallySpecifyServerSettings", false);
        editor.putString("login", jid);
        editor.putString("password", password);
        
        editor.commit();
    }
}
