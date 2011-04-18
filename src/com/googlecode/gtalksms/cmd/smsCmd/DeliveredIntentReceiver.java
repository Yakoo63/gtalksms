package com.googlecode.gtalksms.cmd.smsCmd;

import java.util.Map;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.tools.GoogleAnalyticsHelper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

public class DeliveredIntentReceiver extends SmsPendingIntentReceiver {

    public DeliveredIntentReceiver(MainService mainService, Map<Integer, Sms> smsMap) {
        super(mainService, smsMap);
    }
    
    // TODO this class needs some refactoring, more code could be shared in superclass
    // e.g. implement an abstract method smsIntentReceived(null or sms)

    @Override
    public void onReceive(Context context, Intent intent) {
        int smsID = intent.getIntExtra("smsID", -1);
        int partNum = intent.getIntExtra("partNum", -1);
        int res = getResultCode();
        Sms s = getSms(smsID);

        if (s != null) { // we could find the sms in the smsMap
            this.answerTo = s.getAnswerTo();
            s.setDelIntentTrue(partNum);
            boolean delIntComplete = s.delIntentsComplete();
            String to;
            if (s.getTo() != null) { // prefer a name over a number in the to field
                // TODO to contains a full jid (incl. resource), but this resource could became offline
                // we should check here that the resource is still connected and provide an adequate fallback
                // implement this check in SmsPendingIntentReceiver
                to = s.getTo();
            } else {
                to = s.getNumber();
            }

            if (res == Activity.RESULT_OK && delIntComplete) {
                send(context.getString(R.string.chat_sms_delivered_to, s.getShortendMessage(), to));
            } else if (s.getResSentIntent() == -1) {
                if(res == Activity.RESULT_CANCELED) {
                    send(context.getString(R.string.chat_sms_not_delivered_to, s.getShortendMessage(), to));
                }
                s.setResSentIntent(res);
            }
            if (delIntComplete) {
                removeSms(smsID);
            }

        } else { // we could NOT find the sms in the smsMap - fall back to old the behavior
            answerTo = null;
            GoogleAnalyticsHelper.trackAndLogWarning("sms in smsMap missing");
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

}
