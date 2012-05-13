package com.googlecode.gtalksms.panels.wizard;

import com.googlecode.gtalksms.R;

import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Spinner;

public class ChooseServerRadioGroupChangeListener implements OnCheckedChangeListener {
    
    private Spinner mSpinner;
    private EditText mTextServer;
    
    public ChooseServerRadioGroupChangeListener(Spinner spinner, EditText textServer) {
        mSpinner = spinner;
        mTextServer = textServer;
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (checkedId == R.id.radioChooseServer) {
            mSpinner.setEnabled(true);
            mTextServer.setEnabled(false);
        } else if (checkedId == R.id.radioManualServer) {
            mTextServer.setEnabled(true);
            mSpinner.setEnabled(false);
        }
    }

}
