package com.googlecode.gtalksms.cmd.smsCmd;

import java.util.Map;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.databases.SMSHelper;
import com.googlecode.gtalksms.tools.GoogleAnalyticsHelper;

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
        String to;
        if (s.getTo() != null) { // prefer a name over a number in the to field
            to = checkResource(s.getTo());
        } else {
            to = s.getNumber();
        }

        if (res == Activity.RESULT_OK && sentIntComplete) {
            send(context.getString(R.string.chat_sms_sent_to, s.getShortendMessage(), to));
        } else if (s.getResSentIntent() == -1) {
            switch (res) {
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                send(context.getString(R.string.chat_sms_failure_to, s.getShortendMessage(), to));
                break;
            case SmsManager.RESULT_ERROR_NO_SERVICE:
                send(context.getString(R.string.chat_sms_no_service_to, s.getShortendMessage(), to));
                break;
            case SmsManager.RESULT_ERROR_NULL_PDU:
                send(context.getString(R.string.chat_sms_null_pdu_to, s.getShortendMessage(), to));
                break;
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                send(context.getString(R.string.chat_sms_radio_off_to, s.getShortendMessage(), to));
                break;
            }
            s.setResSentIntent(res);
        }
        if (settings.notifySmsDelivered == false && sentIntComplete) {
            removeSms(smsID);  
        }        
    }

    @Override
    public void onReceiveWithoutSms(Context context, int partNum, int res) {
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
