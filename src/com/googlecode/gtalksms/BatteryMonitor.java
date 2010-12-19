package com.googlecode.gtalksms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class BatteryMonitor {

    BroadcastReceiver mBatInfoReceiver = null;
    int lastPercentageNotified = -1;
    Context _context;
    SettingsManager _settings;

    public BatteryMonitor(SettingsManager settings, Context baseContext) {
        _settings = settings;
        _context = baseContext;

        mBatInfoReceiver = new BroadcastReceiver() {
            
            @Override
            public void onReceive(Context arg0, Intent intent) {
                int level = intent.getIntExtra("level", 0);
                if (lastPercentageNotified == -1) {
                    notifyAndSavePercentage(level);
                } else {
                    if (level != lastPercentageNotified && level % _settings.batteryNotificationInterval == 0) {
                        notifyAndSavePercentage(level);
                    }
                }
            }

            private void notifyAndSavePercentage(int level) {
                lastPercentageNotified = level;                
                if (_settings.notifyBattery) {
                    sendBatteryInfos();
                }
            }
        };
        _context.registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    public void sendBatteryInfos() {
        MainService.getInstance().send("Battery level " + lastPercentageNotified + "%");
    }

    /** clear the battery monitor */
    public void clearBatteryMonitor() {
        if (mBatInfoReceiver != null) {
            _context.unregisterReceiver(mBatInfoReceiver);
        }
        mBatInfoReceiver = null;
    }
}
