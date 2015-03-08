package com.googlecode.gtalksms;

import java.util.Map;
import java.util.Set;

import org.jivesoftware.smackx.ping.PingManager;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;

import com.googlecode.gtalksms.cmd.Cmd;
import com.googlecode.gtalksms.cmd.CommandHandlerBase;
import com.googlecode.gtalksms.cmd.RecipientCmd;

import com.googlecode.gtalksms.data.contacts.ContactsManager;
import com.googlecode.gtalksms.panels.MainActivity;
import com.googlecode.gtalksms.receivers.NetworkConnectivityReceiver;
import com.googlecode.gtalksms.receivers.PublicIntentReceiver;
import com.googlecode.gtalksms.receivers.StorageLowReceiver;
import com.googlecode.gtalksms.services.KeyboardInputMethodService;
import com.googlecode.gtalksms.tools.CrashedStartCounter;
import com.googlecode.gtalksms.tools.DisplayToast;
import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppBuddies;
import com.googlecode.gtalksms.xmpp.XmppMsg;
import com.googlecode.gtalksms.xmpp.XmppMuc;
import com.googlecode.gtalksms.xmpp.XmppStatus;

public class MainService extends Service {
    private static MainService sIntance = null;

    private final static int NOTIFICATION_CONNECTION = 1;
    private final static int NOTIFICATION_STOP_RINGING = 2;
    
    // The following actions are documented and registered in our manifest
    public final static String ACTION_CONNECT = "com.googlecode.gtalksms.action.CONNECT";
    public final static String ACTION_DISCONNECT = "com.googlecode.gtalksms.action.DISCONNECT";
    public final static String ACTION_TOGGLE = "com.googlecode.gtalksms.action.TOGGLE";
    public final static String ACTION_SEND = "com.googlecode.gtalksms.action.SEND";
    public final static String ACTION_COMMAND = "com.googlecode.gtalksms.action.COMMAND";

    // The following actions are undocumented and internal to our implementation.
    public final static String ACTION_BROADCAST_STATUS = "com.googlecode.gtalksms.action.BROADCAST_STATUS";
    public final static String ACTION_SMS_RECEIVED = "com.googlecode.gtalksms.action.SMS_RECEIVED";
    public final static String ACTION_NETWORK_STATUS_CHANGED = "com.googlecode.gtalksms.action.NETWORK_STATUS_CHANGED";
    public final static String ACTION_SMS_SENT = "com.googlecode.gtalksms.action.SMS_SENT";
    public final static String ACTION_SMS_DELIVERED = "com.googlecode.gtalksms.action.SMS_DELIVERED";
    public final static String ACTION_WIDGET_ACTION = "com.googlecode.gtalksms.action.widget.ACTION";

    // A list of intent actions that the XmppManager broadcasts.
    public static final String ACTION_XMPP_MESSAGE_RECEIVED = "com.googlecode.gtalksms.action.XMPP.MESSAGE_RECEIVED";
    public static final String ACTION_XMPP_PRESENCE_CHANGED = "com.googlecode.gtalksms.action.XMPP.PRESENCE_CHANGED";
    public static final String ACTION_XMPP_CONNECTION_CHANGED = "com.googlecode.gtalksms.action.XMPP.CONNECTION_CHANGED";

    public static final String SERVICE_THREAD_NAME = Tools.APP_NAME + ".Service";

    private static final int STATUS_ICON_GREEN = 0;
    private static final int STATUS_ICON_ORANGE = 1;
    private static final int STATUS_ICON_RED = 2;
    private static final int STATUS_ICON_BLUE = 3;

    // A bit of a hack to allow global receivers to know whether or not
    // the service is running, and therefore whether to tell the service
    // about some events
    public static boolean IsRunning = false;

    private static boolean sListenersActive = false;

    private static SettingsManager sSettingsMgr;
    private static NotificationManager sNotificationManager;
    private static XmppManager sXmppMgr;
    private static BroadcastReceiver sXmppConChangedReceiver;
    private static BroadcastReceiver sStorageLowReceiver;
    private static KeyboardInputMethodService sKeyboardInputMethodService;
    private static PowerManager sPm;
    private static PowerManager.WakeLock sWl;
    private static PendingIntent sPendingIntentLaunchApplication = null;
    private static PendingIntent sPendingIntentStopRinging = null;

    // This is the object that receives interactions from clients. See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    private CommandManager mCommandManager;

    private long mHandlerThreadId;

    private static Context sUiContext;

    private static volatile Handler sToastHandler = new Handler();
    private static Handler sDelayedDisconnectHandler;

    // some stuff for the async service implementation - borrowed heavily from
    // the standard IntentService, but that class doesn't offer fine enough
    // control for "foreground" services.
    private static volatile Looper sServiceLooper;
    private static volatile ServiceHandler sServiceHandler;

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            // ensure XMPP manager is setup (but not yet connected)
            if (sXmppMgr == null) {
                setupXmppManagerAndCommands();
            }

            Intent intent = (Intent)msg.obj;
            String action = intent.getAction();
            int id = msg.arg1;

            if (action.equals(ACTION_CONNECT)
                    || action.equals(ACTION_DISCONNECT)
                    || action.equals(ACTION_TOGGLE)
                    || action.equals(ACTION_NETWORK_STATUS_CHANGED)) {
                onHandleIntentTransportConnection(intent);
            } else if (!action.equals(ACTION_XMPP_CONNECTION_CHANGED)){
                onHandleIntentMessage(intent);
            }

            // stop the service if we are disconnected (but stopping the service
            // doesn't mean the process is terminated - onStart can still happen.)
            if (getConnectionStatus() == XmppManager.DISCONNECTED) {
                if (stopSelfResult(id)) {
                    Log.i("service is stopping because we are disconnected and no pending intents exist");
                } else {
                    Log.i("we are disconnected, but more pending intents to be delivered - service will not stop");
                }
            }
        }
    }

    void connectTransport() {
        sXmppMgr.xmppRequestStateChange(XmppManager.CONNECTED);
    }

    void disconnectTransport() {
        sXmppMgr.xmppRequestStateChange(XmppManager.DISCONNECTED);
    }

    void onHandleIntentTransportConnection(final Intent intent) {
        // Set Disconnected state by force to manage pending tasks
        // This is not actively used any more
        if (intent.getBooleanExtra("force", false) && intent.getBooleanExtra("disconnect", false)) {
            disconnectTransport();
        }

        if (Thread.currentThread().getId() != mHandlerThreadId) {
            throw new IllegalThreadStateException();
        }

        String action = intent.getAction();
        int initialState = getConnectionStatus();
        Log.i("handling action '" + action + "' while in state " + XmppManager.statusAsString(initialState));

        // Start with handling the actions the could result in a change
        // of the connection status
        if (action.equals(ACTION_CONNECT)) {
            if (intent.getBooleanExtra("disconnect", false)) {
                disconnectTransport();
            } else {
                connectTransport();
            }
        } else if (action.equals(ACTION_DISCONNECT)) {
            disconnectTransport();
        } else if (action.equals(ACTION_TOGGLE)) {
            switch (initialState) {
                case XmppManager.CONNECTED:
                case XmppManager.CONNECTING:
                case XmppManager.WAITING_TO_CONNECT:
                case XmppManager.WAITING_FOR_NETWORK:
                    disconnectTransport();
                    break;
                case XmppManager.DISCONNECTED:
                case XmppManager.DISCONNECTING:
                    connectTransport();
                    break;
                default:
                    throw new IllegalStateException("Unknown initialState while handling" + MainService.ACTION_TOGGLE);
            }
        } else if (action.equals(ACTION_NETWORK_STATUS_CHANGED)) {
            boolean networkChanged = intent.getBooleanExtra("networkChanged", false);
            boolean connectedOrConnecting = intent.getBooleanExtra("connectedOrConnecting", true);
            boolean connected = intent.getBooleanExtra("connected", true);
            Log.i("NETWORK_CHANGED networkChanged=" + networkChanged + " connected=" + connected
                    + " connectedOrConnecting=" + connectedOrConnecting + " state="
                    + XmppManager.statusAsString(initialState));

            if (!connectedOrConnecting && (initialState == XmppManager.CONNECTED || initialState == XmppManager.CONNECTING)) {
                // We are connected but the network has gone down - disconnect
                // and go into WAITING state so we auto-connect when we get a future
                // notification that a network is available.
                sXmppMgr.xmppRequestStateChange(XmppManager.WAITING_FOR_NETWORK);
            } else if (connected && (initialState == XmppManager.WAITING_TO_CONNECT || initialState == XmppManager.WAITING_FOR_NETWORK)) {
                sXmppMgr.xmppRequestStateChange(XmppManager.CONNECTED);
            } else if (networkChanged && initialState == XmppManager.CONNECTED) {
                // The network has changed (WiFi <-> GSM switch) and we are connected, reconnect now
                disconnectTransport();
                connectTransport();
            }
        } else {
            Log.w("Unexpected intent: " + action);
        }

        // Now that the connection state may has changed either because of a
        // Intent Action or because of connection changes that happened "externally"
        // (eg, due to a connection error, or running out of retries, or a retry
        // handler actually succeeding etc.) we may need to update the listener
        // TODO issue with asynch connection
        updateListenersToCurrentState(getConnectionStatus());
    }

    void onHandleIntentMessage(final Intent intent) {
        String action = intent.getAction();
        int initialState = getConnectionStatus();
        Log.i("handling action '" + action + "' while in state " + XmppManager.statusAsString(initialState));

        if (action.equals(ACTION_SEND)) {
            XmppMsg xmppMsg = intent.getParcelableExtra("xmppMsg");
            if (xmppMsg == null) {
                xmppMsg = new XmppMsg(intent.getStringExtra("message"));
            }
            sXmppMgr.send(xmppMsg, intent.getStringExtra("to"));
        } else if (action.equals(ACTION_XMPP_MESSAGE_RECEIVED)) {
            maybeAcquireWakeLock();
            String message = intent.getStringExtra("message");
            if (message != null) {
                handleCommandFromXMPP(message, intent.getStringExtra("from"));
            }
            sWl.release();
        } else if (action.equals(ACTION_SMS_RECEIVED)) {
            sWl.acquire();
            // A incoming SMS has been received by our SmsReceiver
            String number = intent.getStringExtra("sender");
            String name = ContactsManager.getContactName(this, number);
            String message = intent.getStringExtra("message");
            boolean roomExists = XmppMuc.getInstance(this).roomExists(number);

            Log.i(MainService.ACTION_SMS_RECEIVED + ": number=" + number + " message=" + message + " roomExists=" + roomExists + "notifySame=" + sSettingsMgr.notifySmsInSameConversation);

            // The user wants to be notified in the same conversation window,
            // which just means that we do not notify a MUC but the default
            // notification address BUT ONLY IF THERE IS NO MUC ALREADY
            if (sSettingsMgr.notifySmsInSameConversation && !roomExists) {
                XmppMsg msg = new XmppMsg();
                msg.appendBold(getString(R.string.chat_sms_from, name));
                msg.append(message);
                Log.i("Sending message form " + number + " via chat");
                sXmppMgr.send(msg, null);
                RecipientCmd.setLastRecipient(number);
            }
            // Forward the incoming SMS message to an MUC
            // either because the user wants all notifications in MUCs or
            // because there is already an MUC for the senders number
            if (sSettingsMgr.notifySmsInChatRooms || roomExists) {
                try {
                    XmppMuc.getInstance(this).writeRoom(number, name, message, XmppMuc.MODE_SMS);
                } catch (Exception e) {
                    // room creation and/or writing failed - notify about this error
                    // and send the message to the notification address
                    XmppMsg msg = new XmppMsg();
                    msg.appendLine("ACTION_SMS_RECEIVED - Error writing to MUC: " + e);
                    msg.appendBold(getString(R.string.chat_sms_from, name));
                    msg.append(message);
                    Log.w("Sending message from " + number + " via MUC failed, message will be send as chat message");
                    sXmppMgr.send(msg, null);
                }
            }
            sWl.release(); 
        } else if (action.equals(ACTION_COMMAND)) {
            String cmd = intent.getStringExtra("cmd");
            if (cmd != null) {
                String args = intent.getStringExtra("args");
                // from can be a regular user JID with or without resource part or a MUC,
                String from = intent.getStringExtra("from");
                // Send to the notification address (from = null) if the command is from a MUC
                // and we don't want to be notified about status messages in MUCs
                if (intent.getBooleanExtra("fromMuc", false) && !sSettingsMgr.notifyInMuc) {
                    from = null;
                }
                executeCommand(cmd, args, from);
            } else {
                Log.w("Intent " + MainService.ACTION_COMMAND + " without extra cmd");
            }
        } else {
            Log.w("Unexpected intent: " + action);
        }
        Log.i("handled action '" + action + "' - state now: " + sXmppMgr.statusString());
    }

    public int getConnectionStatus() {
        return sXmppMgr == null ? XmppManager.DISCONNECTED : sXmppMgr.getConnectionStatus();
    }

    public String getConnectionStatusAction() {
        return sXmppMgr == null ? "" : sXmppMgr.getConnectionStatusAction();
    }
    
    public static Set<CommandHandlerBase> getCommandHandlersSet() {
        if (sIntance != null && sIntance.mCommandManager != null) {
            return sIntance.mCommandManager.getCommandHandlerSet();
        }
        return null;
    }

    public static Map<String, CommandHandlerBase> getCommandHandlersMap() {
        if (sIntance != null && sIntance.mCommandManager != null) {
            return sIntance.mCommandManager.getCommandHandlersMap();
        }
        return null;
    }

    public boolean getTLSStatus() {
        // null check necessary
        return sXmppMgr != null && sXmppMgr.getTLSStatus();
    }

    public boolean getCompressionStatus() {
        // null check necessary
        return sXmppMgr != null && sXmppMgr.getCompressionStatus();
    }
    
    public PingManager getPingManager() {
        return sXmppMgr == null ? null : sXmppMgr.getPingManger();
    }

    public void updateBuddies() {
        if (sXmppMgr != null) {
            XmppBuddies.getInstance(this).retrieveFriendList();
        }
    }

    /**
     * Class for clients to access. Because we know this service always runs in
     * the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public MainService getService() {
            return MainService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sIntance = this;

        NetworkConnectivityReceiver.setLastActiveNetworkName(this);
        
        sPm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        sWl = sPm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Tools.APP_NAME + " WakeLock");

        sSettingsMgr = SettingsManager.getSettingsManager(this);
        Log.initialize(sSettingsMgr);
        Tools.setLocale(sSettingsMgr, this);

        // Start a new thread for the service
        HandlerThread thread = new HandlerThread(SERVICE_THREAD_NAME);
        thread.start();
        mHandlerThreadId = thread.getId();
        sServiceLooper = thread.getLooper();
        sServiceHandler = new ServiceHandler(sServiceLooper);
        sDelayedDisconnectHandler = new Handler(sServiceLooper);

        sUiContext = this;

        // Setup the stop ringing intent
        Intent intent = new Intent(MainService.ACTION_COMMAND);
        intent.putExtra("cmd", "ring");
        intent.putExtra("args", "stop");
        intent.setClass(getBaseContext(), MainService.class);
        sPendingIntentStopRinging = PendingIntent.getService(getBaseContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        sPendingIntentLaunchApplication = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
        sNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        mCommandManager = new CommandManager();
        
        Log.i("onCreate(): service thread created - IsRunning is set to true");
        IsRunning = true;

        // it seems that with gingerbread android doesn't issue null intents any
        // more when restarting a service but only calls the service's onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            int lastStatus = XmppStatus.getInstance(this).getLastKnowState();
            int currentStatus = (sXmppMgr == null) ? XmppManager.DISCONNECTED : sXmppMgr.getConnectionStatus();
            if (lastStatus != currentStatus && lastStatus != XmppManager.DISCONNECTING) {
                Log.i("onCreate(): issuing connect intent because we are on gingerbread (or higher). " + "lastStatus is " + lastStatus + " and currentStatus is " + currentStatus);
                startService(new Intent(MainService.ACTION_CONNECT));
                CrashedStartCounter.getInstance(this).count();
            }
        }
        
        PublicIntentReceiver.initReceiver(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            // The application has been killed by Android and
            // we try to restart the connection
            // this null intent behavior is only for SDK < 9
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
                CrashedStartCounter.getInstance(this).count();
                startService(new Intent(MainService.ACTION_CONNECT));
            } else {
                Log.w("onStartCommand() null intent with Gingerbread or higher");
            }
            return START_STICKY;
        }
        Log.i("onStartCommand(): Intent " + intent.getAction());
        // A special case for the 'broadcast status' intent - we avoid setting
        // up the _xmppMgr etc
        if (intent.getAction().equals(ACTION_BROADCAST_STATUS)) {
            // A request to broadcast our current status even if _xmpp is null.
            int state = getConnectionStatus();
            XmppManager.broadcastStatus(this, state, state, getConnectionStatusAction());
            // A real action request
        } else {
            // redirect the intent to the service handler thread
            sendToServiceHandler(startId, intent);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i("MainService onDestroy(): IsRunning is set to false");
        PublicIntentReceiver.onServiceStop();
        IsRunning = false;
        // If the _xmppManager is non-null, then our service was "started" (as
        // opposed to simply "created" - so tell the user it has stopped.
        if (sXmppMgr != null) {
            // do some cleanup
            unregisterReceiver(sXmppConChangedReceiver);
            sXmppConChangedReceiver = null;

            unregisterReceiver(sStorageLowReceiver);
            sStorageLowReceiver = null;

            sXmppMgr.xmppRequestStateChange(XmppManager.DISCONNECTED);
            sXmppMgr.mSmackAndroid.onDestroy();
            sXmppMgr = null;
        }

        // All data must be cleaned, because onDestroy can be call without releasing the current object
        // It's due to BIND_AUTO_CREATE used for Service Binder
        // http://developer.android.com/reference/android/content/Context.html#stopService(android.content.Intent)
        if (mCommandManager != null) {
            mCommandManager.stopCommands();
            mCommandManager.cleanupCommands();
            mCommandManager = null;
        }
        
        sServiceLooper.quit();
        sIntance = null;

        super.onDestroy();
        Log.i("MainService onDestroy(): service destroyed");
    }

    /**
     * Wrapper for send(XmppMsg msg... method
     * 
     * @param msg
     * @param to
     *            The receiving JID, if null the default notification address is
     *            used
     */
    public void send(String msg, String to) {
        send(new XmppMsg(msg), to);
    }

    /**
     * Sends an XmppMsg to the specified JID or to the default notification
     * address
     * 
     * @param msg
     * @param to
     *            - the receiving jid. if null the default notification address
     *            is used
     */
    public void send(XmppMsg msg, String to) {
        if (sXmppMgr != null) {
            sXmppMgr.send(msg, to);
        } else {
            Log.w("MainService send XmppMsg: _xmppMgr == null");
        }
    }

    public void setKeyboard(KeyboardInputMethodService keyboard) {
        sKeyboardInputMethodService = keyboard;
    }

    public KeyboardInputMethodService getKeyboard() {
        return sKeyboardInputMethodService;
    }

    /**
     * Provides a clean way to display toast messages that don't get stuck
     * 
     * @param text
     *            The Text to show as toast
     * @param extraInfo can be null
     * @param showPrefix show the app name as prefix to the toast message
     */
    public static void displayToast(String text, String extraInfo, boolean showPrefix) {
        sToastHandler.post(new DisplayToast(text, extraInfo, sUiContext, showPrefix));
    }

    /**
     * Display a string resource i as toast messages
     * 
     * @param i
     *            The resource ID of the string to show as toast
     * @param extraInfo
     *            can be null
     */
    public static void displayToast(int i, String extraInfo) {
        displayToast(sUiContext.getString(i), extraInfo, true);
    }

    /**
     * Does an initial one-time setup on the MainService by - Creating a
     * XmppManager Instance - Registering the commands - Registering a Listener
     * for ACTION_XMPP_CONNECTION_CHANGED which is issued by XmppManager for
     * every state change of the XMPP connection
     */
    private void setupXmppManagerAndCommands() {
        sXmppConChangedReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                intent.setClass(MainService.this, MainService.class);
                onConnectionStatusChanged(intent.getIntExtra("old_state", 0), intent.getIntExtra("new_state", 0));
                startService(intent);
            }
        };
        IntentFilter intentFilter = new IntentFilter(ACTION_XMPP_CONNECTION_CHANGED);
        registerReceiver(sXmppConChangedReceiver, intentFilter);

        sStorageLowReceiver = new StorageLowReceiver();
        intentFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
        registerReceiver(sStorageLowReceiver, intentFilter);

        mCommandManager.setupCommands(this);
        sXmppMgr = XmppManager.getInstance(this);
    }

    private void executeCommand(String cmd, String args, String answerTo) {
        assert (cmd != null);
        cmd = cmd.toLowerCase();

        CommandHandlerBase exec = mCommandManager.getCommandHandler(cmd);
        if (exec != null) {
            Log.d("MainService executing command: \"" + cmd + ":" + Tools.shortenMessage(args) + "\"");
            try {
                Cmd execCmd = exec.getCommand(cmd);
                if (execCmd != null && execCmd.isActive()) {
                    exec.execute(cmd, args == null ? "" : args, answerTo);
                } else {
                    send(getString(R.string.chat_command_disabled), answerTo);
                }
            } catch (Exception e) {
                String error = cmd + ":" + args + " Exception: " + e.getLocalizedMessage();
                String chatError = getString(R.string.chat_error, error);

                Log.e("executeCommand() Exception", e);

                // Display the user detailed information about the exception if debugLog is enabled
                if (sSettingsMgr.debugLog) {
                    XmppMsg msg = new XmppMsg();
                    msg.appendBoldLine(chatError);
                    msg.append(Tools.STMArrayToString(e.getStackTrace()));
                    send(msg, answerTo);
                } else {
                    send(chatError, answerTo);
                }
            }
        } else if (cmd.equals("stop")) {
            send(getString(R.string.chat_stop_actions), answerTo);
            mCommandManager.stopCommands();
        } else {
            send(getString(R.string.chat_error_unknown_cmd, cmd), answerTo);
        }
    }

    private int getImageStatus(int color) {
        String index = sSettingsMgr.displayIconIndex;
        int res = 0;
        try {
            switch (color) {
                case STATUS_ICON_GREEN:
                    res = R.drawable.class.getField("status_green_" + index).getInt(null);
                    break;
                case STATUS_ICON_ORANGE:
                    res = R.drawable.class.getField("status_orange_" + index).getInt(null);
                    break;
                case STATUS_ICON_RED:
                    res = R.drawable.class.getField("status_red_" + index).getInt(null);
                    break;
                case STATUS_ICON_BLUE:
                    res = R.drawable.class.getField("status_blue_" + index).getInt(null);
                    break;
            }
        } catch (Exception e) {
            Log.e("Failed to retrieve Image Status: color=" + color + ", index=" + index + ". Ex: ", e);
        }

        return res;
    }

    /**
     * Displays an notification in the status bar to send the command ring:stop
     */
    public void displayRingingNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setWhen(System.currentTimeMillis());
        builder.setContentTitle(Tools.APP_NAME);
        builder.setContentText(getString(R.string.main_service_notification_stop_ringing));
        builder.setSmallIcon(R.drawable.ring_0);
        builder.setContentIntent(sPendingIntentStopRinging);
        builder.setOngoing(true);
        
        sNotificationManager.notify(NOTIFICATION_STOP_RINGING, builder.getNotification());
    }
    
    /**
     * Hides the stop ringing notification
     */
    public void hideRingingNotification() {
        sNotificationManager.cancel(NOTIFICATION_STOP_RINGING);
    }
    
    /** Updates the status about the service state (and the status bar) */
    private void onConnectionStatusChanged(int oldStatus, int status) {
        if (sSettingsMgr.showStatusIcon) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder.setWhen(System.currentTimeMillis());
            
            switch (status) {
                case XmppManager.CONNECTED:
                    builder.setContentText(getString(R.string.main_service_connected, getConnectionStatusAction()));
                    builder.setSmallIcon(getImageStatus(STATUS_ICON_GREEN));
                    break;
                case XmppManager.CONNECTING:
                    builder.setContentText(getString(R.string.main_service_connecting));
                    builder.setSmallIcon(getImageStatus(STATUS_ICON_ORANGE));
                    break;
                case XmppManager.DISCONNECTED:
                    builder.setContentText(getString(R.string.main_service_disconnected));
                    builder.setSmallIcon(getImageStatus(STATUS_ICON_RED));
                    break;
                case XmppManager.DISCONNECTING:
                    builder.setContentText(getString(R.string.main_service_disconnecting));
                    builder.setSmallIcon(getImageStatus(STATUS_ICON_ORANGE));
                    break;
                case XmppManager.WAITING_TO_CONNECT:
                case XmppManager.WAITING_FOR_NETWORK:
                    builder.setContentText(getString(R.string.main_service_waiting_to_connect) + "\n" + getConnectionStatusAction());
                    builder.setSmallIcon(getImageStatus(STATUS_ICON_BLUE));
                    break;
                default:
                    return;
            }
            builder.setContentIntent(sPendingIntentLaunchApplication);
            builder.setContentTitle(Tools.APP_NAME);

            startForeground(NOTIFICATION_CONNECTION, builder.getNotification());
        }
    }

    /**
     * Handles the different commands that came with the XMPP connection usually
     * from an intent with ACTION_XMPP_MESSAGE_RECEIVED
     * 
     * @param commandLine
     */
    private void handleCommandFromXMPP(String commandLine, String from) {
        String command;
        String args;

        // Split the command and args from the commandLine String and trim these
        if (commandLine.contains(":")) {
            command = commandLine.substring(0, commandLine.indexOf(":")).trim();
            args = commandLine.substring(commandLine.indexOf(":") + 1);
        } else {
            command = commandLine.trim();
            args = "";
        }

        executeCommand(command, args, from);
    }

    private int updateListenersToCurrentState(int currentState) {
        boolean wantListeners;
        switch (currentState) {
            case XmppManager.CONNECTED:
            case XmppManager.CONNECTING:
            case XmppManager.DISCONNECTING:
            case XmppManager.WAITING_TO_CONNECT:
            case XmppManager.WAITING_FOR_NETWORK:
                wantListeners = true;
                break;
            case XmppManager.DISCONNECTED:
                wantListeners = false;
                break;
            default:
                throw new IllegalStateException("updateListeners found invalid  int: " + currentState);
        }

        if (wantListeners && !sListenersActive) {
            Log.i("setupListenersForConnection()");
            mCommandManager.updateAndReturnStatus();
            sListenersActive = true;
        } else if (!wantListeners) {
            Log.i("tearDownListenersForConnection()");
            mCommandManager.stopCommands();
            mCommandManager.cleanupCommands();
            sListenersActive = false;
        }

        return currentState;
    }

    public static void updateCommandState() {
        if (sIntance != null && sIntance.mCommandManager != null) {
            sIntance.mCommandManager.updateCommandState();
        }
    }

    public static Looper getServiceLooper() {
        return sServiceLooper;
    }

    public static Handler getDelayedDisconnectHandler() {
        return sDelayedDisconnectHandler;
    }
    
    public static boolean sendToServiceHandler(int i, Intent intent) {
        if (sServiceHandler != null) {
            Message msg = sServiceHandler.obtainMessage();
            msg.arg1 = i;
            msg.obj = intent;
            sServiceHandler.sendMessage(msg);
            return true;
        } else {
            Log.w("sendToServiceHandler() called with " + intent.getAction() + " when service handler is null");
            return false;
        }
    }

    public static boolean sendToServiceHandler(Intent intent) {
        return sendToServiceHandler(0, intent);
    }

    public static void maybeAcquireWakeLock() {
        if (!sWl.isHeld()) {
            sWl.acquire();
        }
    }
}
