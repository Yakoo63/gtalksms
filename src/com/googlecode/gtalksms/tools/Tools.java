package com.googlecode.gtalksms.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class Tools {
    public static final String AUTHORS_URL = "http://gtalksms.googlecode.com/git/AUTHORS";
    public static final String DONORS_URL = "http://gtalksms.googlecode.com/git/Donors";
    public static final String CHANGELOG_URL = "http://gtalksms.googlecode.com/git/Changelog";
    public final static String LOG_TAG = "gtalksms";
    public final static String APP_NAME = "GTalkSMS";
    public final static String LineSep = System.getProperty("line.separator");
    private final static int shortenTo = 35;
    
    public static String getFileFormat(Calendar c) {
        return 
            c.get(Calendar.YEAR) + 
            "-" + 
            String.format("%02d", (c.get(Calendar.MONTH)+ 1)) + 
            "-" + 
            String.format("%02d", c.get(Calendar.DAY_OF_MONTH)) + 
            " " + 
            String.format("%02d", c.get(Calendar.HOUR_OF_DAY)) + 
            "h" + 
            String.format("%02d", c.get(Calendar.MINUTE)) + 
            "m" + 
            String.format("%02d", c.get(Calendar.SECOND)) + 
            "s";
    }
    
    public static String getVersionName(Context context) {

        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return "";
        }
    }
    
    public static String getVersion(Context context, Class<?> cls) {

        try {
            ComponentName comp = new ComponentName(context, cls);
            return context.getPackageManager().getPackageInfo(comp.getPackageName(), 0).versionName;
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
    
    public static void setLocale(SettingsManager setting, Context context) {

        Configuration config = new Configuration();
        config.setToDefaults();
        config.locale = setting.locale;
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }
    
    public static boolean isInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException nfe) {}
        return false;
    }
    
    public static Boolean parseBool(String value) {
        Boolean res = null;
        if (value.compareToIgnoreCase("true") == 0) {
            res = true;
        } else if (value.compareToIgnoreCase("false") == 0) {
            res = false;
        }
        return res;
    }
    
    public static Integer parseInt(String value) {
        Integer res = null;
        try { 
            res = Integer.parseInt(value); 
        } catch(Exception e) {}
        
        return res;
    }
    
    public static Integer parseInt(String value, Integer defaultValue) {
        Integer res = defaultValue;
        try { 
            res = Integer.parseInt(value); 
        } catch(Exception e) {}
        
        return res;
    }
    
    public static int getMinNonNeg(int... x) {
        int min = Integer.MAX_VALUE;
        for(int i : x) {
            if(i >= 0 && i < min)
                min = i;
        }
        return min;
    }
    
    public static boolean isDonateAppInstalled(Context context) {
        return context.getPackageManager().checkSignatures(context.getPackageName(), "com.googlecode.gtalksmsdonate") == PackageManager.SIGNATURE_MATCH;
    }
    
    public static boolean copyFile(File from, File to) {
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
    
    public static boolean writeFile(byte[] data, File file) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private static String getAppBaseDir(Context ctx) {
        String filesDir = ctx.getFilesDir().toString();
        int index = filesDir.indexOf("/files");
        return filesDir.substring(0, index);
    }
    
    public static String getSharedPrefDir(Context ctx) {
        return getAppBaseDir(ctx) + "/shared_prefs";
    }
    
    public static String shortenString(String s) {
        if (s.length() > 20) {
            return s.substring(0, 20);
        } else {
            return s;
        }
    }
    
    public static String shortenMessage(String message) {
        String shortenedMessage;
        if (message == null) {
            shortenedMessage = "";
        } else if (message.length() < shortenTo) {
            shortenedMessage = message.replace("\n", " ");
        } else {
            shortenedMessage = message.substring(0, shortenTo).replace("\n", " ") + "...";
        }
        return shortenedMessage;
    }    
    
    /**
     * Sends an String via an service intent
     * 
     * @param msg
     * @param to destination jid, can be null
     * @param ctx
     * @return
     */
    public static boolean send(String msg, String to, Context ctx) {
        return send(new XmppMsg(msg), to, ctx);
    }
    
    /**
     * Sends a XMPP Message via an service intent
     * 
     * @param msg
     * @param to destination jid, can be null
     * @param ctx
     * @return
     */
    public static boolean send(XmppMsg msg, String to, Context ctx) {
        Intent intent = new Intent(MainService.ACTION_SEND);
        intent.setClass(ctx, MainService.class);
        if (to != null) {
            intent.putExtra("to", to);
        }
        intent.putExtra("xmppMsg", msg);
        return MainService.sendToServiceHandler(intent);
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
        MainService.sendToServiceHandler(i);
    }
    
    public static void openLink(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
    
    /**
     * Returns a String in the simple date format
     * 
     * @return the current date in dd/MM/yyyy format
     */
    public static String currentDate() {
        DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
        Calendar cal = Calendar.getInstance();
        return DATE_FORMAT.format(cal.getTime());
    }
    
    public static String ipIntToString(int ip) {
        return String.format("%d.%d.%d.%d", 
        (ip & 0xff), 
        (ip >> 8 & 0xff),
        (ip >> 16 & 0xff),
        (ip >> 24 & 0xff));
    }

    public static String STMArrayToString(StackTraceElement[] stma) {
        String res = "";
        for (StackTraceElement e : stma) {
            res += (e.toString() + '\n');
        }
        return res;
    }
}
