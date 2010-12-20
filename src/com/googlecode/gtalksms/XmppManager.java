package com.googlecode.gtalksms;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

import android.app.Service;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.googlecode.gtalksms.tools.Tools;

public class XmppManager {

    
    public static final int DISCONNECTED = 0;
    public static final int CONNECTING = 1;
    public static final int CONNECTED = 2;
    public static final int DISCONNECTING = 3;
    public static final int EXIT = 4;

    // Indicates the current state of the service (disconnected/connecting/connected)
    private int mStatus = DISCONNECTED;

    private ConnectionConfiguration mConnectionConfiguration = null;
    private XMPPConnection mConnection = null;
    private PacketListener mPacketListener = null;
    
    // Our current retry attempt, plus a runnable and handler to implement retry
    private int mCurrentRetryCount = 0;
    Runnable mReconnectRunnable = null;
    Handler mReconnectHandler = new Handler();

    private SettingsManager _settings;
    private Context _context;
    private XmppListener _listener;
    
    public XmppManager(XmppListener listener, SettingsManager settings, Context context) {
        _settings = settings;
        _context = context;
        _listener = listener;
    }

    public void start() {
        mConnectionConfiguration = new ConnectionConfiguration(_settings.serverHost, _settings.serverPort, _settings.serviceName);

        updateStatus(DISCONNECTED);

        mCurrentRetryCount = 0;
        mReconnectRunnable = new Runnable() {
            public void run() {
                if (mCurrentRetryCount > 0) {
                    Log.v(Tools.LOG_TAG, "attempting reconnection");
                    Toast.makeText(_context, "Reconnecting", Toast.LENGTH_SHORT).show();
                }
                initConnection();
            }
        };
        
        mReconnectHandler.postDelayed(mReconnectRunnable, 10);
    }
    
    public void stop() {
        if (isConnected()) {
            updateStatus(DISCONNECTING);
            if (_settings.notifyApplicationConnection) {
                send("GTalkSMS stopped.");
            }
        }
        
        if (mReconnectRunnable != null) {
            mReconnectHandler.removeCallbacks(mReconnectRunnable);
        }
        
        if (mConnection != null) {
            if (mPacketListener != null) {
                mConnection.removePacketListener(mPacketListener);
            }
            // don't try to disconnect if already disconnected
            if (isConnected()) {
                mConnection.disconnect();
            }
        }
        mConnection = null;
        mPacketListener = null;
        mConnectionConfiguration = null;
        updateStatus(DISCONNECTED);
    }
    
    /** Updates the status about the service state (and the statusbar)*/
    private void updateStatus(int status) {
        if (status != mStatus) {
            _listener.onConnectionStatusChanged(mStatus, status);
            mStatus = status;
        }
    }

    private void maybeStartReconnect() {
        if (mCurrentRetryCount > 5) {
            // we failed after all the retries - just die.
            Log.v(Tools.LOG_TAG, "maybeStartReconnect ran out of retrys");
            updateStatus(DISCONNECTED);
            Toast.makeText(_context, "Failed to connect.", Toast.LENGTH_SHORT).show();
            updateStatus(EXIT);
            return;
        } else {
            mCurrentRetryCount += 1;
            // a simple linear-backoff strategy.
            int timeout = 5000 * mCurrentRetryCount;
            Log.e(Tools.LOG_TAG, "maybeStartReconnect scheduling retry in " + timeout);
            mReconnectHandler.postDelayed(mReconnectRunnable, timeout);
        }
    }

    /** init the XMPP connection */
    private void initConnection() {
        updateStatus(CONNECTING);
        NetworkInfo active = ((ConnectivityManager)_context.getSystemService(Service.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (active==null || !active.isAvailable()) {
            Log.e(Tools.LOG_TAG, "connection request, but no network available");
            Toast.makeText(_context, "Waiting for network to become available.", Toast.LENGTH_SHORT).show();
            // we don't destroy the service here - our network receiver will notify us when
            // the network comes up and we try again then.
            updateStatus(DISCONNECTED);
            return;
        }
        
        XMPPConnection connection = new XMPPConnection(mConnectionConfiguration);
        try {
            connection.connect();
        } catch (Exception e) {
            Log.e(Tools.LOG_TAG, "xmpp connection failed: " + e);
            Toast.makeText(_context, "Connection failed.", Toast.LENGTH_SHORT).show();
            maybeStartReconnect();
            return;
        }
        
        try {
            connection.login(_settings.mLogin, _settings.mPassword, "GTalkSMS");
        } catch (Exception e) {
            try {
                connection.disconnect();
            } catch (Exception e2) {
                Log.e(Tools.LOG_TAG, "xmpp disconnect failed: " + e2);
            }
            
            Log.e(Tools.LOG_TAG, "xmpp login failed: " + e);
            // sadly, smack throws the same generic XMPPException for network
            // related messages (eg "no response from the server") as for
            // authoritative login errors (ie, bad password).  The only
            // differentiator is the message itself which starts with this
            // hard-coded string.
            if (e.getMessage().indexOf("SASL authentication")==-1) {
                // doesn't look like a bad username/password, so retry
                Toast.makeText(_context, "Login failed", Toast.LENGTH_SHORT).show();
                maybeStartReconnect();
            } else {
                Toast.makeText(_context, "Invalid username or password", Toast.LENGTH_SHORT).show();
                updateStatus(EXIT);
            }
            return;
        }
        
        mConnection = connection;
        onConnectionComplete();
    }

    private void onConnectionComplete() {
        
        Log.v(Tools.LOG_TAG, "connection established");
        mCurrentRetryCount = 0;
        PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
        mPacketListener = new PacketListener() {
            public void processPacket(Packet packet) {
                Message message = (Message) packet;

                if ( message.getFrom().toLowerCase().startsWith(_settings.mTo.toLowerCase() + "/") && 
                     !message.getFrom().equals(mConnection.getUser())) {
                    if (message.getBody() != null) {
                        _listener.onMessageReceived(message.getBody());
                    }
                }
            }
        };
        mConnection.addPacketListener(mPacketListener, filter);
        updateStatus(CONNECTED);
        // Send welcome message
        if (_settings.notifyApplicationConnection) {
            send("Welcome to GTalkSMS " + Tools.getVersionName(_context, getClass()) + ". Send \"?\" for getting help");
        }
        Presence presence = new Presence(Presence.Type.available);
        presence.setStatus("GTalkSMS");
        presence.setPriority(24);                   
        mConnection.sendPacket(presence);
    }
    
    /** returns true if the service is correctly connected */
    public boolean isConnected() {
        return    (mConnection != null
                && mConnection.isConnected()
                && mConnection.isAuthenticated());
    }

    /** returns true if the service is correctly connected */
    public int getConnectionStatus() {
        return mStatus;
    }

    /** sends a message to the user */
    public void send(String message) {
        if (isConnected()) {
            Message msg = new Message(_settings.mTo, Message.Type.chat);
            msg.setBody(message);
            mConnection.sendPacket(msg);
        }
    }
}