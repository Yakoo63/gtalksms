package com.googlecode.gtalksms.cmd;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.cmd.bluetoothCmd.BluetoothStateReceiver;

public class BluetoothCmd extends CommandHandlerBase {
    
    private static BluetoothAdapter sBluetoothAdapter;

    public BluetoothCmd(MainService mainService) {
        super(mainService, new String[] {"bluetooth", "bt"}, CommandHandlerBase.TYPE_SYSTEM);
        sBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    protected void execute(String cmd, String args) {
        if(args.equals("on")) {
            enableAdapter();
        } else if (args.equals("off")) {
            disableAdapter();
        } else if (args.equals("state")) {
            showState();
        } else {
            send("Unkown argument \"" + args + "\" for command \"" + cmd + "\"");
        }
    }
    
    private void enableAdapter() {
        if (sBluetoothAdapter.enable()) {
            send("Bluetooth adapter startup has begun");
            addBroadcastReceiver();
        } else {
            sendFailure();
        }
    }
    
    private void disableAdapter() {
        if (sBluetoothAdapter.disable()) {
            send("Bluetooth adapter shutdown has begun");
            addBroadcastReceiver();
        } else {
            sendFailure();
        }
    }
    
    private void showState() {
        int state = sBluetoothAdapter.getState();
        String res = "Bluetooth adapter is ";
        res += stateInt2String(state);
        send(res);
    }
    
    private void sendFailure() {
        send("Error on Bluetooth adapter");
    }
    
    private void addBroadcastReceiver() {
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        BroadcastReceiver br = new BluetoothStateReceiver(sMainService, mAnswerTo);
        sContext.registerReceiver(br, filter);
    }
    
    public static String stateInt2String(int state) {
        String res;
        switch(state) {
        case BluetoothAdapter.STATE_OFF:
            res = "off";
            break;
        case BluetoothAdapter.STATE_ON:
            res = "on";
            break;
        case BluetoothAdapter.STATE_TURNING_OFF:
            res = "turning off";
            break;
        case BluetoothAdapter.STATE_TURNING_ON:
            res = "turning on";
            break;
        default:
            throw new IllegalStateException();        
        }
        return res;
    }   
    
    @Override
    public String[] help() {
        return null;
    }

}
