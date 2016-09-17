package com.googlecode.gtalksms.xmpp;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.StringUtils;
import org.jxmpp.util.XmppStringUtils;

import com.googlecode.gtalksms.SettingsManager;

class PresencePacketListener implements PacketListener  {
    private final XMPPConnection mConnection;
    private final SettingsManager mSettings;
    
    public PresencePacketListener(XMPPConnection connection, SettingsManager settings) {
        this.mConnection = connection;
        this.mSettings = settings;
    }

    @Override
    public void processPacket(Stanza packet) throws SmackException.NotConnectedException {
        for (String notifiedAddress : mSettings.getNotifiedAddresses().getAll()) {
            Presence presence = (Presence) packet;
            String fromJID = XmppStringUtils.parseBareJid(presence.getFrom());
            
            if (fromJID.equals(notifiedAddress) && presence.getType().equals(Presence.Type.subscribe)) {
                XmppBuddies.grantSubscription(notifiedAddress, mConnection);
            }
        }
    }
}
