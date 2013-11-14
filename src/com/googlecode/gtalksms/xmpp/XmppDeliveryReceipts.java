package com.googlecode.gtalksms.xmpp;

import com.googlecode.gtalksms.XmppManager;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;

/**
 * Created by Florent on 14/11/13.
 */
public class XmppDeliveryReceipts {
    private static XmppDeliveryReceipts sXmppDeliveryReceipts;

    public static XmppDeliveryReceipts getInstance() {
        if (sXmppDeliveryReceipts == null) {
            sXmppDeliveryReceipts = new XmppDeliveryReceipts();
        }
        return sXmppDeliveryReceipts;
    }

    public void registerListener(XmppManager xmppMgr) {
        XmppConnectionChangeListener listener = new XmppConnectionChangeListener() {
            public void newConnection(XMPPConnection connection) {
                final DeliveryReceiptManager drm = DeliveryReceiptManager.getInstanceFor(connection);
                drm.enableAutoReceipts();
            }
        };
        xmppMgr.registerConnectionChangeListener(listener);
    }
}