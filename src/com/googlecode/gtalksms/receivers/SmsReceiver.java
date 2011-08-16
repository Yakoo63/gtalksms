package com.googlecode.gtalksms.receivers;

import java.util.HashMap;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import com.googlecode.gtalksms.Log;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.tools.Tools;


public class SmsReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO remove the settingsManager when issues 203 and 149 are resolved
        Log.initialize(SettingsManager.getSettingsManager(context));
        
        // There can be multiple SMS from multiple senders, there can be a maximum of nbrOfpdus different senders
        // However, send long SMS of same sender in one message
        Log.i("SmsReceiver: got new sms intent, calling RetrieveMessages");
        Map<String, String> msg = RetrieveMessages(intent);
           
        if (MainService.IsRunning) {
            // send all SMS via XMPP by sender
            for (String sender : msg.keySet()) {
                Intent svcintent = Tools.newSvcIntent(context, MainService.ACTION_SMS_RECEIVED, msg.get(sender), null);
                svcintent.putExtra("sender", sender);
                Log.i("SmsReceiver: Issuing service intent for incoming SMS. sender=" + sender + " message=" + msg.get(sender).substring(0,  20));
                context.startService(svcintent);
            }
        // MainService is not active, test if we find a sms with the magic word
        } else {
            String magicWord = MainService.getSettingsManager().smsMagicWord.trim().toLowerCase();
            
            for (String sender : msg.keySet()) {
                String message = msg.get(sender);
                if (message.trim().toLowerCase().compareTo(magicWord) == 0) {
                    Log.i("Connection command received by SMS from " + sender + " issuing intent " + MainService.ACTION_CONNECT);
                    Tools.startSvcIntent(context, MainService.ACTION_CONNECT);
                }
            }
        }
    }
    
    private static Map<String, String> RetrieveMessages(Intent intent) {
        Map<String, String> msg = new HashMap<String, String>();
        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs = null;
        
        if (bundle != null && bundle.containsKey("pdus")) {
            Object[] pdus = (Object[]) bundle.get("pdus");

            if (pdus != null) {
                int nbrOfpdus = pdus.length;
                msgs = new SmsMessage[nbrOfpdus];
                
                for (int i = 0; i < nbrOfpdus; i++) {
                    msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                    
                    String msgString = msg.get(msgs[i].getOriginatingAddress());
                    
                    // Check if index with number exists                    
                    if(msgString == null) { // Index with number doesn't exist                                               
                        // Save string into associative array with sender number as index
                        msg.put(msgs[i].getOriginatingAddress(), msgs[i].getMessageBody()); 
                        
                    } else {    // Number has been there, add content
                        // msgString already contains sms:sndrNbr:previousparts of SMS, just add this part
                        msgString = msgString + msgs[i].getMessageBody();
                        msg.put(msgs[i].getOriginatingAddress(), msgString);
                    }
                }
            }
        }
        
        return msg;
    }
}
