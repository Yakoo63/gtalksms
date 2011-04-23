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
        this.settings = mainService.getSettingsManager();
        this.smsMap = smsMap;
        this.smsHelper = smsHelper;
    }
    
    public abstract void onReceive(Context context, Intent intent);
    
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
    
    // TODO to contains a full jid (incl. resource), but this resource could became offline
    // we should check here that the resource is still connected and provide an adequate fallback
    // implement this check in SmsPendingIntentReceiver
    protected String checkResource(String resource) {
        return resource;
    }
}
