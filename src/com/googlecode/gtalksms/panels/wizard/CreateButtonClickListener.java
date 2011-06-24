package com.googlecode.gtalksms.panels.wizard;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppAccountManager;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

public class CreateButtonClickListener implements OnClickListener {
    
    private SettingsManager mSettingsMgr;
    private Wizard mWizard;
    private EditText mTextUsername;
    private EditText mTextPsw1;
    private EditText mTextPsw2;
    
    public CreateButtonClickListener(Wizard wiz, SettingsManager sm, EditText username, EditText psw1, EditText psw2) {
        this.mSettingsMgr = sm;
        this.mWizard = wiz;
        this.mTextUsername = username;
        this.mTextPsw1 = psw1;
        this.mTextPsw2 = psw2;
    }

    @Override
    public void onClick(View v) {

        String login = mTextUsername.getText().toString().trim();
        String psw1 = mTextPsw1.getText().toString().trim();
        String psw2 = mTextPsw2.getText().toString().trim();
        if (psw1.equals(psw2)) {
            String res = "Error on account creation";
            XMPPConnection con = null;
            try {
                con = XmppAccountManager.tryToCreateAccount(login, mWizard.mChoosenServername, psw1);
            } catch (XMPPException e) {
                res = e.getLocalizedMessage();
            }
            String toastMessage;
            if (con != null) {
                toastMessage = "Account succesfull created";
            } else {
                toastMessage = res;
            }
            MainService.displayToast(toastMessage, null);
            
            if (con != null) {
                String jid = login + "@" + mWizard.mChoosenServername;
                // this will inform the XmppManager about the newly created
                // connection and also reuse the connection
                XmppManager.getInstance(mWizard, con);
                Tools.startSvcIntent(mWizard, MainService.ACTION_CONNECT);
                XmppAccountManager.savePreferences(jid, psw1, mWizard.mNotifiedAddress, mSettingsMgr);
                mWizard.initView(Wizard.VIEW_CREATE_SUCCESS);
            }
        } else {
            MainService.displayToast("The passwords do not match", null);
        }
    }

}
