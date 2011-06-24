package com.googlecode.gtalksms.panels.wizard;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.xmpp.XmppTools;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;


public class UseAccountNextButtonClickListener implements OnClickListener {
    
    private Wizard mWizard;
    private EditText mLogin;
    private EditText mPassword;

    public UseAccountNextButtonClickListener(Wizard wizard, EditText login, EditText password) {
        mWizard = wizard;
        mLogin = login;
        mPassword = password;
    }
    
    @Override
    public void onClick(View v) {
        // TODO decide how to that whats to do here :)
    }
}
