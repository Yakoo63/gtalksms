package com.googlecode.gtalksms.receivers;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.googlecode.gtalksms.MainService;

public class PhoneCallListener extends PhoneStateListener {
    
    static boolean _manageIncoming = true;
    
    public void onCallStateChanged(int state, String incomingNumber) {
        switch(state) {
            case TelephonyManager.CALL_STATE_IDLE:
                _manageIncoming = true;
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                _manageIncoming = true;
                break;
            case TelephonyManager.CALL_STATE_RINGING:
                MainService service = MainService.getInstance();
                if (service != null && _manageIncoming) {
                    _manageIncoming = false;
                    service.OnIncomingCall(incomingNumber);
                }
                break;
        }
    }
}
