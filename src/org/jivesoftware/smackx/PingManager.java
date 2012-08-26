package org.jivesoftware.smackx;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.packet.Ping;
import org.jivesoftware.smackx.packet.Pong;
import org.jivesoftware.smackx.provider.PingProvider;

public class PingManager {
    
    public static final String NAMESPACE = "urn:xmpp:ping";
    public static final String ELEMENT = "ping";
    
    static {
        ProviderManager.getInstance().addIQProvider(ELEMENT, NAMESPACE, new PingProvider());
    }
    
    private Connection connection;
    private Thread serverPingThread;

    
    public PingManager(Connection connection, int pingIntervall) {
        this.connection = connection;
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        sdm.addFeature(NAMESPACE);
        PacketFilter pingPacketFilter = new PacketTypeFilter(Ping.class);
        connection.addPacketListener(new PingPacketListener(connection), pingPacketFilter);
        
        if (pingIntervall > 0) {
            ServerPingTask serverPingTask = new ServerPingTask(pingIntervall);
            serverPingThread = new Thread(serverPingTask);
            serverPingThread.setDaemon(true);
            serverPingThread.setName("Smack Ping Server Task (" + connection.getServiceName() + ")");
            serverPingThread.start();
        }
    }
    
    public boolean ping(String to) {
        Ping ping = new Ping(connection.getUser(), to);
        
        PacketCollector collector =
                connection.createPacketCollector(new PacketIDFilter(ping.getPacketID()));
        
        connection.sendPacket(ping);
        
        IQ result = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        
        collector.cancel();
        if (result == null) {
            return false;
        }
        if (result.getType() == IQ.Type.ERROR) {
            // A Error response is as good as a pong response
            return true;
        }
        return true;
    }


    private class PingPacketListener implements PacketListener {
        private Connection connection;

        public PingPacketListener(Connection connection) {
            this.connection = connection;
        }
        
        public void processPacket(Packet packet) {
            Pong pong = new Pong(packet);
            this.connection.sendPacket(pong);
        }
    }
    
    private class ServerPingTask implements Runnable {
        private int delay;
        
        ServerPingTask(int delay) {
            this.delay = delay;
        }
        
        public void run() {
            boolean res;
            
            try {
                // Sleep a minimum of 15 seconds plus delay before sending first heartbeat. This will give time to
                // properly finish TLS negotiation and then start sending heartbeats.
                Thread.sleep(15000 + delay);
            }
            catch (InterruptedException ie) {
                // Do nothing
            }
            
            while(true) {
                if (connection.isAuthenticated()) {
                    res = ping(connection.getServiceName());
                }
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
}
