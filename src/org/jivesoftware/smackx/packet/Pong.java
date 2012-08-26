package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;

public class Pong extends IQ {
    
    public Pong(Packet ping) {
        setType(IQ.Type.RESULT);
        setFrom(ping.getTo());
        setTo(ping.getFrom());
        setPacketID(ping.getPacketID());
    }
    
    public String getChildElementXML() {
        return null;
    }

}
