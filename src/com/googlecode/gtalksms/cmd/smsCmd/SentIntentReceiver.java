package com.googlecode.gtalksms.cmd.smsCmd;

import java.util.Map;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.tools.GoogleAnalyticsHelper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;

public class SentIntentReceiver extends SmsPendingIntentReceiver {

    public SentIntentReceiver(MainService mainService, Map<Integer, Sms> smsMap) {
        super(mainService, smsMap);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int smsID = intent.getIntExtra("smsID", -1);
        int partNum = intent.getIntExtra("partNum", -1);
        int res = getResultCode();
        Sms s = getSms(smsID);

        if (s != null) {  // we could find the sms in the smsMap
            answerTo = s.answerTo; // I surely hope that broadcastReceiver don result in concurrent calls;
            s.sentIntents[partNum] = true;
            boolean sentIntComplete = s.sentIntentsComplete();
            String to;
            if (s.to != null) { // prefer a name over a number in the to field
                to = s.to;
            } else {
                to = s.number;
            }

            if (res == Activity.RESULT_OK && sentIntComplete) {
                send(context.getString(R.string.chat_sms_sent_to, s.shortendMessage, to));
            } else if (s.resSentIntent == -1) {
                switch (res) {
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    send(context.getString(R.string.chat_sms_failure_to, s.shortendMessage, to));
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    send(context.getString(R.string.chat_sms_no_service_to, s.shortendMessage, to));
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    send(context.getString(R.string.chat_sms_null_pdu_to, s.shortendMessage, to));
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    send(context.getString(R.string.chat_sms_radio_off_to, s.shortendMessage, to));
                    break;
                }
                s.resSentIntent = res;
            }
            if (settings.notifySmsDelivered == false && sentIntComplete) {
                removeSms(smsID);  
            }
            
        } else { // we could NOT find the sms in the smsMap - fall back to old behavior
            answerTo = null;
            GoogleAnalyticsHelper.trackAndLogWarning("sms in smsMap missing");
            switch (res) {
            case Activity.RESULT_OK:
                send(context.getString(R.string.chat_sms_sent));
                break;
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                send(context.getString(R.string.chat_sms_failure));
                break;
            case SmsManager.RESULT_ERROR_NO_SERVICE:
                send(context.getString(R.string.chat_sms_no_service));
                break;
            case SmsManager.RESULT_ERROR_NULL_PDU:
                send(context.getString(R.string.chat_sms_null_pdu));
                break;
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                send(context.getString(R.string.chat_sms_radio_off));
                break;
            }
        }
    }

}
