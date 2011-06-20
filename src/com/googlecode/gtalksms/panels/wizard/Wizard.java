package com.googlecode.gtalksms.panels.wizard;

import org.jivesoftware.smack.XMPPException;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.googlecode.gtalksms.Log;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.tools.StringFmt;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppAccountManager;

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
                Button mCreate = (Button) findViewById(R.id.createBut);
                mCreate.setOnClickListener(
                        new View.OnClickListener() {
                            
                            @Override
                            public void onClick(View v) {
                                EditText login = (EditText)findViewById(R.id.login);
                                EditText pass1 = (EditText)findViewById(R.id.password1);
                                EditText pass2 = (EditText)findViewById(R.id.password2);
                                String jid = login.getText().toString().trim();
                                String psw1 = pass1.getText().toString().trim();
                                String psw2 = pass2.getText().toString().trim();
                                if (psw1.equals(psw2)) {
                                    String res = null;
                                    try {
                                        res = XmppAccountManager.tryToCreateAccount(jid, psw2, mSettingsMgr);
                                    } catch (XMPPException e) {
                                        res = e.getLocalizedMessage();
                                    }
                                    if (res == null) {
                                        res = "Account succesfull created";
                                    }
                                    MainService.displayToast(res, null);
                                }
                                
                            }
                        }
                    );
//                    Button deleteButton = (Button) findViewById(R.id.deleteBut);
//                    deleteButton.setOnClickListener(
//                            new View.OnClickListener() {
//                                @Override
//                                public void onClick(View arg0) {
//                                    XmppManager.deleteAccount();
//                                }
//                            }
//                    );
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
        label.setText(StringFmt.Style(Tools.APP_NAME + " " + Tools.getVersionName(getBaseContext()), Typeface.BOLD));

        mCurrentView = viewId;
    }

    /** Called when the activity is first created. */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }    
}
