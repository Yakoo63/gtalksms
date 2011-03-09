package com.googlecode.gtalksms;

import java.util.Locale;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.provider.Settings;
import android.util.Log;

import com.googlecode.gtalksms.tools.Tools;
/**
 * 
 * @author GTalkSMS Team
 * 
 * In order to work flawlessly with the BackupAgent
 * ALL settings in SettingsManager have to be of the same type
 * as within the SharedPreferences backend AND they need to have
 * the same name
 *
 */
public class SettingsManager {
    // XMPP connection
    public String serverHost;
    public String serviceName;
    public int serverPort;
    
    public String login;
    public String password;
    public String notifiedAddress;
    public boolean useDifferentAccount;
    public String roomPassword;
    public String mucServer;
    public boolean useCompression;
    
    // notifications
    public boolean notifyApplicationConnection;
    public boolean formatResponses;
    public boolean showStatusIcon;
    
    // geo location
    public boolean useGoogleMapUrl;
    public boolean useOpenStreetMapUrl;

    // ring
    public String ringtone = null;

    // battery
    public boolean notifyBatteryInStatus;
    public boolean notifyBattery;
    public int batteryNotificationIntervalInt;
    public String batteryNotificationInterval;

    // sms
    public int smsNumber;
    public boolean showSentSms;
    public boolean notifySmsSent;
    public boolean notifySmsDelivered;
    public boolean notifySmsSentDelivered;
    public boolean notifyIncomingCalls;
    public boolean notifySmsInChatRooms;
    public boolean notifySmsInSameConversation;
    public boolean notifyInMuc;
    
    // calls
    public int callLogsNumber;
    
    // locale
    public Locale locale;
    
    public boolean backupAgentAvailable;
    
    public boolean debugLog;
    
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
    
    public SharedPreferences.Editor getEditor() {
    	return _sharedPreferences.edit();
    }
    
    public boolean SharedPreferencesContains(String key) {
    	return _sharedPreferences.contains(key);
    }

    public void OnPreferencesUpdated() {
    	if(backupAgentAvailable) {
    		BackupManager.dataChanged(_context.getPackageName());
    	}
    }
    
    /** imports the preferences */
    @SuppressWarnings("unchecked")
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
        
        useGoogleMapUrl = _sharedPreferences.getBoolean("useGoogleMapUrl", true);
        useOpenStreetMapUrl = _sharedPreferences.getBoolean("useOpenStreetMapUrl", false);
        
        showStatusIcon = _sharedPreferences.getBoolean("showStatusIcon", true);
        
        notifyApplicationConnection = _sharedPreferences.getBoolean("notifyApplicationConnection", true);
        notifyBattery = _sharedPreferences.getBoolean("notifyBattery", true);
        notifyBatteryInStatus = _sharedPreferences.getBoolean("notifyBatteryInStatus", true);
        batteryNotificationInterval = _sharedPreferences.getString("batteryNotificationInterval", "10");
        batteryNotificationIntervalInt = Integer.parseInt(batteryNotificationInterval);
        notifySmsSent = _sharedPreferences.getBoolean("notifySmsSent", true);
        notifySmsDelivered = _sharedPreferences.getBoolean("notifySmsDelivered", true);
        notifySmsSentDelivered = notifySmsSent || notifySmsDelivered;
        ringtone = _sharedPreferences.getString("ringtone", Settings.System.DEFAULT_RINGTONE_URI.toString());
        showSentSms = _sharedPreferences.getBoolean("showSentSms", false);
        smsNumber = _sharedPreferences.getInt("smsNumber", 5);
        callLogsNumber = _sharedPreferences.getInt("callLogsNumber", 10);
        formatResponses = _sharedPreferences.getBoolean("formatResponses", false);
        notifyIncomingCalls = _sharedPreferences.getBoolean("notifyIncomingCalls", false);

        String localeStr = _sharedPreferences.getString("locale", "default");
        if (localeStr.equals("default")) {
            locale = Locale.getDefault();
        } else {
            locale = new Locale(localeStr);
        }
        
        roomPassword = _sharedPreferences.getString("roomPassword", "gtalksms");
        mucServer = _sharedPreferences.getString("mucServer", "conference.jwchat.org");
        String notificationIncomingSmsType = _sharedPreferences.getString("notificationIncomingSmsType", "same");
        
        if (notificationIncomingSmsType.equals("both")) {
            notifySmsInChatRooms = true;
            notifySmsInSameConversation = true;
        } else if (notificationIncomingSmsType.equals("no")) {
            notifySmsInChatRooms = false;
            notifySmsInSameConversation = false;
        } else if (notificationIncomingSmsType.equals("separate")) {
            notifySmsInChatRooms = true;
            notifySmsInSameConversation = false;
        } else {
            notifySmsInSameConversation = true;
            notifySmsInChatRooms = false;
        }
        
        try {
        	@SuppressWarnings("unused")
			Class c = Class.forName("android.app.backup.BackupAgent");
        	backupAgentAvailable = true;
        } catch (Exception e) {
        	backupAgentAvailable = false;
        }
        
        debugLog = true; // TODO make this a preference
        notifyInMuc = notifySmsInChatRooms; // TODO for testing purpose the same as notifySmsInChatRooms
    }
}
