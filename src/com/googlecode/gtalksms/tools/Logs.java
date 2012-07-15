package com.googlecode.gtalksms.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.googlecode.gtalksms.SettingsManager;

public class Logs {
    public final static String LINE_SEPARATOR = System.getProperty("line.separator");
    boolean mStop = false;
    boolean mIncludeContext = false;
    String mTags = "";
    
    public Logs(boolean includeContext) {
        mIncludeContext = includeContext;
    }
    
    public Logs(String tags, boolean includeContext) {
        mIncludeContext = includeContext;
        mTags = tags;
    }
    
    public void stop() {
        mStop = true;
    }
    
    public String getLogs(Context ctx, int maxLength){
        return getLogs(ctx, maxLength, Arrays.asList(new String[] {"-v", "time", "AndroidRuntime:E", "gtalksms:V", "*:S", mTags}));
    }
    
    public String getLogs(Context ctx, int maxLength, List<String> list){
        final StringBuilder log = new StringBuilder();
        log.append(LINE_SEPARATOR);
        try{
            ArrayList<String> commandLine = new ArrayList<String>();
            commandLine.add("logcat");//$NON-NLS-1$
            commandLine.add("-d");//$NON-NLS-1$
            if (list != null){
                commandLine.addAll(list);
            }

            Process process = Runtime.getRuntime().exec(commandLine.toArray(new String[0]));
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
           
            String line;
            ArrayList<String> lines = new ArrayList<String>();
            while ((line = bufferedReader.readLine()) != null){
                if (mStop) {
                    return "";
                }
                
                lines.add(line);
            }
            
            // Truncate if necessary
            for (int i = Math.max(0, lines.size() - maxLength) ; i < lines.size() ; ++i) {
                log.append(lines.get(i));
                log.append(LINE_SEPARATOR);
            }
        }
        catch (Exception e){
            Log.e(Tools.LOG_TAG, "CollectLogTask.doInBackground failed", e);
        }
        if (mIncludeContext) {
            log.insert(0, LINE_SEPARATOR);
            log.insert(0, LINE_SEPARATOR);
            log.insert(0, "Kernel info: " + getFormattedKernelVersion());
            log.insert(0, LINE_SEPARATOR);
            log.insert(0, "Android API: " + Build.VERSION.SDK_INT); 
            log.insert(0, LINE_SEPARATOR);
            log.insert(0, Tools.APP_NAME + " Version: " + getVersionNumber(ctx));
            log.insert(0, LINE_SEPARATOR);
            log.insert(0, getPreferences(ctx));
        }
        return log.toString();
    }
    
    private String getVersionNumber(Context context)
    {
        String version = "?";
        try
        {
            PackageInfo packagInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = packagInfo.versionName;
        }
        catch (PackageManager.NameNotFoundException e){};
       
        return version;
    }
   
    private String getFormattedKernelVersion()
    {
        String procVersionStr;

        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/version"), 256);
            try {
                procVersionStr = reader.readLine();
            } finally {
                reader.close();
            }

            final String PROC_VERSION_REGEX =
                "\\w+\\s+" + /* ignore: Linux */
                "\\w+\\s+" + /* ignore: version */
                "([^\\s]+)\\s+" + /* group 1: 2.6.22-omap1 */
                "\\(([^\\s@]+(?:@[^\\s.]+)?)[^)]*\\)\\s+" + /* group 2: (xxxxxx@xxxxx.constant) */
                "\\([^)]+\\)\\s+" + /* ignore: (gcc ..) */
                "([^\\s]+)\\s+" + /* group 3: #26 */
                "(?:PREEMPT\\s+)?" + /* ignore: PREEMPT (optional) */
                "(.+)"; /* group 4: date */

            Pattern p = Pattern.compile(PROC_VERSION_REGEX);
            Matcher m = p.matcher(procVersionStr);

            if (!m.matches()) {
                Log.e(Tools.LOG_TAG, "Regex did not match on /proc/version: " + procVersionStr);
            } else if (m.groupCount() < 4) {
                Log.e(Tools.LOG_TAG, "Regex match on /proc/version only returned " + m.groupCount() + " groups");
            } else {
                return (new StringBuilder(m.group(1)).append("\n").append(
                        m.group(2)).append(" ").append(m.group(3)).append("\n")
                        .append(m.group(4))).toString();
            }
        } catch (IOException e) {  
            Log.e(Tools.LOG_TAG, "IO Exception when getting kernel version for Device Info screen", e);
        }
        return "--";
    }
    
    private String getPreferences(Context ctx) {
        StringBuilder res = new StringBuilder();
        res.append(Tools.APP_NAME + " Preferences" + LINE_SEPARATOR);
        SettingsManager settings = SettingsManager.getSettingsManager(ctx);
        Map<String, ?> allSharedPrefs = settings.getAllSharedPreferences();
        for (Map.Entry<String, ?> pairs : allSharedPrefs.entrySet()) {
            String key = pairs.getKey();
            String value = pairs.getValue().toString();
            if (!key.equals("password")) {
                res.append(key + ": " + value + LINE_SEPARATOR);
            }
        }
        return res.toString();
    }
}
