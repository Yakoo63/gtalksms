package com.googlecode.gtalksms.panels.wizard;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppAccountManager;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

public class CreateButtonClickListener implements OnClickListener {
    
    private SettingsManager mSettingsMgr;
    private Context mContext;
    
    public CreateButtonClickListener(Context ctx, SettingsManager sm) {
        this.mSettingsMgr = sm;
        this.mContext = ctx;
    }

    @Override
    public void onClick(View v) {
        EditText login = (EditText)v.findViewById(R.id.login);
        EditText pass1 = (EditText)v.findViewById(R.id.password1);
        EditText pass2 = (EditText)v.findViewById(R.id.password2);
        String jid = login.getText().toString().trim();
        String psw1 = pass1.getText().toString().trim();
        String psw2 = pass2.getText().toString().trim();
        if (psw1.equals(psw2)) {
            String res = null;
            XMPPConnection  con = null;
            try {
                con = XmppAccountManager.tryToCreateAccount(jid, psw2, mSettingsMgr);
            } catch (XMPPException e) {
                res = e.getLocalizedMessage();
            }
            if (res == null) {
                res = "Account succesfull created";
            }
            MainService.displayToast(res, null);
            // this will inform the XmppManager about the newly created
            // connection and also reuse the connection
            XmppManager.getInstance(mContext, con);
            Tools.startSvcIntent(mContext, MainService.ACTION_CONNECT);
            // TODO go here to wizard_create_successfull view
        }   
    }

}
