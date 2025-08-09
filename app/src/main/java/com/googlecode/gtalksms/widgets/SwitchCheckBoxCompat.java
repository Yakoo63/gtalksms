package com.googlecode.gtalksms.widgets;

import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

public class SwitchCheckBoxCompat {
    private Switch mSwitch;
    private CheckBox mCheckBox;
    private boolean mIsSwitchSupported;
    
    public SwitchCheckBoxCompat (View v, int id) {
        try {
            Class.forName("android.widget.Switch");
            mSwitch = (Switch) v.findViewById(id);
            mIsSwitchSupported = true;
        } catch (Exception e) {
            mIsSwitchSupported = false;
            mCheckBox = (CheckBox) v.findViewById(id);
        }
    }
    
    public void setEnabled(boolean isEnabled) {
        if (mIsSwitchSupported) {
            mSwitch.setEnabled(isEnabled);
        } else {
            mCheckBox.setEnabled(isEnabled);
        }
    }
    
    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        if (mIsSwitchSupported) {
            mSwitch.setOnCheckedChangeListener(listener);
        } else {
            mCheckBox.setOnCheckedChangeListener(listener);
        }
    }  
    
    public void setChecked(boolean isChecked) {
        if (mIsSwitchSupported) {
            mSwitch.setChecked(isChecked);
        } else {
            mCheckBox.setChecked(isChecked);
        }
    }
    
    public boolean isChecked() {
        return mIsSwitchSupported ? mSwitch.isChecked() : mCheckBox.isChecked();
    }
}
