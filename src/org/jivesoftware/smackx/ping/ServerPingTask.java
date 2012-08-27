package org.jivesoftware.smackx.ping;

import java.lang.ref.WeakReference;
import java.util.Set;

import org.jivesoftware.smack.Connection;

class ServerPingTask implements Runnable {
    
    // This has to be a weak reference because IIRC all threads are roots
    // for objects and we have a new thread here that should hold a strong
    // reference to connection so that it can be GCed.
    private WeakReference<Connection> weakConnection;
    private int pingInterval;
    private volatile long lastSuccessfulPing = -1;
    
    private int delta = 1000; // 1 seconds
    private int tries = 3; // 3 tries
    
    protected ServerPingTask(Connection connection, int pingIntervall) {
        this.weakConnection = new WeakReference<Connection>(connection);
        this.pingInterval = pingIntervall;
    }
    
    protected void setDone() {
        this.pingInterval = -1;
    }
    
    protected void setPingInterval(int pingIntervall) {
        this.pingInterval = pingIntervall;
    }
    
    protected int getIntInterval() {
        return pingInterval;
    }
    
    protected long getLastSucessfulPing() {
        return lastSuccessfulPing;
    }
    
    public void run() {
        sleep(60000);
        
        outerLoop:
        while(pingInterval > 0) {
            Connection connection = weakConnection.get();
            if (connection == null) {
                // connection has been collected by GC
                // which means we can stop the thread by breaking the loop
                break;
            }
            if (connection.isAuthenticated()) {
                PingManager pingManager = PingManager.getInstaceFor(connection);
                boolean res = false;
                
                for(int i = 0; i < tries; i++) {
                    if (i != 0) {
                        try {
                            Thread.sleep(delta);
                        } catch (InterruptedException e) {
                            // We received an interrupt
                            // This only happens if we should stop pinging
                            break outerLoop;
                        }
                    }
                    res = pingManager.pingMyServer();
                    // stop when we receive a pong back
                    if (res) {
                        lastSuccessfulPing = System.currentTimeMillis();
                        break;
                    }
                }
                if (!res) {
                    Set<PingFailedListener> pingFailedListeners = pingManager.getPingFailedListeners();
                    for (PingFailedListener l : pingFailedListeners) {
                        l.pingFailed();
                    }
                }
            }
            sleep();
        }
    }
    
    private void sleep(int extraSleepTime) {
        int totalSleep = pingInterval + extraSleepTime;
        if (totalSleep > 0) {
            try {
                Thread.sleep(totalSleep);
            } catch (InterruptedException e) {
                /* Ignore */
            }
        }
    }
    
    /**
     * Sleeps the amount of pingInterval
     */
    private void sleep() {
        sleep(0);
    }
}
