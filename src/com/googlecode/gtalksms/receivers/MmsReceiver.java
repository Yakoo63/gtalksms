package com.googlecode.gtalksms.receivers;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.cmd.smsCmd.Mms;
import com.googlecode.gtalksms.cmd.smsCmd.MmsManager;
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
                    MmsManager mmsManager = new MmsManager(context);
                    String buffer = new String(bundle.getByteArray("data"));
                    Log.d(Tools.LOG_TAG, "MMS data = " + buffer);
                    
                    // Ensure that the MMS is managed by the stock application
                    for (int i = 0; i < 10; ++i) {
                        try {
                            Thread.sleep(2000);
                        } catch (Exception e) {}
                        
                        // We assume that we don't received more than 10 messages at the same time
                        ArrayList<Mms> allMms = mmsManager.getLastReceivedMmsDetails(10);
                        
                        for (Mms mms: allMms) {
                            // Check if the retrieved MMS is the good one
                            if (mms != null && mms.getId() != null && buffer.contains(mms.getId())) {
                                context.startService(Tools.newSvcIntent(context, MainService.ACTION_SEND, 
                                context.getString(R.string.chat_mms_from, mms.getSender()) + mms.getSubject() + "\n" + mms.getMessage(), null));
                                return;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(Tools.LOG_TAG, "MMS data Exception caught: " + e.getMessage(), e);
        }
    }
}
