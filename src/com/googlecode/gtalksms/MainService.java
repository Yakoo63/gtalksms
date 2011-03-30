package com.googlecode.gtalksms;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smack.XMPPException;

import android.app.Notification;
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

import com.googlecode.gtalksms.cmd.AliasCmd;
import com.googlecode.gtalksms.cmd.BatteryCmd;
import com.googlecode.gtalksms.cmd.CallCmd;
import com.googlecode.gtalksms.cmd.ClipboardCmd;
import com.googlecode.gtalksms.cmd.Command;
import com.googlecode.gtalksms.cmd.ContactCmd;
import com.googlecode.gtalksms.cmd.ExitCmd;
import com.googlecode.gtalksms.cmd.FileCmd;
import com.googlecode.gtalksms.cmd.GeoCmd;
import com.googlecode.gtalksms.cmd.HelpCmd;
import com.googlecode.gtalksms.cmd.KeyboardCmd;
import com.googlecode.gtalksms.cmd.RingCmd;
import com.googlecode.gtalksms.cmd.ShellCmd;
import com.googlecode.gtalksms.cmd.SmsCmd;
import com.googlecode.gtalksms.cmd.SystemCmd;
import com.googlecode.gtalksms.cmd.UrlsCmd;
import com.googlecode.gtalksms.data.contacts.ContactsManager;
import com.googlecode.gtalksms.databases.AliasHelper;
import com.googlecode.gtalksms.panels.MainScreen;
import com.googlecode.gtalksms.panels.Preferences;
import com.googlecode.gtalksms.tools.DisplayToast;
import com.googlecode.gtalksms.tools.GoogleAnalyticsHelper;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class MainService extends Service {
    public final static int ID = 1;

    // The following actions are documented and registered in our manifest
    public final static String ACTION_CONNECT = "com.googlecode.gtalksms.action.CONNECT";
    public final static String ACTION_TOGGLE = "com.googlecode.gtalksms.action.TOGGLE";
    public final static String ACTION_SEND = "com.googlecode.gtalksms.action.SEND";
    public final static String ACTION_COMMAND = "com.googlecode.gtalksms.action.COMMAND";

    // The following actions are undocumented and internal to our implementation.
    public final static String ACTION_BROADCAST_STATUS = "com.googlecode.gtalksms.action.BROADCAST_STATUS";
    public final static String ACTION_SMS_RECEIVED = "com.googlecode.gtalksms.action.SMS_RECEIVED";
    public final static String ACTION_NETWORK_CHANGED = "com.googlecode.gtalksms.action.NETWORK_CHANGED";
    public final static String ACTION_SMS_SENT = "com.googlecode.gtalksms.action.SMS_SENT";
    public final static String ACTION_SMS_DELIVERED = "com.googlecode.gtalksms.action.SMS_DELIVERED";
    public final static String ACTION_WIDGET_ACTION = "com.googlecode.gtalksms.action.widget.ACTION";
    
    // A list of intent actions that the XmppManager broadcasts.
    public static final String ACTION_XMPP_MESSAGE_RECEIVED = "com.googlecode.gtalksms.action.XMPP.MESSAGE_RECEIVED";
    public static final String ACTION_XMPP_PRESENCE_CHANGED = "com.googlecode.gtalksms.action.XMPP.PRESENCE_CHANGED";
    public static final String ACTION_XMPP_CONNECTION_CHANGED = "com.googlecode.gtalksms.action.XMPP.CONNECTION_CHANGED";
       
    // 
    public static final String SERVICE_THREAD_NAME = Tools.APP_NAME + ".Service";
    
    // A bit of a hack to allow global receivers to know whether or not
    // the service is running, and therefore whether to tell the service
    // about some events
    public static boolean IsRunning = false;

    private SettingsManager _settingsMgr;
    private XmppManager _xmppMgr;
    private BroadcastReceiver _xmppConChangedReceiver;
    private KeyboardInputMethod _keyboard;
    
    private Map<String, Command> _commands = new HashMap<String, Command>();
    private Set<Command> _commandSet = new HashSet<Command>();
    private PendingIntent _contentIntent = null;

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder _binder = new LocalBinder();

    private long _handlerThreadId;
    
    // to get the helper use MainService.getAnalyticsHelper()
    private static GoogleAnalyticsHelper _gAnalytics;
    
    private static AliasHelper _aliasHelper;
    
    private static Context _uiContext;
    
    private static volatile Handler _toastHandler;  
    
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
    
    // TODO move the following method into the subclass above ?
    /**
     * The IntentService(-like) implementation manages taking the intents passed
     * to startService and delivering them to this function which runs in its
     * own thread (so can block Pretty-much everything using the _xmppMgr is
     * here...
     * 
     * ACTION_XMPP_CONNECTION_CHANGED is handled implicitly, by every call of 
     * this method.
     * 
     * @param intent
     * @param id
     */
    protected void onHandleIntent(final Intent intent, int id) {
        if (intent == null) {  // TODO remove this if block
            GoogleAnalyticsHelper.trackAndLogError("onHandleIntent: Intent null");  
            return;
        }
        
        // Set Disconnected state by force to manage pending tasks
        if (intent.getBooleanExtra("force", false) && intent.getBooleanExtra("disconnect", false)) {
            // request to disconnect.
            _xmppMgr.xmppRequestStateChange(XmppManager.DISCONNECTED);
        }

        if (Thread.currentThread().getId() != _handlerThreadId)
            throw new IllegalThreadStateException();
        // We need to handle xmpp state changes which happened "externally" - eg,
        // due to a connection error, or running out of retries, or a retry
        // handler actually succeeding etc.
        int initialState = getConnectionStatus(); 
        updateListenersToCurrentState(initialState);
        
        String action = intent.getAction();
        if(_settingsMgr.debugLog) Log.i(Tools.LOG_TAG, "handling action '" + action + "' while in state " + initialState);
        
        if (action.equals(ACTION_CONNECT)) {
            if (intent.getBooleanExtra("disconnect", false)) {
                // request to disconnect.
                _xmppMgr.xmppRequestStateChange(XmppManager.DISCONNECTED);
            } else {
                // a simple 'connect' request.
                _xmppMgr.xmppRequestStateChange(XmppManager.CONNECTED);
            }
        } else if (action.equals(ACTION_TOGGLE)) {     
            switch (initialState) {
                case XmppManager.CONNECTED:
                case XmppManager.WAITING_TO_CONNECT:
                    _xmppMgr.xmppRequestStateChange(XmppManager.DISCONNECTED);
                    break;
                case XmppManager.DISCONNECTED:
                    _xmppMgr.xmppRequestStateChange(XmppManager.CONNECTED);
                    break;
                default:
                    Log.e(Tools.LOG_TAG, "Invalid xmpp state: "+ initialState);
                    break;
            }
        } else if (action.equals(ACTION_SEND)) {
            _xmppMgr.send(new XmppMsg(intent.getStringExtra("message")), intent.getStringExtra("to"));
        } else if (action.equals(ACTION_XMPP_MESSAGE_RECEIVED)) {
            String message = intent.getStringExtra("message");
            if (message != null) {
                onCommandReceived(message, intent.getStringExtra("from"));
            }
        } else if (action.equals(ACTION_SMS_RECEIVED)) {
            if (initialState == XmppManager.CONNECTED) {
                String number = intent.getStringExtra("sender");
                String name = ContactsManager.getContactName(this, number);
                String message = intent.getStringExtra("message");
                
                if (_settingsMgr.notifySmsInSameConversation && !_xmppMgr.getXmppMuc().roomExists(number, name)) {
                    XmppMsg msg = new XmppMsg();
                    msg.appendBold(getString(R.string.chat_sms_from, name));
                    msg.append(message);
                    _xmppMgr.send(msg, null);
                    if (_commands.containsKey("sms")) {
                        ((SmsCmd)_commands.get("sms")).setLastRecipient(number);
                    }
                }
                if (_settingsMgr.notifySmsInChatRooms || _xmppMgr.getXmppMuc().roomExists(number, name)) {
                    try {
                        _xmppMgr.getXmppMuc().writeRoom(number, name, message);
                    } catch (XMPPException e) {
                        //room creation failed - notify about this error
                        // and send the message to the notification address
                        XmppMsg msg = new XmppMsg();
                        msg.appendLine("ACTION_SMS_RECEIVED - Error writing to MUC: " + e);
                        msg.appendBold(getString(R.string.chat_sms_from, name));
                        msg.append(message);
                        _xmppMgr.send(msg, null);
                    }
                }                
            }
        } else if (action.equals(ACTION_NETWORK_CHANGED)) {
            boolean available = intent.getBooleanExtra("available", true);
            if(_settingsMgr.debugLog) Log.i(Tools.LOG_TAG, "network_changed with available=" + available + " and with state=" + initialState);
            if(available) {
                GoogleAnalyticsHelper.dispatch();
            }
            // TODO wait few seconds if network not available ? to avoid multiple reconnections
            if (available && initialState == XmppManager.WAITING_TO_CONNECT) {
                // We are in a waiting state and have a network - try to connect.
                _xmppMgr.xmppRequestStateChange(XmppManager.CONNECTED);
            } else if (!available && initialState == XmppManager.CONNECTED) {
                // We are connected but the network has gone down - disconnect and go
                // into WAITING state so we auto-connect when we get a future 
                // notification that a network is available.
                _xmppMgr.xmppRequestStateChange(XmppManager.WAITING_TO_CONNECT);
            }
        } else if (action.equals(ACTION_COMMAND)) {
            String cmd = intent.getStringExtra("cmd");
            if (cmd != null) {
                String args = intent.getStringExtra("args");
                String from = intent.getStringExtra("from");
                if (intent.getBooleanExtra("fromMuc", false) && !_settingsMgr.notifyInMuc)
                    from = null;
                executeCommand(cmd, args, from);
            } else {
                Log.w(Tools.LOG_TAG, "Intent " + MainService.ACTION_COMMAND + " without extra cmd");
            }
        } else if(!action.equals(ACTION_XMPP_CONNECTION_CHANGED)) {            
            GoogleAnalyticsHelper.trackAndLogWarning("Unexpected intent: " + action);
        }
        if(_settingsMgr.debugLog) Log.i(Tools.LOG_TAG, "handled action '" + action + "' - state now " + getConnectionStatus());
        // stop the service if we are disconnected (but stopping the service
        // doesn't mean the process is terminated - onStart can still happen.)
        if (getConnectionStatus() == XmppManager.DISCONNECTED) {
            if (stopSelfResult(id) == true) {
                if(_settingsMgr.debugLog) Log.i(Tools.LOG_TAG, "service is stopping (we are disconnected and no pending intents exist.)");
            } else {
                if(_settingsMgr.debugLog) Log.i(Tools.LOG_TAG, "we are disconnected, but more pending intents to be delivered - service will not stop");
            }
        }
    }

    public int getConnectionStatus() {
        return _xmppMgr == null ? XmppManager.DISCONNECTED : _xmppMgr.getConnectionStatus();
    }
    
    public Map<String, Command> getCommands() {
        return _commands;
    }
    
    public Set<Command> getCommandSet() {
        return _commandSet;
    }
    
    public boolean getTLSStatus() {
        return _xmppMgr == null ? false : _xmppMgr.getTLSStatus();
    }           // null check necessary
    
    public boolean getCompressionStatus() {
    	return _xmppMgr == null ? false : _xmppMgr.getCompressionStatus();   	    
    }          // null check necessary
    
    public static GoogleAnalyticsHelper getAnalyticsHelper() {
    	return _gAnalytics;
    }
    
    public AliasHelper createAndGetAliasHelper() {
        if(_aliasHelper == null) {
            _aliasHelper = new AliasHelper(getBaseContext());
        }
        return _aliasHelper;
    }

    public void updateBuddies() {
        if (_xmppMgr != null) {
            _xmppMgr.getXmppBuddies().retrieveFriendList();
        }
    }
    
    /** Intent helper functions.
     *  As many of our intent objects use a 'message' extra, we have a helper that
     *  allows you to provide that too.  Any other extras must be set manually
     */
    public static Intent newSvcIntent(Context ctx, String action) {
        return newSvcIntent(ctx, action, null, null);
    }
    
    /**
     * 
     * @param ctx
     * @param action
     * @param message
     * @param to - full JID to send message to, can be null for default notification address
     * @return
     */
    public static Intent newSvcIntent(Context ctx, String action, String message, String to) {
        Intent i = new Intent(action, null, ctx, MainService.class);
        if (message != null) {
            i.putExtra("message", message);
        }
        if (to != null) {
            i.putExtra("to", to);
        }
        return i;
    }
    
    public XmppManager getXmppmanager() {
        return _xmppMgr;
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
        _gAnalytics = new GoogleAnalyticsHelper(getApplicationContext());
        _gAnalytics.trackInstalls();
        
        _settingsMgr = new SettingsManager(this) {
            @Override public void OnPreferencesUpdated(String key) {
            	super.OnPreferencesUpdated(key);
                Tools.setLocale(_settingsMgr, getBaseContext());
            }
        };
        
        Tools.setLocale(_settingsMgr, this);
        
        HandlerThread thread = new HandlerThread(SERVICE_THREAD_NAME);
        thread.start();
        _handlerThreadId = thread.getId();
        _serviceLooper = thread.getLooper();
        _serviceHandler = new ServiceHandler(_serviceLooper);
        
        _uiContext = this;
        _toastHandler = new Handler();
        
        _contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainScreen.class), 0);
        
        if(_settingsMgr.debugLog) Log.i(Tools.LOG_TAG, "onCreate(): service thread created");
        IsRunning = true; 
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        _gAnalytics.trackServiceStartsPerDay();
        if (intent == null) { 
            // The application has been killed by Android and
            // we try to restart the connection
            startService(new Intent(MainService.ACTION_CONNECT));
            return START_STICKY;
        }
        if(_settingsMgr.debugLog) Log.i(Tools.LOG_TAG, "onStartCommand(): Intent " + intent.getAction());
        // A special case for the 'broadcast status' intent - we avoid setting
        // up the _xmppMgr etc
        if (intent.getAction().equals(ACTION_BROADCAST_STATUS)) {
            // A request to broadcast our current status even if _xmpp is null.
            int state = getConnectionStatus();
            XmppManager.broadcastStatus(this, state, state);
        } else {
            // OK - a real action request - ensure xmpp is setup (but not yet connected)
            // in preparation for the worker thread performing the request.
            if (_xmppMgr == null) {
                if (_settingsMgr.notifiedAddress == null || _settingsMgr.notifiedAddress.equals("")
                        || _settingsMgr.notifiedAddress.equals("your.login@gmail.com")) {
                    Log.i(Tools.LOG_TAG, "Preferences not set! Opens preferences page.");
                    Intent settingsActivity = new Intent(getBaseContext(), Preferences.class);
                    settingsActivity.putExtra("panel", R.xml.prefs_connection);
                    settingsActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(settingsActivity);
                    return START_STICKY;
                }

                _xmppConChangedReceiver = new BroadcastReceiver() {
                    public void onReceive(Context context, Intent intent) {
                        intent.setClass(MainService.this, MainService.class);
                        onConnectionStatusChanged(intent.getIntExtra("old_state", 0), intent.getIntExtra("new_state", 0));
                        startService(intent);
                    }
                };
                IntentFilter intentFilter = new IntentFilter(MainService.ACTION_XMPP_CONNECTION_CHANGED);
                registerReceiver(_xmppConChangedReceiver, intentFilter);
                
                _xmppMgr = new XmppManager(_settingsMgr, getBaseContext());
            }
            
            Message msg = _serviceHandler.obtainMessage();
            msg.arg1 = startId;
            msg.obj = intent;
            _serviceHandler.sendMessage(msg);
        }
        return START_STICKY;
    }    

    @Override
    public void onDestroy() {
        Log.i(Tools.LOG_TAG, "service destroyed");
        IsRunning = false;
        // If the _xmppManager is non-null, then our service was "started" (as
        // opposed to simply "created" - so tell the user it has stopped.
        if (_xmppMgr != null) {
            unregisterReceiver(_xmppConChangedReceiver);
            _xmppConChangedReceiver = null;
            
            _xmppMgr.stop();
            _xmppMgr = null;
        }
        teardownListenersForConnection();
        GoogleAnalyticsHelper.stop();
        _serviceLooper.quit();
        super.onDestroy();
    }
    
    /**
     * Wrapper to send a string to the user via XMPP
     * needed by some receivers
     * starts the MainService with a new intent
     * 
     * @param ctx
     * @param msg
     */
    public static void send(Context ctx, String msg) {
        ctx.startService(newSvcIntent(ctx, ACTION_SEND, msg, null));
    }
    
    /**
     * Wrapper to send a string to the user via XMPP
     * needed by some receivers
     * starts the MainService with a new intent
     * 
     * @param ctx
     * @param msg
     * @param to
     */
    public static void send(Context ctx, String msg, String to) {
        ctx.startService(newSvcIntent(ctx, ACTION_SEND, msg, to));
    }
    
    /**
     * Wrapper for send(XmppMsg msg... method
     * 
     * @param msg
     * @param to The receiving JID, if null the default notification address is used
     */
    public void send(String msg, String to) {
       send(new XmppMsg(msg), to);
    }
    
    /**
     * Sends an XmppMsg to the specified JID or to the default notification address
     * 
     * @param msg
     * @param to - the receiving jid. if null the default notification address is used
     */
    public void send(XmppMsg msg, String to) {
        if (_xmppMgr != null) {
            _xmppMgr.send(msg, to);
        } else {
            GoogleAnalyticsHelper.trackAndLogError("MainService send XmppMsg: _xmppMgr == null");
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
     * Provides a clean way to display toast messages that don't get stuck
     * 
     * @param text
     * @parm extraInfo can be null
     * @param ctx
     */
    public static void displayToast(String text, String extraInfo) {      
        _toastHandler.post(new DisplayToast(text, extraInfo, _uiContext));
    }
    
    /**
     * Display a string resource i as toast messages
     * 
     * @param i
     * @param extraInfo can be null
     */
    public static void displayToast(int i, String extraInfo) {
        displayToast(_uiContext.getString(i), extraInfo);
    }
    
    private void executeCommand(String cmd, String args, String answerTo) {
        assert(cmd != null);
        if (_settingsMgr.debugLog)
            Log.i(Tools.LOG_TAG, "executeCommand: _commands.size=" + _commands.size());
        if (_commands.containsKey(cmd)) {
            _commands.get(cmd).execute(cmd, args, answerTo);
        } else {
            send(getString(R.string.chat_error_unknown_cmd, cmd), answerTo);
        }
    }
    
    /** Updates the status about the service state (and the status bar) */
    private void onConnectionStatusChanged(int oldStatus, int status) {
        if (_settingsMgr.showStatusIcon) {
            Notification notification = new Notification();
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
                GoogleAnalyticsHelper.trackAndLogError("onConnectionStatusChanged(): unkown status int");
                return;
            }

            notification.setLatestEventInfo(getApplicationContext(), Tools.APP_NAME, msg, _contentIntent);
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            notification.flags |= Notification.FLAG_NO_CLEAR;
            notification.tickerText = null;

            startForeground(ID, notification);
        }
    }
    
    /**
     * Handels the different commands
     * that came with the xmpp connection
     * usually from an intent with
     * ACTION_XMPP_MESSAGE_RECEIVED
     * 
     * @param commandLine
     */
    private void onCommandReceived(String commandLine, String from) {
        if (_settingsMgr.debugLog) {
            Log.i(Tools.LOG_TAG, "onCommandReceived(): \"" + Tools.shortenMessage(commandLine) + "\"");
        }
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
            if (command.equals("stop")) {
                send(getString(R.string.chat_stop_actions), from);
                stopCommands();
            } else {
                executeCommand(command, args, from);
            }
        } catch (Exception ex) {
            GoogleAnalyticsHelper.trackAndLogError("MainService onCommandReceived exception", ex);
            send(getString(R.string.chat_error, ex), from);
        }
    }
    
    private void setupCommands() {
        Log.i(Tools.LOG_TAG, "Registering Commands. _commands size: " + _commands.size());
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
        registerCommand(new ExitCmd(this));
        registerCommand(new AliasCmd(this));
        registerCommand(new SystemCmd(this)); // used for debugging
        
        registerCommand(new HelpCmd(this));  //help command needs to be registered as last
        Log.i(Tools.LOG_TAG, "Registered Commands. _commands size: " + _commands.size());
    }
    
    /**
     * Calls cleanUp() for every registered command
     * and removes the references for every registered command
     * by calling clear() 
     */
    private void cleanupCommands() {
        for (Command cmd : _commandSet) {
            cmd.cleanUp();
        }
        Log.i(Tools.LOG_TAG, "Clearing _commands. Size: " + _commands.size());
        _commands.clear();
        Log.i(Tools.LOG_TAG, "Cleared  _commands. Size: " + _commands.size());
        _commandSet.clear();
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
     * registers the commands, executing their constructor
     *  
     */
    private void setupListenersForConnection() {
        if(_settingsMgr.debugLog) Log.i(Tools.LOG_TAG, "setupListenersForConnection()");  
        
        try {
            setupCommands();
        } catch (Exception e) {
            // Should not happen.
            GoogleAnalyticsHelper.trackAndLogError("MainService.setupListenersForConnection: Setup commands error", e);
        } 
    }
    
    private void teardownListenersForConnection() {
        if(_settingsMgr.debugLog) Log.i(Tools.LOG_TAG, "teardownListenersForConnection()");      
        stopForeground(true);
        stopCommands();
        cleanupCommands();
    }
}