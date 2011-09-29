package com.googlecode.gtalksms.panels.wizard;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppAccountManager;

import android.os.AsyncTask;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;


public class UseAccountLoginButtonClickListener implements OnClickListener {
    
    private Wizard mWizard;
    private EditText mLogin;
    private EditText mPassword;
    private boolean mUseSameAccount;
    private SettingsManager mSettings;
    
    /**
     * This constructor is used when the user does not want to use the same
     * account for notifications and remote control
     * 
     * @param wizard
     * @param login
     * @param password
     */
    public UseAccountLoginButtonClickListener(Wizard wizard, EditText login, EditText password) {
        mSettings = SettingsManager.getSettingsManager(wizard);
        mWizard = wizard;
        mLogin = login;
        mPassword = password;
        mUseSameAccount = false;
    }
    
    /**
     * This constructor is used when the user wants to use the same account for
     * notifications and remote control - NOT RECOMMENDED      
     * 
     * @param wizard
     * @param login
     * @param password
     */
    public UseAccountLoginButtonClickListener(Wizard wizard, EditText password) {
        mSettings = SettingsManager.getSettingsManager(wizard);
        mWizard = wizard;
        mPassword = password;
        mUseSameAccount = true;
    }
    
    @Override
    public void onClick(View v) {
        String login;
        String password;

        password = mPassword.getText().toString();
        mWizard.mPassword1 = password;
        // also save the login if it isn't the notification address
        if (!mUseSameAccount) {
            login = mLogin.getText().toString();
            mWizard.mLogin = login;
        } else {
            login = mWizard.mNotifiedAddress;
        }

        class ConAndSaveAsync extends AsyncTask<String, Float, Boolean> {
            private final SettingsManager mSettings;
            private Exception e = null;

            public ConAndSaveAsync(SettingsManager settings) {
                this.mSettings = settings;
            }

            @Override
            protected Boolean doInBackground(String... params) {
                XMPPConnection con;
                String login = params[0];
                String password = params[1];
                String notifiedAddress = params[2];

                try {
                    con = XmppAccountManager.makeConnectionAndSavePreferences(login, password, notifiedAddress, mSettings);

                    publishProgress(0.5f);
                    // inform the xmppManager about the new connection
                    XmppManager.getInstance(mWizard, con);
                    publishProgress(0.7f);
                    // inform the service that it should be now in a CONNECTED
                    // state
                    Tools.startSvcIntent(mWizard, MainService.ACTION_CONNECT);

                } catch (XMPPException e) {
                    this.e = e;
                    return false;
                }
                return true;
            }

            protected void onPostExecute(Boolean success) {
                if (success) {
                    // show the success view
                    mWizard.initView(Wizard.VIEW_CREATE_SUCCESS);
                } else {
                    MainService.displayToast("Could not configure: " + this.e.getLocalizedMessage(), null);
                }
            }

        }

        ConAndSaveAsync conAndSaveAsync = new ConAndSaveAsync(mSettings);
        conAndSaveAsync.execute(login, password, mWizard.mNotifiedAddress);
    }
}
