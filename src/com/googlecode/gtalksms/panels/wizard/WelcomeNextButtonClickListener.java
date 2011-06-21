package com.googlecode.gtalksms.panels.wizard;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.xmpp.XmppTools;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;


public class WelcomeNextButtonClickListener implements OnClickListener {
    
    private Wizard mWizard;
    private EditText mTextNotiAddress;
    
    public WelcomeNextButtonClickListener(Wizard wizard, EditText textNotiAddress) {
        mWizard = wizard;
        mTextNotiAddress = textNotiAddress;
    }

    @Override
    public void onClick(View v) {
        String notificationAddress = mTextNotiAddress.getText().toString();
        if (!XmppTools.isValidJID(notificationAddress)) {
            MainService.displayToast("Not a valid JID", null);
        } else {
            mWizard.initView(Wizard.VIEW_CHOOSE_METHOD);
        }
    }

}
