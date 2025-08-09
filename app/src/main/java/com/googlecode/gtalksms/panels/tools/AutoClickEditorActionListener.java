package com.googlecode.gtalksms.panels.tools;

import android.os.Build;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class AutoClickEditorActionListener implements OnEditorActionListener {
    private final Button mButton;
    
    public AutoClickEditorActionListener(Button button) {
        mButton = button;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                mButton.callOnClick(); 
            } else {
                mButton.performClick();
            }
        }
        return false;
    }
}
