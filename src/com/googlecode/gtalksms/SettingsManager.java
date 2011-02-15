package com.googlecode.gtalksms;

import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.provider.Settings;
import android.util.Log;

import com.googlecode.gtalksms.tools.Tools;

public class SettingsManager {
    // XMPP connection
    public String serverHost;
    public String serviceName;
    public int serverPort;
    
    public String login;
    public String password;
    public String notifiedAddress;
    public boolean useDifferentAccount;
    public String roomsPassword;
    public String mucServer;
    public boolean useCompression;
    
    // notifications
    public boolean notifyApplicationConnection;
    public boolean formatChatResponses;
    public boolean showStatusIcon;
    public boolean showToastMessages;
    
    // geo location
    public boolean useGoogleMap;
    public boolean useOpenStreetMap;

    // ring
    public String ringtone = null;

    // battery
    public boolean notifyBatteryInStatus;
    public boolean notifyBattery;
    public int batteryNotificationInterval;

    // sms
    public int smsNumber;
    public boolean displaySentSms;
    public boolean notifySmsSent;
    public boolean notifySmsDelivered;
    public boolean notifySmsSentDelivered;
    public boolean notifyIncomingCalls;
    public boolean notifySmsInChatRooms;
    public boolean notifySmsInSameConversation;
    
    // calls
    public int callLogsNumber;
    
    // locale
    public Locale locale;
    
    private SharedPreferences _sharedPreferences;
    private Context _context;
    private OnSharedPreferenceChangeListener _changeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.d(Tools.LOG_TAG,"Preferences updated: key=" + key);
            importPreferences();
            OnPreferencesUpdated();
        }
    };
    
    public SettingsManager(Context context) {
        _context = context;
        _sharedPreferences = _context.getSharedPreferences("GTalkSMS", 0);
        _sharedPreferences.registerOnSharedPreferenceChangeListener(_changeListener);
        
        importPreferences();
    }
    
    public void Destroy() {
        _sharedPreferences.unregisterOnSharedPreferenceChangeListener(_changeListener);
    }

    public void OnPreferencesUpdated() {}
    
    /** imports the preferences */
    private void importPreferences() {
        serverHost = _sharedPreferences.getString("serverHost", "");
        serverPort = _sharedPreferences.getInt("serverPort", 0);
        serviceName = _sharedPreferences.getString("serviceName", "");
        notifiedAddress = _sharedPreferences.getString("notifiedAddress", "");
        password =  _sharedPreferences.getString("password", "");
        useDifferentAccount = _sharedPreferences.getBoolean("useDifferentAccount", false);
        if (useDifferentAccount) {
            login = _sharedPreferences.getString("login", "");
        } else{
            login = notifiedAddress;
        }
        useCompression = _sharedPreferences.getBoolean("useCompression", false);
        
        useGoogleMap = _sharedPreferences.getBoolean("useGoogleMapUrl", true);
        useOpenStreetMap = _sharedPreferences.getBoolean("useOpenStreetMapUrl", false);
        
        showStatusIcon = _sharedPreferences.getBoolean("showStatusIcon", true);
        showToastMessages = showStatusIcon;
        
        notifyApplicationConnection = _sharedPreferences.getBoolean("notifyApplicationConnection", true);
        notifyBattery = _sharedPreferences.getBoolean("notifyBattery", true);
        notifyBatteryInStatus = _sharedPreferences.getBoolean("notifyBatteryInStatus", true);
        batteryNotificationInterval = Integer.valueOf(_sharedPreferences.getString("batteryNotificationInterval", "10"));
        notifySmsSent = _sharedPreferences.getBoolean("notifySmsSent", true);
        notifySmsDelivered = _sharedPreferences.getBoolean("notifySmsDelivered", true);
        notifySmsSentDelivered = notifySmsSent || notifySmsDelivered;
        ringtone = _sharedPreferences.getString("ringtone", Settings.System.DEFAULT_RINGTONE_URI.toString());
        displaySentSms = _sharedPreferences.getBoolean("showSentSms", false);
        smsNumber = _sharedPreferences.getInt("smsNumber", 5);
        callLogsNumber = _sharedPreferences.getInt("callLogsNumber", 10);
        formatChatResponses = _sharedPreferences.getBoolean("formatResponses", false);
        notifyIncomingCalls = _sharedPreferences.getBoolean("notifyIncomingCalls", false);

        String localeStr = _sharedPreferences.getString("locale", "default");
        if (localeStr.equals("default")) {
            locale = Locale.getDefault();
        } else {
            locale = new Locale(localeStr);
        }
        
        roomsPassword = _sharedPreferences.getString("roomPassword", "gtalksms");
        mucServer = _sharedPreferences.getString("mucServer", "conference.jwchat.org");
        String smsNotificationType = _sharedPreferences.getString("notificationIncomingSmsType", "same");
        
        if (smsNotificationType.equals("both")) {
            notifySmsInChatRooms = true;
            notifySmsInSameConversation = true;
        } else if (smsNotificationType.equals("no")) {
            notifySmsInChatRooms = false;
            notifySmsInSameConversation = false;
        } else if (smsNotificationType.equals("separate")) {
            notifySmsInChatRooms = true;
            notifySmsInSameConversation = false;
        } else {
            notifySmsInSameConversation = true;
            notifySmsInChatRooms = false;
        }
    }
}
