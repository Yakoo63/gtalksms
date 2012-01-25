package com.googlecode.gtalksms;

import java.util.Locale;
import java.util.Map;

import org.jivesoftware.smack.util.StringUtils;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.googlecode.gtalksms.tools.Tools;
/**
 * 
 * @author GTalkSMS Team
 * 
 * In order to work flawlessly with the BackupAgent
 * ALL settings in SettingsManager have to be of the same type
 * as within the SharedPreferences back-end AND they need to have
 * the same name
 *
 */
public class SettingsManager {
    public static final String[] xmppConnectionSettings = { "serverHost", "serviceName", "serverPort", 
                                                            "login", "password", "useDifferentAccount",
                                                            "xmppSecurityMode", "manuallySpecifyServerSettings",
                                                            "useCompression"};
    
    public static final int XMPPSecurityDisabled = 1;
    public static final int XMPPSecurityRequired = 2;
    public static final int XMPPSecurityOptional = 3;
    
    public static final String FRAMEBUFFER_ARGB_8888 = "ARGB_8888";
    public static final String FRAMEBUFFER_RGB_565 = "RGB_565";
    
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
    public boolean forceMucServer;
    public boolean useCompression;
    public String xmppSecurityMode;
    public int xmppSecurityModeInt;
    public boolean manuallySpecifyServerSettings;
    
    public static boolean connectionSettingsObsolete;
    
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
    public boolean smsReplySeparate;
    public boolean markSmsReadOnReply;
    public String smsMagicWord;
    
    // screen shots
    public String framebufferMode;
    
    // calls
    public int callLogsNumber;
    
    // locale
    public Locale locale;
    
    // app settings
    public boolean debugLog;
    public boolean connectOnMainscreenShow;
    public String displayIconIndex;
    
    // auto start and stop settings
    public boolean startOnBoot;
    public boolean startOnPowerConnected;
    public boolean startOnWifiConnected;
    public boolean stopOnPowerDisconnected;
    public boolean stopOnWifiDisconnected;
    public int stopOnPowerDelay;
    
    // public intents settings
    public boolean publicIntentsEnabled;
    public boolean publicIntentTokenRequired;
    public String publicIntentToken;
    
    private static SettingsManager sSettingsManager = null;
    
    private SharedPreferences mSharedPreferences;
    private Context mContext;
    private OnSharedPreferenceChangeListener mChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if (debugLog) {
				Log.i(Tools.LOG_TAG, "Preferences updated: key=" + key);
			}
            importPreferences();
            OnPreferencesUpdated(key);
        }
    };
    
    private SettingsManager(Context context) {
        mContext = context;
        mSharedPreferences = mContext.getSharedPreferences(Tools.APP_NAME, 0);
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mChangeListener);
        
        importPreferences();
    }
    
    public static SettingsManager getSettingsManager(Context context) {
        if (sSettingsManager == null) {
            sSettingsManager = new SettingsManager(context);           
        } 
        return sSettingsManager;        
    }
    
    public void Destroy() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mChangeListener);
    }
    
    public SharedPreferences.Editor getEditor() {
    	return mSharedPreferences.edit();
    }
    
    public Map<String, ?> getAllSharedPreferences() {
        return mSharedPreferences.getAll();
    }
    
    public boolean SharedPreferencesContains(String key) {
    	return mSharedPreferences.contains(key);
    }

    public void OnPreferencesUpdated(String key) {
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
    		BackupManager.dataChanged(mContext.getPackageName());
    	}
    	for (String s : xmppConnectionSettings)
    	    if (s.equals(key)) {
    	        connectionSettingsObsolete = true;
    	    }
    	if (key.equals("locale")) {
            Tools.setLocale(this, mContext);
    	}
    }
    
    /** imports the preferences */
	private void importPreferences() {	   
            serverHost = mSharedPreferences.getString("serverHost", "");
            serverPort = mSharedPreferences.getInt("serverPort", 0);
            
            notifiedAddress = mSharedPreferences.getString("notifiedAddress", "");
            
            useDifferentAccount = mSharedPreferences.getBoolean("useDifferentAccount", false);
            if (useDifferentAccount) {
                login = mSharedPreferences.getString("login", "");
            } else {
                login = notifiedAddress;
            }
            
            manuallySpecifyServerSettings = mSharedPreferences.getBoolean("manuallySpecifyServerSettings", true);
            if (manuallySpecifyServerSettings) {
                serviceName = mSharedPreferences.getString("serviceName", "");
            } else {
                serviceName = StringUtils.parseServer(login);
            }
            
            password =  mSharedPreferences.getString("password", "");
            xmppSecurityMode = mSharedPreferences.getString("xmppSecurityMode", "opt");
            if(xmppSecurityMode.equals("req")) {
                xmppSecurityModeInt = XMPPSecurityRequired;
            } else if (xmppSecurityMode.equals("dis")) {
                xmppSecurityModeInt = XMPPSecurityDisabled;
            } else {
                xmppSecurityModeInt = XMPPSecurityOptional;
            }
            useCompression = mSharedPreferences.getBoolean("useCompression", false);
            
            useGoogleMapUrl = mSharedPreferences.getBoolean("useGoogleMapUrl", true);
            useOpenStreetMapUrl = mSharedPreferences.getBoolean("useOpenStreetMapUrl", false);
            
            showStatusIcon = mSharedPreferences.getBoolean("showStatusIcon", false);
            
            notifyApplicationConnection = mSharedPreferences.getBoolean("notifyApplicationConnection", false);
            notifyBattery = mSharedPreferences.getBoolean("notifyBattery", false);
            notifyBatteryInStatus = mSharedPreferences.getBoolean("notifyBatteryInStatus", true);
            batteryNotificationInterval = mSharedPreferences.getString("batteryNotificationInterval", "10");
            batteryNotificationIntervalInt = Integer.parseInt(batteryNotificationInterval);
            notifySmsSent = mSharedPreferences.getBoolean("notifySmsSent", true);
            notifySmsDelivered = mSharedPreferences.getBoolean("notifySmsDelivered", false);
            notifySmsSentDelivered = notifySmsSent || notifySmsDelivered;
            ringtone = mSharedPreferences.getString("ringtone", Settings.System.DEFAULT_RINGTONE_URI.toString());
            showSentSms = mSharedPreferences.getBoolean("showSentSms", false);
            markSmsReadOnReply = mSharedPreferences.getBoolean("markSmsReadOnReply", false);
            smsNumber = mSharedPreferences.getInt("smsNumber", 5);
            callLogsNumber = mSharedPreferences.getInt("callLogsNumber", 10);
            formatResponses = mSharedPreferences.getBoolean("formatResponses", false);
            notifyIncomingCalls = mSharedPreferences.getBoolean("notifyIncomingCalls", false);
            displayIconIndex = mSharedPreferences.getString("displayIconIndex", "0");
            
            String localeStr = mSharedPreferences.getString("locale", "default");
            if (localeStr.equals("default")) {
                locale = Locale.getDefault();
            } else {
                locale = new Locale(localeStr);
            }
            
            roomPassword = mSharedPreferences.getString("roomPassword", "gtalksms");
            forceMucServer = mSharedPreferences.getBoolean("forceMucServer", false);
            mucServer = mSharedPreferences.getString("mucServer", "conference.jwchat.org");
            String notificationIncomingSmsType = mSharedPreferences.getString("notificationIncomingSmsType", "same");
            
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
            smsMagicWord = mSharedPreferences.getString("smsMagicWord", "GTalkSMS");
            notifyInMuc = mSharedPreferences.getBoolean("notifyInMuc", false); 
            smsReplySeparate = mSharedPreferences.getBoolean("smsReplySeparate", false);
            framebufferMode = mSharedPreferences.getString("framebufferMode", "ARGB_8888");
            connectOnMainscreenShow = mSharedPreferences.getBoolean("connectOnMainscreenShow", false);
            debugLog = mSharedPreferences.getBoolean("debugLog", false);
            
            // auto start and stop settings
            startOnBoot = mSharedPreferences.getBoolean("startOnBoot", false);
            startOnPowerConnected = mSharedPreferences.getBoolean("startOnPowerConnected", false);
            startOnWifiConnected = mSharedPreferences.getBoolean("startOnWifiConnected", false);
            stopOnPowerDisconnected = mSharedPreferences.getBoolean("stopOnPowerDisconnected", false);
            stopOnWifiDisconnected = mSharedPreferences.getBoolean("stopOnWifiDisconnected", false);
            stopOnPowerDelay = mSharedPreferences.getInt("stopOnPowerDelay", 1);
            
            // pulic intent settings
            publicIntentsEnabled = mSharedPreferences.getBoolean("publicIntentsEnabled", false); // TODO
            publicIntentTokenRequired = mSharedPreferences.getBoolean("publicIntentTokenRequired", false);          
            publicIntentToken = mSharedPreferences.getString("publicIntentToken", "secret");
	}
}
