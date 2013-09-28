package com.googlecode.gtalksms.cmd.bluetoothCmd;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.cmd.BluetoothCmd;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BluetoothStateReceiver extends BroadcastReceiver {
    
    private final MainService mMainService;
    private final String mAnswerTo;
    
    public BluetoothStateReceiver(MainService mainService, String answerTo) {
        this.mMainService = mainService;
        this.mAnswerTo = answerTo;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
        int prev_state = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);
        String stateStr = BluetoothCmd.stateInt2String(state);
        String prev_stateStr = BluetoothCmd.stateInt2String(prev_state);
        
        send("Bluetooth Adapter state changed from \"" + prev_stateStr + "\" to \"" + stateStr + "\"");
        context.unregisterReceiver(this);
    }
    
    private void send(String msg) {
        mMainService.send(msg, mAnswerTo);
    }

}
