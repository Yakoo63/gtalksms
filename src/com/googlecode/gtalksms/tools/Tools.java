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
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.database.Cursor;
import android.widget.Toast;

import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;

public class Tools {
    public final static String LOG_TAG = "gtalksms";
    public final static String APP_NAME = "GTalkSMS";
    public final static String LineSep = System.getProperty("line.separator");
    public final static String GTalkSMSSignature = "TODO: Add GtalkSMS Signature Fingerprint Here"; // TODO add key
    
    public static void toastMessage(Context ctx, String msg) {
        String toastMsg  = ctx.getString(R.string.app_name) + ": " + msg;
        Toast.makeText(ctx, toastMsg, Toast.LENGTH_SHORT).show();
    }
        
    public static String getVersionName(Context context, Class<?> cls) {

        try {
            ComponentName comp = new ComponentName(context, cls);
            PackageInfo pinfo = context.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
            String donate = "";
            
            if(isDonateAppInstalled(context)) {
                donate = "Full ";
            }
            return donate + "v" + pinfo.versionName + "\n@ Yakoo";
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
    
    public static void setLocale(SettingsManager setting, Context context) {

        Configuration config = new Configuration();
        config.setToDefaults();
        config.locale = setting.locale;
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }
    
    public static Integer parseInt(String value) {
        Integer res = null;
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
        List<PackageInfo> packs = context.getPackageManager().getInstalledPackages(0);  
        for(PackageInfo p : packs) {
            if (p.packageName.equalsIgnoreCase("com.googlecode.gtalksmsdonate")) {  
                for (Signature s : p.signatures) {
                    if(s.toString().equals(GTalkSMSSignature)) {
                        return true;
                    }
                }
                return false;  // package found but has not the right signature
            }  
        }
        return false;
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
    
    public static String getAppBaseDir(Context ctx) {
        String filesDir = ctx.getFilesDir().toString();
        int index = filesDir.indexOf("/files");
        String res = filesDir.substring(0, index);
        return res;
    }
    
    public static String getSharedPrefDir(Context ctx) {
        return getAppBaseDir(ctx) + "/shared_prefs";
    }
}
