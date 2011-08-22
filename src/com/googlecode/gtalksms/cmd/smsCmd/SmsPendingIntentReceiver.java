package com.googlecode.gtalksms.cmd.smsCmd;

import java.util.Map;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.databases.SMSHelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public abstract class SmsPendingIntentReceiver extends BroadcastReceiver {
    
    private Map<Integer, Sms> smsMap;
    protected SMSHelper smsHelper;
    private MainService mainService;
    protected String answerTo;
    protected SettingsManager settings;
    
    public SmsPendingIntentReceiver(MainService mainService, Map<Integer, Sms> smsMap, SMSHelper smsHelper) {
        this.mainService = mainService;
        this.settings = SettingsManager.getSettingsManager(mainService);
        this.smsMap = smsMap;
        this.smsHelper = smsHelper;
    }
    
    public void onReceive(Context context, Intent intent) {
        int smsID = intent.getIntExtra("smsID", -1);
        int partNum = intent.getIntExtra("partNum", -1);
        int res = getResultCode();
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
    
    public abstract void onReceiveWithSms(Context context, Sms s, int partNum, int res, int smsID);
    
    public abstract void onReceiveWithoutSms(Context context, int partNum, int res);
    
    protected void send(String s) {
        mainService.send(s, answerTo);
    }   
    
    protected Sms getSms(int smsId) {
        Integer i = new Integer(smsId);
        return smsMap.get(i);
    }
    
    protected void removeSms(int smsId) {
        Integer i = new Integer(smsId);
        smsMap.remove(i);
        smsHelper.deleteSMS(smsId);
    }    
}
