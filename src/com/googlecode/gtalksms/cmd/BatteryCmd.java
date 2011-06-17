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
    private static BroadcastReceiver sBatInfoReceiver = null;
    private static int sLastKnownPercentage = -1; // flag so the BroadcastReceiver can set the percentage
    private static String sPowerSource;
    private static int sLastStatusPercentage = -1;
    private static String sLastStatusPowersource;
    private static XmppBuddies sXmppBuddies;
    
    public BatteryCmd(MainService mainService) {
        super(mainService, new String[] {"battery", "batt"}, CommandHandlerBase.TYPE_SYSTEM);
        sXmppBuddies = XmppBuddies.getInstance(sContext);
        sPowerSource = "Unknown";
        
        sBatInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                float levelFloat = ((float)level / (float)scale)*100;
                level = (int) levelFloat;
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
                if (sLastKnownPercentage == -1) {
                    notifyAndSave(level, pSourceStr);
                } else if (level != sLastKnownPercentage || sPowerSource.compareTo(pSourceStr) != 0) {
                    notifyAndSave(level, pSourceStr);
                }
            }
        };
        sContext.registerReceiver(sBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }
    
    /**
     * 
     * @param force
     */
    private void sendBatteryInfos(boolean force) {
        if (force || (sSettingsMgr.notifyBattery && sLastKnownPercentage % sSettingsMgr.batteryNotificationIntervalInt == 0)) {
            send(R.string.chat_battery_level, sLastKnownPercentage);
        }
        if (sSettingsMgr.notifyBatteryInStatus) {
            // only send an notification to the user if he is available
            // and if something has changed
            if (sXmppBuddies.isNotificationAddressAvailable() 
                    && (sLastKnownPercentage != sLastStatusPercentage || !sPowerSource.equals(sLastStatusPowersource))) {
                XmppManager.setStatus("GTalkSMS - " + sLastKnownPercentage + "%" + " - " + sPowerSource);
                sLastStatusPercentage = sLastKnownPercentage;
                sLastStatusPowersource = sPowerSource;
            }
        }
    }
    
    private void notifyAndSave(int level, String powerSource) {
        sPowerSource = powerSource;
        sLastKnownPercentage = level;                
        sendBatteryInfos(false);
    }
    
    @Override
    protected void execute(String cmd, String args) {
        if (args.equals("silent")) {
            sendBatteryInfos(false);
        } else {
            sendBatteryInfos(true);
        }
    }

    @Override
    public void cleanUp() {
        if (sBatInfoReceiver != null) {
            sContext.unregisterReceiver(sBatInfoReceiver);
        }
        sBatInfoReceiver = null;
    }

    @Override
    public String[] help() {
        String[] s = { 
                getString(R.string.chat_help_battery, makeBold("\"battery\""), makeBold("\"batt\"")) 
                };
        return s;
    }
}
