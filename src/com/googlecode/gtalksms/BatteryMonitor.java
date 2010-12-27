package com.googlecode.gtalksms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public abstract class BatteryMonitor {

    BroadcastReceiver _batInfoReceiver = null;
    int _lastPercentageNotified = -1;
    Context _context;
    SettingsManager _settings;

    public BatteryMonitor(SettingsManager settings, Context baseContext) {
        _settings = settings;
        _context = baseContext;

        _batInfoReceiver = new BroadcastReceiver() {
            
            @Override
            public void onReceive(Context arg0, Intent intent) {
                int level = intent.getIntExtra("level", 0);
                if (_lastPercentageNotified == -1) {
                    notifyAndSavePercentage(level);
                } else {
                    if (level != _lastPercentageNotified) {
                        notifyAndSavePercentage(level);
                    }
                }
            }

            private void notifyAndSavePercentage(int level) {
                _lastPercentageNotified = level;                
                sendBatteryInfos(level, false);
            }
        };
        _context.registerReceiver(_batInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }


    void sendBatteryInfos(boolean force) {
        sendBatteryInfos(_lastPercentageNotified, force);
    }

    abstract void sendBatteryInfos(int level, boolean force);

    /** clear the battery monitor */
    public void clearBatteryMonitor() {
        if (_batInfoReceiver != null) {
            _context.unregisterReceiver(_batInfoReceiver);
        }
        _batInfoReceiver = null;
    }
}
