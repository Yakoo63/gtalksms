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
        settingsMgr = svc.getSettingsManager();
    }

    private MainService svc;
    private SettingsManager settingsMgr;

    public void onCallStateChanged(int state, String incomingNumber) {
        if (state == TelephonyManager.CALL_STATE_RINGING) {
            if(settingsMgr.debugLog) 
                Log.d(Tools.LOG_TAG, "PhoneCallListener Call State Ringing with incomingNumber:" + incomingNumber);
            String contact = ContactsManager.getContactName(svc, incomingNumber);
            svc.send(svc.getString(R.string.chat_is_calling, contact), null);
        }
    }
}
