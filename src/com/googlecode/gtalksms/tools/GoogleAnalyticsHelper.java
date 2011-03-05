package com.googlecode.gtalksms.tools;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import android.content.Context;
import android.util.Log;

public class GoogleAnalyticsHelper {
    public static final int NONE = 0;
    public static final int FRESH_INSTALL = 1;
    public static final int UPDATE = 2;
    
    private final String version_filename;
    private final Context ctx;
    private final String version;
    private final String datefile;
    private static GoogleAnalyticsTracker gAnalytics;
    private static boolean statisticsEnabled;
    private static boolean run = false;
    
    public GoogleAnalyticsHelper(Context c) {
        statisticsEnabled = true;  //TODO preference
        version = Tools.getVersionCode(c, getClass());
        this.ctx = c;
        version_filename = Tools.LOG_TAG + "_version_" + version;
        datefile = Tools.LOG_TAG + "_datefile";
        if (statisticsEnabled) {  //avoid running GoogleAnalytics if it is not wanted to save memory and cpu
            gAnalytics = GoogleAnalyticsTracker.getInstance();
            gAnalytics.setProductVersion(Tools.getVersion(c, getClass()),
                    Tools.getVersionCode(c, getClass()));
            gAnalytics.start("UA-21339659-1", c);
        }
    }
    
    public static void stop() {
        if (gAnalytics != null) {
            gAnalytics.stop();
        }
    }
    
    public static void trackEvent(String action, String label, int value) {
        if(gAnalytics != null) {
            gAnalytics.trackEvent(Tools.APP_NAME, action, label, value);
        }
    }
    
    public static void trackAndLogError(String errorMsg) {
        if (gAnalytics != null) {
            gAnalytics.trackEvent(Tools.APP_NAME, "error", errorMsg, 0);
        }
        Log.e(Tools.LOG_TAG, errorMsg);
    }

    public static void trackAndLogWarning(String warningMsg) {
        if (gAnalytics != null) {
            gAnalytics.trackEvent(Tools.APP_NAME, "error", warningMsg, 0);
        }
        Log.w(Tools.LOG_TAG, warningMsg);
    }

    public static void trackAndLogError(String errorMsg, Exception e) {
        if (gAnalytics != null) {
            gAnalytics.trackEvent(Tools.APP_NAME, "error", errorMsg, 0);
        }
        Log.e(Tools.LOG_TAG, errorMsg + " " + e);
    }

    public static void trackAndLogWarning(String warningMsg, Exception e) {
        if (gAnalytics != null) {
            gAnalytics.trackEvent(Tools.APP_NAME, "error", warningMsg, 0);
        }
        Log.w(Tools.LOG_TAG, warningMsg + " " + e);
    }

    public void trackInstalls() {
        if (!run && (gAnalytics != null)) {
            switch (isNewInstallUpdate()) {
            case GoogleAnalyticsHelper.FRESH_INSTALL:
                gAnalytics.trackEvent("GTalkSMS", // Category
                        "Fresh Install", // Action
                        "Fresh Install:  " + Tools.getVersionName(ctx, getClass()), // Label
                        0); // Value
                break;
            case GoogleAnalyticsHelper.UPDATE:
                gAnalytics.trackEvent("GTalkSMS", // Category
                        "Update", // Action
                        "Update: " + Tools.getVersionName(ctx, getClass()), // Label
                        0); // Value
                break;
            }
            run = true;
        }
    }
    
    public void trackServiceStartsPerDay() {
       if(!datefileHasCurrentDate()) {
           gAnalytics.trackEvent("GTalkSMS", // Category
                   "Service", // Action
                   "StartPerDay", // Label
                   0); // Value
       }
    }
    
    public static boolean dispatch() {
        if (gAnalytics != null) {
            return gAnalytics.dispatch();
        } else {
            return false;
        }
    }
    
    /**
     * checks the contents of the datefile
     * and updates the datefile if its outdated
     * 
     * @return false if datefile is outdated, true otherwise
     */
    private boolean datefileHasCurrentDate() {
        char[] inputBuffer = new char[10];
        try {
            FileInputStream fIn = ctx.openFileInput(datefile);
            InputStreamReader isr = new InputStreamReader(fIn);
            isr.read(inputBuffer);
        } catch (IOException e) {
//            trackAndLogError("Reading datefile", e);  //commented out - just spams the event on fresh installs
        }
        if ((new String(inputBuffer)).equals(currentDate())) {
            return true;
        }
        createDatefile();
        return false;
    }
    
    private boolean createDatefile() {
        try {
            FileOutputStream fOut = ctx.openFileOutput(datefile, Context.MODE_PRIVATE); //MODE_APPEND not set, should be ok
            OutputStreamWriter osw = new OutputStreamWriter(fOut);
            osw.write(currentDate());
            osw.close();
        } catch (IOException ioe) {
            return false;
        }
        return true;
    }
    
    private String currentDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Calendar cal = Calendar.getInstance();
        return dateFormat.format(cal.getTime());
    }
    
//    private boolean datefileExists() {
//        for (String s : ctx.fileList()) {
//            if (s.contains(datefile)) {
//                return true;
//            }
//        }
//        return false;
//    }
    
    /**
     * Checks if the user has newly installed this app
     * or if it is an update
     * 
     * @return 2 on update, 1 if this is an fresh install, otherwise 0
     */
    private int isNewInstallUpdate() {
        if(isVersionInstalled()) {
            return NONE;
        } else if (hasOldVersionFile()) {
            deleteOldVersionFiles();
            createVersionFile();
            return UPDATE;
        } else {
            createVersionFile();
            return FRESH_INSTALL;
        }
    }
    
    /**
     * Checks if there are old version files
     * 
     * @return true if one or more old version files where found, otherwise false
     */
    private boolean hasOldVersionFile() {
        for(String s : ctx.fileList()) {
            if(s.contains(Tools.LOG_TAG + "_version_")) {  //TODO change to good regex
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if this version is already installed
     * 
     * @return true if this version is installed, otherwise false
     */
    private boolean isVersionInstalled() {
        for(String s : ctx.fileList())
            if(s.equals(version_filename))
                return true;
        
        return false;
    }
    
    /**
     * Creates the version file in the apps file storage
     * 
     * @return true on success, otherwise false
     */
    private boolean createVersionFile() {
        try {
            FileOutputStream fOut = ctx.openFileOutput(version_filename, Context.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);
            osw.write("GTalkSMS Version File. Versioin: " + version);
            osw.close();
        } catch (IOException ioe) {
            return false;
        }
        return true;
    }
    
    private void deleteOldVersionFiles() {
        for(String s : ctx.fileList()) {
            if (!s.equals(version_filename)) {
                ctx.deleteFile(s);
            }
        }
    }
}
