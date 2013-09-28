package com.googlecode.gtalksms.receivers;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.data.contacts.ContactsManager;
import com.googlecode.gtalksms.tools.Tools;

public class PhoneCallListener extends PhoneStateListener {
    public PhoneCallListener(MainService svc) {
        super();
        this.svc = svc;
        settingsMgr = SettingsManager.getSettingsManager(svc);
    }

    private final MainService svc;
    private final SettingsManager settingsMgr;
    
    // Android seems to send the intent not only once per call
    // but every 10 seconds for ongoing ringing
    // we prevent multiple "is calling" notifications with this boolean
    private static boolean manageIncoming = true;

    /**
     * incomingNumber is null when the caller ID is hidden
     * 
     * @param state
     * @param incomingNumber
     */
    public void onCallStateChanged(int state, String incomingNumber) {
        if (MainService.IsRunning) {
            switch (state) {
            case TelephonyManager.CALL_STATE_IDLE:
                manageIncoming = true;
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                manageIncoming = true;
                break;
            case TelephonyManager.CALL_STATE_RINGING:
                if (settingsMgr.debugLog) {
                	Log.d(Tools.LOG_TAG, "PhoneCallListener Call State Ringing with incomingNumber=" + incomingNumber + " manageIncoming=" + manageIncoming);
                }
                
                if (manageIncoming) {
                    manageIncoming = false;
                    String contact = ContactsManager.getContactName(svc, incomingNumber);
                    svc.send(svc.getString(R.string.chat_is_calling, contact), null);
                }
                break;
            default:
                break;
            }
        }
    }
}
