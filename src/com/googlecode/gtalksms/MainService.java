package com.googlecode.gtalksms;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.ClipboardManager;
import android.util.Log;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.googlecode.gtalksms.data.contacts.Contact;
import com.googlecode.gtalksms.data.contacts.ContactAddress;
import com.googlecode.gtalksms.data.contacts.ContactsManager;
import com.googlecode.gtalksms.data.phone.Call;
import com.googlecode.gtalksms.data.phone.Phone;
import com.googlecode.gtalksms.data.phone.PhoneManager;
import com.googlecode.gtalksms.data.sms.Sms;
import com.googlecode.gtalksms.data.sms.SmsMmsManager;
import com.googlecode.gtalksms.geo.GeoManager;
import com.googlecode.gtalksms.panels.MainScreen;
import com.googlecode.gtalksms.panels.Preferences;
import com.googlecode.gtalksms.receivers.PhoneCallListener;
import com.googlecode.gtalksms.tools.Tools;

public class MainService extends Service {

    // The following actions are documented and registered in our manifest
    public final static String ACTION_CONNECT = "com.googlecode.gtalksms.action.CONNECT";
    public final static String ACTION_TOGGLE = "com.googlecode.gtalksms.action.TOGGLE";
    public final static String ACTION_SEND = "com.googlecode.gtalksms.action.SEND";
    // The following actions are undocumented and internal to our implementation.
    public final static String ACTION_BROADCAST_STATUS = "com.googlecode.gtalksms.action.BROADCAST_STATUS";
    public final static String ACTION_SMS_RECEIVED = "com.googlecode.gtalksms.action.SMS_RECEIVED";
    public final static String ACTION_NETWORK_CHANGED = "com.googlecode.gtalksms.action.NETWORK_CHANGED";

    // A bit of a hack to allow global receivers to know whether or not
    // the service is running, and therefore whether to tell the service
    // about some events
    public static boolean running = false;
    private SettingsManager _settingsMgr = new SettingsManager();

    private MediaManager _mediaMgr;
    private XmppManager _xmppMgr;
    private BroadcastReceiver _xmppreceiver;
    private SmsMmsManager _smsMgr;
    private PhoneManager _phoneMgr;
    private GeoManager _geoMgr;

    private BatteryMonitor _batteryMonitor;
    private SmsMonitor _smsMonitor;
    
    private PhoneCallListener _phoneListener = null;

    public KeyboardInputMethod _keyboard;
    
    // last person who sent sms/who we sent an sms to
    public String _lastRecipient = null;
    
    private boolean _hasOutgoingAction = false;

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

    GoogleAnalyticsTracker _gAnalytics;

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
        String a = intent.getAction();
        Log.d(Tools.LOG_TAG, "handling action '" + a + "' while in state " + getConnectionStatus());
        if (a.equals(ACTION_CONNECT)) {
            if (intent.getBooleanExtra("disconnect", false)) {
                // request to disconnect.
                xmppStop();
            } else {
                // a simple 'connect' request.
                if (_xmppMgr == null) {
                    xmppStart();
                }
            }
        } else if (a.equals(ACTION_TOGGLE)) {
            switch (getConnectionStatus()) {
            case XmppManager.CONNECTED:
            case XmppManager.WAITING_TO_CONNECT:
                xmppStop();
                assert getConnectionStatus() == XmppManager.DISCONNECTED;
                break;
            case XmppManager.DISCONNECTED:
                xmppStart();
                break;
            default:
                Log.e(Tools.LOG_TAG, "Invalid xmpp state: "+ getConnectionStatus());
                break;
            }
        } else if (a.equals(ACTION_BROADCAST_STATUS)) {
            // A request to broadcast our current status.
            int state = _xmppMgr == null ? XmppManager.DISCONNECTED : _xmppMgr.getConnectionStatus();
            XmppManager.broadcastStatus(this, state, state);
        } else if (a.equals(ACTION_SEND)) {
            if (getConnectionStatus() == XmppManager.CONNECTED) {
                _xmppMgr.send(intent.getStringExtra("message"));
            }
        } else if (a.equals(ACTION_SMS_RECEIVED)) {
            if (getConnectionStatus() == XmppManager.CONNECTED) {
                String number = intent.getStringExtra("sender");
                String sender = makeBold(getString(R.string.chat_sms_from, 
                        ContactsManager.getContactName(this, number)));
                _xmppMgr.send(sender + intent.getStringExtra("message")); 
                setLastRecipient(number);
            }
        } else if (a.equals(ACTION_NETWORK_CHANGED)) {
            boolean available = intent.getBooleanExtra("available", true);
            Log.d(Tools.LOG_TAG, "network_changed with available=" + available + 
                                 " and with _xmpp=" + (_xmppMgr != null));

            if (available && getConnectionStatus() != XmppManager.CONNECTED 
                          && getConnectionStatus() != XmppManager.CONNECTING) {
                xmppStop(XmppManager.WAITING_TO_CONNECT);
                xmppStart();
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
        HandlerThread thread = new HandlerThread("GTalkSMS.Service");
        thread.start();
        _serviceLooper = thread.getLooper();
        _serviceHandler = new ServiceHandler(_serviceLooper);
        initNotificationStuff();
        Log.i(Tools.LOG_TAG, "service created");
        running = true;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Message msg = _serviceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        _serviceHandler.sendMessage(msg);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return START_STICKY;
    }

    private void xmppStart() {
        _gAnalytics = GoogleAnalyticsTracker.getInstance();
        _gAnalytics.setProductVersion(
                Tools.getVersion(getBaseContext(), getClass()), 
                Tools.getVersionCode(getBaseContext(), getClass()));
        _gAnalytics.start("UA-20245441-1", this);
        _gAnalytics.trackEvent(
                "GTalkSMS",  // Category
                "Service",  // Action
                "Start " + Tools.getVersionName(getBaseContext(), getClass()), // Label
                0);       // Value      
        _gAnalytics.dispatch();
        
        _settingsMgr.importPreferences(getBaseContext());
        if (_settingsMgr.mTo == null || _settingsMgr.mTo.equals("") || _settingsMgr.mTo.equals("your.login@gmail.com")) {
            Log.i(Tools.LOG_TAG, "Preferences not set! Opens preferences page.");
            Intent settingsActivity = new Intent(getBaseContext(), Preferences.class);
            settingsActivity.putExtra("panel", R.xml.prefs_connection);
            settingsActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(settingsActivity);
            return;
        }

        _mediaMgr = new MediaManager(_settingsMgr, getBaseContext());
        _geoMgr = new GeoManager(_settingsMgr, getBaseContext());
        _smsMgr = new SmsMmsManager(_settingsMgr, getBaseContext());
        _phoneMgr = new PhoneManager(_settingsMgr, getBaseContext());
        _batteryMonitor = new BatteryMonitor(_settingsMgr, getBaseContext()) {
            void sendBatteryInfos(int level, boolean force) {
                if (force || (_settings.notifyBattery && level % _settings.batteryNotificationInterval == 0)) {
                    send(getString(R.string.chat_battery_level, level));
                }
                if (_settings.notifyBatteryInStatus && _xmppMgr != null) {
                    _xmppMgr.setStatus(level);
                }
            }
        };
        
        _smsMonitor = new SmsMonitor(_settingsMgr, getBaseContext()) {
            void sendSmsStatus(String message) {
                send(message);
            }
        };

        if (_settingsMgr.notifyIncomingCalls) {
            _phoneListener = new PhoneCallListener(this);
            TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            telephony.listen(_phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
            
        _mediaMgr.initMediaPlayer();

        _xmppreceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(XmppManager.ACTION_MESSAGE_RECEIVED)) {
                    onMessageReceived(intent.getStringExtra("message"));
                } else if (action.equals(XmppManager.ACTION_CONNECTION_CHANGED)) {
                    onConnectionStatusChanged(intent.getIntExtra("old_state", 0),
                                              intent.getIntExtra("new_state", 0));
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter(XmppManager.ACTION_MESSAGE_RECEIVED);
        intentFilter.addAction(XmppManager.ACTION_CONNECTION_CHANGED);
        registerReceiver(_xmppreceiver, intentFilter);
        if (_xmppMgr == null) {
            _xmppMgr = new XmppManager(_settingsMgr, getBaseContext());
        }
        _xmppMgr.start();
    }

    @Override
    public void onDestroy() {
        Log.i(Tools.LOG_TAG, "service destroyed");
        running = false;
        xmppStop();
        _serviceLooper.quit();
        super.onDestroy();
    }
    
    private void xmppStop() {
        xmppStop(XmppManager.DISCONNECTED);
    }

    private void xmppStop(int finalState) {
        if (_gAnalytics != null) {
            _gAnalytics.stop();
            _gAnalytics = null;
        }
        
        stopNotifications();
        if (_xmppMgr != null) {
            Tools.toastMessage(this, getString(R.string.main_service_stop));
            _xmppMgr.stop();
            _xmppMgr = null;
            // If we have been asked to go into some state other than
            // disconnected, create the new _xmppMgr now with that state.
            if (finalState == XmppManager.WAITING_TO_CONNECT) {
                _xmppMgr = new XmppManager(_settingsMgr, getBaseContext());
                _xmppMgr.start(finalState);
            }
        }
        if (_xmppreceiver != null) {
            unregisterReceiver(_xmppreceiver);
            _xmppreceiver = null;
        }

        stopForegroundCompat(XmppManager.DISCONNECTED);

        if (_geoMgr != null) {
            _geoMgr.stopLocatingPhone();
            _geoMgr = null;
        }
        if (_mediaMgr != null) {
            _mediaMgr.clearMediaPlayer();
            _mediaMgr = null;
        }
        if (_smsMonitor != null) {
            _smsMonitor.clearSmsMonitor();
            _smsMonitor = null;
        }
        if (_phoneListener != null) {
            TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            telephony.listen(_phoneListener, 0);
            _phoneListener = null;
        }

        if (_batteryMonitor != null) {
            _batteryMonitor.clearBatteryMonitor();
            _batteryMonitor = null;
        }
    }

    public static void send(Context ctx, String msg) {
        ctx.startService(newSvcIntent(ctx, ACTION_SEND, msg));
    }

    public void send(String msg) {
        send(this, msg);
    }

    /** handles the different commands */
    public void onMessageReceived(String commandLine) {
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

            if (command.equals("?")) {
                showHelp();
            } else if (command.equals("exit")) {
                stopService(new Intent(ACTION_CONNECT));
            } else if (command.equals("sms")) {
                int separatorPos = args.indexOf(":");
                String contact = null;
                String message = null;
                if (-1 != separatorPos) {
                    contact = args.substring(0, separatorPos);
                    message = args.substring(separatorPos + 1);
                    sendSMS(message, contact);
                } else if (args.length() > 0) {
                    readSMS(args);
                } else {
                    readLastSMS();
                }
            } else if (command.equals("reply")) {
                if (args.length() == 0) {
                    displayLastRecipient();
                } else if (_lastRecipient == null) {
                    send(getString(R.string.chat_error_no_recipient));
                } else {
                    _smsMgr.markAsRead(_lastRecipient);
                    sendSMS(args, _lastRecipient);
                }
            } else if (command.equals("markasread") || command.equals("mar")) {
                if (args.length() > 0) {
                    markSmsAsRead(args);
                } else if (_lastRecipient == null) {
                    send(getString(R.string.chat_error_no_recipient));
                } else {
                    markSmsAsRead(_lastRecipient);
                }
            } else if (command.equals("calls")) {
                readCallLogs();
            } else if (command.equals("battery") || command.equals("batt")) {
                _batteryMonitor.sendBatteryInfos(true);
            } else if (command.equals("copy")) {
                if (args.length() > 0) {
                    copyToClipboard(args);
                } else {
                    sendClipboard();
                }
            } else if (command.equals("write") || command.equals("w")) {
                writeText(args);
            } else if (command.equals("geo")) {
                geo(args);
            } else if (command.equals("dial")) {
                dial(args);
            } else if (command.equals("contact")) {
                displayContacts(args);
            } else if (command.equals("where")) {
                geoLocate();
            } else if (command.equals("stop")) {
                stopNotifications();
            } else if (command.equals("ring")) {
                ring();
            } else if (command.equals("cmd")) {
                shellCmd(args);
            } else if (command.equals("http")) {
                openLink("http:" + args);
            } else if (command.equals("https")) {
                openLink("https:" + args);
            } else {
                send(getString(R.string.chat_error_unknown_cmd, commandLine));
            }
        } catch (Exception ex) {
            send(getString(R.string.chat_error, ex));
        }
    }

    private void setLastRecipient(String phoneNumber) {
        if (_lastRecipient == null || !phoneNumber.equals(_lastRecipient)) {
            _lastRecipient = phoneNumber;
            displayLastRecipient();
        }
    }

    private boolean askRootAccess() {
        try {
            Process p = Runtime.getRuntime().exec("su");

            // Attempt to write a file to a root-only
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("echo \"Do I have root?\" >/system/sd/temporary.txt\n");
            os.writeBytes("exit\n");
            os.flush();
            p.waitFor();
            if (p.exitValue() != 255) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void shellCmd(String cmd) {
        final StringBuilder sb = new StringBuilder();
        sb.append(cmd);
        sb.append(Tools.LineSep);
        
        if (!askRootAccess()) {
            sb.append(getString(R.string.chat_error_root) + Tools.LineSep);
        }

        Process myproc = null;
        BufferedReader reader = null;
        try {
            myproc = Runtime.getRuntime().exec(new String[] {"/system/bin/sh", "-c", cmd});
           
            String line;
            reader = new BufferedReader(new InputStreamReader(myproc.getInputStream()));
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append(Tools.LineSep);
            }
            reader = new BufferedReader(new InputStreamReader(myproc.getErrorStream()));
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append(Tools.LineSep);
            }
            send(sb.toString());
        }
        catch (Exception ex) {
            Log.w(Tools.LOG_TAG, "Shell command error", ex);
        }
    }
    
    public void displayLastRecipient() {
        if (_lastRecipient == null) {
            send(getString(R.string.chat_error_no_recipient));
        } else {
            String contact = ContactsManager.getContactName(this, _lastRecipient);
            if (Phone.isCellPhoneNumber(_lastRecipient) && contact.compareTo(_lastRecipient) != 0) {
                contact += " (" + _lastRecipient + ")";
            }
            send(getString(R.string.chat_reply_contact, contact));
        }
    }

    public String makeBold(String in) {
        if (_settingsMgr.formatChatResponses) {
            return " *" + in + "* ";
        }
        return in;
    }

    public String makeItalic(String in) {
        if (_settingsMgr.formatChatResponses) {
            return " _" + in + "_ ";
        }
        return in;
    }

    public void showHelp() {
        StringBuilder builder = new StringBuilder();
        builder.append(getString(R.string.chat_help_title)).append(Tools.LineSep);
        builder.append(getString(R.string.chat_help_help, makeBold("\"?\""))).append(Tools.LineSep);
        builder.append(getString(R.string.chat_help_stop, makeBold("\"exit\""))).append(Tools.LineSep);
        builder.append(getString(R.string.chat_help_dial, makeBold("\"dial:#contact#\""))).append(Tools.LineSep);
        builder.append(getString(R.string.chat_help_sms_reply, makeBold("\"reply:#message#\""))).append(Tools.LineSep);
        builder.append(getString(R.string.chat_help_sms_show_all, makeBold("\"sms\""))).append(Tools.LineSep);
        builder.append(getString(R.string.chat_help_sms_show_contact, makeBold("\"sms:#contact#\""))).append(Tools.LineSep);
        builder.append(getString(R.string.chat_help_sms_send, makeBold("\"sms:#contact#:#message#\""))).append(Tools.LineSep);
        builder.append(getString(R.string.chat_help_mark_as_read, makeBold("\"markAsRead:#contact#\""), makeBold("\"mar\""))).append(Tools.LineSep);
        builder.append(getString(R.string.chat_help_battery, makeBold("\"battery\""), makeBold("\"batt\""))).append(Tools.LineSep);
        builder.append(getString(R.string.chat_help_calls, makeBold("\"calls\""))).append(Tools.LineSep);
        builder.append(getString(R.string.chat_help_contact, makeBold("\"contact:#contact#\""))).append(Tools.LineSep);
        builder.append(getString(R.string.chat_help_geo, makeBold("\"geo:#address#\""))).append(Tools.LineSep);
        builder.append(getString(R.string.chat_help_where, makeBold("\"where\""))).append(Tools.LineSep);
        builder.append(getString(R.string.chat_help_ring, makeBold("\"ring\""))).append(Tools.LineSep);
        builder.append(getString(R.string.chat_help_copy, makeBold("\"copy:#text#\""))).append(Tools.LineSep);
        builder.append(getString(R.string.chat_help_cmd, makeBold("\"cmd:#command#\""))).append(Tools.LineSep);
        builder.append(getString(R.string.chat_help_write, makeBold("\"write:#text#\""), makeBold("\"w:#text#\""))).append(Tools.LineSep);
        builder.append(getString(R.string.chat_help_urls)).append(Tools.LineSep);
        send(builder.toString());
    }

    public void geoLocate() {
        _hasOutgoingAction = true;
        send(getString(R.string.chat_start_locating));
        _geoMgr.startLocatingPhone();
    }

    public void ring() {
        _hasOutgoingAction = true;
        send(getString(R.string.chat_start_ringing));
        if (!_mediaMgr.ring()) {
            send(getString(R.string.chat_error_ringing));
        }
    }

    public void stopNotifications() {
        if (_hasOutgoingAction) {
            send(getString(R.string.chat_stop_actions));
        }
        _hasOutgoingAction = false;
        if (_geoMgr != null) {
            _geoMgr.stopLocatingPhone();
        }
        if (_mediaMgr != null) {
            _mediaMgr.stopRinging();
        }
    }

    /** sends a SMS to the last contact */
    public void sendSMS(String message) {
        sendSMS(message, _lastRecipient);
    }

    /** sends a SMS to the specified contact */
    public void sendSMS(String message, String contact) {
        if (Phone.isCellPhoneNumber(contact)) {
            send(getString(R.string.chat_send_sms, ContactsManager.getContactName(this, contact)));
            sendSMSByPhoneNumber(message, contact);
        } else {
            ArrayList<Phone> mobilePhones = ContactsManager.getMobilePhones(this, contact);
            if (mobilePhones.size() > 1) {
                send(getString(R.string.chat_specify_details));

                for (Phone phone : mobilePhones) {
                    send(phone.contactName + " - " + phone.cleanNumber);
                }
            } else if (mobilePhones.size() == 1) {
                Phone phone = mobilePhones.get(0);
                send(getString(R.string.chat_send_sms, phone.contactName + " (" + phone.cleanNumber + ")"));
                setLastRecipient(phone.cleanNumber);
                sendSMSByPhoneNumber(message, phone.cleanNumber);
            } else {
                send(getString(R.string.chat_no_match_for, contact));
            }
        }
    }
    
    /** Sends a sms to the specified phone number */
    public void sendSMSByPhoneNumber(String message, String phoneNumber) {

        _smsMonitor.sendSMSByPhoneNumber(message, phoneNumber);
        _smsMgr.addSmsToSentBox(message, phoneNumber);
    }

    public void markSmsAsRead(String contact) {

        if (Phone.isCellPhoneNumber(contact)) {
            send(getString(R.string.chat_mark_as_read, ContactsManager.getContactName(this, contact)));
            _smsMgr.markAsRead(contact);
        } else {
            ArrayList<Phone> mobilePhones = ContactsManager.getMobilePhones(this, contact);
            if (mobilePhones.size() > 0) {
                send(getString(R.string.chat_mark_as_read, mobilePhones.get(0).contactName));

                for (Phone phone : mobilePhones) {
                    _smsMgr.markAsRead(phone.number);
                }
            } else {
                send(getString(R.string.chat_no_match_for, contact));
            }
        }
    }

    /** reads (count) SMS from all contacts matching pattern */
    public void readSMS(String searchedText) {

        ArrayList<Contact> contacts = ContactsManager.getMatchingContacts(this, searchedText);
        ArrayList<Sms> sentSms = new ArrayList<Sms>();
        if (_settingsMgr.displaySentSms) {
            sentSms = _smsMgr.getAllSentSms();
        }

        if (contacts.size() > 0) {

            StringBuilder noSms = new StringBuilder();
            Boolean hasMatch = false;
            for (Contact contact : contacts) {
                ArrayList<Sms> smsArrayList = _smsMgr.getSms(contact.rawIds, contact.name);
                if (_settingsMgr.displaySentSms) {
                    smsArrayList.addAll(_smsMgr.getSentSms(ContactsManager.getPhones(this, contact.id), sentSms));
                }
                Collections.sort(smsArrayList);

                List<Sms> smsList = Tools.getLastElements(smsArrayList, _settingsMgr.smsNumber);
                if (smsList.size() > 0) {
                    hasMatch = true;
                    StringBuilder smsContact = new StringBuilder();
                    smsContact.append(makeBold(contact.name));
                    for (Sms sms : smsList) {
                        smsContact.append(Tools.LineSep + makeItalic(sms.date.toLocaleString() + " - " + sms.sender));
                        smsContact.append(Tools.LineSep + sms.message);
                    }
                    if (smsList.size() < _settingsMgr.smsNumber) {
                        smsContact.append(Tools.LineSep + makeItalic(getString(R.string.chat_only_got_n_sms, smsList.size())));
                    }
                    send(smsContact.toString() + Tools.LineSep);
                } else {
                    noSms.append(makeBold(contact.name) + getString(R.string.chat_no_sms) + Tools.LineSep);
                }
            }
            if (!hasMatch) {
                send(noSms.toString());
            }
        } else {
            send(getString(R.string.chat_no_match_for, searchedText));
        }
    }

    /** reads last (count) SMS from all contacts */
    public void readLastSMS() {

        ArrayList<Sms> smsArrayList = _smsMgr.getAllReceivedSms();
        StringBuilder allSms = new StringBuilder();

        if (_settingsMgr.displaySentSms) {
            smsArrayList.addAll(_smsMgr.getAllSentSms());
        }
        Collections.sort(smsArrayList);

        List<Sms> smsList = Tools.getLastElements(smsArrayList, _settingsMgr.smsNumber);
        if (smsList.size() > 0) {
            for (Sms sms : smsList) {
                allSms.append(Tools.LineSep + makeItalic(sms.date.toLocaleString() + " - " + sms.sender));
                allSms.append(Tools.LineSep + sms.message);
            }
        } else {
            allSms.append(getString(R.string.chat_no_sms));
        }
        send(allSms.toString() + Tools.LineSep);
    }

    /** reads last Call Logs from all contacts */
    public void readCallLogs() {

        ArrayList<Call> arrayList = _phoneMgr.getPhoneLogs();
        StringBuilder all = new StringBuilder();

        List<Call> callList = Tools.getLastElements(arrayList, _settingsMgr.callLogsNumber);
        if (callList.size() > 0) {
            for (Call call : callList) {
                String caller = makeBold(ContactsManager.getContactName(this, call.phoneNumber));

                all.append(Tools.LineSep + makeItalic(call.date.toLocaleString()) + " - " + caller);
                // TODO of
                all.append(" - " + call.type(this) + getString(R.string.chat_call_duration) + call.duration());
            }
        } else {
            all.append(getString(R.string.chat_no_call));
        }
        send(all.toString() + Tools.LineSep);
    }

    /** reads (count) SMS from all contacts matching pattern */
    public void displayContacts(String searchedText) {

        ArrayList<Contact> contacts = ContactsManager.getMatchingContacts(this, searchedText);

        if (contacts.size() > 0) {

            if (contacts.size() > 1) {
                send(getString(R.string.chat_contact_found, contacts.size(), searchedText));
            }

            for (Contact contact : contacts) {
                StringBuilder strContact = new StringBuilder();
                strContact.append(makeBold(contact.name));

                // strContact.append(Tools.LineSep + "Id : " + contact.id);
                // strContact.append(Tools.LineSep + "Raw Ids : " + TextUtils.join(" ",
                // contact.rawIds));

                ArrayList<Phone> mobilePhones = ContactsManager.getPhones(this, contact.id);
                if (mobilePhones.size() > 0) {
                    strContact.append(Tools.LineSep + makeItalic(getString(R.string.chat_phones)));
                    for (Phone phone : mobilePhones) {
                        strContact.append(Tools.LineSep + phone.label + " - " + phone.cleanNumber);
                    }
                }

                ArrayList<ContactAddress> emails = ContactsManager.getEmailAddresses(this, contact.id);
                if (emails.size() > 0) {
                    strContact.append(Tools.LineSep + makeItalic(getString(R.string.chat_emails)));
                    for (ContactAddress email : emails) {
                        strContact.append(Tools.LineSep + email.label + " - " + email.address);
                    }
                }

                ArrayList<ContactAddress> addresses = ContactsManager.getPostalAddresses(this, contact.id);
                if (addresses.size() > 0) {
                    strContact.append(Tools.LineSep + makeItalic(getString(R.string.chat_addresses)));
                    for (ContactAddress address : addresses) {
                        strContact.append(Tools.LineSep + address.label + " - " + address.address);
                    }
                }
                send(strContact.toString() + Tools.LineSep);
            }
        } else {
            send(getString(R.string.chat_no_match_for, searchedText));
        }
    }

    /** Open geolocalization application */
    private void geo(String text) {
        List<Address> addresses = _geoMgr.geoDecode(text);
        if (addresses != null) {
            if (addresses.size() > 1) {
                send(getString(R.string.chat_specify_details));
                for (Address address : addresses) {
                    StringBuilder addr = new StringBuilder();
                    for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                        addr.append(address.getAddressLine(i) + Tools.LineSep);
                    }
                    send(addr.toString());
                }
            } else if (addresses.size() == 1) {
                _geoMgr.launchExternal(addresses.get(0).getLatitude() + "," + addresses.get(0).getLongitude());
            }
        } else {
            send(getString(R.string.chat_no_match_for, text));
            // For emulation testing
            // GeoManager.launchExternal("48.833199,2.362232");
        }
    }

    /** copy text as keyboard */
    private void writeText(String text) {
        try {
            if (_keyboard != null) {
                _keyboard.setText(text);
            }
        } catch (Exception ex) {
            Log.w(Tools.LOG_TAG, "writeText error", ex);
        }
    }

    /** copy text to clipboard */
    private void copyToClipboard(String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Service.CLIPBOARD_SERVICE);
            clipboard.setText(text);
            send(getString(R.string.chat_text_copied));
        } catch (Exception ex) {
            Log.w(Tools.LOG_TAG, "Clipboard error", ex);
            send(getString(R.string.chat_error_clipboard));
        }
    }

    /** copy text to clipboard */
    public void sendClipboard() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Service.CLIPBOARD_SERVICE);
            send(getString(R.string.chat_clipboard, clipboard.getText()));
            
        } catch (Exception ex) {
            Log.w(Tools.LOG_TAG, "Clipboard error", ex);
            send(getString(R.string.chat_error_clipboard));
        }
    }

    /** lets the user choose an activity compatible with the url */
    private void openLink(String url) {
        Intent target = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        Intent intent = Intent.createChooser(target, getString(R.string.chat_choose_activity));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /** dial the specified contact */
    private void dial(String contactInfo) {
        String number = null;
        String contact = null;

        if (Phone.isCellPhoneNumber(contactInfo)) {
            number = contactInfo;
            contact = ContactsManager.getContactName(this, number);
        } else {
            ArrayList<Phone> mobilePhones = ContactsManager.getMobilePhones(this, contactInfo);
            if (mobilePhones.size() > 1) {
                send(getString(R.string.chat_specify_details));

                for (Phone phone : mobilePhones) {
                    send(phone.contactName + " - " + phone.cleanNumber);
                }
            } else if (mobilePhones.size() == 1) {
                Phone phone = mobilePhones.get(0);
                contact = phone.contactName;
                number = phone.cleanNumber;
            } else {
                send(getString(R.string.chat_no_match_for, contactInfo));
            }
        }

        if (number != null) {
            send(getString(R.string.chat_dial, contact + " (" + number + ")"));
            if (!_phoneMgr.Dial(number)) {
                send(getString(R.string.chat_error_dial));
            }
        }
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
}
