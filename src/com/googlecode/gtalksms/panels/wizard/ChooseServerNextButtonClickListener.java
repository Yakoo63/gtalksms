package com.googlecode.gtalksms.panels.wizard;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.xmpp.XmppTools;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;

public class ChooseServerNextButtonClickListener implements OnClickListener {
    
    private Wizard mWizard;
    private RadioGroup mRadioGroupServer;
    private Spinner mPredefinedServer;
    private EditText mTextServer;
    
    /**
     * 
     * @param wizard
     * @param rg
     * @param sp
     * @param et
     */
    public ChooseServerNextButtonClickListener(Wizard wizard, RadioGroup rg, Spinner sp, EditText et) {
        mWizard = wizard;
        mRadioGroupServer = rg;
        mPredefinedServer = sp;
        mTextServer = et;
    }
    
    @Override
    public void onClick(View v) {
        int checkedButton = mRadioGroupServer.getCheckedRadioButtonId();
        mWizard.mChoosenServer = checkedButton;
        switch (checkedButton) {
            case R.id.radioChooseServer:
                mWizard.mChoosenServerSpinner = mPredefinedServer.getSelectedItemPosition();
                // TODO check that this String cast here works
                // we set the servername here also, so that we can retrive it 
                // later when creating the account
                mWizard.mChoosenServername = (String) mPredefinedServer.getSelectedItem();
                break;
            case R.id.radioManualServer:
                String servername = mTextServer.getText().toString();
                if (!XmppTools.isValidServername(servername)) {
                    MainService.displayToast("\" " + servername + " \" is not a valid servername" , null, true);
                    return;
                }
                mWizard.mChoosenServername = servername;
                break;
            default:
                throw new IllegalStateException();
        }
        mWizard.initView(Wizard.VIEW_CREATE);
    }
}
