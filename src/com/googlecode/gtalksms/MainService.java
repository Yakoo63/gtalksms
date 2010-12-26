package com.googlecode.gtalksms;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.location.Address;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.text.ClipboardManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

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
import com.googlecode.gtalksms.receivers.XmppListener;
import com.googlecode.gtalksms.tools.Tools;

public class MainService extends Service implements XmppListener {

    // Service instance
    private static MainService instance = null;

    private SettingsManager _settingsMgr = new SettingsManager();

    private MediaManager _mediaMgr;
    private XmppManager _xmppMgr;
    private XmppListener _xmppListener = null;
    private SmsMmsManager _smsMgr;
    private PhoneManager _phoneMgr;
    private GeoManager _geoMgr;

    private BatteryMonitor _batteryMonitor;
    private SmsMonitor _smsMonitor;
    
    // last person who sent sms/who we sent an sms to
    public String _lastRecipient = null;
    
    private boolean _hasOutgoingAction = false;

    // notification stuff
    @SuppressWarnings("unchecked")
    private static final Class[] mStartForegroundSignature = new Class[] { int.class, Notification.class };
    @SuppressWarnings("unchecked")
    private static final Class[] mStopForegroundSignature = new Class[] { boolean.class };
    private NotificationManager mNM;
    private Method mStartForeground;
    private Method mStopForeground;
    private Object[] mStartForegroundArgs = new Object[2];
    private Object[] mStopForegroundArgs = new Object[1];
    private PendingIntent contentIntent = null;

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    GoogleAnalyticsTracker _gAnalytics;

    
    public void setXmppListener(XmppListener listener) {
        _xmppListener = listener;
    }
    
    public void onPresenceStatusChanged(String person, String status) {
        if (_xmppListener != null) {
            _xmppListener.onPresenceStatusChanged(person, status);
        }
    }
        
    /** Updates the status about the service state (and the status bar) */
    public void onConnectionStatusChanged(int oldStatus, int status) {
        if (_xmppListener != null) {
            _xmppListener.onConnectionStatusChanged(oldStatus, status);
        }
        // Get the layout for the AppWidget and attach an on-click listener to the button
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.appwidget);

        Notification notification = new Notification();
        switch (status) {
            case XmppManager.EXIT:
                onDestroy();
                return;
            case XmppManager.CONNECTED:
                notification = new Notification(R.drawable.status_green, "Connected", System.currentTimeMillis());
                notification.setLatestEventInfo(getApplicationContext(), "GTalkSMS", "Connected", contentIntent);
                views.setImageViewResource(R.id.Button, R.drawable.icon_green);
                break;
            case XmppManager.CONNECTING:
                notification = new Notification(R.drawable.status_orange, "Connecting...", System.currentTimeMillis());
                notification.setLatestEventInfo(getApplicationContext(), "GTalkSMS", "Connecting...", contentIntent);
                views.setImageViewResource(R.id.Button, R.drawable.icon_orange);
                break;
            case XmppManager.DISCONNECTED:
                notification = new Notification(R.drawable.status_red, "Disconnected", System.currentTimeMillis());
                notification.setLatestEventInfo(getApplicationContext(), "GTalkSMS", "Disconnected", contentIntent);
                views.setImageViewResource(R.id.Button, R.drawable.icon_red);
                break;
            case XmppManager.DISCONNECTING:
                notification = new Notification(R.drawable.status_orange, "Disconnecting...", System.currentTimeMillis());
                notification.setLatestEventInfo(getApplicationContext(), "GTalkSMS", "Disconnecting...", contentIntent);
                views.setImageViewResource(R.id.Button, R.drawable.icon_orange);
                break;
            default:
                break;
        }

        // Update all AppWidget with current status
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        ComponentName component = new ComponentName(getBaseContext().getPackageName(), WidgetProvider.class.getName());
        manager.updateAppWidget(manager.getAppWidgetIds(component), views);

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
        if (mStartForeground != null) {
            mStartForegroundArgs[0] = Integer.valueOf(id);
            mStartForegroundArgs[1] = notification;
            try {
                mStartForeground.invoke(this, mStartForegroundArgs);
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
        mNM.notify(id, notification);
    }

    /**
     * This is a wrapper around the stopForeground method, using the older APIs
     * if it is not available.
     */
    void stopForegroundCompat(int id) {
        // If we have the new stopForeground API, then use it.
        if (mStopForeground != null) {
            mStopForegroundArgs[0] = Boolean.TRUE;
            try {
                mStopForeground.invoke(this, mStopForegroundArgs);
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
            mNM.cancel(id);
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
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        try {
            mStartForeground = getClass().getMethod("startForeground", mStartForegroundSignature);
            mStopForeground = getClass().getMethod("stopForeground", mStopForegroundSignature);
        } catch (NoSuchMethodException e) {
            // Running on an older platform.
            mStartForeground = mStopForeground = null;
        }
        contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainScreen.class), 0);
    }

    public boolean isConnected() {
        if (_xmppMgr != null) {
            return _xmppMgr.isConnected();
        }
        
        return false;
    }

    public int getConnectionStatus() {
        if (_xmppMgr != null) {
            return _xmppMgr.getConnectionStatus();
        }
        
        return XmppManager.DISCONNECTED;
    }

    public void stopConnection() {
        stopNotifications();
        _xmppMgr.stop();
    }

    public void startConnection() {
        _xmppMgr.start();
    }
    
    public static MainService getInstance() {
        return instance;
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
        return mBinder;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        
        // Get configuration
        if (instance == null) {
            instance = this;

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
                instance = null;
                return;
            }

            _xmppMgr = new XmppManager(instance, _settingsMgr, getBaseContext());
            _mediaMgr = new MediaManager(_settingsMgr, getBaseContext());
            _geoMgr = new GeoManager(_settingsMgr, getBaseContext());
            _smsMgr = new SmsMmsManager(_settingsMgr, getBaseContext());
            _phoneMgr = new PhoneManager(_settingsMgr, getBaseContext());
            _batteryMonitor = new BatteryMonitor(_settingsMgr, getBaseContext()) {
                void sendBatteryInfos(int level) {
                    if (_settings.notifyBattery && level % _settings.batteryNotificationInterval == 0) {
                        send("Battery level " + level + "%");
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
            
            Runnable asyncInit = new Runnable() {
                public void run() {
                    initNotificationStuff();
                    _mediaMgr.initMediaPlayer();
                    Log.i(Tools.LOG_TAG, "Starting xmpp.");
                    _xmppMgr.start();
                }
            };
            
            // Async start to release UI lock
            new Handler().postDelayed(asyncInit, 50);
        }
    };

    @Override
    public void onDestroy() {
        _gAnalytics.stop();
        stopForegroundCompat(XmppManager.DISCONNECTED);

        instance = null;
        Toast.makeText(this, "GTalkSMS stopped", Toast.LENGTH_SHORT).show();

        stopNotifications();
        _xmppMgr.stop();
        
        _geoMgr.stopLocatingPhone();
        _mediaMgr.clearMediaPlayer();
        _smsMonitor.clearSmsMonitor();
        _batteryMonitor.clearBatteryMonitor();
    }

    public void send(String msg) {
        if (_xmppMgr != null) {
            _xmppMgr.send(msg);
        }
    }

    /** handles the different commands */
    public void onMessageReceived(String commandLine) {
        if (_xmppListener != null) {
            _xmppListener.onMessageReceived(commandLine);
        }
        
        try {
            String command;
            String args;
            if (-1 != commandLine.indexOf(":")) {
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
                stopService(new Intent(".GTalkSMS.ACTION"));
            } else if (command.equals("sms")) {
                int separatorPos = args.indexOf(":");
                String contact = null;
                String message = null;
                if (-1 != separatorPos) {
                    contact = args.substring(0, separatorPos);
                    message = args.substring(separatorPos + 1);
                    setLastRecipient(contact);
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
                    send("Error: no recipient registered.");
                } else {
                    _smsMgr.markAsRead(_lastRecipient);
                    sendSMS(args, _lastRecipient);
                }
            } else if (command.equals("markasread") || command.equals("mar")) {
                if (args.length() > 0) {
                    markSmsAsRead(args);
                } else if (_lastRecipient == null) {
                    send("Error: no recipient registered.");
                } else {
                    markSmsAsRead(_lastRecipient);
                }
            } else if (command.equals("calls")) {
                readCallLogs();
            } else if (command.equals("battery") || command.equals("batt")) {
                _batteryMonitor.sendBatteryInfos();
            } else if (command.equals("copy")) {
                if (args.length() > 0) {
                    copyToClipboard(args);
                } else {
                    sendClipboard();
                }
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
            } else if (command.equals("http")) {
                openLink("http:" + args);
            } else if (command.equals("https")) {
                openLink("https:" + args);
            } else {
                send('"' + commandLine + '"' + ": unknown command. Send \"?\" for getting help");
            }
        } catch (Exception ex) {
            send("Error : " + ex);
        }
    }

    private void setLastRecipient(String phoneNumber) {
        if (_lastRecipient == null || !phoneNumber.equals(_lastRecipient)) {
            _lastRecipient = phoneNumber;
            displayLastRecipient();
        }
    }

    public void displayLastRecipient() {
        if (_lastRecipient == null) {
            send("Reply contact is not set");
        } else {
            String contact = ContactsManager.getContactName(_lastRecipient);
            if (Phone.isCellPhoneNumber(_lastRecipient) && contact.compareTo(_lastRecipient) != 0) {
                contact += " (" + _lastRecipient + ")";
            }
            send("Reply contact is " + contact);
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
        builder.append("Available commands:\n");
        builder.append("- " + makeBold("\"?\"") + ": shows this help.\n");
        builder.append("- " + makeBold("\"dial:#contact#\"") + ": dial the specified contact.\n");
        builder.append("- " + makeBold("\"reply:#message#\"") + ": send a sms to your last recipient with content message.\n");
        builder.append("- " + makeBold("\"sms\"") + ": display last sent sms from all contact.\n");
        builder.append("- " + makeBold("\"sms:#contact#\"") + ": display last sent sms from searched contacts.\n");
        builder.append("- " + makeBold("\"sms:#contact#:#message#\"") + ": sends a sms to number with content message.\n");
        builder.append("- " + makeBold("\"markAsRead:#contact#\"") + " or " + makeBold("\"mar\"") + ": mark sms as read for last recipient or given contact.\n");
        builder.append("- " + makeBold("\"battery\"") + " or " + makeBold("\"batt\"") + ": show battery level in percent.\n");
        builder.append("- " + makeBold("\"calls\"") + ": display call log.\n");
        builder.append("- " + makeBold("\"contact:#contact#\"") + ": display informations of a searched contact.\n");
        builder.append("- " + makeBold("\"geo:#address#\"") + ": Open Maps or Navigation or Street view on specific address\n");
        builder.append("- " + makeBold("\"where\"") + ": sends you google map updates about the location of the phone until you send \"stop\"\n");
        builder.append("- " + makeBold("\"ring\"") + ": rings the phone until you send \"stop\"\n");
        builder.append("- " + makeBold("\"copy:#text#\"") + ": copy text to clipboard or sent phone clipboard if text is empty\n");
        builder.append("and you can paste links and open it with the appropriate app\n");
        send(builder.toString());
    }

    public void geoLocate() {
        _hasOutgoingAction = true;
        send("Start locating phone");
        _geoMgr.startLocatingPhone();
    }

    public void ring() {
        _hasOutgoingAction = true;
        send("Ringing phone");
        if (!_mediaMgr.ring()) {
            send("Unable to ring, change the ringtone in the options");
        }
    }

    public void stopNotifications() {
        if (_hasOutgoingAction) {
            send("Stopping ongoing actions");
        }
        _hasOutgoingAction = false;
        _geoMgr.stopLocatingPhone();
        _mediaMgr.stopRinging();
    }

    /** sends a SMS to the last contact */
    public void sendSMS(String message) {
        sendSMS(message, _lastRecipient);
    }

    /** sends a SMS to the specified contact */
    public void sendSMS(String message, String contact) {
        setLastRecipient(contact);

        if (Phone.isCellPhoneNumber(contact)) {
            send("Sending sms to " + ContactsManager.getContactName(contact));
            sendSMSByPhoneNumber(message, contact);
        } else {
            ArrayList<Phone> mobilePhones = ContactsManager.getMobilePhones(contact);
            if (mobilePhones.size() > 1) {
                send("Specify more details:");

                for (Phone phone : mobilePhones) {
                    send(phone.contactName + " - " + phone.cleanNumber);
                }
            } else if (mobilePhones.size() == 1) {
                Phone phone = mobilePhones.get(0);
                send("Sending sms to " + phone.contactName + " (" + phone.cleanNumber + ")");
                sendSMSByPhoneNumber(message, phone.cleanNumber);
            } else {
                send("No match for \"" + contact + "\"");
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
            send("Mark " + ContactsManager.getContactName(contact) + "'s sms as read");
            _smsMgr.markAsRead(contact);
        } else {
            ArrayList<Phone> mobilePhones = ContactsManager.getMobilePhones(contact);
            if (mobilePhones.size() > 0) {
                send("Mark " + mobilePhones.get(0).contactName + "'s sms as read");

                for (Phone phone : mobilePhones) {
                    _smsMgr.markAsRead(phone.number);
                }
            } else {
                send("No match for \"" + contact + "\"");
            }
        }
    }

    /** reads (count) SMS from all contacts matching pattern */
    public void readSMS(String searchedText) {

        ArrayList<Contact> contacts = ContactsManager.getMatchingContacts(searchedText);
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
                    smsArrayList.addAll(_smsMgr.getSentSms(ContactsManager.getPhones(contact.id), sentSms));
                }
                Collections.sort(smsArrayList);

                List<Sms> smsList = Tools.getLastElements(smsArrayList, _settingsMgr.smsNumber);
                if (smsList.size() > 0) {
                    hasMatch = true;
                    StringBuilder smsContact = new StringBuilder();
                    smsContact.append(makeBold(contact.name));
                    for (Sms sms : smsList) {
                        smsContact.append("\r\n" + makeItalic(sms.date.toLocaleString() + " - " + sms.sender));
                        smsContact.append("\r\n" + sms.message);
                    }
                    if (smsList.size() < _settingsMgr.smsNumber) {
                        smsContact.append("\r\n" + makeItalic("Only got " + smsList.size() + " sms"));
                    }
                    send(smsContact.toString() + "\r\n");
                } else {
                    noSms.append(contact.name + " - No sms found\r\n");
                }
            }
            if (!hasMatch) {
                send(noSms.toString());
            }
        } else {
            send("No match for \"" + searchedText + "\"");
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
                allSms.append("\r\n" + makeItalic(sms.date.toLocaleString() + " - " + sms.sender));
                allSms.append("\r\n" + sms.message);
            }
        } else {
            allSms.append("No sms found");
        }
        send(allSms.toString() + "\r\n");
    }

    public void OnReceivedSms(String sender, String message) {
        send(message);
        setLastRecipient(sender);
    }

    /** reads last Call Logs from all contacts */
    public void readCallLogs() {

        ArrayList<Call> arrayList = _phoneMgr.getPhoneLogs();
        StringBuilder all = new StringBuilder();

        List<Call> callList = Tools.getLastElements(arrayList, _settingsMgr.callLogsNumber);
        if (callList.size() > 0) {
            for (Call call : callList) {
                String caller = makeBold(ContactsManager.getContactName(call.phoneNumber));

                all.append("\r\n" + makeItalic(call.date.toLocaleString()) + " - " + caller);
                all.append(" - " + call.type + " of " + call.duration());
            }
        } else {
            all.append("No sms found");
        }
        send(all.toString() + "\r\n");
    }

    /** reads (count) SMS from all contacts matching pattern */
    public void displayContacts(String searchedText) {

        ArrayList<Contact> contacts = ContactsManager.getMatchingContacts(searchedText);

        if (contacts.size() > 0) {

            if (contacts.size() > 1) {
                send(contacts.size() + " contacts found for \"" + searchedText + "\"");
            }

            for (Contact contact : contacts) {
                StringBuilder strContact = new StringBuilder();
                strContact.append(makeBold(contact.name));

                // strContact.append("\r\n" + "Id : " + contact.id);
                // strContact.append("\r\n" + "Raw Ids : " + TextUtils.join(" ",
                // contact.rawIds));

                ArrayList<Phone> mobilePhones = ContactsManager.getPhones(contact.id);
                if (mobilePhones.size() > 0) {
                    strContact.append("\r\n" + makeItalic("Phones"));
                    for (Phone phone : mobilePhones) {
                        strContact.append("\r\n" + phone.label + " - " + phone.cleanNumber);
                    }
                }

                ArrayList<ContactAddress> emails = ContactsManager.getEmailAddresses(contact.id);
                if (emails.size() > 0) {
                    strContact.append("\r\n" + makeItalic("Emails"));
                    for (ContactAddress email : emails) {
                        strContact.append("\r\n" + email.label + " - " + email.address);
                    }
                }

                ArrayList<ContactAddress> addresses = ContactsManager.getPostalAddresses(contact.id);
                if (addresses.size() > 0) {
                    strContact.append("\r\n" + makeItalic("Addresses"));
                    for (ContactAddress address : addresses) {
                        strContact.append("\r\n" + address.label + " - " + address.address);
                    }
                }
                send(strContact.toString() + "\r\n");
            }
        } else {
            send("No match for \"" + searchedText + "\"");
        }
    }

    /** Open geolocalization application */
    private void geo(String text) {
        List<Address> addresses = _geoMgr.geoDecode(text);
        if (addresses != null) {
            if (addresses.size() > 1) {
                send("Specify more details:");
                for (Address address : addresses) {
                    StringBuilder addr = new StringBuilder();
                    for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                        addr.append(address.getAddressLine(i) + "\n");
                    }
                    send(addr.toString());
                }
            } else if (addresses.size() == 1) {
                _geoMgr.launchExternal(addresses.get(0).getLatitude() + "," + addresses.get(0).getLongitude());
            }
        } else {
            send("No match for \"" + text + "\"");
            // For emulation testing
            // GeoManager.launchExternal("48.833199,2.362232");
        }
    }

    /** copy text to clipboard */
    private void copyToClipboard(String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Service.CLIPBOARD_SERVICE);
            clipboard.setText(text);
            send("Text copied");
        } catch (Exception ex) {
            send("Clipboard access failed");
        }
    }

    /** copy text to clipboard */
    public void sendClipboard() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Service.CLIPBOARD_SERVICE);
            send("GPhone clipboard: " + clipboard.getText());
        } catch (Exception ex) {
            send("Clipboard access failed");
        }
    }

    /** lets the user choose an activity compatible with the url */
    private void openLink(String url) {
        Intent target = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        Intent intent = Intent.createChooser(target, "GTalkSMS: choose an activity");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /** dial the specified contact */
    private void dial(String contactInfo) {
        String number = null;
        String contact = null;

        if (Phone.isCellPhoneNumber(contactInfo)) {
            number = contactInfo;
            contact = ContactsManager.getContactName(number);
        } else {
            ArrayList<Phone> mobilePhones = ContactsManager.getMobilePhones(contactInfo);
            if (mobilePhones.size() > 1) {
                send("Specify more details:");

                for (Phone phone : mobilePhones) {
                    send(phone.contactName + " - " + phone.cleanNumber);
                }
            } else if (mobilePhones.size() == 1) {
                Phone phone = mobilePhones.get(0);
                contact = phone.contactName;
                number = phone.cleanNumber;
            } else {
                send("No match for \"" + contactInfo + "\"");
            }
        }

        if (number != null) {
            send("Dial " + contact + " (" + number + ")");
            if (!_phoneMgr.Dial(number)) {
                send("Error can't dial.");
            }
        }
    }

    public void OnIncomingCall(String incomingNumber) {
        String contact = ContactsManager.getContactName(incomingNumber);
        send(contact + " is calling");
    }
}
