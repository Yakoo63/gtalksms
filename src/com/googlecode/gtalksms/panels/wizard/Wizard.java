package com.googlecode.gtalksms.panels.wizard;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
 
    private int mCurrentView = 0;
    private SettingsManager mSettingsMgr;
    
    /** 
     * Called when the activity is first created. 
     */
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
    
    protected void initView(int viewId) {
        
        Button next;
        RadioGroup rg;
        switch (viewId) {
            case VIEW_WELCOME:
                setContentView(R.layout.wizard_welcome);
                next = (Button) findViewById(R.id.nextBut);
                EditText textNotiAddress = (EditText) findViewById(R.id.notificationAddress);
                next.setOnClickListener(new WelcomeNextButtonClickListener(this, textNotiAddress));
                break;
            case VIEW_CHOOSE_METHOD:
                setContentView(R.layout.wizard_choose_method);
                mapWizardButton(R.id.backBut, VIEW_WELCOME);
                next = (Button) findViewById(R.id.nextBut);
                rg = (RadioGroup) findViewById(R.id.radioGroupMethod);
                next.setOnClickListener(new ChooseMethodNextButtonClickListener(this, rg));
                break;
            case VIEW_CREATE_CHOOSE_SERVER:
                setContentView(R.layout.wizard_create_choose_server);
                Spinner spinner = (Spinner) findViewById(R.id.serverChooser);
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.predefined_xmpp_servers, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
                EditText textServer = (EditText) findViewById(R.id.textServer);
                rg = (RadioGroup) findViewById(R.id.radioGroupServer);
                rg.setOnCheckedChangeListener(new ChooseServerRadioGroupChangeListener(spinner, textServer));
                mapWizardButton(R.id.backBut, VIEW_CHOOSE_METHOD);
                mapWizardButton(R.id.nextBut, VIEW_CREATE);
                break;
            case VIEW_CREATE:
                setContentView(R.layout.wizard_create);
                mapWizardButton(R.id.backBut, VIEW_CREATE_CHOOSE_SERVER);
                Button create = (Button) findViewById(R.id.createBut);
                create.setOnClickListener(new CreateButtonClickListener(this, mSettingsMgr));
                break;
            case VIEW_SAME_ACCOUNT:
                setContentView(R.layout.wizard_existing_account);
                String login = ((EditText)findViewById(R.id.notificationAddress)).getText().toString();
                EditText loginText = (EditText) findViewById(R.id.login);
                loginText.setEnabled(false);
                loginText.setText(login);
                mapWizardButton(R.id.backBut, VIEW_CHOOSE_METHOD);
                // TODO map next button
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
