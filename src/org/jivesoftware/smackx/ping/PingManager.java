package org.jivesoftware.smackx.ping;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionCreationListener;
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
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.Ping;
import org.jivesoftware.smackx.packet.Pong;
import org.jivesoftware.smackx.provider.PingProvider;

public class PingManager {
    
    public static final String NAMESPACE = "urn:xmpp:ping";
    public static final String ELEMENT = "ping";
    private static Map<Connection, PingManager> instances =
            Collections.synchronizedMap(new WeakHashMap<Connection, PingManager>());
    
    static {
        ProviderManager.getInstance().addIQProvider(ELEMENT, NAMESPACE, new PingProvider());
        Connection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(Connection connection) {
                new PingManager(connection);
            }
        });
    }
    
    private Connection connection;
    private Thread serverPingThread;
    private ServerPingTask serverPingTask;
    private int pingIntervall = 30*60*1000;
    private Set<PingFailedListener> pingFailedListeners = Collections
            .synchronizedSet(new HashSet<PingFailedListener>());
    
    private PingManager(Connection connection) {
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        sdm.addFeature(NAMESPACE);
        PacketFilter pingPacketFilter = new PacketTypeFilter(Ping.class);
        connection.addPacketListener(new PingPacketListener(connection), pingPacketFilter);
        this.connection = connection;
        connection.addConnectionListener(new PingConnectionListener());
        instances.put(connection, this);
        maybeStartPingServerTask();
    }
    
    public static PingManager getInstaceFor(Connection connection) {
        PingManager pingManager = instances.get(connection);
        
        if (pingManager == null) {
            pingManager = new PingManager(connection);
        }
        
        return pingManager;
    }
    
    public void setPingIntervall(int pingIntervall) {
        this.pingIntervall = pingIntervall;
        serverPingTask.setPingIntervall(pingIntervall);
    }
    
    public int getPingIntervall() {
        return pingIntervall;
    }
    
    public void registerPingFailedListener(PingFailedListener listener) {
        pingFailedListeners.add(listener);
    }
    
    public void unregisterPingFailedListener(PingFailedListener listener) {
        pingFailedListeners.remove(listener);
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
            // at least somebody is answering :)
            return true;
        }
        return true;
    }
    
    protected Set<PingFailedListener> getPingFailedListeners() {
        return pingFailedListeners;
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
            serverPingTask = new ServerPingTask(connection, pingIntervall);
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
        
        /**
         * Sends a Pong for every Ping
         */
        public void processPacket(Packet packet) {
            Pong pong = new Pong(packet);
            this.connection.sendPacket(pong);
        }
    }
}
