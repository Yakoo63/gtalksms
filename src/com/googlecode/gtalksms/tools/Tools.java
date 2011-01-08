package com.googlecode.gtalksms.tools;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.googlecode.gtalksms.SettingsManager;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.widget.Toast;

public class Tools {
    public final static String LOG_TAG = "gtalksms";
    public final static String LineSep = System.getProperty("line.separator");
    
    public static void toastMessage(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }
    
    public static String getVersionName(Context context, Class<?> cls) {

        try {
            ComponentName comp = new ComponentName(context, cls);
            PackageInfo pinfo = context.getPackageManager().getPackageInfo(comp.getPackageName(), 0);

            return "v" + pinfo.versionName + " @ Yakoo";
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return "";
        }
    }
    
    public static String getVersion(Context context, Class<?> cls) {

        try {
            ComponentName comp = new ComponentName(context, cls);
            PackageInfo pinfo = context.getPackageManager().getPackageInfo(comp.getPackageName(), 0);

            return pinfo.versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return "";
        }
    }
    
    public static String getVersionCode(Context context, Class<?> cls) {

        try {
            ComponentName comp = new ComponentName(context, cls);
            PackageInfo pinfo = context.getPackageManager().getPackageInfo(comp.getPackageName(), 0);

            return "" + pinfo.versionCode;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return "";
        }
    }
    
    public static <T> List<T> getLastElements(ArrayList<T> list, int nbElems) {
        return list.subList(Math.max(list.size() - nbElems, 0), list.size());
    }
    
    public static Long getLong(Cursor c, String col) {
        return c.getLong(c.getColumnIndex(col));
    }
    
    public static int getInt(Cursor c, String col) {
        return c.getInt(c.getColumnIndex(col));
    }

    public static String getString(Cursor c, String col) {
        return c.getString(c.getColumnIndex(col));
    }

    public static boolean getBoolean(Cursor c, String col) {
        return getInt(c, col) == 1;
    }

    public static Date getDateSeconds(Cursor c, String col) {
        return new Date(Long.parseLong(Tools.getString(c, col)) * 1000);
    }

    public static Date getDateMilliSeconds(Cursor c, String col) {
        return new Date(Long.parseLong(Tools.getString(c, col)));
    }
    
    public static void setLocale(Context c) {
    	SettingsManager settingsMgr = new SettingsManager();
    	settingsMgr.importPreferences(c);
    	Locale l = settingsMgr.locale;

    	//Locale.setDefault(l);
    	
    	Configuration config = new Configuration();
    	config.setToDefaults();
        config.locale = l;
        c.getResources().updateConfiguration(config, c.getResources().getDisplayMetrics());
    }
    
}
