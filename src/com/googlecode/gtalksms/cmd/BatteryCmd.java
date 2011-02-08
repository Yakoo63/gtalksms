package com.googlecode.gtalksms.cmd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;

public class BatteryCmd extends Command {
    private BroadcastReceiver _batInfoReceiver = null;
    private int _lastPercentageNotified = -1;
    
    public BatteryCmd(MainService mainService) {
        super(mainService, new String[] {"battery", "batt"});
        
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

    private void sendBatteryInfos(int level, boolean force) {
        if (force || (_settingsMgr.notifyBattery && level % _settingsMgr.batteryNotificationInterval == 0)) {
            send(getString(R.string.chat_battery_level, level));
        }
        if (_settingsMgr.notifyBatteryInStatus) {
            _mainService.setXmppStatus("GTalkSMS - " + level + "%");
        }
    }
    
    @Override
    public void execute(String cmd, String args) {
        sendBatteryInfos(_lastPercentageNotified, true);
    }

    @Override
    public void cleanUp() {
        if (_batInfoReceiver != null) {
            _context.unregisterReceiver(_batInfoReceiver);
        }
        _batInfoReceiver = null;
    }
}
