package com.googlecode.gtalksms;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.XHTMLManager;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.packet.ChatStateExtension;
import org.jivesoftware.smackx.packet.DelayInformation;
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
import com.googlecode.gtalksms.xmpp.XmppMsg;

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
    
    // Our current retry attempt, plus a runnable and handler to implement retry
    private int _currentRetryCount = 0;
    Runnable _reconnectRunnable = new Runnable() {
        public void run() {
            if (_currentRetryCount > 0) {
                Log.v(Tools.LOG_TAG, "attempting reconnection");
                Tools.toastMessage(_context, _context.getString(R.string.xmpp_manager_reconnecting));
            }
            initConnection();
        }
    };

    Handler _reconnectHandler = new Handler();

    private SettingsManager _settings;
    private Context _context;
    
    public XmppManager(SettingsManager settings, Context context) {
        _settings = settings;
        _context = context;
        configure(ProviderManager.getInstance());
    }

    public void start() {
        start(CONNECTED);
    }
    
    public void start(int initialState) {
        _currentRetryCount = 0;
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
    
    public void stop() {
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
    
    private final ReentrantLock _lock = new ReentrantLock();
    

    /** init the XMPP connection */
    private void initConnection() {
       
        _lock.lock();
        try {
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
    
            XMPPConnection connection = new XMPPConnection(new ConnectionConfiguration(_settings.serverHost, _settings.serverPort, _settings.serviceName));
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
                roomNumbers.clear();
                rooms.clear();
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
        } finally {
            _lock.unlock();
        }
        onConnectionComplete();

    }

    private void onConnectionComplete() {
        Log.v(Tools.LOG_TAG, "connection established");
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
        Presence presence = new Presence(Presence.Type.available);
        presence.setStatus(_presenceMessage);
        presence.setPriority(24);                   
        _connection.sendPacket(presence);
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

    /** sends a message to the user */
    public void send(String message) {
        send(new XmppMsg(message));
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
//            if (XHTMLManager.isServiceEnabled(_connection, _settings.notifiedAddress)) {
                XHTMLManager.addBody(msg, message.generateXhtml());
//            }

            _connection.sendPacket(msg);
        }
    }
    
    public void setStatus(String status) {
        _presenceMessage = status;
        
        if (isConnected()) {
            Presence presence = new Presence(Presence.Type.available);
            presence.setStatus(_presenceMessage);
            presence.setPriority(24);                   
            _connection.sendPacket(presence);
        }
    }
    
    
    // Chat rooms source code
    Map<String, MultiUserChat> rooms = new HashMap<String, MultiUserChat>();
    
    public void writeRoom(String number, String sender, String message) {
        String room = sender + " (" + number + ")";
        try {
            MultiUserChat muc;
            if (!rooms.containsKey(room)) {
                muc = createRoom(number, room, sender);
                
                if (muc != null) {
                    rooms.put(room, muc);
                }
                
            } else {
                muc = rooms.get(room);
                
                // TODO: test if occupants content sender (in case we invite other people)
                if (muc != null && muc.getOccupantsCount() < 2) {
                    muc.invite(_settings.notifiedAddress, "SMS conversation with " + sender);
                }
            }
            
            if (muc != null) {
                muc.sendMessage(message);
            }
        } catch (Exception ex) {
            Log.e(Tools.LOG_TAG, "writeRoom: room = " + room, ex);
        }
    }
    
    Set<Integer> roomNumbers = new HashSet<Integer>();
    
    public MultiUserChat createRoom(String number, String room, String sender) {
        
        MultiUserChat multiUserChat = null;
        boolean passwordMode = false;
        Integer randomInt;
        
        do {
            randomInt = (new Random()).nextInt();
        } while (roomNumbers.contains(randomInt));
        roomNumbers.add(randomInt);
        
        // With "@conference.jabber.org" messages are sent several times... Jwchat seems to work fine and is the default
        String cnx = "GTalkSMS_" + randomInt + "_" + _settings.login.replaceAll("@", "_") 
            + "@" + _settings.mucServer; 
        try {
            // Create the room
            multiUserChat = new MultiUserChat(_connection, cnx);
            multiUserChat.create(room);
                
            try {
                // Since this is a private room, make the room not public and set user as owner of the room.
                Form submitForm = multiUserChat.getConfigurationForm().createAnswerForm();
                submitForm.setAnswer("muc#roomconfig_publicroom", false);
                submitForm.setAnswer("muc#roomconfig_roomname", room);

                    
                try {
                    List<String> owners = new ArrayList<String>();
                    owners.add(_settings.login);
                    owners.add(_settings.notifiedAddress);
                    submitForm.setAnswer("muc#roomconfig_roomowners", owners);
                    //submitForm.setAnswer("muc#roomconfig_roomadmins", owners);  //throws exception (at least on my server)
                }
                catch (Exception ex) {
                    Log.e(Tools.LOG_TAG, "Unable to configure room owners. Falling back to room passwords", ex);
                    submitForm.setAnswer("muc#roomconfig_passwordprotectedroom", true);
                    submitForm.setAnswer("muc#roomconfig_roomsecret", _settings.roomsPassword);
                    passwordMode = true;
                }
                    
                if (!passwordMode) {
                 	submitForm.setAnswer("muc#roomconfig_membersonly", true);
                }
                                    
                multiUserChat.sendConfigurationForm(submitForm);
            }
            catch (XMPPException e1) {
                Log.e(Tools.LOG_TAG, "Unable to send conference room configuration form.", e1);
                send(_context.getString(R.string.chat_sms_muc_conf_error, e1.getMessage()));
                return null; //then we also should not send an invite as the room will be locked
            }
               
            multiUserChat.invite(_settings.notifiedAddress, "SMS conversation with " + sender);

            ChatPacketListener chatListener = new ChatPacketListener(number, multiUserChat);
            multiUserChat.addMessageListener(chatListener);
        } catch (Exception ex) {
            Log.e(Tools.LOG_TAG, "createRoom() - Error creating room: " + room, ex);
            send(_context.getString(R.string.chat_sms_muc_error, ex.getMessage()));
            return null;
        }
        return multiUserChat;
    }
    
    class ChatPacketListener implements PacketListener {
        private String _number;
        private Date _lastDate;
        private MultiUserChat _muc;
        
        public ChatPacketListener(String number, MultiUserChat muc) {
            _number = number;
            _lastDate = new Date(0);
            _muc = muc;
        }
        
        @Override
        public void processPacket(Packet packet) {
            Message message = (Message) packet;
            String from = message.getFrom();
        
            Log.d(Tools.LOG_TAG, "Xmpp chat room packet received");
            
            if (!from.contains(_number)) {
                if (message.getBody() != null) {
                    DelayInformation inf = (DelayInformation)message.getExtension("x", "jabber:x:delay");
                    Date sentDate;
                    if (inf != null) {
                        sentDate = inf.getStamp();
                    } else {
                        sentDate = new Date();
                    }
                    
                    if (sentDate.compareTo(_lastDate) > 0 ) {
                        Intent intent = new Intent(ACTION_MESSAGE_RECEIVED);
                        if(_muc.getOccupantsCount() > 2) {
                        	intent.putExtra("message", "sms:" + _number + ":" + from + ": " + message.getBody());
                        } else {
                        	intent.putExtra("message", "sms:" + _number + ":" + message.getBody());
                        }
                        
                        _context.sendBroadcast(intent);
                        _lastDate = sentDate;
                    } else {
                        Log.w(Tools.LOG_TAG, "Receive old message: date=" + sentDate.toLocaleString() + " ; message=" + message.getBody());
                    }
                }
            }
        }
    }
    
    public void configure(ProviderManager pm) {
        
        //  Private Data Storage
        pm.addIQProvider("query","jabber:iq:private", new PrivateDataManager.PrivateDataIQProvider());
 
        //  Time
        try {
            pm.addIQProvider("query","jabber:iq:time", Class.forName("org.jivesoftware.smackx.packet.Time"));
        } catch (ClassNotFoundException e) {
            Log.w("TestClient", "Can't load class for org.jivesoftware.smackx.packet.Time");
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
            //  Not sure what's happening here.
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
        //   FileTransfer
        pm.addIQProvider("si","http://jabber.org/protocol/si", new StreamInitiationProvider());
        pm.addIQProvider("query","http://jabber.org/protocol/bytestreams", new BytestreamsProvider());
        pm.addIQProvider("open","http://jabber.org/protocol/ibb", new IBBProviders.Open());
        pm.addIQProvider("close","http://jabber.org/protocol/ibb", new IBBProviders.Close());
        pm.addExtensionProvider("data","http://jabber.org/protocol/ibb", new IBBProviders.Data());
    }
}
