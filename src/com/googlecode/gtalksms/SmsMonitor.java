package com.googlecode.gtalksms;

import java.util.ArrayList;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;

public abstract class SmsMonitor {
    // intents for sms sending
    public BroadcastReceiver sentSmsReceiver = null;
    public BroadcastReceiver deliveredSmsReceiver = null;
    public PendingIntent sentPI = null;
    public PendingIntent deliveredPI = null;

    Context _context;
    SettingsManager _settings;

    public SmsMonitor(SettingsManager settings, Context baseContext) {
        _settings = settings;
        _context = baseContext;

        if (_settings.notifySmsSent) {
            String SENT = "SMS_SENT";
            sentPI = PendingIntent.getBroadcast(_context, 0, new Intent(SENT), 0);
            sentSmsReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context arg0, Intent arg1) {
                    switch (getResultCode()) {
                        case Activity.RESULT_OK:
                            sendSmsStatus("SMS sent");
                            break;
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                            sendSmsStatus("Generic failure");
                            break;
                        case SmsManager.RESULT_ERROR_NO_SERVICE:
                            sendSmsStatus("No service");
                            break;
                        case SmsManager.RESULT_ERROR_NULL_PDU:
                            sendSmsStatus("Null PDU");
                            break;
                        case SmsManager.RESULT_ERROR_RADIO_OFF:
                            sendSmsStatus("Radio off");
                            break;
                    }
                }
            };
            _context.registerReceiver(sentSmsReceiver, new IntentFilter(SENT));
        }

        if (_settings.notifySmsDelivered) {
            String DELIVERED = "SMS_DELIVERED";
            deliveredPI = PendingIntent.getBroadcast(_context, 0, new Intent(DELIVERED), 0);
            deliveredSmsReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context arg0, Intent arg1) {
                    switch (getResultCode()) {
                        case Activity.RESULT_OK:
                            sendSmsStatus("SMS delivered");
                            break;
                        case Activity.RESULT_CANCELED:
                            sendSmsStatus("SMS not delivered");
                            break;
                    }
                }
            };

            _context.registerReceiver(deliveredSmsReceiver, new IntentFilter(DELIVERED));
        }
    }

    /** Sends a sms to the specified phone number */
    public void sendSMSByPhoneNumber(String message, String phoneNumber) {
        SmsManager sms = SmsManager.getDefault();
        ArrayList<String> messages = sms.divideMessage(message);

        // création des liste d'instents
        ArrayList<PendingIntent> listOfSentIntents = new ArrayList<PendingIntent>();
        listOfSentIntents.add(sentPI);
        ArrayList<PendingIntent> listOfDelIntents = new ArrayList<PendingIntent>();
        listOfDelIntents.add(deliveredPI);
        for (int i = 1; i < messages.size(); i++) {
            listOfSentIntents.add(null);
            listOfDelIntents.add(null);
        }

        sms.sendMultipartTextMessage(phoneNumber, null, messages, listOfSentIntents, listOfDelIntents);
    }

    /** clear the sms monitoring related stuff */
    public void clearSmsMonitor() {
        if (sentSmsReceiver != null) {
            _context.unregisterReceiver(sentSmsReceiver);
        }
        if (deliveredSmsReceiver != null) {
            _context.unregisterReceiver(deliveredSmsReceiver);
        }
        sentSmsReceiver = null;
        deliveredSmsReceiver = null;
    }
    
    abstract void sendSmsStatus(String message);
}
