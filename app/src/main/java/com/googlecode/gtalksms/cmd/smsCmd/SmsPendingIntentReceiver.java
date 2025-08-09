package com.googlecode.gtalksms.cmd.smsCmd;

import java.util.Map;

import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.databases.SMSHelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

abstract class SmsPendingIntentReceiver extends BroadcastReceiver {
    
    private final Map<Integer, Sms> smsMap;
    final SMSHelper smsHelper;
    private final MainService mainService;
    String answerTo;
    final SettingsManager settings;
    
    SmsPendingIntentReceiver(MainService mainService, Map<Integer, Sms> smsMap, SMSHelper smsHelper) {
        this.mainService = mainService;
        this.settings = SettingsManager.getSettingsManager(mainService);
        this.smsMap = smsMap;
        this.smsHelper = smsHelper;
    }
    
    public void onReceive(Context context, Intent intent) {
        int smsID = intent.getIntExtra("smsID", -1);
        int partNum = intent.getIntExtra("partNum", -1);
        int res = getResultCode();
        Log.i("PendingIntentReceiver onReceive(): intent=" + intent + " smsID= " + smsID + " partNum=" + partNum + " result=" + res);
        // check if we found the sms in our database
        Sms s = getSms(smsID);
        if (s != null) {
            // we have found a sms in our database
            onReceiveWithSms(context, s, partNum, res, smsID);
        } else {
            // the sms is missing in our database
            onReceiveWithoutSms(context, partNum, res);
        }
    }
    
    protected abstract void onReceiveWithSms(Context context, Sms s, int partNum, int res, int smsID);
    
    protected abstract void onReceiveWithoutSms(Context context, int partNum, int res);
    
    void send(String s) {
        mainService.send(s, answerTo);
    }   
    
    Sms getSms(int smsId) {
        Integer i = smsId;
        return smsMap.get(i);
    }
    
    void removeSms(int smsId) {
        Integer i = smsId;
        smsMap.remove(i);
        smsHelper.deleteSMS(smsId);
    }    
}
