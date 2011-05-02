package com.googlecode.gtalksms.cmd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.xmpp.XmppBuddies;

public class BatteryCmd extends CommandHandlerBase {
    private BroadcastReceiver _batInfoReceiver = null;
    private int _lastPercentageNotified = -1;
    private String _powerSource;
    private static XmppBuddies xmppBuddies;
    
    public BatteryCmd(MainService mainService) {
        super(mainService, new String[] {"battery", "batt"}, CommandHandlerBase.TYPE_SYSTEM);
        xmppBuddies = XmppBuddies.getInstance(_context);
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
                } else if (level != _lastPercentageNotified || _powerSource.compareTo(pSourceStr) != 0) {
                    notifyAndSave(level, pSourceStr);
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
    
    /**
     * 
     * @param level
     * @param force
     */
    private void sendBatteryInfos(int level, boolean force) {
        if (force || (_settingsMgr.notifyBattery && level % _settingsMgr.batteryNotificationIntervalInt == 0)) {
            send(R.string.chat_battery_level, level);
        }
        if (_settingsMgr.notifyBatteryInStatus) {
            // only send an notification to the user if he is available
            if (xmppBuddies.isNotificationAddressAvailable() || force) {
                XmppManager.setStatus("GTalkSMS - " + level + "%" + " - " + _powerSource);
            }
        }
    }
    
    @Override
    protected void execute(String cmd, String args) {
        sendBatteryInfos(_lastPercentageNotified, true);
    }

    @Override
    public void cleanUp() {
        if (_batInfoReceiver != null) {
            _context.unregisterReceiver(_batInfoReceiver);
        }
        _batInfoReceiver = null;
    }

    @Override
    public String[] help() {
        String[] s = { 
                getString(R.string.chat_help_battery, makeBold("\"battery\""), makeBold("\"batt\"")) 
                };
        return s;
    }
}
