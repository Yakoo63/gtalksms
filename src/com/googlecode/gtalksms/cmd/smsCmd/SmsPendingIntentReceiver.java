package com.googlecode.gtalksms.cmd.smsCmd;

import java.util.Map;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.SettingsManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public abstract class SmsPendingIntentReceiver extends BroadcastReceiver {
    
    private Map<Integer, Sms> smsMap;
    private MainService mainService;
    protected String answerTo;
    protected SettingsManager settings;
    
    public SmsPendingIntentReceiver(MainService mainService, Map<Integer, Sms> smsMap) {
        this.mainService = mainService;
        this.settings = mainService.getSettingsManager();
        this.smsMap = smsMap;
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
    }
    
    

}
