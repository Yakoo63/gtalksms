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
    
    public String mLogin;
    public String mPassword;
    public String mTo;
    public boolean useDifferentAccount;
    
    // notifications
    public boolean notifyApplicationConnection;
    public boolean formatChatResponses;
    public boolean showStatusIcon;
    
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
    public boolean notifyIncomingCalls;
    
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
        mTo = _sharedPreferences.getString("notifiedAddress", "");
        mPassword =  _sharedPreferences.getString("password", "");
        useDifferentAccount = _sharedPreferences.getBoolean("useDifferentAccount", false);
        if (useDifferentAccount) {
            mLogin = _sharedPreferences.getString("login", "");
        } else{
            mLogin = mTo;
        }
        
        showStatusIcon = _sharedPreferences.getBoolean("showStatusIcon", true);
        notifyApplicationConnection = _sharedPreferences.getBoolean("notifyApplicationConnection", true);
        notifyBattery = _sharedPreferences.getBoolean("notifyBattery", true);
        notifyBatteryInStatus = _sharedPreferences.getBoolean("notifyBatteryInStatus", true);
        batteryNotificationInterval = Integer.valueOf(_sharedPreferences.getString("batteryNotificationInterval", "10"));
        notifySmsSent = _sharedPreferences.getBoolean("notifySmsSent", true);
        notifySmsDelivered = _sharedPreferences.getBoolean("notifySmsDelivered", true);
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
    }
}
