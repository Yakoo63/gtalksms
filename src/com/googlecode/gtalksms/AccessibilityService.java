package com.googlecode.gtalksms;

import java.util.List;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.googlecode.gtalksms.tools.Tools;

// TODO: get application name from package name
// TODO: add a desc for the service
// TODO: manage all messages even for GTalkSMS
// TODO: remove GTalkSMS notifications

public class AccessibilityService extends android.accessibilityservice.AccessibilityService {

    private String getEventText(List<CharSequence> msg) {
        StringBuilder sb = new StringBuilder();
        for (CharSequence s : msg) {
            sb.append(s);
        }
        return sb.toString();
    }
 
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        StringBuilder str = new StringBuilder();
        str.append("New notification:\n");
        Log.e(Tools.LOG_TAG, "onAccessibilityEvent");
        Log.e(Tools.LOG_TAG, "[ClassName]           " + event.getClassName());
        Log.e(Tools.LOG_TAG, "[PackageName]         " + event.getPackageName());
        str.append(event.getPackageName()+ "\n");
        Log.e(Tools.LOG_TAG, "[ContentDescription]  " + event.getContentDescription());
        Log.e(Tools.LOG_TAG, "[EventTime]           " + event.getEventTime());
        Log.e(Tools.LOG_TAG, "[BeforeText]          " + event.getBeforeText());
        Log.e(Tools.LOG_TAG, "[Text]                " + getEventText(event.getText()));
        str.append(getEventText(event.getText()) + "\n");
        Log.e(Tools.LOG_TAG, "[ItemCount]           " + event.getItemCount());
        Log.e(Tools.LOG_TAG, "[RecordCount]         " + event.getRecordCount());
        for (int i = 0; i < event.getRecordCount(); ++i) {
            Log.e(Tools.LOG_TAG, "[RecordText " + i + "]                  " + getEventText(event.getRecord(i).getText()));
            str.append(i + " - " + getEventText(event.getRecord(i).getText()) + "\n");
        }
        getBaseContext().startService(Tools.newSvcIntent(getBaseContext(), MainService.ACTION_SEND, str.toString(), null));
    }
 
    @Override
    public void onInterrupt() {
        Log.v(Tools.LOG_TAG, "onInterrupt");
    }
 
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.v(Tools.LOG_TAG, "onServiceConnected");
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.flags = AccessibilityServiceInfo.DEFAULT;
        info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        setServiceInfo(info);
    }
}
