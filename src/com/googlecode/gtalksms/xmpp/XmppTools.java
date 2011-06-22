package com.googlecode.gtalksms.xmpp;

import java.util.Iterator;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverInfo.Feature;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.packet.DiscoverItems.Item;

public class XmppTools {
    public static final String MUC_FEATURE = "http://jabber.org/protocol/muc";
    
    /**
     * Tries to discover if the server of the given XMPPConnection provides 
     * also a MUC component. Will return the MUCs Component ID if one is found
     * otherwise returns null
     * 
     * @param connection
     * @return
     * @throws XMPPException
     */
    public static String disocverMUC(XMPPConnection connection) throws XMPPException {
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        DiscoverItems ditem = sdm.discoverItems(connection.getServiceName());
        Iterator<Item> it = ditem.getItems();
        while (it.hasNext()) {
            Item i = it.next();
            DiscoverInfo dinfo = sdm.discoverInfo(i.getEntityID());
            Iterator<Feature> itFeatures = dinfo.getFeatures();
            while (itFeatures.hasNext()) {
                Feature f = itFeatures.next();
                if (f.getVar().equals(MUC_FEATURE)) {
                    return i.getEntityID();
                }
            }
        }        
        return null;
    }
    
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
}
