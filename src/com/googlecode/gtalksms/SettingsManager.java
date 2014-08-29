package com.googlecode.gtalksms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.jivesoftware.smack.util.StringUtils;

import android.annotation.TargetApi;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.provider.Settings;
import android.media.CamcorderProfile;

import com.googlecode.gtalksms.tools.ArrayStringSetting;
import com.googlecode.gtalksms.tools.Log;
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

    private static final String[] xmppConnectionSettings = { "serverHost", "serviceName", "serverPort",
                                                            "login", "password", "useDifferentAccount",
                                                            "xmppSecurityMode", "manuallySpecifyServerSettings",
                                                            "useCompression"};
    
    public static final int XMPPSecurityDisabled = 1;
    public static final int XMPPSecurityRequired = 2;
    public static final int XMPPSecurityOptional = 3;
    
    // XMPP connection
    public String serverHost;
    public String serviceName;
    public int serverPort;
    public int pingIntervalInSec;
    
    private String _login;
    public String getLogin() { return _login; }
    public void setLogin(String value) { _login = saveSetting("login", value); }
    
    private String _password;
    public String getPassword() { return _password; }
    public void setPassword(String value) { _password = saveSetting("password", value); }

    private ArrayStringSetting _blockedResourcePrefixes = new ArrayStringSetting("blockedResourcePrefixes", "\n", this);
    public ArrayStringSetting getBlockedResourcePrefixes() { return _blockedResourcePrefixes; }

    private ArrayStringSetting _notifiedAddresses = new ArrayStringSetting("notifiedAddress", this);
    public ArrayStringSetting getNotifiedAddresses() { return _notifiedAddresses; }

    /**
     * Checks if the given fromJid is part of the notified Address set. fromJid can either be a fullJid or a bareJid
     * 
     * @param fromJid
     *            The JID we received a message from
     * @return true if the given JID is part of the notified Address set, otherwise false
     */
    public boolean cameFromNotifiedAddress(String fromJid) {
        String sanitizedNotifiedAddress;
        String sanitizedJid = fromJid.toLowerCase();
        for (String notifiedAddress : _notifiedAddresses.getAll()) {
            sanitizedNotifiedAddress = notifiedAddress.toLowerCase();
            // If it's a fullJID, append a slash for security reasons
            if (sanitizedJid.startsWith(sanitizedNotifiedAddress + "/")
            // A bare JID should be equals to one of the notified Address set
                    || sanitizedNotifiedAddress.equals(sanitizedJid)) {
                return true;
            }
        }
        return false;
    }

    private boolean _connectOnMainScreenStartup;
    
    public boolean getConnectOnMainScreenStartup() { 
        return _connectOnMainScreenStartup; 
    }

    public void setConnectOnMainScreenStartup(boolean value) { 
        _connectOnMainScreenStartup = saveSetting("connectOnMainscreenShow", value); 
    }
    
    public String roomPassword;
    public String mucServer;
    public boolean forceMucServer;
    public boolean useCompression;
    private String xmppSecurityMode;
    public int xmppSecurityModeInt;
    public boolean manuallySpecifyServerSettings;

    public static boolean connectionSettingsObsolete;
    
    // notifications
    public boolean notifyApplicationConnection;
    public boolean formatResponses;
    public boolean showStatusIcon;
    public boolean displayContactNumber;
    public int notificationIgnoreDelay;

    private ArrayStringSetting _notifHiddenApps = new ArrayStringSetting("hiddenNotifications", "#sep#", this);
    public ArrayStringSetting getNotifHiddenApps() { return _notifHiddenApps; }

    private ArrayStringSetting _notifHiddenMsg = new ArrayStringSetting("hiddenMsgNotifications", "#sep#", this);
    public ArrayStringSetting getNotifHiddenMsgs() { return _notifHiddenMsg; }

    // geo location
    public boolean useGoogleMapUrl;
    public boolean useOpenStreetMapUrl;

    // ring
    public String ringtone = null;

    // battery
    public boolean notifyBatteryInStatus;
    public boolean notifyBattery;
    public int batteryNotificationIntervalInt;
    private String batteryNotificationInterval;

    // sms
    public int smsNumber;
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
    
    // locale
    public Locale locale;
    
    // app settings
    public boolean debugLog;
    public String displayIconIndex;
    
    // auto start and stop settings
    private boolean startOnBoot;
    public boolean startOnPowerConnected;
    private boolean startOnWifiConnected;
    public boolean stopOnPowerDisconnected;
    private boolean stopOnWifiDisconnected;
    public int stopOnPowerDelay;
    
    // public intents settings
    public boolean publicIntentsEnabled;
    public boolean publicIntentTokenRequired;
    public String publicIntentToken;
    
    // recipient command settings
    public boolean dontDisplayRecipient;

    // Camera settings
    public int cameraMaxDurationInSec;
    public int cameraRotationInDegree;
    public long cameraMaxFileSizeInMegaBytes;
    public String cameraProfile;


    private static SettingsManager sSettingsManager = null;
    
    private final ArrayList<String> mProtectedSettings = new ArrayList<String>();
    private final ArrayList<String> mHiddenSettings = new ArrayList<String>();
    private final SharedPreferences mSharedPreferences;
    private final Context mContext;
    
    private final OnSharedPreferenceChangeListener mSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.i("Preferences updated: key=" + key);
            try {
                importPreferences();
            } catch (Exception e) {
                Log.e("Failed to load settings", e);
            }
            OnPreferencesUpdated(key);
        }
    };

    public interface OnSettingChangeListener {
        public void OnSettingChanged(boolean connectionSettingsObsolete);
    }

    private ArrayList<OnSettingChangeListener> mSettingChangeListeners = new ArrayList<OnSettingChangeListener>();

    public void addSettingChangeListener(OnSettingChangeListener listener) {
        mSettingChangeListeners.add(listener);
    }

    public void delSettingChangeListener(OnSettingChangeListener listener) {
        mSettingChangeListeners.remove(listener);
    }
    
    private SettingsManager(Context context) {
        mContext = context;
        mSharedPreferences = mContext.getSharedPreferences(Tools.APP_NAME, 0);

        mProtectedSettings.add("serverHost");
        mProtectedSettings.add("serverPort");
        mProtectedSettings.add("notifiedAddress");
        mProtectedSettings.add("login");
        mProtectedSettings.add("manuallySpecifyServerSettings");
        mProtectedSettings.add("serviceName");
        mProtectedSettings.add("password");
        mProtectedSettings.add("xmppSecurityMode");
        mProtectedSettings.add("useCompression");

        mHiddenSettings.add("login");
        mHiddenSettings.add("password");
        mHiddenSettings.add("notifiedAddress");
        mHiddenSettings.add("roomPassword");

        try {
            importPreferences();
        } catch (Exception e) {
            Log.e("Failed to load settings", e);
        }

        // Registering the listener after the first import
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
    }
    
    public static SettingsManager getSettingsManager(Context context) {
        if (sSettingsManager == null) {
            sSettingsManager = new SettingsManager(context);           
        } 
        return sSettingsManager;        
    }
    
    public ArrayList<String> getProtectedSettings() {
        return new ArrayList<String>(mProtectedSettings);
    }
    
    public ArrayList<String> getHiddenSettings() {
        return new ArrayList<String>(mHiddenSettings);
    }
    
    public SharedPreferences.Editor getEditor() {
        return mSharedPreferences.edit();
    }
    
    public Boolean saveSetting(String key, Boolean value) {
        getEditor().putBoolean(key, value).commit();
        OnPreferencesUpdated(key);
        return value;
    }
    
    public String saveSetting(String key, String value) {
        getEditor().putString(key, value).commit();
        OnPreferencesUpdated(key);
        return value;
    }
    
    public Integer saveSetting(String key, Integer value) {
        getEditor().putInt(key, value).commit();
        OnPreferencesUpdated(key);
        return value;
    }
    
    public Map<String, ?> getAllSharedPreferences() {
        Map<String, ?> result = mSharedPreferences.getAll();
        for (String key: mHiddenSettings) {
            result.remove(key);
        }
        return result;
    }
    
    public boolean SharedPreferencesContains(String key) {
        return mSharedPreferences.contains(key);
    }

    void OnPreferencesUpdated(String key) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            BackupManager bm = new BackupManager(mContext);
            bm.dataChanged();
        }
        for (String s : xmppConnectionSettings) {
            if (s.equals(key)) {
                connectionSettingsObsolete = true;
            }
        }
        if (key.equals("locale")) {
            Tools.setLocale(this, mContext);
        }

        for (Iterator<OnSettingChangeListener> it = mSettingChangeListeners.iterator(); it.hasNext(); ) {
            try {
                it.next().OnSettingChanged(connectionSettingsObsolete);
            } catch (Exception e) {
                Log.e("Failed to notified listener.", e);
            }
        }
    }
    
    private String getString(String key, String defaultValue) {
        try {
            if (mSharedPreferences.contains(key)) {
                return mSharedPreferences.getString(key, defaultValue);
            }
        } catch (ClassCastException  e) {
            Log.e("Failed to retrieve setting " + key, e);
        }
        saveSetting(key, defaultValue);
        return defaultValue;
    }
    
    private int getInt(String key, int defaultValue) {
        try {
            if (mSharedPreferences.contains(key)) {
                return mSharedPreferences.getInt(key, defaultValue);
            }
        } catch (ClassCastException  e) {
            Log.e("Failed to retrieve setting " + key, e);
        }
        saveSetting(key, defaultValue);
        return defaultValue;
    }
    
    private boolean getBoolean(String key, boolean defaultValue) {
        try {
            if (mSharedPreferences.contains(key)) {
                return mSharedPreferences.getBoolean(key, defaultValue);
            }
        } catch (ClassCastException  e) {
            Log.e("Failed to retrieve setting " + key, e);
        }
        saveSetting(key, defaultValue);
        return defaultValue;
    }
    
    /** imports the preferences */
    private void importPreferences() {

        serverHost = getString("serverHost", "talk.google.com");
        serverPort = getInt("serverPort", 5222);
        pingIntervalInSec = getInt("pingIntervalInSec", 600);

        _blockedResourcePrefixes.set(getString(_blockedResourcePrefixes.getKey(), "android\nMessagingA"));
        _notifiedAddresses.set(getString(_notifiedAddresses.getKey(), ""));
        _login = getString("login", "");

        manuallySpecifyServerSettings = getBoolean("manuallySpecifyServerSettings", false);
        if (manuallySpecifyServerSettings) {
            serviceName = getString("serviceName", "gmail.com");
        } else {
            serviceName = StringUtils.parseServer(_login);
        }
        
        _password =  getString("password", "");
        xmppSecurityMode = getString("xmppSecurityMode", "opt");
        if(xmppSecurityMode.equals("req")) {
            xmppSecurityModeInt = XMPPSecurityRequired;
        } else if (xmppSecurityMode.equals("dis")) {
            xmppSecurityModeInt = XMPPSecurityDisabled;
        } else {
            xmppSecurityModeInt = XMPPSecurityOptional;
        }
        useCompression = getBoolean("useCompression", false);
        
        useGoogleMapUrl = getBoolean("useGoogleMapUrl", true);
        useOpenStreetMapUrl = getBoolean("useOpenStreetMapUrl", false);
        
        showStatusIcon = getBoolean("showStatusIcon", true);
        
        notifyApplicationConnection = getBoolean("notifyApplicationConnection", false);
        notifyBattery = getBoolean("notifyBattery", false);
        notifyBatteryInStatus = getBoolean("notifyBatteryInStatus", true);
        batteryNotificationInterval = getString("batteryNotificationInterval", "10");
        batteryNotificationIntervalInt = Integer.parseInt(batteryNotificationInterval);
        notifySmsSent = getBoolean("notifySmsSent", true);
        notifySmsDelivered = getBoolean("notifySmsDelivered", false);
        notifySmsSentDelivered = notifySmsSent || notifySmsDelivered;
        ringtone = getString("ringtone", Settings.System.DEFAULT_RINGTONE_URI.toString());
        markSmsReadOnReply = getBoolean("markSmsReadOnReply", false);
        smsNumber = getInt("smsNumber", 5);
        formatResponses = getBoolean("formatResponses", false);
        displayContactNumber = getBoolean("displayContactNumber", true);
        notifyIncomingCalls = getBoolean("notifyIncomingCalls", true);
        displayIconIndex = getString("displayIconIndex", "0");
        
        String localeStr = getString("locale", "default");
        if (localeStr.equals("default")) {
            locale = Locale.getDefault();
        } else {
            locale = new Locale(localeStr);
        }
        
        roomPassword = getString("roomPassword", "GTalkSMS");
        forceMucServer = getBoolean("forceMucServer", false);
        mucServer = getString("mucServer", "conference.jabber.org");
        String notificationIncomingSmsType = getString("notificationIncomingSmsType", "same");
        
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
        smsMagicWord = getString("smsMagicWord", "GTalkSMS");
        notifyInMuc = getBoolean("notifyInMuc", false); 
        smsReplySeparate = getBoolean("smsReplySeparate", false);
        _connectOnMainScreenStartup = getBoolean("connectOnMainscreenShow", false);
        debugLog = getBoolean("debugLog", false);
        
        // auto start and stop settings
        startOnBoot = getBoolean("startOnBoot", false);
        startOnPowerConnected = getBoolean("startOnPowerConnected", false);
        startOnWifiConnected = getBoolean("startOnWifiConnected", false);
        stopOnPowerDisconnected = getBoolean("stopOnPowerDisconnected", false);
        stopOnWifiDisconnected = getBoolean("stopOnWifiDisconnected", false);
        stopOnPowerDelay = getInt("stopOnPowerDelay", 1);
        
        // pulic intent settings
        publicIntentsEnabled = getBoolean("publicIntentsEnabled", false);
        publicIntentTokenRequired = getBoolean("publicIntentTokenRequired", false);
        publicIntentToken = getString("publicIntentToken", "secret");
        
        // Manage notifications
        _notifHiddenApps.set(getString(_notifHiddenApps.getKey(), "GTalkSMS"));
        _notifHiddenMsg.set(getString(_notifHiddenMsg.getKey(), ""));
        notificationIgnoreDelay = getInt("notificationIgnoreDelay", 1000);

        // Manage camera settings
        cameraMaxDurationInSec = getInt("cameraMaxDurationInSec", 0);
        cameraMaxFileSizeInMegaBytes = getInt("cameraMaxFileSizeInMegaBytes", 0);
        cameraRotationInDegree = getInt("cameraRotationInDegree", 90);
        cameraProfile = getString("cameraProfile", "");

        // reply command settings
        dontDisplayRecipient = false;
    }

    @TargetApi(8)
    public CamcorderProfile getCamcorderProfileApi8() {
        int proId = CamcorderProfile.QUALITY_LOW;

        if (cameraProfile.equals("High")) {
            proId =  CamcorderProfile.QUALITY_HIGH;
        }

        return CamcorderProfile.get(proId);
    }

    @TargetApi(11)
    public CamcorderProfile getCamcorderProfileApi11() {
        int proId = CamcorderProfile.QUALITY_480P;

        if (cameraProfile.equals("1080p")) {
            proId =  CamcorderProfile.QUALITY_1080P;
        } else if (cameraProfile.equals("720p")) {
            proId =  CamcorderProfile.QUALITY_720P;
        } else if (cameraProfile.equals("CIF")) {
            proId =  CamcorderProfile.QUALITY_CIF;
        } else if (cameraProfile.equals("QCIF")) {
            proId =  CamcorderProfile.QUALITY_QCIF;
        }

        return CamcorderProfile.get(proId);
    }
}
