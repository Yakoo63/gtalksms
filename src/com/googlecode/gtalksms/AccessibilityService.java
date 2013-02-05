package com.googlecode.gtalksms;

import java.util.HashMap;
import java.util.List;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppMsg;

// TODO: auto activate this service in accessibity options of Android
// TODO: add a desc for the service
// TODO: manage all messages even for GTalkSMS
// TODO: filter package messages to avoid flooding like download package
// TODO: manage parcelable data of the event

public class AccessibilityService extends android.accessibilityservice.AccessibilityService {

    HashMap<String, String> mInstalledApplications = new HashMap<String, String>();
    HashMap<String, String> mLastMessage = new HashMap<String, String>();
    HashMap<String, Long> mLastTimeStamp = new HashMap<String, Long>();

    private String getEventText(List<CharSequence> msg) {
        StringBuilder sb = new StringBuilder();
        for (CharSequence s : msg) {
            sb.append(s);
        }
        return sb.toString();
    }
 
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(Tools.LOG_TAG, "onAccessibilityEvent");
        Log.d(Tools.LOG_TAG, "[ClassName]           " + event.getClassName());
        Log.d(Tools.LOG_TAG, "[PackageName]         " + event.getPackageName());

        String appName = getApplicationName(event.getPackageName().toString());
        Log.d(Tools.LOG_TAG, "[Application]         " + appName);

        Log.d(Tools.LOG_TAG, "[ContentDescription]  " + event.getContentDescription());
        Log.d(Tools.LOG_TAG, "[EventTime]           " + event.getEventTime());
        Log.d(Tools.LOG_TAG, "[BeforeText]          " + event.getBeforeText());
        Log.d(Tools.LOG_TAG, "[Text]                " + getEventText(event.getText()));
        Log.d(Tools.LOG_TAG, "[ItemCount]           " + event.getItemCount());
        Log.d(Tools.LOG_TAG, "[RecordCount]         " + event.getRecordCount());
        for (int i = 0; i < event.getRecordCount(); ++i) {
            Log.d(Tools.LOG_TAG, "[RecordText " + i + "]                  " + getEventText(event.getRecord(i).getText()));
            Log.d(Tools.LOG_TAG, "[getContentDescription " + i + "]       " + event.getRecord(i).getContentDescription());
        }

        XmppMsg msg = new XmppMsg();
        msg.append("New notification from  ");
        msg.appendBold(appName + ": ");
        msg.append(getEventText(event.getText()));

        boolean ignore = false;
        // Avoid duplicated notifications sent in less than 1s
        if (mLastMessage.containsKey(appName) && mLastMessage.get(appName).equals(msg.generateTxt())) {
            long old = mLastTimeStamp.get(appName);
            if (event.getEventTime() - old < 1000) {
                ignore = true;
            }
        }
            
        // Ignore GTalkSMS notifications and send others
        if (!ignore && !event.getPackageName().equals(getBaseContext().getPackageName())) {
            Tools.send(msg, null, getBaseContext());
        }
        
        mLastMessage.put(appName, msg.generateTxt());
        mLastTimeStamp.put(appName, event.getEventTime());
    }
    
    private String getApplicationName(String packageName) {
        String name = packageName;
        
        if (mInstalledApplications.containsKey(packageName)) {
            name = mInstalledApplications.get(packageName);
        } else {
            refreshApplicationList();
            if (mInstalledApplications.containsKey(packageName)) {
                name = mInstalledApplications.get(packageName);
            }
        }
        
        return name;
    }
    
    private void refreshApplicationList() {
        final PackageManager pm = getBaseContext().getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        
        for (ApplicationInfo packageInfo : packages) {
            mInstalledApplications.put(packageInfo.packageName, packageInfo.loadLabel(pm).toString());
        }
    }
 
    @Override
    public void onInterrupt() {
        Log.v(Tools.LOG_TAG, "onInterrupt");
    }
 
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.v(Tools.LOG_TAG, "onServiceConnected");

        refreshApplicationList();
        mLastMessage.clear();
        mLastTimeStamp.clear();
        
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.flags = AccessibilityServiceInfo.DEFAULT;
        info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        setServiceInfo(info);
    }
}
