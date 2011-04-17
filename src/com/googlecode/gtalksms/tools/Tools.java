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
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.util.Log;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class Tools {
    public final static String LOG_TAG = "gtalksms";
    public final static String APP_NAME = "GTalkSMS";
    public final static String LineSep = System.getProperty("line.separator");
    public final static int shortenTo = 35;
        
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
    
    public final static void writeFile(byte[] data, String filename) {
        File file = new File(filename);
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
            Log.i(LOG_TAG, "Writing file '" + filename + "'");
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to save file '" + filename + "'", e);
        }
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
     * Sends an String via an service intent
     * 
     * @param msg
     * @param to destination jid, can be null
     * @param ctx
     */
    public static void send(String msg, String to, Context ctx) {
        send(new XmppMsg(msg), to, ctx);
    }
    
    /**
     * Sends a XMPP Message via an service intent
     * 
     * @param msg
     * @param to destination jid, can be null
     * @param ctx
     */
    public static void send(XmppMsg msg, String to, Context ctx) {
        Intent intent = new Intent(MainService.ACTION_SEND);
        intent.setClass(ctx, MainService.class);
        if (to != null) {
            intent.putExtra("to", to);
        }
        intent.putExtra("xmppMsg", msg);
        ctx.startService(intent);
    }
    
    /**
     * Starts the GTalkSMS service with the given action
     * 
     * @param ctx
     * @param action
     */
    public static void startSvcIntent(final Context ctx, final String action) {
        final Intent i = newSvcIntent(ctx, action, null, null);
        ctx.startService(i);
    }
    
    /**
     * Composes a new intent for the GTalkSMS MainService
     * 
     * @param ctx
     * @param action
     * @param message the String extra "message", can be null
     * @param to the String extra "to", can be null for default notification address
     * @return
     */
    public static Intent newSvcIntent(final Context ctx, final String action, final String message, final String to) {
        final Intent i = new Intent(action, null, ctx, MainService.class);
        if (message != null) {
            i.putExtra("message", message);
        }
        if (to != null) {
            i.putExtra("to", to);
        }
        return i;
    }
    
    /**
     * Starts the GTalkSMS Service with an XMPP Message Received intent
     * 
     * @param ctx
     * @param message
     * @param from
     */
    public static void startSvcXMPPMsg(final Context ctx, final String message, final String from) {
        final Intent i = new Intent(MainService.ACTION_XMPP_MESSAGE_RECEIVED, null, ctx, MainService.class);
        i.putExtra("message", message);
        i.putExtra("from", from);
        ctx.startService(i);
    }
}
