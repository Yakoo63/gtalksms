package com.googlecode.gtalksms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.AndroidConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.StreamError;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.XHTMLManager;
import org.jivesoftware.smackx.bytestreams.ibb.provider.CloseIQProvider;
import org.jivesoftware.smackx.bytestreams.ibb.provider.DataPacketProvider;
import org.jivesoftware.smackx.bytestreams.ibb.provider.OpenIQProvider;
import org.jivesoftware.smackx.bytestreams.socks5.provider.BytestreamsProvider;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.packet.ChatStateExtension;
import org.jivesoftware.smackx.packet.LastActivity;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.packet.SharedGroupsInfo;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.jivesoftware.smackx.provider.DelayInformationProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.MUCAdminProvider;
import org.jivesoftware.smackx.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.provider.MUCUserProvider;
import org.jivesoftware.smackx.provider.MessageEventProvider;
import org.jivesoftware.smackx.provider.MultipleAddressesProvider;
import org.jivesoftware.smackx.provider.RosterExchangeProvider;
import org.jivesoftware.smackx.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.provider.XHTMLExtensionProvider;
import org.jivesoftware.smackx.search.UserSearch;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;

import com.googlecode.gtalksms.tools.GoogleAnalyticsHelper;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.ChatPacketListener;
import com.googlecode.gtalksms.xmpp.ClientOfflineMessages;
import com.googlecode.gtalksms.xmpp.PresencePacketListener;
import com.googlecode.gtalksms.xmpp.XmppBuddies;
import com.googlecode.gtalksms.xmpp.XmppConnectionChangeListener;
import com.googlecode.gtalksms.xmpp.XmppFileManager;
import com.googlecode.gtalksms.xmpp.XmppLocalS5BProxyManager;
import com.googlecode.gtalksms.xmpp.XmppMsg;
import com.googlecode.gtalksms.xmpp.XmppMuc;
import com.googlecode.gtalksms.xmpp.XmppOfflineMessages;
import com.googlecode.gtalksms.xmpp.XmppPresenceStatus;
import com.googlecode.gtalksms.xmpp.XmppStatus;

public class XmppManager {
    
    private static final boolean DEBUG = false;
    
    // my first measuring showed that the disconnect in fact does not hang
    // but takes sometimes a lot of time
    // disconnectED xmpp connection. Took: 1048.576 s
    public static final int DISCON_TIMEOUT = 1000 * 10; // 10s
    // The timeout for XMPP connections that get created with
    // DNS SRV information
    public static final int DNSSRV_TIMEOUT = 1000 * 30; // 30s
    
    public static final int DISCONNECTED = 1;
    // A "transient" state - will only be CONNECTING *during* a call to start()
    public static final int CONNECTING = 2;
    public static final int CONNECTED = 3;
    // A "transient" state - will only be DISCONNECTING *during* a call to stop()
    public static final int DISCONNECTING = 4;
    // This state means we are waiting for a retry attempt etc.
    // mostly because a connection went down
    public static final int WAITING_TO_CONNECT = 5;
    // We are waiting for a valid data connection
    public static final int WAITING_FOR_NETWORK = 6;
    
    private static XmppManager sXmppManager = null;
    private static int sReusedConnectionCount = 0;
    private static int sNewConnectionCount = 0;
    
    // Indicates the current state of the service (disconnected/connecting/connected)
    private int mStatus = DISCONNECTED;
    
    private List<XmppConnectionChangeListener> mConnectionChangeListeners;
    private XMPPConnection mConnection = null;
    private PacketListener mPacketListener = null;
    
    private PacketListener mPresencePacketListener = null;
    
    private ConnectionListener mConnectionListener = null;    
    private XmppMuc mXmppMuc;
    private XmppBuddies mXmppBuddies;
    private XmppFileManager mXmppFileMgr;
    private ClientOfflineMessages mClientOfflineMessages;
    private XmppStatus mXmppStatus;
    private XmppPresenceStatus mXmppPresenceStatus;    
        
    // Our current retry attempt, plus a runnable and handler to implement retry
    private int mCurrentRetryCount = 0;
    private Runnable mReconnectRunnable = new Runnable() {
        public void run() {
            Log.i("attempting reconnection by issuing intent " + MainService.ACTION_CONNECT);
            Tools.startSvcIntent(mContext, MainService.ACTION_CONNECT);
        }
    };

    private Handler mReconnectHandler = new Handler();

    private SettingsManager mSettings;
    private Context mContext;
    
    /**
     * Constructor for an XmppManager instance, connection is optional. It 
     * servers only a purpose when creating the instance with an already
     * connected connection, e.g. when registering new accounts with an server
     * and using this connection.
     * 
     * @param context - required
     * @param connection - optional
     */
    private XmppManager(Context context, XMPPConnection connection) {
        mConnectionChangeListeners = new ArrayList<XmppConnectionChangeListener>();
        mSettings = SettingsManager.getSettingsManager(context);
        Log.initialize(mSettings);
        mContext = context;
        configure(ProviderManager.getInstance());
        mXmppBuddies = XmppBuddies.getInstance(context);
        mXmppFileMgr = XmppFileManager.getInstance(context);
        mXmppMuc = XmppMuc.getInstance(context);
        mClientOfflineMessages = ClientOfflineMessages.getInstance(context);
        mXmppStatus = XmppStatus.getInstance(context);
        mXmppPresenceStatus = XmppPresenceStatus.getInstance(context);
        mXmppBuddies.registerListener(this);
        mXmppFileMgr.registerListener(this);
        mXmppMuc.registerListener(this);
        mClientOfflineMessages.registerListener(this);
        mXmppPresenceStatus.registerListener(this);
        XmppLocalS5BProxyManager.getInstance(context).registerListener(this);
        sReusedConnectionCount = 0;
        sNewConnectionCount = 0;
        ServiceDiscoveryManager.setIdentityName(Tools.APP_NAME);
        ServiceDiscoveryManager.setIdentityType("bot"); // http://xmpp.org/registrar/disco-categories.html
        if (DEBUG) {
            Connection.DEBUG_ENABLED = true;
        }
        SmackConfiguration.setKeepAliveInterval(1000 * 60 * 12);  // 12 min
        SmackConfiguration.setPacketReplyTimeout(1000 * 20);      // 20 sec
        SmackConfiguration.setLocalSocks5ProxyEnabled(true);
        Roster.setDefaultSubscriptionMode(Roster.SubscriptionMode.manual);
        // connection can be null, it is created on demand
        mConnection = connection;
    }
    
    /**
     * This getter creates the XmppManager and inits the XmppManager
     * with a new connection with the current preferences.
     * 
     * @param ctx
     * @return
     */
    public static XmppManager getInstance(Context ctx) {
        if (sXmppManager == null) {
            sXmppManager = new XmppManager(ctx, null);            
        }
        return sXmppManager;
    }
    
    /**
     * This getter is solely for the purpose that the setup wizard is able to
     * inform the XmppManager that a new connection has been created
     * and that the XmppManager should use this connection from now on
     * 
     * @param ctx
     * @param connection
     * @return
     */
    public static XmppManager getInstance(Context ctx, XMPPConnection connection) {
        if (sXmppManager == null) {
            sXmppManager = new XmppManager(ctx, connection);
        } else {
            // remove all possible references to the old connection
            // note that this will note change sStatus
            sXmppManager.cleanupConnection();
            // init XmppManager with the new connection
            // setting up all packetListeners etc.
            sXmppManager.onConnectionEstablished(connection);
        }
        return sXmppManager;
    }
    
    private void start(int initialState) {
        switch (initialState) {
            case CONNECTED:
                initConnection();
                break;
            case WAITING_TO_CONNECT:
            case WAITING_FOR_NETWORK:
                updateStatus(initialState);
                break;
            default:
                throw new IllegalStateException("xmppMgr start() Invalid State: " + initialState);
        }
    }
    
    /**
     * calls cleanupConnection and 
     * sets _status to DISCONNECTED
     */
    private void stop() {
        updateStatus(DISCONNECTING);
        cleanupConnection();
        updateStatus(DISCONNECTED);
    }
    
    /**
     * Removes all references to the old connection.
     * 
     * Spawns a new disconnect runnable if the connection
     * is still connected and removes packetListeners and 
     * Callbacks for the reconnectHandler
     */
    private void cleanupConnection() {
        mReconnectHandler.removeCallbacks(mReconnectRunnable);

        if (mConnection != null) {
            if (mConnection.isConnected()) {
                xmppDisconnect(mConnection);
            }
            // xmppDisconnect may has set _connection = null, so we have to double check
            if (mConnection != null) {
                if (mPacketListener != null) {
                    mConnection.removePacketListener(mPacketListener);
                }
                if (mConnectionListener != null) {
                    mConnection.removeConnectionListener(mConnectionListener);
                }
                if (mPresencePacketListener != null) {
                    mConnection.removePacketListener(mPresencePacketListener);
                }
            }
        }
        mPacketListener = null; 
        mConnectionListener = null;
        mPresencePacketListener = null;
    }
    
    /** 
     * This method *requests* a state change - what state things actually
     * wind up in is impossible to know (eg, a request to connect may wind up
     * with a state of CONNECTED, DISCONNECTED or WAITING_TO_CONNECT...
     */
    protected void xmppRequestStateChange(int newState) {
        int currentState = getConnectionStatus();
        switch (newState) {
        case XmppManager.CONNECTED:
            switch (currentState) {
            case XmppManager.CONNECTED:
                break;
            case XmppManager.CONNECTING:    
            case XmppManager.DISCONNECTED:
            case XmppManager.WAITING_TO_CONNECT:
            case XmppManager.WAITING_FOR_NETWORK:
                cleanupConnection();
                start(XmppManager.CONNECTED);
                break;
            default:
                throw new IllegalStateException("xmppRequestStateChange() unexpected current state when moving to connected: " + currentState);
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
            case XmppManager.WAITING_FOR_NETWORK:
                cleanupConnection();
                start(XmppManager.CONNECTED);
                break;
            default:
                throw new IllegalStateException("xmppRequestStateChange() xmppRequestStateChangeunexpected current state when moving to waiting: " + currentState);
            }
            break;
        case XmppManager.WAITING_FOR_NETWORK:
            switch (currentState) {
            case XmppManager.CONNECTED:
                stop();
                start(XmppManager.WAITING_FOR_NETWORK);
                break;
            case XmppManager.DISCONNECTED:
                start(XmppManager.WAITING_FOR_NETWORK);
                break;
            case XmppManager.WAITING_TO_CONNECT:
                cleanupConnection();
            	break;
            case XmppManager.WAITING_FOR_NETWORK:
                break;
            default:
                throw new IllegalStateException("xmppRequestStateChange() xmppRequestStateChangeunexpected current state when moving to waiting: " + currentState);
            }
            break;
        default:
            throw new IllegalStateException("xmppRequestStateChange() invalid state to switch to: " + statusAsString(newState));
        }
        // Now we have requested a new state, our state receiver will see when
        // the state actually changes and update everything accordingly.
    }

    private void xmppDisconnect(XMPPConnection connection) {
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
            private XMPPConnection con;

            public DisconnectRunnable(XMPPConnection con) {
                this.con = con;
            }
            
            public void run() {
                if (con.isConnected()) {
                    Log.i("disconnectING xmpp connection");
                    float start = System.currentTimeMillis();
                    try {
                        con.disconnect();
                    } catch (Exception e2) {
                        // Even if we double check that the connection is still connected
                        // sometimes the connection timeout occurs when the disconnect method
                        // is running, so we just log that here
                        Log.i("xmpp disconnect failed: " + e2);
                    }
                    float stop = System.currentTimeMillis();
                    float diff = stop - start;
                    diff = diff / 1000;
                    Log.i("disconnectED xmpp connection. Took: " + diff + " s");
                    GoogleAnalyticsHelper.trackDisconTime(diff);
                }
            }
        }
        
        Thread t = new Thread(new DisconnectRunnable(connection), "xmpp-disconnector");
        // we don't want this thread to hold up process shutdown so mark as daemon.
        t.setDaemon(true);
        t.start();
        
        try {
            t.join(DISCON_TIMEOUT);
        } catch (InterruptedException e) {}
        // the thread is still alive, this means that the disconnect is still running
        // we don't have the time, so prepare for a new connection
        if (t.isAlive()) {
            Log.i(t.getName() + " was still alive: connection will be set to null");
            mConnection = null;
        }
    }

    /**
     * Updates the status about the service state (and the statusbar)
     * by sending an ACTION_XMPP_CONNECTION_CHANGED intent with the new
     * and old state.
     * needs to be static, because its called by MainService even when
     * xmppMgr is not created yet
     * 
     * @param ctx
     * @param old_state
     * @param new_state
     */
    public void broadcastStatus(Context ctx, int old_state, int new_state) {  
        Intent intent = new Intent(MainService.ACTION_XMPP_CONNECTION_CHANGED);                      
        intent.putExtra("old_state", old_state);
        intent.putExtra("new_state", new_state);
        if (new_state == CONNECTED && mConnection != null) {
            intent.putExtra("TLS", mConnection.isUsingTLS());
            intent.putExtra("Compression", mConnection.isUsingCompression());
        }
        ctx.sendBroadcast(intent);
    }
    
    /**
     * updates the connection status
     * and calls broadCastStatus()
     * 
     * @param status
     */
    private void updateStatus(int status) {
        if (status != mStatus) {
            // ensure _status is set before broadcast, just in-case
            // a receiver happens to wind up querying the state on
            // delivery.
            int old = mStatus;
            mStatus = status;     
            mXmppStatus.setState(status);
            Log.i("broadcasting state transition from " + statusAsString(old) + " to " + statusAsString(status) + " via Intent " + MainService.ACTION_XMPP_CONNECTION_CHANGED);
            broadcastStatus(mContext, old, status);
        }
    }

    private void maybeStartReconnect() {
            int timeout;
            updateStatus(WAITING_TO_CONNECT);
            cleanupConnection();
            mCurrentRetryCount += 1;
            if (mCurrentRetryCount < 20) {
                // a simple linear-backoff strategy.
                timeout = 5000 * mCurrentRetryCount;
            } else {
                // every 5 min
                timeout = 1000 * 60 * 5;
            }
            Log.i("maybeStartReconnect scheduling retry in " + timeout + "ms. Retry #" + mCurrentRetryCount);
            mReconnectHandler.postDelayed(mReconnectRunnable, timeout);
    }
    

    /**
     * Initializes the XMPP connection
     * 
     * 1. Creates a new XMPPConnection object if necessary
     * 2. Connects the XMPPConnection
     * 3. Authenticates the user with the server
     * 
     * Calls maybeStartReconnect() if something went wrong
     * 
     */
    private void initConnection() {
        XMPPConnection connection;

        // assert we are only ever called from one thread
        String currentThreadName = Thread.currentThread().getName();
        assert (!currentThreadName.equals(MainService.SERVICE_THREAD_NAME));
        
        NetworkInfo active = ((ConnectivityManager)mContext.getSystemService(Service.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (active == null || !active.isAvailable()) {
            Log.e("initConnection: connection request, but no network available");
            // we don't destroy the service here - our network receiver will notify us when
            // the network comes up and we try again then.
            updateStatus(WAITING_FOR_NETWORK);
            return;
        }
        
        // everything is ready for a connection attemp
        updateStatus(CONNECTING);

        // create a new connection if the connection is obsolete or if the
        // old connection is still active
        if (SettingsManager.connectionSettingsObsolete 
                || mConnection == null 
                || mConnection.isConnected() ) {
            
            try {
                connection = createNewConnection(mSettings);
            } catch (XMPPException e) {
                // connection failure
                Log.e("Exception creating new XMPP Connection", e);
                maybeStartReconnect();
                return;
            }
            SettingsManager.connectionSettingsObsolete = false;
            if (!connectAndAuth(connection)) {
                // connection failure
                return;
            }                  
            sNewConnectionCount++;
        } else {
            // reuse the old connection settings
            connection = mConnection;
            // we reuse the xmpp connection so only connect() is needed
            if (!connectAndAuth(connection)) {
                // connection failure
                return;
            }
            sReusedConnectionCount++;
        }
        // this code is only executed if we have an connection established
        onConnectionEstablished(connection);
    }
    
    private void onConnectionEstablished(XMPPConnection connection) {
        mConnection = connection;               
        mConnectionListener = new ConnectionListener() {
            @Override
            public void connectionClosed() {
                // connection was closed by the foreign host
                // or we have closed the connection
                Log.i("ConnectionListener: connectionClosed() called - connection was shutdown by foreign host or by us");
            }

            @Override
            public void connectionClosedOnError(Exception e) {
                // this happens mainly because of on IOException
                // eg. connection timeouts because of lost connectivity
                Log.w("xmpp disconnected due to error: ", e);
                if (e.getMessage().startsWith("Attr.value missing")) {
                    Log.w((android.util.Log.getStackTraceString(e)));
                }
                // We update the state to disconnected (mainly to cleanup listeners etc)
                // then schedule an automatic reconnect.
                maybeStartReconnect();
            }

            @Override
            public void reconnectingIn(int arg0) {
                throw new IllegalStateException("Reconnection Manager is running");
            }

            @Override
            public void reconnectionFailed(Exception arg0) {
                throw new IllegalStateException("Reconnection Manager is running");
            }

            @Override
            public void reconnectionSuccessful() {
                throw new IllegalStateException("Reconnection Manager is running");
            }
        };
        mConnection.addConnectionListener(mConnectionListener);            

        try {
            informListeners(mConnection);

            PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
            mPacketListener = new ChatPacketListener(mConnection, mContext);
            mConnection.addPacketListener(mPacketListener, filter);

            filter = new PacketTypeFilter(Presence.class);
            mPresencePacketListener = new PresencePacketListener(mConnection, mSettings);
            mConnection.addPacketListener(mPresencePacketListener, filter);

            try {
                mConnection.getRoster().addRosterListener(mXmppBuddies);
                mConnection.getRoster().setSubscriptionMode(Roster.SubscriptionMode.manual);
                mXmppBuddies.retrieveFriendList();
            } catch (Exception ex) {
                GoogleAnalyticsHelper.trackAndLogError("Failed to setup XMPP friend list roster.", ex);
            }

            // It is important that we query the server for offline messages
            // BEFORE we send the first presence stanza
            XmppOfflineMessages.handleOfflineMessages(mConnection, mSettings.notifiedAddress, mContext);
        } catch (Exception e) {
            // see issue 126 for an example where this happens because
            // the connection drops while we are in initConnection()
            GoogleAnalyticsHelper.trackAndLogError("xmppMgr exception caught", e);
            maybeStartReconnect();
            return;
        } 
        
        Log.i("connection established with parameters: con=" + mConnection.isConnected() + 
                " auth=" + mConnection.isAuthenticated() + 
                " enc=" + mConnection.isUsingTLS() + 
                " comp=" + mConnection.isUsingCompression());
        
        // Send welcome message
        if (mSettings.notifyApplicationConnection) {
            Tools.send((mContext.getString(R.string.chat_welcome, Tools.getVersionName(mContext))), null, mContext);
        }
        
        mCurrentRetryCount = 0;        
        updateStatus(CONNECTED);
    }
    
    private void informListeners(XMPPConnection connection) {
        for (XmppConnectionChangeListener listener : mConnectionChangeListeners) {
            listener.newConnection(connection);
        }
    }
    
    /**
     * Tries to fully establish the given XMPPConnection
     * Calls maybeStartReconnect() or stop() in an error case
     * 
     * @param connection
     * @return true if we are connected and authenticated, false otherwise
     */
    private boolean connectAndAuth(XMPPConnection connection) {
        try {
            connection.connect();
        } catch (Exception e) {
            Log.w("xmpp connection failed: " + e.getMessage());
            // "No response from server" usually means that the connection is somehow in an undefined state
            // so we throw away the XMPPConnection by null-ing it
            // see also issue 133 - http://code.google.com/p/gtalksms/issues/detail?id=133
            if (e.getMessage() != null && e.getMessage().startsWith("Connection failed. No response from server")) {
                Log.w("xmpp connection in an unusable state, marking it as obsolete", e);
                mConnection = null;
            }
            if (e instanceof XMPPException) {
                XMPPException xmppEx = (XMPPException) e;
                StreamError error = xmppEx.getStreamError();
                // Make sure the error is not null
                if (error != null) {
                    Log.w(error.toString());
                }
            }
            maybeStartReconnect();
            return false;
        }          
        
        // we reuse the connection and the auth was done with the connect()
        if (connection.isAuthenticated()) {
            return true;
        }
        
        ServiceDiscoveryManager serviceDiscoMgr = ServiceDiscoveryManager.getInstanceFor(connection);
        XHTMLManager.setServiceEnabled(connection, false);   
        serviceDiscoMgr.addFeature("http://jabber.org/protocol/disco#info");
        serviceDiscoMgr.addFeature("http://jabber.org/protocol/muc");
        serviceDiscoMgr.addFeature("bug-fix-gtalksms");
        
        try {
            connection.login(mSettings.login, mSettings.password, Tools.APP_NAME);
        } catch (Exception e) {
            xmppDisconnect(connection);
            Log.e("xmpp login failed: " + e.getMessage());
            // sadly, smack throws the same generic XMPPException for network
            // related messages (eg "no response from the server") as for
            // authoritative login errors (ie, bad password).  The only
            // differentiator is the message itself which starts with this
            // hard-coded string.
            if (e.getMessage().indexOf("SASL authentication") == -1) {
                // doesn't look like a bad username/password, so retry
                maybeStartReconnect();
            } else {
                MainService.displayToast(R.string.xmpp_manager_invalid_credentials, null);
                stop();
            }
            return false;
        }
        return true;
    }    
    
    /**
     * Parses the current preferences and returns an new unconnected
     * XMPPConnection 
     * @return
     * @throws XMPPException 
     */
    private static XMPPConnection createNewConnection(SettingsManager settings) throws XMPPException {
        ConnectionConfiguration conf;
        if (settings.manuallySpecifyServerSettings) {
            // trim the serverHost here because of Issue 122
            conf = new ConnectionConfiguration(settings.serverHost.trim(), settings.serverPort, settings.serviceName);
        } else {
            // DNS SRV lookup, yeah! :)
            // Note: The Emulator will throw here an BadAddressFamily Exception
            // but on a real device it just works fine
            // see: http://stackoverflow.com/questions/2879455/android-2-2-and-bad-address-family-on-socket-connect
            // and http://code.google.com/p/android/issues/detail?id=9431            
            conf = new AndroidConnectionConfiguration(settings.serviceName, DNSSRV_TIMEOUT);
        }
        
        conf.setTruststorePath("/system/etc/security/cacerts.bks");
        conf.setTruststorePassword("changeit");
        conf.setTruststoreType("bks");
        switch (settings.xmppSecurityModeInt) {
        case SettingsManager.XMPPSecurityOptional:
            conf.setSecurityMode(ConnectionConfiguration.SecurityMode.enabled);
            break;
        case SettingsManager.XMPPSecurityRequired:
            conf.setSecurityMode(ConnectionConfiguration.SecurityMode.required);
            break;
        case SettingsManager.XMPPSecurityDisabled:
        default:
            conf.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
            break;
        }
        if (settings.useCompression) {
            conf.setCompressionEnabled(true);
        }
        
        // disable the built-in ReconnectionManager
        // since we handle this
        conf.setReconnectionAllowed(false);
        conf.setSendPresence(false);
        
        return new XMPPConnection(conf);     
    }

    /** returns the current connection state */
    public int getConnectionStatus() {
        return mStatus;
    }
    
    public boolean getTLSStatus() {
        return mConnection == null ? false : mConnection.isUsingTLS();
    }
    
    public boolean getCompressionStatus() {
    	return mConnection == null ? false : mConnection.isUsingCompression();
    }
    
    /**
     * Sends a XMPP Message, but only if we are connected
     * This method is thread safe.
     * 
     * @param message
     * @param to - the receiving JID - if null the default notification address will be used
     * @return true, if we were connected and the message was handled over to the connection - otherwise false
     */
    public boolean send(XmppMsg message, String to) {
        if (to == null) {
            Log.i("Sending message \"" + message.toShortString() + "\"");
        } else {
            Log.i("Sending message \"" + message.toShortString() + "\" to " + to);
        }
        Message msg;
        MultiUserChat muc = null;

        // to is null, so send to the default, which is the notifiedAddress
        if (to == null) {
            msg = new Message();
        } else {
            msg = new Message(to);
            // check if "to" is an known MUC JID
            muc = mXmppMuc.getRoomViaRoomName(to);
        }

        if (mSettings.formatResponses) {
            msg.setBody(message.generateFmtTxt());
        } else {
            msg.setBody(message.generateTxt());
        }

        // add an XTHML Body either when
        // - we don't know the recipient
        // - we know that the recipient is able to read XHTML-IM
        // - we are disconnected and therefore send the message later
        if ((to == null) || 
                (mConnection != null && (XHTMLManager.isServiceEnabled(mConnection, to) || !mConnection.isConnected()))) {
            String xhtmlBody = message.generateXHTMLText().toString();
            XHTMLManager.addBody(msg, xhtmlBody);
        }

        // determine the type of the message, groupchat or chat
        if (muc == null) {
            msg.setType(Message.Type.chat);
        } else {
            msg.setType(Message.Type.groupchat);
        }
        

        if (isConnected()) {
            // Message has no destination information
            // Send to all known resources 
            if (muc == null && to == null) {
                Iterator<Presence> presences = mConnection.getRoster().getPresences(mSettings.notifiedAddress);
                while (presences.hasNext()) {
                    Presence p = presences.next();
                    String toPresence = p.getFrom();
                    String toResource = StringUtils.parseResource(toPresence);
                    if (toResource != null && !toResource.equals("") && (false || !toResource.startsWith("android"))) {
                        msg.setTo(toPresence);
                        mConnection.sendPacket(msg);
                    }
                }
            // Message has a known destination information
            // And we have set the to-address before
            } else if (muc == null) {
                mConnection.sendPacket(msg);
            // Message is for a known MUC
            } else {
                try {
                    muc.sendMessage(msg);
                } catch (XMPPException e) {
                    return false;
                }
            }
            return true;
        } else {
            Log.d("Adding message: \"" + message.toShortString() + "\" to offline queue, because we are not connected. Status=" + statusString());
            return mClientOfflineMessages.addOfflineMessage(msg);
        }
    }
    
    public boolean isConnected() {
        boolean res = (mConnection != null 
                && mConnection.isConnected() 
                && mStatus == CONNECTED);
        return res;
    }
    
    public void registerConnectionChangeListener(XmppConnectionChangeListener listener) {
        mConnectionChangeListeners.add(listener);
    }
    
    public static int getNewConnectionCount() {
        return sNewConnectionCount;
    }
    
    public static int getReusedConnectionCount() {
        return sReusedConnectionCount;
    }    

    private static void configure(ProviderManager pm) {
        //  Private Data Storage
        pm.addIQProvider("query","jabber:iq:private", new PrivateDataManager.PrivateDataIQProvider());
 
        //  Time
        try {
            pm.addIQProvider("query","jabber:iq:time", Class.forName("org.jivesoftware.smackx.packet.Time"));
        } catch (ClassNotFoundException e) {
            Log.w("Can't load class for org.jivesoftware.smackx.packet.Time");
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
        pm.addIQProvider("open","http://jabber.org/protocol/ibb", new OpenIQProvider());
        pm.addIQProvider("close","http://jabber.org/protocol/ibb", new CloseIQProvider());
        pm.addExtensionProvider("data","http://jabber.org/protocol/ibb", new DataPacketProvider());
        
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
            Log.w("Can't load class for org.jivesoftware.smackx.packet.Version");
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
    
    public static String statusAsString(int state) {
        String res;
        switch(state) {
        case 1:
            res = "Disconnected";
            break;
        case 2:
            res = "Connecting";
            break;
        case 3:
            res = "Connected";
            break;
        case 4:
            res = "Disconnecting";
            break;
        case 5:
            res = "Waiting to connect";
            break;
        case 6:
            res = "Waiting for network";
            break;
        default:
            throw new IllegalStateException();
        }
        return res;                        
    }
    
    public String statusString() {
        return statusAsString(mStatus);
    }
}
