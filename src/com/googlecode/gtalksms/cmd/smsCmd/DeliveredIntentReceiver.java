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

    @Override
    public void onReceive(Context context, Intent intent) {
        int smsID = intent.getIntExtra("smsID", -1);
        int partNum = intent.getIntExtra("partNum", -1);
        int res = getResultCode();
        Sms s = getSms(smsID);

        if (s != null) { // we could find the sms in the smsMap
            this.answerTo = s.answerTo;
            s.delIntents[partNum] = true;
            boolean delIntComplete = s.delIntentsComplete();
            String to;
            if (s.to != null) { // prefer a name over a number in the to field
                to = s.to;
            } else {
                to = s.number;
            }

            if (res == Activity.RESULT_OK && delIntComplete) {
                send(context.getString(R.string.chat_sms_delivered_to, s.shortendMessage, to));
            } else if (s.resSentIntent == -1) {
                if(res == Activity.RESULT_CANCELED) {
                    send(context.getString(R.string.chat_sms_not_delivered_to, s.shortendMessage, to));
                }
                s.resSentIntent = res;
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
