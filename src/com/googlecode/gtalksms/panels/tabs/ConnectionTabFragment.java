package com.googlecode.gtalksms.panels.tabs;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.widgets.SwitchCheckBoxCompat;

public class ConnectionTabFragment extends SherlockFragment {
    
    private SettingsManager mSettingsMgr;
    private EditText mEditTextLogin;
    private EditText mEditNotificationAddress;
    private EditText mEditTextPassword;
    private SwitchCheckBoxCompat mSwitchConnection;
    private Button mStartStopButton;
    private TextView mStatusActionTextView;
    private String mCurrentAction = MainService.ACTION_CONNECT;
    private int mCurrentStatus = XmppManager.DISCONNECTED;
    private String mCurrentStatusAction = "";

    @Override
    public void onResume() {
        super.onResume();
        updateStatus(mCurrentStatus, mCurrentStatusAction);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_connection, container, false);
        
        mSettingsMgr = SettingsManager.getSettingsManager(view.getContext());
        mEditTextLogin = (EditText) view.findViewById(R.id.editTextLogin);
        mEditNotificationAddress = (EditText) view.findViewById(R.id.editTextNotificationAddress);
        mEditTextPassword = (EditText) view.findViewById(R.id.editTextPassword);
        mStartStopButton = (Button)  view.findViewById(R.id.buttonConnect);
        mStatusActionTextView = (TextView)  view.findViewById(R.id.statusAction);
        mSwitchConnection = new SwitchCheckBoxCompat(view, R.id.switchConnection);

        mEditTextLogin.setText(mSettingsMgr.getLogin());
        mEditNotificationAddress.setText(TextUtils.join("|",mSettingsMgr.getNotifiedAddresses()));
        mEditTextPassword.setText(mSettingsMgr.getPassword());
        mSwitchConnection.setChecked(mSettingsMgr.getConnectOnMainScreenStartup());
        
        if (mSwitchConnection.isChecked()) {
             Tools.startSvcIntent(getActivity().getBaseContext(), MainService.ACTION_CONNECT);
        }
        
        mSwitchConnection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSettingsMgr.setConnectOnMainScreenStartup(mSwitchConnection.isChecked());
            }
        });
        
        mStartStopButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mSettingsMgr.setLogin(mEditTextLogin.getText().toString());
                mSettingsMgr.setNotifiedAddress(mEditNotificationAddress.getText().toString());
                mSettingsMgr.setPassword(mEditTextPassword.getText().toString());
                
                if (!mSettingsMgr.getLogin().equals("")) {
                    Tools.startSvcIntent(getActivity().getBaseContext(), mCurrentAction);
                }
            }
        });
        return view;
    }
    
    public void updateStatus(int status, String action) {
        mCurrentStatus = status;
        mCurrentStatusAction = action;
        if (status == XmppManager.DISCONNECTED) {
            mCurrentAction = MainService.ACTION_CONNECT;
        } else {
            mCurrentAction = MainService.ACTION_DISCONNECT;
        }

        if (mStatusActionTextView != null) {
            mStatusActionTextView.setText(action);
        }

        if (mStartStopButton != null) {
            switch (status) {
                case XmppManager.CONNECTED:
                    mStartStopButton.setText(R.string.panel_connection_button_disconnect);
                    break;
                case XmppManager.DISCONNECTED:
                    mStartStopButton.setText(R.string.panel_connection_button_connect);
                    break;
                case XmppManager.CONNECTING:
                    mStartStopButton.setText(R.string.panel_connection_button_connecting);
                    break;
                case XmppManager.DISCONNECTING:
                    mStartStopButton.setText(R.string.panel_connection_button_disconnecting);
                    break;
                case XmppManager.WAITING_TO_CONNECT:
                case XmppManager.WAITING_FOR_NETWORK:
                    mStartStopButton.setText(R.string.panel_connection_button_waiting);
                    break;
                default:
                    throw new IllegalStateException();
            }
            
            mEditNotificationAddress.setText(TextUtils.join("|",mSettingsMgr.getNotifiedAddresses()));
            if(mCurrentAction.equals(MainService.ACTION_CONNECT)) {
                mStartStopButton.setEnabled(true);
                mEditTextLogin.setEnabled(true);
                mEditNotificationAddress.setEnabled(true);
                mEditTextPassword.setEnabled(true);
            } else {
                mStartStopButton.setEnabled(mCurrentAction.equals(MainService.ACTION_DISCONNECT));
                mEditTextLogin.setEnabled(false);
                mEditNotificationAddress.setEnabled(false);
                mEditTextPassword.setEnabled(false);
            }
        }
    }
}