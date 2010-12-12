package com.googlecode.gtalksms;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Address;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.text.ClipboardManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.googlecode.gtalksms.contacts.Contact;
import com.googlecode.gtalksms.contacts.ContactAddress;
import com.googlecode.gtalksms.contacts.ContactsManager;
import com.googlecode.gtalksms.contacts.Phone;
import com.googlecode.gtalksms.geo.GeoManager;
import com.googlecode.gtalksms.phone.PhoneManager;
import com.googlecode.gtalksms.sms.Sms;
import com.googlecode.gtalksms.sms.SmsMmsManager;

public class XmppService extends Service {

    private static final int DISCONNECTED = 0;
    private static final int CONNECTING = 1;
    private static final int CONNECTED = 2;

    // Indicates the current state of the service (disconnected/connecting/connected)
    private int mStatus = DISCONNECTED;

    // Service instance
    private static XmppService instance = null;

    // XMPP connection
    private String mLogin;
    private String mPassword;
    private String mTo;
    private ConnectionConfiguration mConnectionConfiguration = null;
    private XMPPConnection mConnection = null;
    private PacketListener mPacketListener = null;
    private boolean notifyApplicationConnection;
    private boolean formatChatResponses;

    // ring
    private MediaPlayer mMediaPlayer = null;
    private String ringtone = null;
    private boolean canRing;

    // last person who sent sms/who we sent an sms to
    private String lastRecipient = null;

    // battery
    private BroadcastReceiver mBatInfoReceiver = null;
    private boolean notifyBattery;

    // sms
    public int smsNumber;
    private boolean displaySentSms;

    // notification stuff
    @SuppressWarnings("unchecked")
    private static final Class[] mStartForegroundSignature = new Class[] {
        int.class, Notification.class};
    @SuppressWarnings("unchecked")
    private static final Class[] mStopForegroundSignature = new Class[] {
        boolean.class};
    private NotificationManager mNM;
    private Method mStartForeground;
    private Method mStopForeground;
    private Object[] mStartForegroundArgs = new Object[2];
    private Object[] mStopForegroundArgs = new Object[1];
    private PendingIntent contentIntent = null;

    // Our current retry attempt, plus a runnable and handler to implement retry
    private int mCurrentRetryCount = 0;
    Runnable mReconnectRunnable = null;
    Handler mReconnectHandler = new Handler();

    public final static String LOG_TAG = "gtalksms";
    
    /** Updates the status about the service state (and the statusbar)*/
    private void updateStatus(int status) {
        if (status != mStatus) {
            // Get the layout for the AppWidget and attach an on-click listener to the button
            RemoteViews views = new RemoteViews(getPackageName(), R.layout.appwidget);
            
            Notification notification = new Notification();
            switch(status) {
                case CONNECTED:
                    notification = new Notification(
                            R.drawable.status_green,
                            "Connected",
                            System.currentTimeMillis());
                    notification.setLatestEventInfo(
                            getApplicationContext(),
                            "GTalkSMS",
                            "Connected",
                            contentIntent);
                    views.setImageViewResource(R.id.Button, R.drawable.icon_green);     
                    break;
                case CONNECTING:
                    notification = new Notification(
                            R.drawable.status_orange,
                            "Connecting...",
                            System.currentTimeMillis());
                    notification.setLatestEventInfo(
                            getApplicationContext(),
                            "GTalkSMS",
                            "Connecting...",
                            contentIntent);
                    views.setImageViewResource(R.id.Button, R.drawable.icon_orange);     
                    break;
                case DISCONNECTED:
                    notification = new Notification(
                            R.drawable.status_red,
                            "Disconnected",
                            System.currentTimeMillis());
                    notification.setLatestEventInfo(
                            getApplicationContext(),
                            "GTalkSMS",
                            "Disconnected",
                            contentIntent);
                    views.setImageViewResource(R.id.Button, R.drawable.icon_red);     
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
            stopForegroundCompat(mStatus);
            startForegroundCompat(status, notification);
            mStatus = status;
        }
    }
    
    /**
     * This is a wrapper around the startForeground method, using the older
     * APIs if it is not available.
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
                Log.w(LOG_TAG, "Unable to invoke startForeground", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
                Log.w(LOG_TAG, "Unable to invoke startForeground", e);
            }
            return;
        }
        // Fall back on the old API.
        setForeground(true);
        mNM.notify(id, notification);
    }

    /**
     * This is a wrapper around the stopForeground method, using the older
     * APIs if it is not available.
     */
    void stopForegroundCompat(int id) {
        // If we have the new stopForeground API, then use it.
        if (mStopForeground != null) {
            mStopForegroundArgs[0] = Boolean.TRUE;
            try {
                mStopForeground.invoke(this, mStopForegroundArgs);
            } catch (InvocationTargetException e) {
                // Should not happen.
                Log.w(LOG_TAG, "Unable to invoke stopForeground", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
                Log.w(LOG_TAG, "Unable to invoke stopForeground", e);
            }
            return;
        }

        // Fall back on the old API.  Note to cancel BEFORE changing the
        // foreground state, since we could be killed at that point.
        mNM.cancel(id);
        setForeground(false);
    }

    /**
     * This makes the 2 previous wrappers possible
     */
    private void initNotificationStuff() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        try {
            mStartForeground = getClass().getMethod("startForeground",
                    mStartForegroundSignature);
            mStopForeground = getClass().getMethod("stopForeground",
                    mStopForegroundSignature);
        } catch (NoSuchMethodException e) {
            // Running on an older platform.
            mStartForeground = mStopForeground = null;
        }
        contentIntent =
            PendingIntent.getActivity(
                    this, 0, new Intent(this, MainScreen.class), 0);
    }

    /** imports the preferences */
    private void importPreferences() {
        SharedPreferences prefs = getSharedPreferences("GTalkSMS", 0);
        String serverHost = prefs.getString("serverHost", "");
        int serverPort = prefs.getInt("serverPort", 0);
        String serviceName = prefs.getString("serviceName", "");
        mConnectionConfiguration = new ConnectionConfiguration(serverHost, serverPort, serviceName);
        mTo = prefs.getString("notifiedAddress", "");
        mPassword =  prefs.getString("password", "");
        boolean useDifferentAccount = prefs.getBoolean("useDifferentAccount", false);
        if (useDifferentAccount) {
            mLogin = prefs.getString("login", "");
        } else{
            mLogin = mTo;
        }
        notifyApplicationConnection = prefs.getBoolean("notifyApplicationConnection", true);
        notifyBattery = prefs.getBoolean("notifyBattery", true);
        SmsMmsManager.notifySmsSent = prefs.getBoolean("notifySmsSent", true);
        SmsMmsManager.notifySmsDelivered = prefs.getBoolean("notifySmsDelivered", true);
        ringtone = prefs.getString("ringtone", Settings.System.DEFAULT_RINGTONE_URI.toString());
        displaySentSms = prefs.getBoolean("showSentSms", false);
        smsNumber = prefs.getInt("smsNumber", 5);
        formatChatResponses = prefs.getBoolean("formatResponses", false);
    }

    /** clears the XMPP connection */
    public void clearConnection() {
        if (mReconnectRunnable != null)
            mReconnectHandler.removeCallbacks(mReconnectRunnable);
        
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

    private void maybeStartReconnect() {
        if (mCurrentRetryCount > 5) {
            // we failed after all the retries - just die.
            Log.v(LOG_TAG, "maybeStartReconnect ran out of retrys");
            updateStatus(DISCONNECTED);
            Toast.makeText(this, "Failed to connect.", Toast.LENGTH_SHORT).show();
            onDestroy();
            return;
        } else {
            mCurrentRetryCount += 1;
            // a simple linear-backoff strategy.
            int timeout = 5000 * mCurrentRetryCount;
            Log.e(LOG_TAG, "maybeStartReconnect scheduling retry in " + timeout);
            mReconnectHandler.postDelayed(mReconnectRunnable, timeout);
        }
    }

    /** init the XMPP connection */
    public void initConnection() {
        updateStatus(CONNECTING);
        NetworkInfo active = ((ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (active==null || !active.isAvailable()) {
            Log.e(LOG_TAG, "connection request, but no network available");
            Toast.makeText(this, "Waiting for network to become available.", Toast.LENGTH_SHORT).show();
            // we don't destroy the service here - our network receiver will notify us when
            // the network comes up and we try again then.
            updateStatus(DISCONNECTED);
            return;
        }
        if (mConnectionConfiguration == null) {
            importPreferences();
        }
        XMPPConnection connection = new XMPPConnection(mConnectionConfiguration);
        try {
            connection.connect();
        } catch (Exception e) {
            Log.e(LOG_TAG, "xmpp connection failed: " + e);
            Toast.makeText(this, "Connection failed.", Toast.LENGTH_SHORT).show();
            maybeStartReconnect();
            return;
        }
        try {
            connection.login(mLogin, mPassword);
        } catch (Exception e) {
            try {
                connection.disconnect();
            } catch (Exception e2) {
                Log.e(LOG_TAG, "xmpp disconnect failed: " + e2);
            }
            
            Log.e(LOG_TAG, "xmpp login failed: " + e);
            // sadly, smack throws the same generic XMPPException for network
            // related messages (eg "no response from the server") as for
            // authoritative login errors (ie, bad password).  The only
            // differentiator is the message itself which starts with this
            // hard-coded string.
            if (e.getMessage().indexOf("SASL authentication")==-1) {
                // doesn't look like a bad username/password, so retry
                Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show();
                maybeStartReconnect();
            } else {
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                onDestroy();
            }
            return;
        }
        mConnection = connection;
        onConnectionComplete();
    }

    private void onConnectionComplete() {
        Log.v(LOG_TAG, "connection established");
        mCurrentRetryCount = 0;
        PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
        mPacketListener = new PacketListener() {
            public void processPacket(Packet packet) {
                Message message = (Message) packet;

                if (    message.getFrom().toLowerCase().startsWith(mTo.toLowerCase() + "/")
                    && !message.getFrom().equals(mConnection.getUser()) // filters self-messages
                ) {
                    if (message.getBody() != null) {
                        onCommandReceived(message.getBody());
                    }
                }
            }
        };
        mConnection.addPacketListener(mPacketListener, filter);
        updateStatus(CONNECTED);
        // Send welcome message
        if (notifyApplicationConnection) {
            send("Welcome to GTalkSMS " + Tools.getVersionName(getBaseContext(), getClass()) + 
                 ". Send \"?\" for getting help");
        }
    }

    /** returns true if the service is correctly connected */
    public boolean isConnected() {
        return    (mConnection != null
                && mConnection.isConnected()
                && mConnection.isAuthenticated());
    }

    /** clear the battery monitor*/
    private void clearBatteryMonitor() {
        if (mBatInfoReceiver != null) {
            unregisterReceiver(mBatInfoReceiver);
        }
        mBatInfoReceiver = null;
    }

    /** init the battery stuff */
    private void initBatteryMonitor() {
        if (notifyBattery) {
            mBatInfoReceiver = new BroadcastReceiver(){
                private int lastPercentageNotified = -1;
                @Override
                public void onReceive(Context arg0, Intent intent) {
                    int level = intent.getIntExtra("level", 0);
                    if (lastPercentageNotified == -1) {
                        notifyAndSavePercentage(level);
                    } else {
                        if (level != lastPercentageNotified && level % 5 == 0) {
                            notifyAndSavePercentage(level);
                        }
                    }
                }
                private void notifyAndSavePercentage(int level) {
                    send("Battery level " + level + "%");
                    lastPercentageNotified = level;
                }
            };
            registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        }
    }

    /** clears the media player */
    private void clearMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
        }
        mMediaPlayer = null;
    }

    /** init the media player */
    private void initMediaPlayer() {
        canRing = true;
        Uri alert = Uri.parse(ringtone);
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(this, alert);
        } catch (Exception e) {
            canRing = false;
        }
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        mMediaPlayer.setLooping(true);
    }

    private void _onStart() {
        // Get configuration
        if (instance == null)
        {
            instance = this;

            initNotificationStuff();

            updateStatus(DISCONNECTED);

            // first, clean everything
            clearConnection();
            SmsMmsManager.clearSmsMonitors();
            clearMediaPlayer();
            clearBatteryMonitor();

            // then, re-import preferences
            importPreferences();

            initBatteryMonitor();
            SmsMmsManager.initSmsMonitors();
            initMediaPlayer();

            mCurrentRetryCount = 0;
            mReconnectRunnable = new Runnable() {
                public void run() {
                    Log.v(LOG_TAG, "attempting reconnection");
                    Toast.makeText(XmppService.this, "Reconnecting", Toast.LENGTH_SHORT).show();
                    initConnection();
                }
            };
            initConnection();
        }
    }

    public static XmppService getInstance() {
        return instance;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        _onStart();
    };

    @Override
    public void onDestroy() {
        GeoManager.stopLocatingPhone();

        SmsMmsManager.clearSmsMonitors();
        clearMediaPlayer();
        clearBatteryMonitor();
        clearConnection();

        stopForegroundCompat(mStatus);

        instance = null;

        Toast.makeText(this, "GTalkSMS stopped", Toast.LENGTH_SHORT).show();
    }

    /** sends a message to the user */
    public void send(String message) {
        if (isConnected()) {
            Message msg = new Message(mTo, Message.Type.chat);
            msg.setBody(message);
            mConnection.sendPacket(msg);
        }
    }


    public void setLastRecipient(String phoneNumber) {
        if (lastRecipient == null || !phoneNumber.equals(lastRecipient)) {
            lastRecipient = phoneNumber;
            displayLastRecipient(phoneNumber);
        }
    }

    public String makeBold(String in) {
        if (formatChatResponses) {
            return " *" + in + "* ";
        }
        return in;
    }

    public String makeItalic(String in) {
        if (formatChatResponses) {
            return " _" + in + "_ ";
        }
        return in;
    }

    /** handles the different commands */
    private void onCommandReceived(String commandLine) {
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
                StringBuilder builder = new StringBuilder();
                builder.append("Available commands:\n");
                builder.append("- \"?\": shows this help.\n");
                builder.append("- \"dial:#contact#\": dial the specified contact.\n");
                builder.append("- \"reply:#message#\": send a sms to your last recipient with content message.\n");
                builder.append("- \"sms\": display last sent sms from all contact.\n");
                builder.append("- \"sms:#contact#\": display last sent sms from searched contacts.\n");
                builder.append("- \"sms:#contact#:#message#\": sends a sms to number with content message.\n");
                builder.append("- \"contact:#contact#\": display informations of a searched contact.\n");
                builder.append("- \"geo:#address#\": Open Maps or Navigation or Street view on specific address\n");
                builder.append("- \"where\": sends you google map updates about the location of the phone until you send \"stop\"\n");
                builder.append("- \"ring\": rings the phone until you send \"stop\"\n");
                builder.append("- \"copy:#text#\": copy text to clipboard\n");
                builder.append("and you can paste links and open it with the appropriate app\n");
                send(builder.toString());
            }
            else if (command.equals("sms")) {
                int separatorPos = args.indexOf(":");
                String contact = null;
                String message = null;
                if (-1 != separatorPos) {
                    contact = args.substring(0, separatorPos);
                    setLastRecipient(contact);
                    message = args.substring(separatorPos + 1);
                    sendSMS(message, contact);
                } else if (args.length() > 0) {
                    readSMS(args);
                } else {
                    readLastSMS();
                }
            }
            else if (command.equals("reply")) {
                if (args.length() == 0) {
                    displayLastRecipient(lastRecipient);
                } else if (lastRecipient == null) {
                    send("Error: no recipient registered.");
                } else {
                    sendSMS(args, lastRecipient);
                }
            }
            else if (command.equals("copy")) {
                copyToClipboard(args);
            }
            else if (command.equals("geo")) {
                geo(args);
            }
            else if (command.equals("dial")) {
                dial(args);
            }
            else if (command.equals("contact")) {
                displayContacts(args);
            }
            else if (command.equals("where")) {
                send("Start locating phone");
                GeoManager.startLocatingPhone();
            }
            else if (command.equals("stop")) {
                send("Stopping ongoing actions");
                GeoManager.stopLocatingPhone();
                stopRinging();
            }
            else if (command.equals("ring")) {
                send("Ringing phone");
                ring();
            }
            else if (command.equals("http")) {
                open("http:" + args);
            }
            else if (command.equals("https")) {
                open("https:" + args);
            }
            else {
                send('"'+ commandLine + '"' + ": unknown command. Send \"?\" for getting help");
            }
        } catch (Exception ex) {
            send("Error : " + ex);
        }
    }

    /** dial the specified contact */
    public void dial(String searchedText) {
        String number = null;
        String contact = null;

        if (Phone.isCellPhoneNumber(searchedText)) {
            number = searchedText;
            contact = ContactsManager.getContactName(number);
        } else {
            ArrayList<Phone> mobilePhones = ContactsManager.getMobilePhones(searchedText);
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
                send("No match for \"" + searchedText + "\"");
            }
        }

        if( number != null) {
            send("Dial " + contact + " (" + number + ")");
            if(!PhoneManager.Dial(number)) {
                send("Error can't dial.");
            }
        }
    }

    /** sends a SMS to the specified contact */
    public void sendSMS(String message, String contact) {
        if (Phone.isCellPhoneNumber(contact)) {
            send("Sending sms to " + ContactsManager.getContactName(contact));
            SmsMmsManager.sendSMSByPhoneNumber(message, contact);
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
                SmsMmsManager.sendSMSByPhoneNumber(message, phone.cleanNumber);
            } else {
                send("No match for \"" + contact + "\"");
            }
        }
    }

    /** reads (count) SMS from all contacts matching pattern */
    public void readSMS(String searchedText) {

        ArrayList<Contact> contacts = ContactsManager.getMatchingContacts(searchedText);
        ArrayList<Sms> sentSms = new ArrayList<Sms>();
        if(displaySentSms) {
            sentSms = SmsMmsManager.getAllSentSms();
        }

        if (contacts.size() > 0) {
            
            StringBuilder noSms = new StringBuilder();
            Boolean hasMatch = false;
            for (Contact contact : contacts) {
                ArrayList<Sms> smsArrayList = SmsMmsManager.getSms(contact.rawIds, contact.name);
                if(displaySentSms) {
                    smsArrayList.addAll(SmsMmsManager.getSentSms(ContactsManager.getPhones(contact.id),sentSms));
                }
                Collections.sort(smsArrayList);
                
                List<Sms> smsList = smsArrayList.subList(Math.max(smsArrayList.size() - smsNumber, 0), smsArrayList.size());
                if (smsList.size() > 0) {
                    hasMatch = true;
                    StringBuilder smsContact = new StringBuilder();
                    smsContact.append(makeBold(contact.name));
                    for (Sms sms : smsList) {
                        smsContact.append("\r\n" + makeItalic(sms.date.toLocaleString() + " - " + sms.sender));
                        smsContact.append("\r\n" + sms.message);
                    }
                    if (smsList.size() < smsNumber) {
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

        ArrayList<Sms> smsArrayList = SmsMmsManager.getAllReceivedSms();
        StringBuilder allSms = new StringBuilder();
        
        if (displaySentSms) {
            smsArrayList.addAll(SmsMmsManager.getAllSentSms());
        }
        Collections.sort(smsArrayList);
        
        List<Sms> smsList = smsArrayList.subList(Math.max(smsArrayList.size() - smsNumber, 0), smsArrayList.size());
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


    public void displayLastRecipient(String phoneNumber) {
        if (phoneNumber == null) {
            send("Reply contact is not set");
        } else {
            String contact = ContactsManager.getContactName(phoneNumber);
            if (Phone.isCellPhoneNumber(phoneNumber) && contact.compareTo(phoneNumber) != 0){
                contact += " (" + phoneNumber + ")";
            }
            send("Reply contact is now " + contact);
        }
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
                
//                strContact.append("\r\n" + "Id : " + contact.id);
//                strContact.append("\r\n" + "Raw Ids : " + TextUtils.join(" ", contact.rawIds));
                
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
        List<Address> addresses = GeoManager.geoDecode(text);
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
                GeoManager.launchExternal(addresses.get(0).getLatitude() + "," + addresses.get(0).getLongitude());
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
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setText(text);
            send("Text copied");
        }
        catch(Exception ex) {
            send("Clipboard access failed");
        }
    }

    /** lets the user choose an activity compatible with the url */
    private void open(String url) {
        Intent target = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        Intent intent = Intent.createChooser(target, "GTalkSMS: choose an activity");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /** makes the phone ring */
    private void ring() {
        final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (canRing && audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
            try {
                mMediaPlayer.prepare();
            } catch (Exception e) {
                canRing = false;
                send("Unable to ring, change the ringtone in the options");
            }
            mMediaPlayer.start();
        }
    }

    /** Stops the phone from ringing */
    private void stopRinging() {
        if (canRing) {
            mMediaPlayer.stop();
        }
    }
}
