package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.PingManager;

public class Ping extends IQ {
    
    public Ping() {
    }
    
    public Ping(String from, String to) {
        setTo(to);
        setFrom(from);
        setType(IQ.Type.GET);
        setPacketID(getPacketID());
    }
    
    public String getChildElementXML() {
        return "<" + PingManager.ELEMENT + " xmlns=\'" + PingManager.NAMESPACE + "\' />";
    }

}
