package com.googlecode.gtalksms.cmd;

import java.util.List;
import java.util.Map;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Process;
import android.os.SystemClock;
import android.os.Debug.MemoryInfo;
import android.telephony.TelephonyManager;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.tools.CrashedStartCounter;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class SystemCmd extends CommandHandlerBase {
    
    private final static int myPid = Process.myPid();
    private final static int myPidArray[] = { myPid };
    private static ActivityManager activityManager; 
    private static ConnectivityManager connectivityManager;
    private static MainService mainService;
    private static TelephonyManager telephonyManager;
    private static CrashedStartCounter sNullIntentStartCounter;
    
    public SystemCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_INTERNAL, new Cmd("sysinfo"), new Cmd("telinfo"));
        if (SystemCmd.mainService == null) {
            Context ctx = sContext;
            activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            telephonyManager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            SystemCmd.mainService = mainService;
            sNullIntentStartCounter = CrashedStartCounter.getInstance(ctx);
        }
    }

    @Override
    protected void execute(String cmd, String args) {
        XmppMsg res = new XmppMsg(); 
        if (cmd.equals("sysinfo")) {
            ActivityManager.MemoryInfo memInfoSystem = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memInfoSystem);
            MemoryInfo[] memInfoProc = activityManager.getProcessMemoryInfo(myPidArray);
            appendMemInfo(res, memInfoProc[0]);
            res.newLine();
            appendSystemMemInfo(res, memInfoSystem);
            res.newLine();
            appendImportance(res);
            res.newLine();
            appendDataConnectionStatus(res);
            res.newLine();
            appendXMPPConnectionData(res);
            res.newLine();
            appendSystemUptimeData(res);
            res.newLine();
            appendMonkeyTest(res);
            res.newLine();
            appendPreferences(res);
            res.newLine();
            appendTelephonStatus(res);
            res.newLine();
            appendNullIntentStartCounter(res);
        } else if (cmd.equals("telinfo")) {
            appendTelephonStatus(res);
        }
        send(res);
    }

    @Override
    protected void initializeSubCommands() {
    }
    
    private static String getMyImportance() {
        String res = "unkown";
        RunningAppProcessInfo myInfo = null;
        List<RunningAppProcessInfo> apps = activityManager.getRunningAppProcesses();
        for (RunningAppProcessInfo info : apps) {
            if (info.pid == myPid) {
                myInfo = info;
                break;
            }
        }
        if (myInfo != null) {
            switch (myInfo.importance) {
            case RunningAppProcessInfo.IMPORTANCE_BACKGROUND:
                res = "background";
                break;
            case RunningAppProcessInfo.IMPORTANCE_EMPTY:
                res = "empty";
                break;
            case RunningAppProcessInfo.IMPORTANCE_FOREGROUND:
                res = "foreground";
                break;
            case RunningAppProcessInfo.IMPORTANCE_SERVICE:
                res = "service";
                break;
            case RunningAppProcessInfo.IMPORTANCE_VISIBLE:
                res = "visible";
                break;
            default:
            }

        }        
        return res;
    }
    
    private static String getDataConnectionStatus() {
        String res = null;
        
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        
        res = "Type: '" + networkInfo.getTypeName() 
            + "' SubType: '" + networkInfo.getSubtypeName()
            + "' ExtraInfo: '" + networkInfo.getExtraInfo()
            + "'";
        
        return res;
    }
    
    private static void appendXMPPConnectionData(XmppMsg msg) {
        int reused = XmppManager.getReusedConnectionCount();
        int newcons = XmppManager.getNewConnectionCount();
        int total = reused + newcons;
        msg.appendBoldLine("XMPP Connection Data");
        msg.appendLine("Total connections: " + total + " thereof " + reused + " reused and " + newcons + " new");
    }
    
    private static void appendSystemUptimeData(XmppMsg msg) {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptimeMillis = SystemClock.uptimeMillis();
        long deepSleepMillis = elapsedRealtime - uptimeMillis;
        
        msg.appendBoldLine("System Uptime Information");
        msg.appendLine("System has been up for " + msToDaysHoursMins(elapsedRealtime));
        msg.appendLine("System was in deep sleep for " + msToDaysHoursMins(deepSleepMillis));
        msg.appendLine("System was awake " + getPercent(elapsedRealtime, uptimeMillis) + " of the time");
    }
    
    private static String msToDaysHoursMins(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds %= 60;
        long hours = minutes / 60;
        minutes %= 60;
        long days = hours / 24;
        hours %= 24;
        
        return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
    }
    
    private static String getPercent(long full, long part) {
        float percent = (float) part / (float) full;
        percent = percent * 100;
        int res = (int) percent;
        return res + "%";
    }
    
    private static void appendMonkeyTest(XmppMsg msg) {
        msg.appendBoldLine("Are you a Monkey test");
        if (ActivityManager.isUserAMonkey()) {
            msg.appendLine("You ARE a Monkey");
        } else {
            msg.appendLine("Sadly, you are someting else. Maybe even human");
        }
    }
    
    private static void appendMemInfo(XmppMsg msg, MemoryInfo memInfoProc) {
        if (memInfoProc == null) 
            return;
        
        msg.appendBoldLine(Tools.APP_NAME + " Memory Information");
        msg.appendItalicLine("Total");
        msg.appendLine("Private dirty: " + memInfoProc.getTotalPrivateDirty()
                        + XmppMsg.makeBold(" Proportial set size: ") + memInfoProc.getTotalPss()
                        + " Shared dirty: " + memInfoProc.getTotalSharedDirty());
        msg.appendItalicLine("Detailed");
        msg.appendLine("private dirty pages used by dalvik: " + memInfoProc.dalvikPrivateDirty);
        msg.appendLine("proportional set size for dalvik: " + memInfoProc.dalvikPss);
        msg.appendLine("shared dirty pages used by dalvik: " + memInfoProc.dalvikSharedDirty);
        msg.appendLine("private dirty pages by the native heap: " + memInfoProc.nativePrivateDirty);
        msg.appendLine("proportional set size for the native heap: " + memInfoProc.nativePss);
        msg.appendLine("shared dirty pages used by the native heap: " + memInfoProc.nativeSharedDirty);
        msg.appendLine("private dirty pages used by everything else: " + memInfoProc.otherPrivateDirty);
        msg.appendLine("proportional set size for everything else: " + memInfoProc.otherPss);
        msg.appendLine("shared dirty pages uses by everything else: " + memInfoProc.otherSharedDirty);  
    }
    
    private static void appendSystemMemInfo(XmppMsg msg, ActivityManager.MemoryInfo memInfoSystem) {
        msg.appendBoldLine("System Memory Information");
        msg.appendLine("AvailMem: " + memInfoSystem.availMem);
        if (memInfoSystem.lowMemory) {
            msg.appendLine("System is IN low memory situation");
        } else {
            msg.appendLine("System is NOT in low memory situation");
        }
        msg.appendLine("Low memory situation if AvailMem is under " + memInfoSystem.threshold);      
    }
    
    private static void appendPreferences(XmppMsg msg) {
        msg.appendBoldLine(Tools.APP_NAME + " Preferences");
        Map<String, ?> allSharedPrefs = sSettingsMgr.getAllSharedPreferences();
        for(Map.Entry<String, ?> pairs : allSharedPrefs.entrySet()) {
            String key = pairs.getKey();
            String value = pairs.getValue().toString();
            if (!key.equals("password")) {
                msg.appendLine(key + ": " + value);
            }
        }
    }
    
    private static void appendImportance(XmppMsg msg) {
        msg.appendBold("Importance: ");
        msg.appendLine(getMyImportance());
    }
    
    private static void appendDataConnectionStatus(XmppMsg msg) {
        msg.appendBoldLine("Data connection status");
        msg.appendLine(getDataConnectionStatus());
    }
    
    private static void appendTelephonStatus(XmppMsg msg) {
        msg.appendBoldLine("TelephonyManager");
        msg.appendLine("DeviceID: " + telephonyManager.getDeviceId());
        msg.appendLine("Device Software Version: " + telephonyManager.getDeviceSoftwareVersion());
        msg.appendLine("Line1Number: " + telephonyManager.getLine1Number());
        msg.appendLine("SIM Serial # " + telephonyManager.getSimSerialNumber());
        msg.appendLine("Subscriber ID: " + telephonyManager.getSubscriberId());
        msg.appendLine("Voice Mail Alpha Tag: " + telephonyManager.getVoiceMailAlphaTag());
        msg.appendLine("Voice Mail Number: " + telephonyManager.getVoiceMailNumber());
        msg.appendLine("Current operator: " + telephonyManager.getNetworkOperatorName());
        msg.appendLine("Sim operator: " + telephonyManager.getSimOperatorName());
        msg.appendLine("Roaming activated: " + telephonyManager.isNetworkRoaming());
     }
    
    private static void appendNullIntentStartCounter(XmppMsg msg) {
        msg.appendBoldLine("Null Intents Starts");
        msg.appendLine("Hot often was " + Tools.APP_NAME + " restarted by Android");
        long[] values = sNullIntentStartCounter.getLastValues(7);
        String line = "";        
        for (int i = 0; i < values.length; i++) {
            line += values[i] + " ";
        }
        msg.appendLine(line);
    }
}
