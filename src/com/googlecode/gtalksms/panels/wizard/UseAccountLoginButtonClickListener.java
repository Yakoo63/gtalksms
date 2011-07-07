package com.googlecode.gtalksms.panels.wizard;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppAccountManager;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;


public class UseAccountLoginButtonClickListener implements OnClickListener {
    
    private Wizard mWizard;
    private EditText mLogin;
    private EditText mPassword;
    private String mLoginStr;
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
    public UseAccountLoginButtonClickListener(Wizard wizard, String login, EditText password) {
        mSettings = SettingsManager.getSettingsManager(wizard);
        mWizard = wizard;
        mLoginStr = login;
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
        XMPPConnection con;
        try {
            String host = StringUtils.parseServer(login);
            con = XmppAccountManager.tryToCreateAccount(login, host, password);
                XmppAccountManager.savePreferences(login, password, mWizard.mNotifiedAddress, mSettings);
                XmppManager.getInstance(mWizard, con);
                Tools.startSvcIntent(mWizard, MainService.ACTION_CONNECT);
        } catch (XMPPException e) {            
            MainService.displayToast("Could not configure: " + e.getLocalizedMessage(), null);
        }
    }
}
