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
    public BroadcastReceiver _sentSmsReceiver = null;
    public BroadcastReceiver _deliveredSmsReceiver = null;
    public PendingIntent _sentPI = null;
    public PendingIntent _deliveredPI = null;

    Context _context;
    SettingsManager _settings;

    public SmsMonitor(SettingsManager settings, Context baseContext) {
        _settings = settings;
        _context = baseContext;

        if (_settings.notifySmsSent) {
            String SENT = "SMS_SENT";
            _sentPI = PendingIntent.getBroadcast(_context, 0, new Intent(SENT), 0);
            _sentSmsReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context arg0, Intent arg1) {
                    switch (getResultCode()) {
                        case Activity.RESULT_OK:
                            sendSmsStatus(_context.getString(R.string.chat_sms_sent));
                            break;
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                            sendSmsStatus(_context.getString(R.string.chat_sms_failure));
                            break;
                        case SmsManager.RESULT_ERROR_NO_SERVICE:
                            sendSmsStatus(_context.getString(R.string.chat_sms_no_service));
                            break;
                        case SmsManager.RESULT_ERROR_NULL_PDU:
                            sendSmsStatus(_context.getString(R.string.chat_sms_null_pdu));
                            break;
                        case SmsManager.RESULT_ERROR_RADIO_OFF:
                            sendSmsStatus(_context.getString(R.string.chat_sms_radio_off));
                            break;
                    }
                }
            };
            _context.registerReceiver(_sentSmsReceiver, new IntentFilter(SENT));
        }

        if (_settings.notifySmsDelivered) {
            String DELIVERED = "SMS_DELIVERED";
            _deliveredPI = PendingIntent.getBroadcast(_context, 0, new Intent(DELIVERED), 0);
            _deliveredSmsReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context arg0, Intent arg1) {
                    switch (getResultCode()) {
                        case Activity.RESULT_OK:
                            sendSmsStatus(_context.getString(R.string.chat_sms_delivered));
                            break;
                        case Activity.RESULT_CANCELED:
                            sendSmsStatus(_context.getString(R.string.chat_sms_not_delivered));
                            break;
                    }
                }
            };

            _context.registerReceiver(_deliveredSmsReceiver, new IntentFilter(DELIVERED));
        }
    }

    /** Sends a sms to the specified phone number */
    public void sendSMSByPhoneNumber(String message, String phoneNumber) {
        SmsManager sms = SmsManager.getDefault();
        ArrayList<String> messages = sms.divideMessage(message);

        // création des liste d'instents
        ArrayList<PendingIntent> listOfSentIntents = new ArrayList<PendingIntent>();
        listOfSentIntents.add(_sentPI);
        ArrayList<PendingIntent> listOfDelIntents = new ArrayList<PendingIntent>();
        listOfDelIntents.add(_deliveredPI);
        for (int i = 1; i < messages.size(); i++) {
            listOfSentIntents.add(null);
            listOfDelIntents.add(null);
        }

        sms.sendMultipartTextMessage(phoneNumber, null, messages, listOfSentIntents, listOfDelIntents);
    }

    /** clear the sms monitoring related stuff */
    public void clearSmsMonitor() {
        if (_sentSmsReceiver != null) {
            _context.unregisterReceiver(_sentSmsReceiver);
        }
        if (_deliveredSmsReceiver != null) {
            _context.unregisterReceiver(_deliveredSmsReceiver);
        }
        _sentSmsReceiver = null;
        _deliveredSmsReceiver = null;
    }
    
    abstract void sendSmsStatus(String message);
}
