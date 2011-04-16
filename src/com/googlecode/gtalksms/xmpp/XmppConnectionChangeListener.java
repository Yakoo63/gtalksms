package com.googlecode.gtalksms.xmpp;

import org.jivesoftware.smack.XMPPConnection;

public abstract class XmppConnectionChangeListener {
    
    public abstract void newConnection(XMPPConnection connection);

}
