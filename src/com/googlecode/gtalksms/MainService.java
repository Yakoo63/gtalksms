package com.googlecode.gtalksms;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.googlecode.gtalksms.cmd.BatteryCmd;
import com.googlecode.gtalksms.cmd.CallCmd;
import com.googlecode.gtalksms.cmd.ClipboardCmd;
import com.googlecode.gtalksms.cmd.Command;
import com.googlecode.gtalksms.cmd.ContactCmd;
import com.googlecode.gtalksms.cmd.FileCmd;
import com.googlecode.gtalksms.cmd.GeoCmd;
import com.googlecode.gtalksms.cmd.HelpCmd;
import com.googlecode.gtalksms.cmd.KeyboardCmd;
import com.googlecode.gtalksms.cmd.RingCmd;
import com.googlecode.gtalksms.cmd.ShellCmd;
import com.googlecode.gtalksms.cmd.SmsCmd;
import com.googlecode.gtalksms.cmd.UrlsCmd;
import com.googlecode.gtalksms.data.contacts.ContactsManager;
import com.googlecode.gtalksms.panels.MainScreen;
import com.googlecode.gtalksms.panels.Preferences;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.tools.GoogleAnalyticsHelper;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class MainService extends Service {

    // The following actions are documented and registered in our manifest
    public final static String ACTION_CONNECT = "com.googlecode.gtalksms.action.CONNECT";
    public final static String ACTION_TOGGLE = "com.googlecode.gtalksms.action.TOGGLE";
    public final static String ACTION_SEND = "com.googlecode.gtalksms.action.SEND";
    // The following actions are undocumented and internal to our implementation.
    public final static String ACTION_BROADCAST_STATUS = "com.googlecode.gtalksms.action.BROADCAST_STATUS";
    public final static String ACTION_SMS_RECEIVED = "com.googlecode.gtalksms.action.SMS_RECEIVED";
    public final static String ACTION_NETWORK_CHANGED = "com.googlecode.gtalksms.action.NETWORK_CHANGED";
    public final static String ACTION_HANDLE_XMPP_NOTIFY = "com.googlecode.gtalksms.action.HANDLE_XMPP_NOTIFY";
    public final static String ACTION_SMS_SENT = "com.googlecode.gtalksms.action.SMS_SENT";
    public final static String ACTION_SMS_DELIVERED = "com.googlecode.gtalksms.action.SMS_DELIVERED";

    // A bit of a hack to allow global receivers to know whether or not
    // the service is running, and therefore whether to tell the service
    // about some events
    public static boolean IsRunning = false;

    private SettingsManager _settingsMgr;
    private XmppManager _xmppMgr;
    private BroadcastReceiver _xmppreceiver;
    private KeyboardInputMethod _keyboard;
    
    private Map<String, Command> _commands = new HashMap<String, Command>();
    private Set<Command> _commandSet = new HashSet<Command>();
    
    
    // notification stuff
    @SuppressWarnings("unchecked")
    private static final Class[] _startForegroundSignature = new Class[] { int.class, Notification.class };
    @SuppressWarnings("unchecked")
    private static final Class[] _stopForegroundSignature = new Class[] { boolean.class };
    private NotificationManager _notificationMgr;
    private Method _startForeground;
    private Method _stopForeground;
    private Object[] _startForegroundArgs = new Object[2];
    private Object[] _stopForegroundArgs = new Object[1];
    private PendingIntent _contentIntent = null;

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder _binder = new LocalBinder();

    private long _handlerThreadId;
    
    // to get the helper use MainService.getAnalyticsHelper()
    private static GoogleAnalyticsHelper _gAnalytics;
    

    // some stuff for the async service implementation - borrowed heavily from
    // the standard IntentService, but that class doesn't offer fine enough
    // control for "foreground" services.
    private volatile Looper _serviceLooper;
    private volatile ServiceHandler _serviceHandler;
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            onHandleIntent((Intent)msg.obj, msg.arg1);
        }
    }

    // The IntentService(-like) implementation manages taking the
    // intents passed to startService and delivering them to
    // this function which runs in its own thread (so can block 
    // Pretty-much everything using the _xmppMgr is here...
    protected void onHandleIntent(final Intent intent, int id) {
        if (intent == null) {
            Log.e(Tools.LOG_TAG, "onHandleIntent: Intent null");
            return;
        }
        
        // Set Disconnected state by force to manage pending tasks
        if (intent.getBooleanExtra("force", false) && intent.getBooleanExtra("disconnect", false)) {
            // request to disconnect.
            xmppRequestStateChange(XmppManager.DISCONNECTED);
        }

        if (Thread.currentThread().getId() != _handlerThreadId)
            throw new IllegalThreadStateException();
        // We need to handle xmpp state changes which happened "externally" - eg,
        // due to a connection error, or running out of retries, or a retry
        // handler actually succeeding etc.
        int initialState = getConnectionStatus(); 
        updateListenersToCurrentState(initialState);
        
        String a = intent.getAction();
        Log.d(Tools.LOG_TAG, "handling action '" + a + "' while in state " + initialState);
        if (a.equals(ACTION_CONNECT)) {
            if (intent.getBooleanExtra("disconnect", false)) {
                // request to disconnect.
                xmppRequestStateChange(XmppManager.DISCONNECTED);
            } else {
                // a simple 'connect' request.
                xmppRequestStateChange(XmppManager.CONNECTED);
            }
        } else if (a.equals(ACTION_TOGGLE)) {
            switch (initialState) {
            case XmppManager.CONNECTED:
            case XmppManager.WAITING_TO_CONNECT:
                xmppRequestStateChange(XmppManager.DISCONNECTED);
                break;
            case XmppManager.DISCONNECTED:
                xmppRequestStateChange(XmppManager.CONNECTED);
                break;
            default:
                Log.e(Tools.LOG_TAG, "Invalid xmpp state: "+ initialState);
                break;
            }
        } else if (a.equals(ACTION_SEND)) {
            if (initialState == XmppManager.CONNECTED) {
                _xmppMgr.send(intent.getStringExtra("message"));
            }
        } else if (a.equals(ACTION_HANDLE_XMPP_NOTIFY)) {
            // If there is a message, then it is what we received.
            // If there is no message, it just means the xmpp connection state
            // changed - and we already handled that earlier in this method.
            String message = intent.getStringExtra("message");
            if (message != null) {
                onMessageReceived(intent.getStringExtra("message"));
            }
        } else if (a.equals(ACTION_SMS_RECEIVED)) {
            if (initialState == XmppManager.CONNECTED) {
                String number = intent.getStringExtra("sender");
                String name = ContactsManager.getContactName(this, number);
                
                if (_settingsMgr.notifySmsInSameConversation) {
                    XmppMsg msg = new XmppMsg();
                    msg.appendBold(getString(R.string.chat_sms_from, ContactsManager.getContactName(this, number)));
                    msg.append(intent.getStringExtra("message"));
                    _xmppMgr.send(msg);
                }
                if (_settingsMgr.notifySmsInChatRooms || _xmppMgr.roomExists(number, name)) {
                    _xmppMgr.writeRoom(number, name, intent.getStringExtra("message"));
                }
                
                if (_commands.containsKey("sms")) {
                    ((SmsCmd)_commands.get("sms")).setLastRecipient(number);
                }
            }
        } else if (a.equals(ACTION_NETWORK_CHANGED)) {
            boolean available = intent.getBooleanExtra("available", true);
            Log.d(Tools.LOG_TAG, "network_changed with available=" + available + " and with state=" + initialState);
            if(available) {
            	GoogleAnalyticsHelper.dispatch();
            }
            // TODO wait few seconds if network not available ? to avoid multiple reconnections
            if (available && initialState == XmppManager.WAITING_TO_CONNECT) {
                // We are in a waiting state and have a network - try to connect.
                xmppRequestStateChange(XmppManager.CONNECTED);
            } else if (!available && initialState == XmppManager.CONNECTED) {
                // We are connected but the network has gone down - disconnect and go
                // into WAITING state so we auto-connect when we get a future 
                // notification that a network is available.
                xmppRequestStateChange(XmppManager.WAITING_TO_CONNECT);
            }
        } else {
            Log.w(Tools.LOG_TAG, "Unexpected intent: " + a);
        }
        Log.d(Tools.LOG_TAG, "handled action '" + a + "' - state now " + getConnectionStatus());
        // stop the service if we are disconnected (but stopping the service
        // doesn't mean the process is terminated - onStart can still happen.)
        if (getConnectionStatus() == XmppManager.DISCONNECTED) {
            if (stopSelfResult(id) == true) {
                Log.d(Tools.LOG_TAG, "service is stopping (we are disconnected and no pending intents exist.)");
            } else {
                Log.d(Tools.LOG_TAG, "more pending intents to be delivered - service will not stop");
            }
        }
    }

    /** Updates the status about the service state (and the status bar) */
    public void onConnectionStatusChanged(int oldStatus, int status) {
        Notification notification = new Notification();
        String appName = getString(R.string.app_name);
        String msg = null;
        switch (status) {
            case XmppManager.CONNECTED:
                msg = getString(R.string.main_service_connected);
                notification = new Notification(R.drawable.status_green, msg, System.currentTimeMillis());
                break;
            case XmppManager.CONNECTING:
                msg = getString(R.string.main_service_connecting);
                notification = new Notification(R.drawable.status_orange, msg, System.currentTimeMillis());
                break;
            case XmppManager.DISCONNECTED:
                msg = getString(R.string.main_service_disconnected);
                notification = new Notification(R.drawable.status_red, msg, System.currentTimeMillis());
                break;
            case XmppManager.DISCONNECTING:
                msg = getString(R.string.main_service_disconnecting);
                notification = new Notification(R.drawable.status_orange, msg, System.currentTimeMillis());
                break;
            case XmppManager.WAITING_TO_CONNECT:
                String msgNotif = getString(R.string.main_service_waiting);
                msg = getString(R.string.main_service_waiting_to_connect);
                notification = new Notification(R.drawable.status_orange, msgNotif, System.currentTimeMillis());
                break;
            default:
                break;
        }
        
        if (msg != null) {
            notification.setLatestEventInfo(getApplicationContext(), appName, msg, _contentIntent);
        }

        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notification.flags |= Notification.FLAG_NO_CLEAR;
        stopForegroundCompat(oldStatus);
        if (_settingsMgr.showStatusIcon) {
            startForegroundCompat(status, notification);
        }
    }

    /**
     * This is a wrapper around the startForeground method, using the older APIs
     * if it is not available.
     */
    void startForegroundCompat(int id, Notification notification) {
        // If we have the new startForeground API, then use it.
        if (_startForeground != null) {
            _startForegroundArgs[0] = Integer.valueOf(id);
            _startForegroundArgs[1] = notification;
            try {
                _startForeground.invoke(this, _startForegroundArgs);
            } catch (InvocationTargetException e) {
                // Should not happen.
                Log.w(Tools.LOG_TAG, "Unable to invoke startForeground", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
                Log.w(Tools.LOG_TAG, "Unable to invoke startForeground", e);
            }
            return;
        }
        // Fall back on the old API.
        setForeground(true);
        _notificationMgr.notify(id, notification);
    }

    /**
     * This is a wrapper around the stopForeground method, using the older APIs
     * if it is not available.
     */
    void stopForegroundCompat(int id) {
        // If we have the new stopForeground API, then use it.
        if (_stopForeground != null) {
            _stopForegroundArgs[0] = Boolean.TRUE;
            try {
                _stopForeground.invoke(this, _stopForegroundArgs);
            } catch (InvocationTargetException e) {
                // Should not happen.
                Log.w(Tools.LOG_TAG, "Unable to invoke stopForeground", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
                Log.w(Tools.LOG_TAG, "Unable to invoke stopForeground", e);
            }
            return;
        }

        try {
            // Fall back on the old API. Note to cancel BEFORE changing the
            // foreground state, since we could be killed at that point.
            _notificationMgr.cancel(id);
            setForeground(false);
        } catch (Exception e) {
            // Should not happen.
            Log.w(Tools.LOG_TAG, "Unable to invoke stopForeground", e);
        }
    }

    /**
     * This makes the 2 previous wrappers possible
     */
    private void initNotificationStuff() {
        _notificationMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        try {
            _startForeground = getClass().getMethod("startForeground", _startForegroundSignature);
            _stopForeground = getClass().getMethod("stopForeground", _stopForegroundSignature);
        } catch (NoSuchMethodException e) {
            // Running on an older platform.
            _startForeground = _stopForeground = null;
        }
        _contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainScreen.class), 0);
    }

    public int getConnectionStatus() {
        return _xmppMgr == null ? XmppManager.DISCONNECTED : _xmppMgr.getConnectionStatus();
    }
    
    public boolean getTLSStatus() {
        return _xmppMgr == null ? false : _xmppMgr.getTLSStatus();
    }
    
    public boolean getCompressionStatus() {
    	return _xmppMgr == null ? false : _xmppMgr.getCompressionStatus();
    }
    
    public static GoogleAnalyticsHelper getAnalyticsHelper() {
    	return _gAnalytics;
    }

    public void updateBuddies() {
        if (_xmppMgr != null) {
            _xmppMgr.retrieveFriendList();
        }
    }
   
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public MainService getService() {
            return MainService.this;
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return _binder;
    }

    @Override
    public void onCreate() {
    	super.onCreate();
        
    	if(_gAnalytics == null)
        	_gAnalytics = new GoogleAnalyticsHelper(getApplicationContext());
        
    	_settingsMgr = new SettingsManager(this) {
            @Override  public void OnPreferencesUpdated() {
                Tools.setLocale(_settingsMgr, getBaseContext());
            }
        };
        
    	Tools.setLocale(_settingsMgr, this);
        HandlerThread thread = new HandlerThread("GTalkSMS.Service");
        thread.start();
        _handlerThreadId = thread.getId();
        _serviceLooper = thread.getLooper();
        _serviceHandler = new ServiceHandler(_serviceLooper);
        initNotificationStuff();
        Log.i(Tools.LOG_TAG, "service created");
        IsRunning = true;           		       
    }

    @Override
    public void onStart(Intent intent, int startId) {
        if (intent == null) {  // The application has been killed by Android and we try to restart the connection
            startService(new Intent(MainService.ACTION_CONNECT));
            return;
        }
        // A special case for the 'broadcast status' intent - we avoid setting up the _xmppMgr etc
        else if (intent.getAction().equals(ACTION_BROADCAST_STATUS)) {
            Log.d(Tools.LOG_TAG, "onStart: ACTION_BROADCAST_STATUS");
            
            // A request to broadcast our current status evenif _xmpp is null.
            int state = getConnectionStatus();
            XmppManager.broadcastStatus(this, state, state);
            return;
        }
        // OK - a real action request - ensure xmpp is setup (but not yet connected)
        // in preparation for the worker thread performing the request.
        if (_xmppMgr == null) {
            if (_settingsMgr.notifiedAddress == null || _settingsMgr.notifiedAddress.equals("") || _settingsMgr.notifiedAddress.equals("your.login@gmail.com")) {
                Log.i(Tools.LOG_TAG, "Preferences not set! Opens preferences page.");
                Intent settingsActivity = new Intent(getBaseContext(), Preferences.class);
                settingsActivity.putExtra("panel", R.xml.prefs_connection);
                settingsActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(settingsActivity);
                return;
            }

            _xmppreceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action.equals(XmppManager.ACTION_MESSAGE_RECEIVED)) {
                        // as this comes in on the main thread and not the worker thread,
                        // we just push the message onto the worker thread queue.
                        startService(newSvcIntent(MainService.this, ACTION_HANDLE_XMPP_NOTIFY, intent.getStringExtra("message")));
                    } else if (action.equals(XmppManager.ACTION_CONNECTION_CHANGED)) {
                        onConnectionStatusChanged(intent.getIntExtra("old_state", 0),
                                                  intent.getIntExtra("new_state", 0));
                        startService(newSvcIntent(MainService.this, ACTION_HANDLE_XMPP_NOTIFY));  //TODO the intent handling could be improved at this point
                    }
                }
            };
            Log.d(Tools.LOG_TAG, "onStart: ACTION_MESSAGE_RECEIVED");
            IntentFilter intentFilter = new IntentFilter(XmppManager.ACTION_MESSAGE_RECEIVED);
            intentFilter.addAction(XmppManager.ACTION_CONNECTION_CHANGED);
            registerReceiver(_xmppreceiver, intentFilter);
            
            _xmppMgr = new XmppManager(_settingsMgr, getBaseContext());
        }

        if (intent != null) {
            Log.d(Tools.LOG_TAG, "onStart: _serviceHandler.sendMessage");
            Message msg = _serviceHandler.obtainMessage();
            msg.arg1 = startId;
            msg.obj = intent;
            _serviceHandler.sendMessage(msg);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return START_STICKY;
    }

    // This method *requests* a state change - what state things actually
    // wind up in is impossible to know (eg, a request to connect may wind up
    // with a state of CONNECTED, DISCONNECTED or WAITING_TO_CONNECT...
    private void xmppRequestStateChange(int newState) {
        int currentState = _xmppMgr.getConnectionStatus();
        switch (newState) {
        case XmppManager.CONNECTED:
            switch (currentState) {
            case XmppManager.CONNECTED:
                break;
            case XmppManager.DISCONNECTED:
            case XmppManager.WAITING_TO_CONNECT:
                _xmppMgr.stop();
                _xmppMgr.start();
                break;
            default:
                throw new IllegalStateException("unexpected current state when moving to connected: " + currentState);
            }
            break;
        case XmppManager.DISCONNECTED:
            _xmppMgr.stop();
            break;
        case XmppManager.WAITING_TO_CONNECT:
            switch (currentState) {
            case XmppManager.CONNECTED:
                _xmppMgr.stop();
                _xmppMgr.start(XmppManager.WAITING_TO_CONNECT);
                break;
            case XmppManager.DISCONNECTED:
                _xmppMgr.start(XmppManager.WAITING_TO_CONNECT);
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

    private int updateListenersToCurrentState(int currentState) {
        boolean wantListeners;
        switch (currentState) {
        case XmppManager.CONNECTED:
            wantListeners = true;
            break;
        case XmppManager.CONNECTING:
        case XmppManager.DISCONNECTED:
        case XmppManager.DISCONNECTING:
        case XmppManager.WAITING_TO_CONNECT:
            wantListeners = false;
            break;
        default:
            throw new IllegalStateException("updateListeners found invalid state: " + currentState);
        }
        
        if (wantListeners && _commands.isEmpty()) {
            setupListenersForConnection();
        } else if (!wantListeners && !_commands.isEmpty()) {
            teardownListenersForConnection();
        }
        
        return currentState;
    }
    
    /**
     * this method is called once in the lifetime of the service
     * and only if we have a network available
     */
    private void setupListenersForConnection() {
        Log.d(Tools.LOG_TAG, "setupListenersForConnection");  
        if(_gAnalytics == null) {
        	_gAnalytics = new GoogleAnalyticsHelper(getApplicationContext());
        }
        _gAnalytics.trackInstalls(); //we only track if we have a data connection

        try {
            setupCommands();
        } catch (Exception e) {
            // Should not happen.
            Log.w(Tools.LOG_TAG, "MainService.setupListenersForConnection: Setup commands error", e);
        } 
    }

    @Override
    public void onDestroy() {
        Log.i(Tools.LOG_TAG, "service destroyed");
        IsRunning = false;
        // If the _xmppManager is non-null, then our service was "started" (as
        // opposed to simply "created" - so tell the user it has stopped.
        if (_xmppMgr != null) {
            unregisterReceiver(_xmppreceiver);
            _xmppreceiver = null;
            
            Tools.toastMessage(this, getString(R.string.main_service_stop));
            _xmppMgr.stop();
            _xmppMgr = null;
        }
        teardownListenersForConnection();
            _gAnalytics.stop();
        _serviceLooper.quit();
        super.onDestroy();
    }
    
    private void teardownListenersForConnection() {
        Log.d(Tools.LOG_TAG, "teardownListenersForConnection");
        
        stopForegroundCompat(getConnectionStatus());

        stopCommands();
        cleanupCommands();
    }

    public static void send(Context ctx, String msg) {
        ctx.startService(newSvcIntent(ctx, ACTION_SEND, msg));
    }

    public void send(String msg) {
        if (_xmppMgr != null) {
            _xmppMgr.send(new XmppMsg(msg));
        }
    }

    public void send(XmppMsg msg) {
        if (_xmppMgr != null) {
            _xmppMgr.send(msg);
        }
    }

    public void sendFile(String fileName) {
        if (_xmppMgr != null) {
            _xmppMgr.sendFile(fileName);
        }
    }
    
    public SettingsManager getSettingsManager() {
        return _settingsMgr;
    }
    
    public void setKeyboard(KeyboardInputMethod keyboard) {
        _keyboard = keyboard;
    }
    
    public KeyboardInputMethod getKeyboard() {
        return _keyboard;
    }
    
    /**
     * Handels the different commands
     * that came with the xmpp connection
     * usually from an intent with
     * ACTION_HANDLE_XMPP_NOTIFY
     * 
     * @param commandLine
     */
    private void onMessageReceived(String commandLine) {
        Log.v(Tools.LOG_TAG, "onMessageReceived: " + commandLine);
        try {
            String command;
            String args;
            if (commandLine.indexOf(":") != -1) {
                command = commandLine.substring(0, commandLine.indexOf(":"));
                args = commandLine.substring(commandLine.indexOf(":") + 1);
            } else {
                command = commandLine;
                args = "";
            }

            // Not case sensitive commands
            command = command.toLowerCase();
            if (command.equals("exit")) {
                stopService(new Intent(ACTION_CONNECT));
            } else if (command.equals("stop")) {
                stopCommands();
            } else if (_commands.containsKey(command)) {
                _commands.get(command).execute(command, args);
            } else {
                send(getString(R.string.chat_error_unknown_cmd, command));
            }
        } catch (Exception ex) {
            send(getString(R.string.chat_error, ex));
        }
    }
    
    public void executeCommand(String cmd, String args) {
        if (_commands.containsKey(cmd)) {
            _commands.get(cmd).execute(cmd, args);
        } else {
            send(getString(R.string.chat_error_unknown_cmd, cmd));
        }
    }
    
    private void setupCommands() {
        registerCommand(new HelpCmd(this));
        registerCommand(new KeyboardCmd(this));
        registerCommand(new BatteryCmd(this));
        registerCommand(new GeoCmd(this));
        registerCommand(new CallCmd(this));
        registerCommand(new ContactCmd(this));
        registerCommand(new ClipboardCmd(this));
        registerCommand(new ShellCmd(this));
        registerCommand(new UrlsCmd(this));
        registerCommand(new RingCmd(this));
        registerCommand(new FileCmd(this));
        registerCommand(new SmsCmd(this));
    }
    
    private void cleanupCommands() {
        for (Command cmd : _commandSet) {
            cmd.cleanUp();
        }
        _commands.clear();
    }
    
    /**
     * used to stop ongoing actions, like gps updates, ringing, ... 
     */
    private void stopCommands() {
        for(Command c : _commandSet)
            c.stop();
    }
    
    private void registerCommand(Command cmd) {
        String[] commands = cmd.getCommands();
        for (String c : commands) {
            _commands.put(c, cmd);
        }
        _commandSet.add(cmd);
    } 

    public void setXmppStatus(String status) {
        _xmppMgr.setStatus(status);
    }
    
    /** Intent helper functions.
     *  As many of our intent objects use a 'message' extra, we have a helper that
     *  allows you to provide that too.  Any other extras must be set manually
     */
    public static Intent newSvcIntent(Context ctx, String action) {
        return newSvcIntent(ctx, action, null);
    }

    public static Intent newSvcIntent(Context ctx, String action, String message) {
        Intent i = new Intent(action, null, ctx, MainService.class);
        if (message != null) {
            i.putExtra("message", message);
        }
        return i;
    }
    
    public XmppManager getXmppmanager() {
    	return _xmppMgr;
    }
}
