package com.googlecode.gtalksms.cmd.smsCmd;

import java.util.Map;

import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.databases.SMSHelper;

import android.app.Activity;
import android.content.Context;

public class DeliveredIntentReceiver extends SmsPendingIntentReceiver {

    public DeliveredIntentReceiver(MainService mainService, Map<Integer, Sms> smsMap, SMSHelper smsHelper) {
        super(mainService, smsMap, smsHelper);
    }

    @Override
    public void onReceiveWithSms(Context context, Sms s, int partNum, int res, int smsID) {
        this.answerTo = s.getAnswerTo();
        s.setDelIntentTrue(partNum);
        smsHelper.setDelIntentTrue(smsID, partNum);
        boolean delIntComplete = s.delIntentsComplete();
        String smsSendTo;
        if (s.getTo() != null) { // prefer a name over a number in the to field
            smsSendTo = s.getTo();
        } else {
            smsSendTo = s.getNumber();
        }

        if (res == Activity.RESULT_OK && delIntComplete) {
            send(context.getString(R.string.chat_sms_delivered_to, s.getShortenedMessage(), smsSendTo));
        } else if (s.getResSentIntent() == -1) {
            if(res == Activity.RESULT_CANCELED) {
                send(context.getString(R.string.chat_sms_not_delivered_to, s.getShortenedMessage(), smsSendTo));
            }
            s.setResSentIntent(res);
        }
        if (delIntComplete) {
            removeSms(smsID);
        }
        
    }

    @Override
    public void onReceiveWithoutSms(Context context, int partNum, int res) {
        answerTo = null;
        Log.w("sms in smsMap missing");
        switch (res) {
        case Activity.RESULT_OK:
            send(context.getString(R.string.chat_sms_delivered));
            break;
        case Activity.RESULT_CANCELED:
            send(context.getString(R.string.chat_sms_not_delivered));
            break;
        }
        
    }

}
