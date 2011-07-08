package com.googlecode.gtalksms.cmd;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
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
            send(R.string.chat_bt_command_error, args, cmd);
        }
    }
    
    private void enableAdapter() {
        if (sBluetoothAdapter.enable()) {
            send(R.string.chat_bt_startup);
            addBroadcastReceiver();
        } else {
            sendFailure();
        }
    }
    
    private void disableAdapter() {
        if (sBluetoothAdapter.disable()) {
            send(R.string.chat_bt_shutdown);
            addBroadcastReceiver();
        } else {
            sendFailure();
        }
    }
    
    private void showState() {
        int state = sBluetoothAdapter.getState();
        send(R.string.chat_bt_adapter_state, stateInt2String(state));
    }
    
    private void sendFailure() {
        send(R.string.chat_bt_adapter_error);
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
            res = sContext.getString(R.string.chat_bt_state_off);
            break;
        case BluetoothAdapter.STATE_ON:
            res = sContext.getString(R.string.chat_bt_state_on);
            break;
        case BluetoothAdapter.STATE_TURNING_OFF:
            res = sContext.getString(R.string.chat_bt_state_turning_off);
            break;
        case BluetoothAdapter.STATE_TURNING_ON:
            res = sContext.getString(R.string.chat_bt_state_turning_on);
            break;
        default:
            throw new IllegalStateException();        
        }
        return res;
    }   
    
    @Override
    public String[] help() {
        // TODO HELP
        String[] s = { 
        };
        //return s;
        return null;
    }

}
