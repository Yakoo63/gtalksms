package com.googlecode.gtalksms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.googlecode.gtalksms.data.sms.Sms;
import com.googlecode.gtalksms.tools.Tools;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import android.util.Log;

public abstract class SmsMonitor {
	public BroadcastReceiver _sentSmsReceiver = null;
	public BroadcastReceiver _deliveredSmsReceiver = null;

	Context _context;
	SettingsManager _settings;
	int smsCount;
	Map<Integer, Sms> smsMap = new HashMap<Integer, Sms>();

	public SmsMonitor(SettingsManager settings, Context baseContext) {
		_settings = settings;
		_context = baseContext;

        if (_settings.notifySmsSent) {
            _sentSmsReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int smsID = intent.getIntExtra("smsID", -1);
                    int partNum = intent.getIntExtra("partNum", -1);
                    int res = getResultCode();
                    Integer SmsID = new Integer(smsID);
                    Sms s = smsMap.get(SmsID);

                    if (s != null) {  // we could find the sms in the smsMap
                        s.sentIntents[partNum] = true;
                        boolean sentIntComplete = s.sentIntentsComplete();
                        String to;
                        if (s.to != null) { // prefer a name over a number in the to field
                            to = s.to;
                        } else {
                            to = s.number;
                        }

                        if (res == Activity.RESULT_OK && sentIntComplete) {
                            sendSmsStatus(_context.getString(R.string.chat_sms_sent_to, s.shortendMessage, to));
                        } else if (s.resSentIntent == -1) {
                            switch (res) {
                            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                                sendSmsStatus(_context.getString(R.string.chat_sms_failure_to, s.shortendMessage, to));
                                break;
                            case SmsManager.RESULT_ERROR_NO_SERVICE:
                                sendSmsStatus(_context.getString(R.string.chat_sms_no_service_to, s.shortendMessage, to));
                                break;
                            case SmsManager.RESULT_ERROR_NULL_PDU:
                                sendSmsStatus(_context.getString(R.string.chat_sms_null_pdu_to, s.shortendMessage, to));
                                break;
                            case SmsManager.RESULT_ERROR_RADIO_OFF:
                                sendSmsStatus(_context.getString(R.string.chat_sms_radio_off_to, s.shortendMessage, to));
                                break;
                            }
                            s.resSentIntent = res;
                        }
                        //TODO needs to be synchronized?
                        if (_settings.notifySmsDelivered == false && sentIntComplete) {
                            smsMap.remove(SmsID);  
                        }
                        
                    } else { // we could NOT find the sms in the smsMap - fall back to old behavior
                        Log.d(Tools.LOG_TAG, "sms in smsMap missing");
                        switch (res) {
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
                }
            };
            _context.registerReceiver(_sentSmsReceiver, new IntentFilter(MainService.ACTION_SMS_SENT));

        }

        if (_settings.notifySmsDelivered) {
            _deliveredSmsReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int smsID = intent.getIntExtra("smsID", -1);
                    int partNum = intent.getIntExtra("partNum", -1);
                    int res = getResultCode();
                    Integer SmsID = new Integer(smsID);
                    Sms s = smsMap.get(SmsID);

                    if (s != null) { // we could find the sms in the smsMap
                        s.delIntents[partNum] = true;
                        boolean delIntComplete = s.delIntentsComplete();
                        String to;
                        if (s.to != null) { // prefer a name over a number in the to field
                            to = s.to;
                        } else {
                            to = s.number;
                        }

                        if (res == Activity.RESULT_OK && delIntComplete) {
                            sendSmsStatus(_context.getString(R.string.chat_sms_sent_to, s.shortendMessage, to));
                        } else if (s.resSentIntent == -1) {
                            if(res == Activity.RESULT_CANCELED) {
                                sendSmsStatus(_context.getString(R.string.chat_sms_not_delivered_to, s.shortendMessage, to));
                            }
                            s.resSentIntent = res;
                        }
                        if (delIntComplete) {
                            smsMap.remove(SmsID); //TODO needs to be synchronized?
                        }

                    } else { // we could NOT find the sms in the smsMap - fall back to old the behavior
                        Log.d(Tools.LOG_TAG, "sms in smsMap missing");
                        switch (res) {
                        case Activity.RESULT_OK:
                            sendSmsStatus(_context.getString(R.string.chat_sms_delivered));
                            break;
                        case Activity.RESULT_CANCELED:
                            sendSmsStatus(_context.getString(R.string.chat_sms_not_delivered));
                            break;
                        }
                    }
                }
            };
            _context.registerReceiver(_deliveredSmsReceiver, new IntentFilter(MainService.ACTION_SMS_DELIVERED));

        }
    }

    /** Sends a sms to the specified phone number with a receiver name */
    public void sendSMSByPhoneNumber(String message, String phoneNumber, String toName) {
        ArrayList<PendingIntent> SentPenIntents = new ArrayList<PendingIntent>();
        ArrayList<PendingIntent> DelPenIntents = new ArrayList<PendingIntent>();
        SmsManager sms = SmsManager.getDefault();
        ArrayList<String> messages = sms.divideMessage(message);
        String shortendMessage = shortenMessage(message);

        int smsID = smsCount++;
        Sms s = new Sms(phoneNumber, toName, shortendMessage, messages.size());
        smsMap.put(new Integer(smsID), s);

        createPendingIntents(SentPenIntents, DelPenIntents, messages, smsID);

        sms.sendMultipartTextMessage(phoneNumber, null, messages, SentPenIntents, DelPenIntents);
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
    
    private void createPendingIntents(ArrayList<PendingIntent> SentPenIntents, ArrayList<PendingIntent> DelPenIntents, ArrayList<String> messages, int smsID) {
        for (int i = 0; i < messages.size(); i++) {
            Intent sentIntent = new Intent(MainService.ACTION_SMS_SENT);
            sentIntent.putExtra("partNum", i);
            sentIntent.putExtra("smsID", smsID);
            PendingIntent sentPenIntent = PendingIntent.getBroadcast(_context, 0, sentIntent, PendingIntent.FLAG_ONE_SHOT);

            Intent deliveredIntent = new Intent(MainService.ACTION_SMS_DELIVERED);
            deliveredIntent.putExtra("partNum", i);
            deliveredIntent.putExtra("smsID", smsID);
            PendingIntent deliveredPenIntent = PendingIntent.getBroadcast(_context, 0, deliveredIntent, PendingIntent.FLAG_ONE_SHOT);

            SentPenIntents.add(sentPenIntent);
            DelPenIntents.add(deliveredPenIntent);
        }
    }
    
    private String shortenMessage(String message) {
        final int shortenTo = 20;
        String shortendMessage;
        if (message.length() < shortenTo) {
            shortendMessage = message;
        } else {
            shortendMessage = message.substring(0, shortenTo) + "...";
        }
        return shortendMessage;
    }

    abstract void sendSmsStatus(String status);
}
