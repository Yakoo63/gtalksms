package com.googlecode.gtalksms.xmpp;

import java.util.Iterator;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.OfflineMessageManager;

import android.content.Context;

import com.googlecode.gtalksms.Log;
import com.googlecode.gtalksms.tools.Tools;

public class XmppOfflineMessages {
    
    public static void handleOfflineMessages(XMPPConnection connection, String notifiedAddress, Context ctx) throws XMPPException {
        Log.i("Begin retrival of offline messages from server");
        OfflineMessageManager offlineMessageManager = new OfflineMessageManager(connection);

        if (!offlineMessageManager.supportsFlexibleRetrieval())
            return;

        Iterator<Message> i = offlineMessageManager.getMessages();
        while (i.hasNext()) {
            Message msg = i.next();
            String fullJid = msg.getFrom();
            String bareJid = StringUtils.parseBareAddress(fullJid);
            String messageBody = msg.getBody();
            Log.i("Retrived offline message from " + fullJid + " with content: " + messageBody.substring(0, 40));
            if (bareJid.equals(notifiedAddress) && (messageBody != null)) {
                Tools.startSvcXMPPMsg(ctx, messageBody, fullJid);
            }
        }
        offlineMessageManager.deleteMessages();
    }
}
