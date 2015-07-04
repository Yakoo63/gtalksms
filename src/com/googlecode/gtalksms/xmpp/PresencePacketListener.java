package com.googlecode.gtalksms.xmpp;

import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
import org.jxmpp.util.XmppStringUtils;

import com.googlecode.gtalksms.SettingsManager;

class PresencePacketListener implements MessageListener {
    private final XMPPConnection mConnection;
    private final SettingsManager mSettings;
    
    public PresencePacketListener(XMPPConnection connection, SettingsManager settings) {
        this.mConnection = connection;
        this.mSettings = settings;
    }

    @Override
    public void processMessage(Message message) {
        for (String notifiedAddress : mSettings.getNotifiedAddresses().getAll()) {
            String fromJID = XmppStringUtils.parseBareJid(message.getFrom());

            if (fromJID.equals(notifiedAddress) && message.getType().equals(Presence.Type.subscribe)) {
                XmppBuddies.grantSubscription(notifiedAddress, mConnection);
            }
        }
    }
}
