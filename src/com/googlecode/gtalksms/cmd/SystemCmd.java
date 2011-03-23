package com.googlecode.gtalksms.cmd;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Process;
import android.os.Debug.MemoryInfo;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class SystemCmd extends Command {
    
    private final static int myPid = Process.myPid();
    private final static int myPidArray[] = { myPid };
    private static ActivityManager activityManager; 
    private static ConnectivityManager connectivityManager;
    
    public SystemCmd(MainService mainService) {
        super(mainService, new String[] {"system"}, Command.TYPE_SYSTEM); 
        activityManager = (ActivityManager) mainService.getBaseContext().getSystemService(Context.ACTIVITY_SERVICE);
        connectivityManager = (ConnectivityManager) mainService.getBaseContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    protected void execute(String cmd, String args) {
        XmppMsg res = new XmppMsg(); 
        ActivityManager.MemoryInfo memInfoSystem = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memInfoSystem);
        MemoryInfo[] memInfoProc = activityManager.getProcessMemoryInfo(myPidArray);
        
        res.appendBoldLine("System Memory Information");
        res.appendLine("AvailMem: " + memInfoSystem.availMem);
        if (memInfoSystem.lowMemory) {
            res.appendLine("System is IN low memory situation");
        } else {
            res.appendLine("System is NOT in low memory situation");
        }
        res.appendLine("Low memory situation if AvailMem is under " + memInfoSystem.threshold);
        
        res.appendBoldLine("GTalkSMS Memory Information");
        res.appendItalicLine("Total");
        res.appendLine("Private dirty: " + memInfoProc[0].getTotalPrivateDirty()
                        + " Proportial set size: " + memInfoProc[0].getTotalPss()
                        + " Shared dirty: " + memInfoProc[0].getTotalSharedDirty());
        res.appendItalicLine("Detailed");
        res.appendLine("private dirty pages used by dalvik: " + memInfoProc[0].dalvikPrivateDirty);
        res.appendLine("proportional set size for dalvik: " + memInfoProc[0].dalvikPss);
        res.appendLine("shared dirty pages used by dalvik: " + memInfoProc[0].dalvikSharedDirty);
        res.appendLine("private dirty pages by the native heap: " + memInfoProc[0].nativePrivateDirty);
        res.appendLine("proportional set size for the native heap: " + memInfoProc[0].nativePss);
        res.appendLine("shared dirty pages used by the native heap: " + memInfoProc[0].nativeSharedDirty);
        res.appendLine("private dirty pages used by everything else: " + memInfoProc[0].otherPrivateDirty);
        res.appendLine("proportional set size for everything else: " + memInfoProc[0].otherPss);
        res.appendLine("shared dirty pages uses by everything else: " + memInfoProc[0].otherSharedDirty);
        
        res.appendBoldLine("Are you a Monkey test");
        if (ActivityManager.isUserAMonkey()) {
            res.appendLine("You ARE a Monkey");
        } else {
            res.appendLine("Sadly, you are someting else. Maybe even human");
        }
        
        res.appendBold("Importance: ");
        res.appendLine(getMyImportance());
        
        res.appendBoldLine("Data connection status");
        res.appendLine(getDataConnectionStatus());
        
        
        
        send(res);
    }

    @Override
    public String[] help() {
        return null;
    }
    
    private String getMyImportance() {
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
    
    private String getDataConnectionStatus() {
        String res = null;
        
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        
        res = "Type: '" + networkInfo.getTypeName() 
            + "' SubType: '" + networkInfo.getSubtypeName()
            + "' ExtraInfo: '" + networkInfo.getExtraInfo()
            + "'";
        
        return res;
    }

}
