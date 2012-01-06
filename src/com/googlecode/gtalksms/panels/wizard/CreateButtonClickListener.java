package com.googlecode.gtalksms.panels.wizard;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import android.os.AsyncTask;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppAccountManager;

public class CreateButtonClickListener implements OnClickListener {
    
    private SettingsManager mSettingsMgr;
    private Wizard mWizard;
    private EditText mTextUsername;
    private EditText mTextPsw1;
    private EditText mTextPsw2;
    
    public CreateButtonClickListener(Wizard wiz, EditText username, EditText psw1, EditText psw2) {
        this.mSettingsMgr = SettingsManager.getSettingsManager(wiz);
        this.mWizard = wiz;
        this.mTextUsername = username;
        this.mTextPsw1 = psw1;
        this.mTextPsw2 = psw2;
    }

    @Override
    public void onClick(View v) {
        class CreateAccountAsync extends AsyncTask<String, Float, Boolean> {
            String mToastMessage;


            @Override
            protected Boolean doInBackground(String... params) {
                String login = params[0];
                String password = params[1];
                XMPPConnection con = null;
                mToastMessage = "Account succesfull created";
                try {
                    con = XmppAccountManager.tryToCreateAccount(login, mWizard.mChoosenServername, password);
                    publishProgress(0.5f);
                } catch (XMPPException e) {
                    mToastMessage = e.getLocalizedMessage();
                }
                
                if (con != null) {
                    String jid = login + "@" + mWizard.mChoosenServername;
                    // this will inform the XmppManager about the newly created
                    // connection and also reuse the connection
                    XmppManager.getInstance(mWizard, con);
                    publishProgress(0.6f);
                    // inform the service that it should be now in a CONNECTED state
                    Tools.startSvcIntent(mWizard, MainService.ACTION_CONNECT);
                    publishProgress(0.8f);
                    XmppAccountManager.savePreferences(jid, password, mWizard.mNotifiedAddress, mSettingsMgr);
                    return true;
                } else {
                    return false;
                }
            }
            protected void onPostExecute(Boolean success) {
                if (success) {
                    mWizard.initView(Wizard.VIEW_CREATE_SUCCESS);
                }
                MainService.displayToast(mToastMessage, null, true);
            }

        }

        String login = mTextUsername.getText().toString().trim();
        String psw1 = mTextPsw1.getText().toString().trim();
        String psw2 = mTextPsw2.getText().toString().trim();
        if (psw1.equals(psw2)) {
            CreateAccountAsync createAccountAsync = new CreateAccountAsync();
            createAccountAsync.execute(login, psw1);
        } else {
            MainService.displayToast("The passwords do not match", null, true);
        }
    }

}
