package com.googlecode.gtalksms.cmd.smsCmd;

import java.util.Map;

import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.databases.SMSHelper;

import android.app.Activity;
import android.content.Context;
import android.telephony.SmsManager;

public class SentIntentReceiver extends SmsPendingIntentReceiver {

    public SentIntentReceiver(MainService mainService, Map<Integer, Sms> smsMap, SMSHelper smsHelper) {
        super(mainService, smsMap, smsHelper);
    }

    @Override
    public void onReceiveWithSms(Context context, Sms s, int partNum, int res, int smsID) {
        answerTo = s.getAnswerTo();
        s.setSentIntentTrue(partNum);
        smsHelper.setSentIntentTrue(smsID, partNum);
        boolean sentIntComplete = s.sentIntentsComplete();
        String smsSendTo;
        if (s.getTo() != null) { // prefer a name over a number in the to field
            smsSendTo = s.getTo();
        } else {
            smsSendTo = s.getNumber();
        }

        if (res == Activity.RESULT_OK && sentIntComplete) {
            send(context.getString(R.string.chat_sms_sent_to, s.getShortenedMessage(), smsSendTo));
        } else if (s.getResSentIntent() == -1) {
            switch (res) {
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                send(context.getString(R.string.chat_sms_failure_to, s.getShortenedMessage(), smsSendTo));
                break;
            case SmsManager.RESULT_ERROR_NO_SERVICE:
                send(context.getString(R.string.chat_sms_no_service_to, s.getShortenedMessage(), smsSendTo));
                break;
            case SmsManager.RESULT_ERROR_NULL_PDU:
                send(context.getString(R.string.chat_sms_null_pdu_to, s.getShortenedMessage(), smsSendTo));
                break;
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                send(context.getString(R.string.chat_sms_radio_off_to, s.getShortenedMessage(), smsSendTo));
                break;
            }
            s.setResSentIntent(res);
        }
        if (!settings.notifySmsDelivered && sentIntComplete) {
            removeSms(smsID);  
        }        
    }

    @Override
    public void onReceiveWithoutSms(Context context, int partNum, int res) {
        answerTo = null;
        Log.w("sms in smsMap missing");
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
