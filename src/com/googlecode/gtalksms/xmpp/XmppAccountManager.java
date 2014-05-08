package com.googlecode.gtalksms.xmpp;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.util.StringUtils;

import android.content.SharedPreferences.Editor;

import com.googlecode.gtalksms.SettingsManager;


class XmppAccountManager {
    // some servers need the username to be a full JID define them here (currently only Google services)
    private static final String[] USERNAME_IS_FULL_JID = new String[] {"gmail.com", "googlemail.com"};                                
    
    /**
     * Tries to create a new account with help of XMPP in-band registration
     * and DNS SRV Records of the server.
     * Returns the XMPPConnection on success, 
     * otherwise throws an XMPPException
     * 
     * @param host
     * @param password
     * @return
     * @throws XMPPException
     */
    public static XMPPConnection tryToCreateAccount(String username, String host, String password) throws Exception {
        username = needsDomainPart(username, host);

        // TODO throws NetworkOnMainThreadException on Honycomb or higher
        // Fix it!
        ConnectionConfiguration conf = new ConnectionConfiguration(host);
        XMPPConnection connection = new XMPPTCPConnection(conf);
        connection.connect();

        AccountManager accManager = AccountManager.getInstance(connection);
        if(!accManager.supportsAccountCreation()) {
            throw new Exception("Server does not support account creation");
        }
        accManager.createAccount(username, password);
        return connection;
    }
    
    /**
     * Tries to return the correct username for the given host.
     * 
     * Some XMPP service provider (like gTalk) require the username
     * to be concatenated with the host part
     * e.g. user -> user@server.tld
     * 
     * @param username
     * @param host
     * @return
     */
    private static String needsDomainPart(String username, String host) {
        for (String s : USERNAME_IS_FULL_JID) {
            if (host.equals(s)) {
                return username + "@" + host;
            }
        }
        return username;
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
    private static void savePreferences(String jid, String password, String notifiedAddress, SettingsManager settings) {
        Editor editor = settings.getEditor();
        boolean useDifferentAccount;
        
        editor.putString("notifiedAddress", notifiedAddress);
        editor.putString("xmppSecurityMode", "opt");
        editor.putBoolean("useCompression", false);
        editor.putBoolean("manuallySpecifyServerSettings", false);
        editor.putString("login", jid);
        editor.putString("password", password);

        useDifferentAccount = !jid.equals(notifiedAddress);
        editor.putBoolean("useDifferentAccount", useDifferentAccount);
        
        editor.commit();
    }
    
    /**
     * Tries to make a connection. If successful returns this connection and 
     * saves the settings. If jid == notifiedAddress, same account mode is
     * assumed.
     * Throws an XMPPException on error.
     * 
     * @param jid
     * @param password
     * @param notifiedAddress
     * @param settings
     * @return
     * @throws XMPPException 
     */
    public static XMPPConnection makeConnectionAndSavePreferences(String jid, String password, String notifiedAddress, SettingsManager settings) throws Exception {
        String domain = StringUtils.parseServer(jid);

        // TODO throws NetworkOnMainThreadException on Honeycomb or higher
        // Fix it!
        XMPPConnection con = new XMPPTCPConnection(new ConnectionConfiguration(domain));
        con.connect();
        con.login(jid, password);
        // looks like we have successfully established a connection save the settings
        savePreferences(jid, password, notifiedAddress, settings);
        return con;
    }
}
