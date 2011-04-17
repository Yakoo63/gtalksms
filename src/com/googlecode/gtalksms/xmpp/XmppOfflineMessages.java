package com.googlecode.gtalksms.xmpp;

import java.util.Iterator;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.OfflineMessageManager;

import android.content.Context;

import com.googlecode.gtalksms.tools.Tools;

public class XmppOfflineMessages {
    
    public static void handleOfflineMessages(XMPPConnection connection, String notifiedAddress, Context ctx) {
        try {
        OfflineMessageManager offlineMessageManager = new OfflineMessageManager(connection);
        
            if (!offlineMessageManager.supportsFlexibleRetrieval())
                return;

            Iterator<Message> i = offlineMessageManager.getMessages();
            while (i.hasNext()) {
                Message msg = i.next();
                String fullJid = msg.getFrom();
                String bareJid = StringUtils.parseBareAddress(fullJid);
                if (bareJid.equals(notifiedAddress) && (msg.getBody() != null)) {
                    Tools.startSvcXMPPMsg(ctx, msg.getBody(), fullJid);
                }
            }
            offlineMessageManager.deleteMessages();
        } catch (XMPPException e) {
            e.printStackTrace();
            return;
        }
    }
}
