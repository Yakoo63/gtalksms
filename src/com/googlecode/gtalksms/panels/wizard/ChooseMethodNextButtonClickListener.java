package com.googlecode.gtalksms.panels.wizard;

import com.googlecode.gtalksms.R;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioGroup;

public class ChooseMethodNextButtonClickListener implements OnClickListener {
    
    private Wizard mWizard;
    private RadioGroup mRg;

    public ChooseMethodNextButtonClickListener(Wizard wizard, RadioGroup rg) {
        mWizard = wizard;
        mRg = rg;
    }
    
    @Override
    public void onClick(View v) {
        int checkedButton = mRg.getCheckedRadioButtonId();
        mWizard.mChoosenMethod = checkedButton;
        if (checkedButton == R.id.radioDifferentAccount) {
            mWizard.initView(Wizard.VIEW_CREATE_CHOOSE_SERVER);
        } else if (checkedButton == R.id.radioSameAccount) {
            mWizard.initView(Wizard.VIEW_SAME_ACCOUNT);
        } else if (checkedButton == R.id.radioExsistingAccount) {
            mWizard.initView(Wizard.VIEW_EXISTING_ACCOUNT);
        } else {
            throw new IllegalStateException();
        }
    }
}
