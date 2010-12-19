package com.googlecode.gtalksms.receivers;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.googlecode.gtalksms.MainService;

public class PhoneCallListener extends PhoneStateListener {

    public void onCallStateChanged(int state, String incomingNumber) {
        switch(state) {
            case TelephonyManager.CALL_STATE_IDLE:
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                break;
            case TelephonyManager.CALL_STATE_RINGING:
                MainService service = MainService.getInstance();
                if (service != null) {
                    service.OnIncomingCall(incomingNumber);
                }
                break;
        }
    }
}
