package com.googlecode.gtalksms.tools;

import android.annotation.TargetApi;
import android.database.Cursor;
import android.os.Build;

import com.googlecode.gtalksms.SettingsManager;

public class Log {
    private static SettingsManager sSettingsMgr;
    private static String sAppPackage = "com.googlecode.gtalksms.";

    /**
     * Initialize the settings manager
     * @param settingsMgr
     */
    public static void initialize(SettingsManager settingsMgr) {
        sSettingsMgr = settingsMgr;
    }

    /**
     * @return True if the settings manager is set and debug logs are enabled, False otherwise
     */
    private static boolean canLog() {
        return sSettingsMgr == null || sSettingsMgr.debugLog;
    }

    /**
     * Returns the caller information from the stack trace
     * @return [Class@Method:line] string or empty
     */
    private static String caller() {
        try
        {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (StackTraceElement elem: stack) {
                String c = elem.getClassName();
                if (c.startsWith(sAppPackage) && !c.equals(Log.class.getName())) {
                    return "[" + elem.getClassName().replace(sAppPackage,"") + "@" + elem.getMethodName() + ":" + elem.getLineNumber() + "] ";
                }
            }
        }
        catch (Exception e) {}
        return "";
    }

    /**
     * Format the log
     * @param msg
     * @return
     */
    private static String f(String msg) {
        return caller() + msg;
    }

    public static void d(String msg) {
        if (canLog()) {
            android.util.Log.d(Tools.LOG_TAG, f(msg));
        }
    }

    public static void d(String msg, Exception e) {
        if (canLog()) {
            android.util.Log.d(Tools.LOG_TAG, f(msg), e);
        }
    }

    public static void i(String msg) {
        if (canLog()) {
            android.util.Log.i(Tools.LOG_TAG, f(msg));
        }
    }
    
    public static void i(String msg, Exception e) {
        if (canLog()) {
            android.util.Log.i(Tools.LOG_TAG, f(msg), e);
        }
    }

    public static void e(String msg) {
        android.util.Log.e(Tools.LOG_TAG, f(msg));
    }
    
    public static void e(String msg, Exception e) {
        android.util.Log.e(Tools.LOG_TAG, f(msg), e);
    }

    public static void w(String msg) {
        android.util.Log.w(Tools.LOG_TAG, f(msg));
    }

    public static void w(String msg, Exception e) {
        android.util.Log.w(Tools.LOG_TAG, f(msg), e);
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
