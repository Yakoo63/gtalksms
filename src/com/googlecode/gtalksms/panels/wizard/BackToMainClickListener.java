package com.googlecode.gtalksms.panels.wizard;

import com.googlecode.gtalksms.R;

import android.view.View;
import android.view.View.OnClickListener;

public class BackToMainClickListener implements OnClickListener {
    
    private Wizard mWizard;

    /**
     * 
     * @param w
     */
    protected BackToMainClickListener(Wizard w) {
        this.mWizard = w;
    }
    
    @Override
    public void onClick(View v) {
        mWizard.setContentView(R.layout.main);
    }

}
