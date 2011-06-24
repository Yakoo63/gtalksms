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
    
    public CreateButtonClickListener(Wizard wiz, SettingsManager sm) {
        this.mSettingsMgr = sm;
        this.mWizard = wiz;
    }

    @Override
    public void onClick(View v) {
        EditText textLogin = (EditText)v.findViewById(R.id.login);
        EditText textPass1 = (EditText)v.findViewById(R.id.password1);
        EditText textPass2 = (EditText)v.findViewById(R.id.password2);
        String login = textLogin.getText().toString().trim();
        String psw1 = textPass1.getText().toString().trim();
        String psw2 = textPass2.getText().toString().trim();
        if (psw1.equals(psw2)) {
            String res = "Error on account creation";
            XMPPConnection  con = null;
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
        }   
    }

}
