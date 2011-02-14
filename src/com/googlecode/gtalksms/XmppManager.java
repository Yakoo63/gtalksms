package com.googlecode.gtalksms;

import java.util.ArrayList;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.XHTMLManager;
import org.jivesoftware.smackx.packet.ChatStateExtension;
import org.jivesoftware.smackx.packet.LastActivity;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.packet.SharedGroupsInfo;
import org.jivesoftware.smackx.provider.BytestreamsProvider;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.jivesoftware.smackx.provider.DelayInformationProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.IBBProviders;
import org.jivesoftware.smackx.provider.MUCAdminProvider;
import org.jivesoftware.smackx.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.provider.MUCUserProvider;
import org.jivesoftware.smackx.provider.MessageEventProvider;
import org.jivesoftware.smackx.provider.MultipleAddressesProvider;
import org.jivesoftware.smackx.provider.RosterExchangeProvider;
import org.jivesoftware.smackx.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.search.UserSearch;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.util.Log;

import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XHTMLExtensionProvider;
import com.googlecode.gtalksms.xmpp.XmppBuddies;
import com.googlecode.gtalksms.xmpp.XmppFileManager;
import com.googlecode.gtalksms.xmpp.XmppFriend;
import com.googlecode.gtalksms.xmpp.XmppMsg;
import com.googlecode.gtalksms.xmpp.XmppMuc;

public class XmppManager {

    public static final int DISCONNECTED = 0;
    // A "transient" state - will only be CONNECTING *during* a call to start()
    public static final int CONNECTING = 1;
    public static final int CONNECTED = 2;
    // A "transient" state - will only be DISCONNECTING *during* a call to stop()
    public static final int DISCONNECTING = 3;
    // This state either means we are waiting for the network to come up,
    // or waiting for a retry attempt etc.
    public static final int WAITING_TO_CONNECT = 4;

    // A list of intent actions we broadcast.
    static final public String ACTION_MESSAGE_RECEIVED = "com.googlecode.gtalksms.XMPP_MESSAGE_RECEIVED";
    static final public String ACTION_PRESENCE_CHANGED = "com.googlecode.gtalksms.XMPP_PRESENCE_CHANGED";
    static final public String ACTION_CONNECTION_CHANGED = "com.googlecode.gtalksms.XMPP_CONNECTION_CHANGED";
    
    // Indicates the current state of the service (disconnected/connecting/connected)
    private int _status = DISCONNECTED;
    private String _presenceMessage = "GTalkSMS";
    
    private static XMPPConnection _connection = null;
    private PacketListener _packetListener = null;
    private ConnectionListener _connectionListener = null;
    private long _myThreadId = 0;
    private XmppMuc _xmppMuc;
    private XmppBuddies _xmppBuddies;
    private XmppFileManager _xmppFileMgr;
    
    // Our current retry attempt, plus a runnable and handler to implement retry
    private int _currentRetryCount = 0;
    Runnable _reconnectRunnable = new Runnable() {
        public void run() {
            if (_currentRetryCount > 0) {
                Log.v(Tools.LOG_TAG, "attempting reconnection");
                Tools.toastMessage(_context, _context.getString(R.string.xmpp_manager_reconnecting));
            }
            _context.startService(MainService.newSvcIntent(_context, MainService.ACTION_CONNECT));
        }
    };

    Handler _reconnectHandler = new Handler();

    private SettingsManager _settings;
    private Context _context;
    
    public XmppManager(SettingsManager settings, Context context) {
        _settings = settings;
        _context = context;
        configure(ProviderManager.getInstance());
        _xmppBuddies = new XmppBuddies(context, settings);
        _xmppFileMgr = new XmppFileManager(context, settings, this);
        _xmppMuc = new XmppMuc(context, settings, this);
    }

    private void start() {
        start(CONNECTED);
    }
    
    private void start(int initialState) {
        switch (initialState) {
            case CONNECTED:
                initConnection();
                break;
            case WAITING_TO_CONNECT:
                updateStatus(initialState);
                break;
            default:
                throw new IllegalStateException("Invalid State: " + initialState);
        }
    }
    
    protected void stop() {
        if (_connection != null && _connection.isConnected()) {
            updateStatus(DISCONNECTING);
            if (_settings.notifyApplicationConnection) {
                send(_context.getString(R.string.chat_app_stop));
            }
        }
        
        _reconnectHandler.removeCallbacks(_reconnectRunnable);
        
        if (_connection != null) {
            if (_packetListener != null) {
                _connection.removePacketListener(_packetListener);
            }
            if (_connectionListener != null) {
                _connection.removeConnectionListener(_connectionListener);
            }
            // don't try to disconnect if already disconnected
            if (isConnected()) {
                xmppDisconnect(_connection);
            }
        }
        _connection = null;
        _packetListener = null;
        _connectionListener = null;
        updateStatus(DISCONNECTED);
    }
    
    // This method *requests* a state change - what state things actually
    // wind up in is impossible to know (eg, a request to connect may wind up
    // with a state of CONNECTED, DISCONNECTED or WAITING_TO_CONNECT...
    protected void xmppRequestStateChange(int newState) {
        int currentState = getConnectionStatus();
        switch (newState) {
        case XmppManager.CONNECTED:
            switch (currentState) {
            case XmppManager.CONNECTED:
                break;
            case XmppManager.DISCONNECTED:
            case XmppManager.WAITING_TO_CONNECT:
                stop();
                start();
                break;
            default:
                throw new IllegalStateException("unexpected current state when moving to connected: " + currentState);
            }
            break;
        case XmppManager.DISCONNECTED:
            stop();
            break;
        case XmppManager.WAITING_TO_CONNECT:
            switch (currentState) {
            case XmppManager.CONNECTED:
                stop();
                start(XmppManager.WAITING_TO_CONNECT);
                break;
            case XmppManager.DISCONNECTED:
                start(XmppManager.WAITING_TO_CONNECT);
                break;
            case XmppManager.WAITING_TO_CONNECT:
                break;
            default:
                throw new IllegalStateException("unexpected current state when moving to waiting: " + currentState);
            }
            break;
        default:
            throw new IllegalStateException("invalid state to switch to: "+newState);
        }
        // Now we have requested a new state, our state receiver will see when
        // the state actually changes and update everything accordingly.
    }

    private static void xmppDisconnect(XMPPConnection connection) {
        // In some cases the 'disconnect' may hang - see
        // http://code.google.com/p/gtalksms/issues/detail?id=12 for an
        // example.  We worm around this by leveraging the fact that we 
        // are going to throw the XmppConnection away after disconnecting,
        // so just spawn a thread to perform the disconnection.  In the
        // usual good case the thread will terminate very quickly, and 
        // in the bad case the thread may hang around much longer - but 
        // at least we are still working and it should go away 
        // eventually...
        class DisconnectRunnable implements Runnable {
            public DisconnectRunnable(XMPPConnection x) {
                _x = x;
            }
            private XMPPConnection _x;
            public void run() {
                try {
                    _x.disconnect();
                } catch (Exception e2) {
                    Log.e(Tools.LOG_TAG, "xmpp disconnect failed: " + e2);
                }
            }
        }
        Thread t = new Thread(new DisconnectRunnable(connection), "xmpp-disconnector");
        // we don't want this thread to hold up process shutdown so mark as daemonic.
        t.setDaemon(true);
        t.start();
    }

    /** Updates the status about the service state (and the statusbar)*/
    public static void broadcastStatus(Context ctx, int old_state, int new_state) {
        Intent intent = new Intent(ACTION_CONNECTION_CHANGED);
        intent.putExtra("old_state", old_state);
        intent.putExtra("new_state", new_state);
        if(new_state == CONNECTED) {
            intent.putExtra("TLS", _connection.isUsingTLS());
            intent.putExtra("Compression", _connection.isUsingCompression());
        }
        ctx.sendBroadcast(intent);
    }

    private void updateStatus(int status) {
        if (status != _status) {
            // ensure _status is set before broadcast, just in-case
            // a receiver happens to wind up querying the state on
            // delivery.
            int old = _status;
            _status = status;
            Log.v(Tools.LOG_TAG, "broadcasting state transition from " + old + " to " + status);
            broadcastStatus(_context, old, status);
        }
    }

    private void maybeStartReconnect() {
        if (_currentRetryCount > 5) {
            // we failed after all the retries - just die.
            Log.v(Tools.LOG_TAG, "maybeStartReconnect ran out of retrys");
            stop(); // will set state to DISCONNECTED.
            Tools.toastMessage(_context, _context.getString(R.string.xmpp_manager_failed_max_attempts));
            return;
        } else {
            _currentRetryCount += 1;
            updateStatus(WAITING_TO_CONNECT);
            // a simple linear-backoff strategy.
            int timeout = 5000 * _currentRetryCount;
            Log.i(Tools.LOG_TAG, "maybeStartReconnect scheduling retry in " + timeout);
            _reconnectHandler.postDelayed(_reconnectRunnable, timeout);
        }
    }
    

    /** init the XMPP connection */
    private void initConnection() {

        // assert we are only ever called from one thread (which is
        // sadly not the thread we are constructed on, hence the special
        // case for when the thread-id is zero...)
        if (_myThreadId==0)
            _myThreadId = Thread.currentThread().getId();
        else if (_myThreadId != Thread.currentThread().getId())
            throw new IllegalThreadStateException();
        if (_connection != null) {
            return;
        }
        
        updateStatus(CONNECTING);
        NetworkInfo active = ((ConnectivityManager)_context.getSystemService(Service.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (active == null || !active.isAvailable()) {
            Log.e(Tools.LOG_TAG, "connection request, but no network available");
            Tools.toastMessage(_context, _context.getString(R.string.xmpp_manager_waiting));
            // we don't destroy the service here - our network receiver will notify us when
            // the network comes up and we try again then.
            updateStatus(WAITING_TO_CONNECT);
            return;
        }

        ConnectionConfiguration conf = new ConnectionConfiguration(_settings.serverHost, _settings.serverPort, _settings.serviceName);
        conf.setTruststorePath("/system/etc/security/cacerts.bks");
        conf.setTruststorePassword("changeit");
        conf.setTruststoreType("bks");
        if(_settings.useCompression) conf.setCompressionEnabled(true); 
        
        XMPPConnection connection = new XMPPConnection(conf);
        try {
            connection.connect();
        } catch (Exception e) {
            Log.e(Tools.LOG_TAG, "xmpp connection failed: " + e);
            Tools.toastMessage(_context, _context.getString(R.string.xmpp_manager_connection_failed));
            maybeStartReconnect();
            return;
        }
        
        try {
            connection.login(_settings.login, _settings.password, "GTalkSMS");
        } catch (Exception e) {
            xmppDisconnect(connection);
            Log.e(Tools.LOG_TAG, "xmpp login failed: " + e);
            // sadly, smack throws the same generic XMPPException for network
            // related messages (eg "no response from the server") as for
            // authoritative login errors (ie, bad password).  The only
            // differentiator is the message itself which starts with this
            // hard-coded string.
            if (e.getMessage().indexOf("SASL authentication") == -1) {
                // doesn't look like a bad username/password, so retry
                Tools.toastMessage(_context, _context.getString(R.string.xmpp_manager_login_failed));
                maybeStartReconnect();
            } else {
                Tools.toastMessage(_context, _context.getString(R.string.xmpp_manager_invalid_credentials));
                stop();
            }
            return;
        }
        
        _connection = connection;
        _connectionListener = new ConnectionListener() {
            @Override
            public void connectionClosed() {
                // This should never happen - we always remove the listener before
                // actually disconnecting - so something strange would be going on
                // for this to happen - but sure enough, something strange can go on :)
                // See issue 37 for one report where we see this - so we attempt
                // auto-reconnect just like in an explicit error case.
                Log.w(Tools.LOG_TAG, "xmpp got an unexpected normal disconnection");
                stop();
                maybeStartReconnect();
            }

            @Override
            public void connectionClosedOnError(Exception arg0) {
                // this is "unexpected" - our main service still thinks it has a 
                // connection.
                Log.w(Tools.LOG_TAG, "xmpp disconnected due to error: " + arg0.toString());
                // We update the state to disconnected (mainly to cleanup listeners etc)
                // then schedule an automatic reconnect.
                stop();
                maybeStartReconnect();
            }

            @Override
            public void reconnectingIn(int arg0) {
            }

            @Override
            public void reconnectionFailed(Exception arg0) {
            }

            @Override
            public void reconnectionSuccessful() {
            }
        };
        _connection.addConnectionListener(_connectionListener);            
        onConnectionComplete();
    }

    private void onConnectionComplete() {
        Log.v(Tools.LOG_TAG, "connection established");
        
        _xmppMuc.initialize(_connection);
        _xmppBuddies.initialize(_connection);
        _xmppFileMgr.initialize(_connection);
               
        _currentRetryCount = 0;
        PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
        _packetListener = new PacketListener() {
            public void processPacket(Packet packet) {
                Message message = (Message) packet;
                
                Log.d(Tools.LOG_TAG, "Xmpp packet received");
                
                if ( message.getFrom().toLowerCase().startsWith(_settings.notifiedAddress.toLowerCase() + "/") && 
                     !message.getFrom().equals(_connection.getUser())) {
                    if (message.getBody() != null) {
                        Intent intent = new Intent(ACTION_MESSAGE_RECEIVED);
                        intent.putExtra("message", message.getBody());
                        _context.sendBroadcast(intent);
                    }
                }
            }
        };
        _connection.addPacketListener(_packetListener, filter);
        updateStatus(CONNECTED);
        // Send welcome message
        if (_settings.notifyApplicationConnection) {
            send(_context.getString(R.string.chat_welcome, Tools.getVersionName(_context, getClass())));
        }
        
        // Manage Xmpp presence status
        Presence presence = new Presence(Presence.Type.available);
        presence.setStatus(_presenceMessage);
        presence.setPriority(24);                   
        _connection.sendPacket(presence);
        
        try {
            _connection.getRoster().addRosterListener(_xmppBuddies);
            _connection.getRoster().setSubscriptionMode(Roster.SubscriptionMode.manual);
            _xmppBuddies.retrieveFriendList();
        } catch (Exception ex) {
            Log.e(Tools.LOG_TAG, "Failed to setup Xmpp Friend list roster.", ex);
        }
    }
    
    /** returns true if the service is correctly connected */
    private boolean isConnected() {
        return    (_connection != null
                && _connection.isConnected()
                && _connection.isAuthenticated());
    }

    /** returns the current connection state */
    public int getConnectionStatus() {
        return _status;
    }
    
    public boolean getTLSStatus() {
        return _connection == null ? false : _connection.isUsingTLS();
    }
    
    public boolean getCompressionStatus() {
    	return _connection == null ? false : _connection.isUsingCompression();
    }

    /** 
     * sends a message to the user
     * but only if we connected
     * does nothing if we are not connected 
     * 
     */
    public void send(String message) {
        if (isConnected()) {
            send(new XmppMsg(message));
        }
    }
    
    /** sends a message to the user */
    public void send(XmppMsg message) {
        if (isConnected()) {
            Message msg = new Message(_settings.notifiedAddress, Message.Type.chat);
            
            if (_settings.formatChatResponses) {
                msg.setBody(message.generateFmtTxt());
            } else {
                msg.setBody(message.generateTxt());
            }
//            if (XHTMLManager.isServiceEnabled(_connection, _settings.notifiedAddress)) {  //TODO does not work. jid with presence? asmack problem?
                String xhtmlBody = message.generateXHTMLText().toString();
                xhtmlBody = xhtmlBody.replace("<br>", "<br/>");  //fix for smackx problem
                XHTMLManager.addBody(msg, xhtmlBody);
//            }

            _connection.sendPacket(msg);
        }
    }
    
    /**
     * Sets the xmpp presence status
     * @param status
     */
    public void setStatus(String status) {
        _presenceMessage = status;
        
        if (isConnected()) {
            Presence presence = new Presence(Presence.Type.available);
            presence.setStatus(_presenceMessage);
            presence.setPriority(24);                   
            _connection.sendPacket(presence);
        }
    }
    
    public boolean roomExists(String number, String name) {
    	return _xmppMuc.roomExists(number, name);
    }
    
    public ArrayList<XmppFriend> retrieveFriendList() {
        return _xmppBuddies.retrieveFriendList();
    }
    
    public void writeRoom(String number, String sender, String message) {
        _xmppMuc.writeRoom(number, sender, message);
    }
    
    public void inviteRoom(String number, String name) {
    	_xmppMuc.inviteRoom(number, name);
    }
    
    public void sendFile(String path) {
        _xmppFileMgr.sendFile(path);
    }
    

    public void configure(ProviderManager pm) {
        //  Private Data Storage
        pm.addIQProvider("query","jabber:iq:private", new PrivateDataManager.PrivateDataIQProvider());
 
        //  Time
        try {
            pm.addIQProvider("query","jabber:iq:time", Class.forName("org.jivesoftware.smackx.packet.Time"));
        } catch (ClassNotFoundException e) {
            Log.w(Tools.LOG_TAG, "Can't load class for org.jivesoftware.smackx.packet.Time");
        }
 
        //  XHTML
        pm.addExtensionProvider("html","http://jabber.org/protocol/xhtml-im", new XHTMLExtensionProvider());

        //  Roster Exchange
        pm.addExtensionProvider("x","jabber:x:roster", new RosterExchangeProvider());
        //  Message Events
        pm.addExtensionProvider("x","jabber:x:event", new MessageEventProvider());
        //  Chat State
        pm.addExtensionProvider("active","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("composing","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("paused","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("inactive","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("gone","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        
        //   FileTransfer
        pm.addIQProvider("si","http://jabber.org/protocol/si", new StreamInitiationProvider());
        pm.addIQProvider("query","http://jabber.org/protocol/bytestreams", new BytestreamsProvider());
        pm.addIQProvider("open","http://jabber.org/protocol/ibb", new IBBProviders.Open());
        pm.addIQProvider("close","http://jabber.org/protocol/ibb", new IBBProviders.Close());
        pm.addExtensionProvider("data","http://jabber.org/protocol/ibb", new IBBProviders.Data());
        
        //  Group Chat Invitations
        pm.addExtensionProvider("x","jabber:x:conference", new GroupChatInvitation.Provider());
        //  Service Discovery # Items    
        pm.addIQProvider("query","http://jabber.org/protocol/disco#items", new DiscoverItemsProvider());
        //  Service Discovery # Info
        pm.addIQProvider("query","http://jabber.org/protocol/disco#info", new DiscoverInfoProvider());
        //  Data Forms
        pm.addExtensionProvider("x","jabber:x:data", new DataFormProvider());
        //  MUC User
        pm.addExtensionProvider("x","http://jabber.org/protocol/muc#user", new MUCUserProvider());
        //  MUC Admin    
        pm.addIQProvider("query","http://jabber.org/protocol/muc#admin", new MUCAdminProvider());
        //  MUC Owner    
        pm.addIQProvider("query","http://jabber.org/protocol/muc#owner", new MUCOwnerProvider());
        //  Delayed Delivery
        pm.addExtensionProvider("x","jabber:x:delay", new DelayInformationProvider());
        //  Version
        try {
            pm.addIQProvider("query","jabber:iq:version", Class.forName("org.jivesoftware.smackx.packet.Version"));
        } catch (ClassNotFoundException e) {
            Log.w(Tools.LOG_TAG, "Can't load class for org.jivesoftware.smackx.packet.Version");
        }
        //  VCard
        pm.addIQProvider("vCard","vcard-temp", new VCardProvider());
        //  Offline Message Requests
        pm.addIQProvider("offline","http://jabber.org/protocol/offline", new OfflineMessageRequest.Provider());
        //  Offline Message Indicator
        pm.addExtensionProvider("offline","http://jabber.org/protocol/offline", new OfflineMessageInfo.Provider());
        //  Last Activity
        pm.addIQProvider("query","jabber:iq:last", new LastActivity.Provider());
        //  User Search
        pm.addIQProvider("query","jabber:iq:search", new UserSearch.Provider());
        //  SharedGroupsInfo
        pm.addIQProvider("sharedgroup","http://www.jivesoftware.org/protocol/sharedgroup", new SharedGroupsInfo.Provider());
        //  JEP-33: Extended Stanza Addressing
        pm.addExtensionProvider("addresses","http://jabber.org/protocol/address", new MultipleAddressesProvider());
    }
    
}
