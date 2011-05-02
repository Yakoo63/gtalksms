package com.googlecode.gtalksms.xmpp;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import com.googlecode.gtalksms.SettingsManager;

public class PresencePacketListener implements PacketListener  {
    private XMPPConnection mConnection;
    private SettingsManager mSettings;
    
    public PresencePacketListener(XMPPConnection connection, SettingsManager settings) {
        this.mConnection = connection;
        this.mSettings = settings;
    }

    @Override
    public void processPacket(Packet packet) {
        final String notifiedAddress = mSettings.notifiedAddress;
        Presence presence = (Presence) packet;
        String fromJID = StringUtils.parseBareAddress(presence.getFrom());
        
        if (fromJID.equals(notifiedAddress) && presence.getType().equals(Presence.Type.subscribe)) {
            XmppBuddies.grantSubscription(notifiedAddress, mConnection);
        }
    }
}
