package org.jivesoftware.smackx.ping;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jivesoftware.smack.AbstractConnectionListener;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
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
    private int pingIntervall = 30*60*1000; // 30 min
    private Set<PingFailedListener> pingFailedListeners = Collections
            .synchronizedSet(new HashSet<PingFailedListener>());
    
    private long pingMinDelta = 100;
    private long lastPingStamp = 0;
    
    private PingManager(Connection connection) {
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        sdm.addFeature(NAMESPACE);
        this.connection = connection;
        PacketFilter pingPacketFilter = new PacketTypeFilter(Ping.class);
        connection.addPacketListener(new PingPacketListener(), pingPacketFilter);
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
        if (serverPingTask != null) {
            serverPingTask.setPingIntervall(pingIntervall);
        }
    }
    
    public int getPingIntervall() {
        return pingIntervall;
    }
    
    public void disablePingFloodProtection() {
        setPingMinimumInterval(-1);
    }
    
    public void setPingMinimumInterval(long ms) {
        this.pingMinDelta = ms;
    }
    
    public long getPingMinimumInterval() {
        return this.pingMinDelta;
    }
    
    public void registerPingFailedListener(PingFailedListener listener) {
        pingFailedListeners.add(listener);
    }
    
    public void unregisterPingFailedListener(PingFailedListener listener) {
        pingFailedListeners.remove(listener);
    }
    
    public IQ ping(String to) {
        Ping ping = new Ping(connection.getUser(), to);
        
        PacketCollector collector =
                connection.createPacketCollector(new PacketIDFilter(ping.getPacketID()));
        
        connection.sendPacket(ping);
        
        IQ result = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        
        collector.cancel();
        return result;
    }
    
    /**
     * Pings the given Entity.
     * 
     * Note that XEP-199 shows that if we receive a error response
     * service-unavailable there is no way to determine if the response was send
     * by the entity (e.g. a user JID) or from a server in between. This is
     * intended behavior to avoid presence leaks.
     * 
     * @param jid
     * @return True if successful, otherwise false
     */
    public boolean pingEntity(String jid) {
        IQ result = ping(jid);
        
        if (result == null 
                || result.getType() == IQ.Type.ERROR) {
            return false;
        } 
        return true;
    }
    
    /**
     * Pings the user's server.
     * 
     * If we receive as response, we can be sure that it came from the server.
     * 
     * @return True if successful, otherwise false
     */
    public boolean pingMyServer() {
        IQ result = ping(connection.getServiceName());
        if (result == null) {
            return false;
        }
        return true;
    }
    
    /**
     * Returns true if XMPP Ping is supported by a given JID
     * 
     * @param jid
     * @return
     */
    public boolean isPingSupported(String jid) {
        try {
            DiscoverInfo result =
                ServiceDiscoveryManager.getInstanceFor(connection).discoverInfo(jid);
            return result.containsFeature(NAMESPACE);
        }
        catch (XMPPException e) {
            return false;
        }
    }
    
    protected Set<PingFailedListener> getPingFailedListeners() {
        return pingFailedListeners;
    }
    
    private class PingConnectionListener extends AbstractConnectionListener {

        @Override
        public void connectionClosed() {
            maybeStopPingServerTask();
        }

        @Override
        public void connectionClosedOnError(Exception arg0) {
            maybeStopPingServerTask();
        }

        @Override
        public void reconnectionSuccessful() {
            maybeStartPingServerTask();
        }

    }
    
    private void maybeStartPingServerTask() {
        if (serverPingTask != null) {
            serverPingTask.setDone();
            serverPingThread.interrupt();
            serverPingTask = null;
            serverPingThread = null;
            System.err.println("Smack PingManger: Found existing serverPingTask");
        }
        
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

        public PingPacketListener() {
        }
        
        /**
         * Sends a Pong for every Ping
         */
        public void processPacket(Packet packet) {
            if (pingMinDelta > 0) {
                // Ping flood protection enabled
                long currentMillies = System.currentTimeMillis();
                long delta = currentMillies - lastPingStamp;
                lastPingStamp = currentMillies;
                if (delta < pingMinDelta) {
                    return;
                }
            }
            Pong pong = new Pong(packet);
            connection.sendPacket(pong);
        }
    }
}
