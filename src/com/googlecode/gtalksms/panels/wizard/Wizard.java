package com.googlecode.gtalksms.panels.wizard;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.googlecode.gtalksms.Log;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.tools.StringFmt;
import com.googlecode.gtalksms.tools.Tools;

/**
 * Wizard control flow:
 * 
 * Welcome --> Choose Method -->  Choose Server --> Create --> Create Success
 *                            \
 *                             --> Same Account
 *                             |
 *                             --> Existing Account
 *                             
 * Not that "Same Account" and "Existing Account" share the same layout, the
 * only difference is that with "Same Account" the notification address is 
 * set as login and the editText field is made unchangeable
 * 
 * @author Florian Schmaus fschmaus@gmail.com - on behalf of the GTalkSMS Team
 *
 */
public class Wizard extends Activity {
    
    protected final static int VIEW_WELCOME = 0;
    protected final static int VIEW_CHOOSE_METHOD = 1;    
    protected final static int VIEW_CREATE_CHOOSE_SERVER = 2;
    protected final static int VIEW_CREATE = 3;
    protected final static int VIEW_CREATE_SUCCESS = 4;
    protected final static int VIEW_EXISTING_ACCOUNT = 5;
    protected final static int VIEW_SAME_ACCOUNT = 6;
        
    // these attributes define the state of the wizzard
    // they should be save restored from savedInstanceState
    protected String mNotifiedAddress;
    protected int mChoosenMethod;
    protected int mChoosenServer;
    protected int mChoosenServerSpinner;
    protected String mChoosenServername;
    protected String mLogin;
    protected String mPassword1;
    protected String mPassword2;
 
    private int mCurrentView = 0;
    private SettingsManager mSettingsMgr;
    
    public void onSaveInstanceState(Bundle savedBundle) {
        // TODO if it is possible save the contents from the widgets of 
        // the View Create to mLogoin mPassword etc 
        // with an if mCurrentview == VIEW_CREATE 
        
        if (mNotifiedAddress != null) 
            savedBundle.putString("mNotifiedAddress", mNotifiedAddress);
        if (mChoosenMethod != 0) 
            savedBundle.putInt("mChoosenMethod", mChoosenMethod);
        if (mChoosenServer != 0)
            savedBundle.putInt("mChoosenServer", mChoosenServer);
        if (mChoosenServerSpinner != 0)
            savedBundle.putInt("mChoosenServerSpinner", mChoosenServerSpinner);
        if (mChoosenServername != null)
            savedBundle.putString("mChoosenServername", mChoosenServername);
        if (mLogin != null)
            savedBundle.putString("mLogin", mLogin);
        if (mPassword1 != null)
            savedBundle.putString("mPassword1", mPassword1);
        if (mPassword2 != null)
            savedBundle.putString("mPassword2", mPassword2);
        if (mCurrentView != 0)
            savedBundle.putInt("mCurrentView", mCurrentView);
    }
    
    private void restoreStateFromBundle(Bundle savedBundle) {
        String nA = savedBundle.getString("mNotifiedAddress");
        int cM = savedBundle.getInt("mChoosenMethod");
        int cS = savedBundle.getInt("mChoosenServer");
        int cSS = savedBundle.getInt("mChoosenServerSpinner");
        String cSN = savedBundle.getString("mChoosenServername");
        String l = savedBundle.getString("mLogin");
        String psw1 = savedBundle.getString("mPassword1");
        String psw2 = savedBundle.getString("mPassword2");
        int cV = savedBundle.getInt("mCurrentView");
        
        if (nA != null)
            mNotifiedAddress = nA;
        if (cM != 0)
            mChoosenMethod = cM;
        if (cS != 0)
            mChoosenServer = cS;
        if (cSS != 0)
            mChoosenServerSpinner = cSS;
        if (cSN != null)
            mChoosenServername = cSN;
        if (l != null)
            mLogin = l;
        if (psw1 != null)
            mPassword1 = psw1;
        if (psw2 != null)
            mPassword2 = psw2;
        if (cV != 0) 
            mCurrentView = cV;
    }
    
    /** 
     * Called when the activity is first created. 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            restoreStateFromBundle(savedInstanceState);
        }
        mSettingsMgr = SettingsManager.getSettingsManager(this);
        
        Log.initialize(mSettingsMgr);
        initView(mCurrentView);
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
    
    protected void initView(int viewId) {
        
        Button next;
        Button login;
        RadioGroup rg;
        EditText textLogin;
        EditText textPassword1;
        EditText textPassword2;
        switch (viewId) {
            case VIEW_WELCOME:
                setContentView(R.layout.wizard_welcome);
                next = (Button) findViewById(R.id.nextBut);
                EditText textNotiAddress = (EditText) findViewById(R.id.notificationAddress);
                if (mNotifiedAddress != null) {
                    textNotiAddress.setText(mNotifiedAddress);
                }
                next.setOnClickListener(new WelcomeNextButtonClickListener(this, textNotiAddress));
                break;
            case VIEW_CHOOSE_METHOD:
                setContentView(R.layout.wizard_choose_method);
                mapWizardButton(R.id.backBut, VIEW_WELCOME);
                next = (Button) findViewById(R.id.nextBut);
                rg = (RadioGroup) findViewById(R.id.radioGroupMethod);
                switch (mChoosenMethod) {
                    case R.id.radioDifferentAccount:
                       ((RadioButton) findViewById(R.id.radioDifferentAccount)).setChecked(true);
                       break;
                    case R.id.radioExsistingAccount:
                        ((RadioButton) findViewById(R.id.radioExsistingAccount)).setChecked(true);
                        break;
                    case R.id.radioSameAccount:
                        ((RadioButton) findViewById(R.id.radioSameAccount)).setChecked(true);
                        break;
                }
                next.setOnClickListener(new ChooseMethodNextButtonClickListener(this, rg));
                break;
            case VIEW_CREATE_CHOOSE_SERVER:
                setContentView(R.layout.wizard_create_choose_server);
                // find and setup the widgets
                next = (Button) findViewById(R.id.nextBut);
                Spinner spinner = (Spinner) findViewById(R.id.serverChooser);
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.predefined_xmpp_servers, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
                EditText textServer = (EditText) findViewById(R.id.textServer);
                textServer.setEnabled(false);
                rg = (RadioGroup) findViewById(R.id.radioGroupServer);
                rg.setOnCheckedChangeListener(new ChooseServerRadioGroupChangeListener(spinner, textServer));
                next = (Button) findViewById(R.id.nextBut);
                // restore the old state
                switch (mChoosenServer) {
                    case R.id.radioChooseServer:
                        ((RadioButton) findViewById(R.id.radioChooseServer)).setChecked(true);
                        if (mChoosenServerSpinner != 0) {
                            spinner.setSelection(mChoosenServerSpinner);
                        }
                        break;
                    case R.id.radioManualServer:
                        ((RadioButton) findViewById(R.id.radioManualServer)).setChecked(true);
                        if (mChoosenServername != null) {
                            ((EditText) findViewById(R.id.textServer)).setText(mChoosenServername);
                        }
                        break;
                }
                // map the listeneres to the buttons
                mapWizardButton(R.id.backBut, VIEW_CHOOSE_METHOD);
                next.setOnClickListener(new ChooseServerNextButtonClickListener(this, rg, spinner, textServer));
                break;
            case VIEW_CREATE:
                setContentView(R.layout.wizard_create);
                // find and setup the widgets
                Button create = (Button) findViewById(R.id.createBut);
                textLogin = (EditText) findViewById(R.id.login);
                textPassword1 = (EditText) findViewById(R.id.password1);
                textPassword2 = (EditText) findViewById(R.id.password2);
                mapWizardButton(R.id.backBut, VIEW_CREATE_CHOOSE_SERVER);
                create.setOnClickListener(new CreateButtonClickListener(this, mSettingsMgr, textLogin, textPassword1, textPassword2));
                break;
            case VIEW_CREATE_SUCCESS:
                setContentView(R.layout.wizard_create_success);
                Button backToMain = (Button) findViewById(R.id.backToMainscreen);
                backToMain.setOnClickListener(new BackToMainClickListener(this));
                break;
            case VIEW_EXISTING_ACCOUNT:
                setContentView(R.layout.wizard_existing_account);
                login = (Button) findViewById(R.id.loginBut);
                textLogin = (EditText) findViewById(R.id.login);
                textPassword1 = (EditText) findViewById(R.id.password1);

                // TODO restore from savedBundle
                mapWizardButton(R.id.backBut, VIEW_CHOOSE_METHOD);
                login.setOnClickListener(new UseAccountLoginButtonClickListener(this, textLogin, textPassword1));
                break;
            case VIEW_SAME_ACCOUNT:
                setContentView(R.layout.wizard_existing_account);
                login = (Button) findViewById(R.id.loginBut);
                textLogin = (EditText) findViewById(R.id.login);
                textPassword1 = (EditText) findViewById(R.id.password1);
                // the user wants to use the same account for notification and gtalksms
                // so just enter as login the notificationAddress
                String loginStr = mNotifiedAddress;
                textLogin.setEnabled(false);
                textLogin.setText(loginStr);
                // TODO restore from savedBundle
                mapWizardButton(R.id.backBut, VIEW_CHOOSE_METHOD);
                login.setOnClickListener(new UseAccountLoginButtonClickListener(this, loginStr, textPassword1));
                break;
            default:
                throw new IllegalStateException();
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
