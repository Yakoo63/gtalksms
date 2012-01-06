package com.googlecode.gtalksms.xmpp;

public class XmppTools {
    
    /**
     * Returns true if a JID in the format
     * user@server.tld is given. 
     * Note: ATM this method checks only if an "@" exists in the string
     * but no "/"
     * 
     * @param jid
     * @return
     */
    public static boolean isValidJID(String jid) {
        if (jid.contains("/") || !jid.contains("@")) {
            return false;
        }
        return true;
    }

    /**
     * Basic check that servername is valid FQDN.
     * Currently checks that there is one one or more dots and that
     * the String does not start or end with an dot
     * 
     * @param servername
     * @return
     */
    public static boolean isValidServername(String servername) {
        int len = servername.length();
        int LastPosOfDot = servername.lastIndexOf('.');
        int FirstPosOfDot = servername.indexOf('c');
        if (len < 3 ||
                LastPosOfDot == -1 ||               
                LastPosOfDot == len-1 ||
                FirstPosOfDot == 0) {
            return false;
        }
        return true;
    }
}
