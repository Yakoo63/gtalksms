package com.googlecode.gtalksms;

import com.googlecode.gtalksms.tools.Tools;

public class Log {
    private static SettingsManager sSettingsMgr;

    public static void initialize(SettingsManager settingsMgr) {
        sSettingsMgr = settingsMgr;
    }
    
    private static boolean checkSettings() {
        if (sSettingsMgr == null) {
            StackTraceElement[] stm = Thread.currentThread().getStackTrace();
            String stmString = Tools.STMArrayToString(stm);
            android.util.Log.e(Tools.LOG_TAG, "Using log without initialize settings manager. " + stmString);
            return false;
        }
        
        return true;
    }
    
    public static void i(String msg) {
        if (checkSettings() && sSettingsMgr.debugLog) {
            android.util.Log.i(Tools.LOG_TAG, msg);
        }
    }
    
    public static void i(String msg, Exception e) {
        if (checkSettings() && sSettingsMgr.debugLog) {
            android.util.Log.i(Tools.LOG_TAG, msg, e);
        }
    }

    public static void e(String msg) {
        android.util.Log.e(Tools.LOG_TAG, msg);
    }
    
    public static void e(String msg, Exception e) {
        android.util.Log.e(Tools.LOG_TAG, msg, e);
    }

    public static void w(String msg) {
        android.util.Log.w(Tools.LOG_TAG, msg);
    }

    public static void w(String msg, Exception e) {
        android.util.Log.w(Tools.LOG_TAG, msg, e);
    }

    public static void d(String msg) {
        if (checkSettings() && sSettingsMgr.debugLog) {
            android.util.Log.d(Tools.LOG_TAG, msg);
        }
    }

    public static void d(String msg, Exception e) {
        if (checkSettings() && sSettingsMgr.debugLog) {
            android.util.Log.d(Tools.LOG_TAG, msg, e);
        }
    }
}
