package com.googlecode.gtalksms.panels.wizard;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.googlecode.gtalksms.Log;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.tools.StringFmt;
import com.googlecode.gtalksms.tools.Tools;

public class Wizard extends Activity {
    private final static int VIEW_WELCOME = 0;
    private final static int VIEW_LOGIN = 1;
    private final static int VIEW_CREATE = 2;
    private final static int VIEW_NOTIFICATIONS = 3;
 
    private int mCurrentView = 0;
    private SettingsManager mSettingsMgr;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSettingsMgr = SettingsManager.getSettingsManager(this);
        
        Log.initialize(mSettingsMgr);
        initView(VIEW_WELCOME);
    }
    
    class WizardButtonListener implements View.OnClickListener {
        private int mView;
        
        public WizardButtonListener(int view) {
            mView = view;
        }
        
        @Override
        public void onClick(View arg0) {
            initView(mView);
        }
    }
    
    private void mapWizardButton(int id, int view) {
        Button button = (Button) findViewById(id);
        if (button != null) {
            button.setOnClickListener(new WizardButtonListener(view));   
        } else {
            Log.w("Failed to initialize Wizard button mapping, id=" + id + ", view=" + view);
        }
    }
    
    private void initView(int viewId) {
        
        switch (viewId) {
            case VIEW_WELCOME:
                setContentView(R.layout.wizard_welcome);
                mapWizardButton(R.id.loginBut, VIEW_LOGIN);
                mapWizardButton(R.id.createBut, VIEW_CREATE);
                break;

            case VIEW_CREATE:
                setContentView(R.layout.wizard_create);
                mapWizardButton(R.id.backBut, VIEW_WELCOME);
                // TODO real account creation
                // mapWizardButton(R.id.createBut, VIEW_CREATE);
                Button createButton = (Button) findViewById(R.id.createBut);
                createButton.setOnClickListener(new OnClickListener() {
                    @Override 
                    public void onClick(View arg0) {
                        createAccount();
                    }
                });
                Button deleteButton = (Button) findViewById(R.id.deleteBut);
                deleteButton.setOnClickListener(new OnClickListener() {
                    @Override 
                    public void onClick(View arg0) {
                        deleteAccount();
                    }
                });
                break;

            case VIEW_LOGIN:
                setContentView(R.layout.wizard_login);
                mapWizardButton(R.id.backBut, VIEW_WELCOME);
                // TODO real login (test?)
                // mapWizardButton(R.id.loginBut, VIEW_LOGIN);
                break;

            default:
                break;
        }
        
        TextView label = (TextView) findViewById(R.id.VersionLabel);
        label.setText(StringFmt.Style(Tools.APP_NAME + Tools.getVersionName(getBaseContext()), Typeface.BOLD));

        mCurrentView = viewId;
    }
    
    private XMPPConnection getConnection(String server) throws Exception {
        // Allow choosing another account
        ConnectionConfiguration conf = new ConnectionConfiguration(server);
        conf.setTruststorePath("/system/etc/security/cacerts.bks");
        conf.setTruststorePassword("changeit");
        conf.setTruststoreType("bks");
        
        XMPPConnection connection = new XMPPConnection(conf);
        connection.connect();
        return connection;
    }

    private void createAccount() {
        TextView server = (TextView) findViewById(R.id.server);
        TextView login = (TextView) findViewById(R.id.login);
        TextView password = (TextView) findViewById(R.id.password1);
        TextView result = (TextView) findViewById(R.id.result);
         
        try {
            result.setText("");
            // TODO use XmppAccountManager.tryToCreateAccount(jid, psw2, mSettingsMgr);
            getConnection(server.getText().toString()).getAccountManager().createAccount(
                    login.getText().toString(), 
                    password.getText().toString());
            result.setText("Ok");
        } catch (Exception e) {
            Log.e("Failed to create jabber account", e);
            result.setText(e.getLocalizedMessage());
        }
    }
    
    private void deleteAccount() {
        TextView server = (TextView) findViewById(R.id.server);
        TextView login = (TextView) findViewById(R.id.login);
        TextView password = (TextView) findViewById(R.id.password1);
        TextView result = (TextView) findViewById(R.id.result);
         
        try {
            result.setText("");
            Connection connection = getConnection(server.getText().toString());
            connection.login(login.getText().toString(), password.getText().toString());
            connection.getAccountManager().deleteAccount();
            result.setText("Ok");
        } catch (Exception e) {
            Log.e("Failed to create jabber account", e);
            result.setText(e.getLocalizedMessage());
        }
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
