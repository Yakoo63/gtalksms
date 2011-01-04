package com.googlecode.gtalksms.receivers;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.data.contacts.ContactsManager;

public class PhoneCallListener extends PhoneStateListener {
    public PhoneCallListener(MainService svc) {
        super();
        _svc = svc;
    }
    private MainService _svc;
    
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
                if (_manageIncoming) {
                    _manageIncoming = false;
                    String contact = ContactsManager.getContactName(_svc, incomingNumber);
                    _svc.send(_svc.getString(R.string.chat_is_calling, contact));
                }
                break;
        }
    }
}
