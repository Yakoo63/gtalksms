package org.jivesoftware.smackx;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionListener;
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
    private ServerPingTask serverPingTask;
    private int pingIntervall;

    
    public PingManager(Connection connection, int pingIntervall) {
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        sdm.addFeature(NAMESPACE);
        PacketFilter pingPacketFilter = new PacketTypeFilter(Ping.class);
        connection.addPacketListener(new PingPacketListener(connection), pingPacketFilter);
        this.connection = connection;
        this.pingIntervall = pingIntervall;
        connection.addConnectionListener(new PingConnectionListener());
        maybeStartPingServerTask();
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
    
    private class PingConnectionListener implements ConnectionListener {

        @Override
        public void connectionClosed() {
            maybeStopPingServerTask();
        }

        @Override
        public void connectionClosedOnError(Exception arg0) {
            maybeStopPingServerTask();
        }

        @Override
        public void reconnectingIn(int arg0) {
        }

        @Override
        public void reconnectionFailed(Exception arg0) {
        }

        @Override
        public void reconnectionSuccessful() {
            maybeStartPingServerTask();
        }
        
    }
    
    private void maybeStartPingServerTask() {
        if (pingIntervall > 0) {
            serverPingTask = new ServerPingTask();
            serverPingThread = new Thread(serverPingTask);
            serverPingThread.setDaemon(true);
            serverPingThread.setName("Smack Ping Server Task (" + connection.getServiceName() + ")");
            serverPingThread.start();
        }
    }
    
    private void maybeStopPingServerTask() {
        if (serverPingThread != null) {
            serverPingTask.setDone();
            serverPingThread.interrupt();
        }
        
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
        boolean done;
        
        ServerPingTask() {
            done = false;
        }
        
        public void setDone() {
            done = true;
        }
        
        public void run() {            
            try {
                // Sleep a minimum of 60 seconds plus delay before sending first ping.
                // This will give time to properly finish TLS negotiation and 
                // then start sending XMPP pings to the server.
                Thread.sleep(60000 + pingIntervall);
            }
            catch (InterruptedException ie) {
                // Do nothing
            }
            
            while(!done) {
                if (connection.isAuthenticated()) {
                    ping(connection.getServiceName());
                }
                try {
                    Thread.sleep(pingIntervall);
                } catch (InterruptedException e) {
                    /* Ignore */
                }
            }
        }
    }
}
