package com.googlecode.gtalksms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppMsg;

// TODO: auto activate this service in accessibility options of Android
// TODO: add a description for the service
// TODO: add shortcut button to accessibility panel Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS); startActivityForResult(intent, 0);

public class AccessibilityService extends android.accessibilityservice.AccessibilityService {

    HashMap<String, String> mInstalledApplications = new HashMap<String, String>();
    HashMap<String, String> mLastMessage = new HashMap<String, String>();
    HashMap<String, Long> mLastTimeStamp = new HashMap<String, Long>();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(Tools.LOG_TAG, "onAccessibilityEvent");
        try {
            String appName = getApplicationName(event.getPackageName().toString());
            Log.d(Tools.LOG_TAG, "[PackageName]         " + event.getPackageName());
            Log.d(Tools.LOG_TAG, "[Application]         " + appName);
            Log.d(Tools.LOG_TAG, "[EventTime]           " + event.getEventTime());
            
            String message = "";
            try {
                // Dump the notification into a local view to parse it
                Notification notification = (Notification) event.getParcelableData();
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                ViewGroup localView = (ViewGroup) inflater.inflate(notification.contentView.getLayoutId(), null);
                notification.contentView.reapply(getApplicationContext(), localView);
                
                // Find all texts of the notification
                ArrayList<TextView> views = new ArrayList<TextView>();
                getAllTextView(views, localView);
                for (TextView v: views) {
                    String text = v.getText().toString();
                    if (!text.isEmpty()) {
                        Log.d(Tools.LOG_TAG, "[Text]                " + text);
                        message += text + "\n";
                    }
                }
            } catch (Exception e) {
                Log.e(Tools.LOG_TAG, "Failed to parse the notification.", e);
            }
            
            if (message.isEmpty()) {
                message = getEventText(event.getText());
            }
            
            // Format the result
            XmppMsg msg = new XmppMsg();
            msg.append("New notification from  ");
            msg.appendBoldLine(appName);
            msg.append(message.trim());
            
            boolean ignore = false;
            // Avoid duplicated notifications sent in less than 2s (ie download manager)
            if (mLastMessage.containsKey(appName) && mLastMessage.get(appName).equals(msg.generateTxt())) {
                long old = mLastTimeStamp.get(appName);
                if (event.getEventTime() - old < 2000) {
                    ignore = true;
                }
            }
                
            // Ignore GTalkSMS notifications and send others
            if (!ignore && !event.getPackageName().equals(getBaseContext().getPackageName())) {
                Tools.send(msg, null, getBaseContext());
            }
            
            // Keep last message reference
            mLastMessage.put(appName, msg.generateTxt());
            mLastTimeStamp.put(appName, event.getEventTime());
        } catch (Exception e) {
            Log.e(Tools.LOG_TAG, "Failed to process notification", e);
        }
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
    
    @Override
    public void onInterrupt() {
        Log.v(Tools.LOG_TAG, "onInterrupt");
    }
 
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.v(Tools.LOG_TAG, "onServiceConnected");

        try {
            refreshApplicationList();
            mLastMessage.clear();
            mLastTimeStamp.clear();
            
            AccessibilityServiceInfo info = new AccessibilityServiceInfo();
            info.flags = AccessibilityServiceInfo.DEFAULT;
            info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
            setServiceInfo(info);
        } catch (Exception e) {
            Log.e(Tools.LOG_TAG, "Failed to configure accessibility service", e);
        }
    }
    
    private String getEventText(List<CharSequence> msg) {
        StringBuilder sb = new StringBuilder();
        for (CharSequence s : msg) {
            sb.append(s);
        }
        return sb.toString();
    }
 
    private void getAllTextView(ArrayList<TextView> views, ViewGroup v)
    {
        if (null == views) {
            return;
        }
        for (int i = 0; i < v.getChildCount(); i++)
        {
            Object child = v.getChildAt(i); 
            if (child instanceof TextView)
            {
                views.add((TextView)child);
            }
            else if(child instanceof ViewGroup)
            {
                getAllTextView(views, (ViewGroup)child);  // Recursive call.
            }
        }
    }
    
    private void refreshApplicationList() {
        final PackageManager pm = getBaseContext().getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        
        for (ApplicationInfo packageInfo : packages) {
            mInstalledApplications.put(packageInfo.packageName, packageInfo.loadLabel(pm).toString());
        }
    } 
}
