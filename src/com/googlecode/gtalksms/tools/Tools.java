package com.googlecode.gtalksms.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.database.Cursor;

import com.googlecode.gtalksms.SettingsManager;

public class Tools {
    public final static String LOG_TAG = "gtalksms";
    public final static String APP_NAME = "GTalkSMS";
    public final static String LineSep = System.getProperty("line.separator");
    public final static int shortenTo = 20;
        
    public final static String getVersionName(Context context) {

        try {
            PackageInfo pinfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String donated = "";
            
            if(isDonateAppInstalled(context)) {
                donated = "Full ";
            }
            return donated + "v" + pinfo.versionName + "\n@ Yakoo";
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return "";
        }
    }
    
    public final static String getVersion(Context context, Class<?> cls) {

        try {
            ComponentName comp = new ComponentName(context, cls);
            PackageInfo pinfo = context.getPackageManager().getPackageInfo(comp.getPackageName(), 0);

            return pinfo.versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return "";
        }
    }
    
    public final static String getVersionCode(Context context, Class<?> cls) {

        try {
            ComponentName comp = new ComponentName(context, cls);
            PackageInfo pinfo = context.getPackageManager().getPackageInfo(comp.getPackageName(), 0);

            return "" + pinfo.versionCode;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return "";
        }
    }
    
    public final static <T> List<T> getLastElements(ArrayList<T> list, int nbElems) {
        return list.subList(Math.max(list.size() - nbElems, 0), list.size());
    }
    
    public final static Long getLong(Cursor c, String col) {
        return c.getLong(c.getColumnIndex(col));
    }
    
    public final static int getInt(Cursor c, String col) {
        return c.getInt(c.getColumnIndex(col));
    }

    public final static String getString(Cursor c, String col) {
        return c.getString(c.getColumnIndex(col));
    }

    public final static boolean getBoolean(Cursor c, String col) {
        return getInt(c, col) == 1;
    }

    public final static Date getDateSeconds(Cursor c, String col) {
        return new Date(Long.parseLong(Tools.getString(c, col)) * 1000);
    }

    public final static Date getDateMilliSeconds(Cursor c, String col) {
        return new Date(Long.parseLong(Tools.getString(c, col)));
    }
    
    public final static void setLocale(SettingsManager setting, Context context) {

        Configuration config = new Configuration();
        config.setToDefaults();
        config.locale = setting.locale;
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }
    
    public final static Integer parseInt(String value) {
        Integer res = null;
        try { 
            res = Integer.parseInt(value); 
        } catch(Exception e) {}
        
        return res;
    }
    
    public final static int getMinNonNeg(int... x) {
        int min = Integer.MAX_VALUE;
        for(int i : x) {
            if(i >= 0 && i < min)
                min = i;
        }
        return min;
    }
    
    public final static boolean isDonateAppInstalled(Context context) {
        return 0 == context.getPackageManager().checkSignatures( context.getPackageName(), "com.googlecode.gtalksmsdonate");
    }
    
    public final static boolean copyFile(File from, File to) {
        if (!from.isFile() || !to.isFile())
            return false;

        InputStream in = null;
        OutputStream out = null;

        try {
            in = new FileInputStream(from);
            out = new FileOutputStream(to);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (Exception e) {
            return false;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    /* Ignore */
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    /* Ignore */
                }
            }
        }
        return true;
    }
    
    public final static String getAppBaseDir(Context ctx) {
        String filesDir = ctx.getFilesDir().toString();
        int index = filesDir.indexOf("/files");
        String res = filesDir.substring(0, index);
        return res;
    }
    
    public final static String getSharedPrefDir(Context ctx) {
        return getAppBaseDir(ctx) + "/shared_prefs";
    }
    
    public final static String shortenString(String s) {
        if (s.length() > 20) {
            return s.substring(0, 20);
        } else {
            return s;
        }
    }
    
    public final static String shortenMessage(String message) {
        String shortendMessage;
        if (message.length() < shortenTo) {
            shortendMessage = message.replace("\n", " ");
        } else {
            shortendMessage = message.substring(0, shortenTo).replace("\n", " ") + "...";
        }
        return shortendMessage;
    }
    
    /**
     * Get the bare jid from a full jid by cuttting any resource parts.
     * 
     * @param resourceJid The resource jid.
     * @return A bare jid.
     */
    public final static String getBareJid(String resourceJid) {
        int index = resourceJid.indexOf('/');
        if (index == -1) {
            return resourceJid;
        }
        return resourceJid.substring(0, index);
    }

    /**
     * Retrieve the user part of a jid.
     * 
     * @param jid The bare or full jid.
     * @return The user part of the jid.
     */
    public final static String getUser(String jid) {
        int index = jid.indexOf('@');
        if (index == -1) {
            return jid;
        }
        return jid.substring(0, index);
    }
    
    /**
     * Retrieve the domain part of a jid.
     * 
     * @param jid The bare or full jid.
     * @return The user part of the jid.
     */    
    public final static String getDomain(String jid) {
        jid = getBareJid(jid);
        int index = jid.indexOf('@');
        if (index == -1) {
            return jid;
        }
        return jid.substring(index + 1, jid.length());
    }
}
