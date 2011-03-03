package com.googlecode.gtalksms.cmd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.XmppManager;

public class BatteryCmd extends Command {
    private BroadcastReceiver _batInfoReceiver = null;
    private int _lastPercentageNotified = -1;
    private String _powerSource;
    private XmppManager _xmppMgr;
    
    public BatteryCmd(MainService mainService) {
        super(mainService, new String[] {"battery", "batt"});
        _xmppMgr = mainService.getXmppmanager();
        _powerSource = "Unknown";
        
        _batInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent intent) {
                int level = intent.getIntExtra("level", 0);
                int pSource = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                String pSourceStr = null;
                switch (pSource) {
                    case 0:
                        pSourceStr = "Battery";
                        break;
                    case BatteryManager.BATTERY_PLUGGED_AC:
                        pSourceStr = "AC";
                        break;
                    case BatteryManager.BATTERY_PLUGGED_USB:
                        pSourceStr = "USB";
                        break;
                    default:
                        pSourceStr = "Unknown";
                        break;
                }                                       
                if (_lastPercentageNotified == -1) {
                    notifyAndSave(level, pSourceStr);
                } else {
                    if (level != _lastPercentageNotified) {
                        notifyAndSave(level, pSourceStr);
                    }
                }
            }

            private void notifyAndSave(int level, String powerSource) {
                _powerSource = powerSource;
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
            _xmppMgr.setStatus("GTalkSMS - " + level + "%" + " - " + _powerSource);
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
