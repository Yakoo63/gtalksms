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
        super(mainService, CommandHandlerBase.TYPE_SYSTEM, "Bluetooth", new Cmd("bluetooth", "bt"));
    }

    @Override
    protected void onCommandActivated() {
        sBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    protected void onCommandDeactivated() {
        sBluetoothAdapter = null;
    }

    protected void execute(Command cmd) {
        if (isMatchingCmd(cmd, "bluetooth")) {
            if(cmd.getArg1().equals("on")) {
                enableAdapter();
            } else if (cmd.getArg1().equals("off")) {
                disableAdapter();
            } else {
                showState();
            }
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
    protected void initializeSubCommands() {
        Cmd bt = mCommandMap.get("bluetooth");
        bt.setHelp(R.string.chat_help_bt_state, null);
        bt.AddSubCmd("on", R.string.chat_help_bt_on, null);
        bt.AddSubCmd("off",R.string.chat_help_bt_off, null);
    }
}
