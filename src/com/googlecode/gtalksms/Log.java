package com.googlecode.gtalksms;

import android.annotation.TargetApi;
import android.database.Cursor;
import android.os.Build;

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
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void dump(String prefix, Cursor cursor) {
        for (String name: cursor.getColumnNames()) {
            int index = cursor.getColumnIndex(name);
            switch (cursor.getType(index))
            {
                case Cursor.FIELD_TYPE_NULL:
                    Log.d(prefix + "Type null   - " + name);
                    break; 
                case Cursor.FIELD_TYPE_INTEGER:
                    Log.d(prefix + "Type int    - " + name + "=" + cursor.getInt(index));
                    break;
                case Cursor.FIELD_TYPE_FLOAT:
                    Log.d(prefix + "Type float  - " + name + "=" + cursor.getFloat(index));
                    break;
                case Cursor.FIELD_TYPE_STRING:
                    Log.d(prefix + "Type string - " + name + "=" + cursor.getString(index));
                    break;
                case Cursor.FIELD_TYPE_BLOB:
                    Log.d(prefix + "Type blob   - " + name + "=" + new String(cursor.getBlob(index)));
                    break;
            }
        }
    }
}
