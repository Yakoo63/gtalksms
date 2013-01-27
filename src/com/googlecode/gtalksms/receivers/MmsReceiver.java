package com.googlecode.gtalksms.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.data.contacts.ContactsManager;
import com.googlecode.gtalksms.tools.Tools;


public class MmsReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Tools.LOG_TAG, "New MMS");
        
        try {
        	Bundle bundle = intent.getExtras();
            if (bundle != null) {
                byte[] dataBytes = bundle.getByteArray("data");
                if (dataBytes != null) {
                    String buffer = new String(bundle.getByteArray("data"));
                    Log.d(Tools.LOG_TAG, "MMS data = " + buffer);
                
                    int indx = buffer.indexOf("/TYPE");
                    if (indx > 0 && (indx - 15) > 0) {
                        int newIndx = indx - 15;
                        String incomingNumber = buffer.substring(newIndx, indx);
                        indx = incomingNumber.indexOf("+");
                        if (indx > 0) {
                        	incomingNumber = ContactsManager.getContactName(context, incomingNumber.substring(indx));
                            context.startService(Tools.newSvcIntent(context, MainService.ACTION_SEND, context.getString(R.string.chat_mms_from, incomingNumber), null));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(Tools.LOG_TAG, "MMS data Exception caught: " + e.getMessage(), e);
        }
    }
}
