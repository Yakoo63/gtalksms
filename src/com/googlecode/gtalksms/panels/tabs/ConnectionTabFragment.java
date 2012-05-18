package com.googlecode.gtalksms.panels.tabs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import com.actionbarsherlock.app.SherlockFragment;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.tools.Tools;

public class ConnectionTabFragment extends SherlockFragment {
    SettingsManager mSettingsMgr;
    EditText mEditTextLogin;
    EditText mEditNotificationAddress;
    EditText mEditTextPassword;
    Switch mSwitchConnection;
    Button mStartStopButton;
    String mCurrentAction = MainService.ACTION_CONNECT;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_connection, container, false);
        
        mSettingsMgr = SettingsManager.getSettingsManager(view.getContext());
        mEditTextLogin = (EditText) view.findViewById(R.id.editTextLogin);
        mEditNotificationAddress = (EditText) view.findViewById(R.id.editTextNotificationAddress);
        mEditTextPassword = (EditText) view.findViewById(R.id.editTextPassword);
        
        mStartStopButton = (Button)  view.findViewById(R.id.buttonConnect);
     
        mEditTextLogin.setText(mSettingsMgr.getLogin());
        mEditNotificationAddress.setText(mSettingsMgr.getNotifiedAddress());
        mEditTextPassword.setText(mSettingsMgr.getPassword());

        if (mSettingsMgr.getConnectOnMainScreenStartup()) {
             if(mSwitchConnection != null) mSwitchConnection.setChecked(true);
             getSherlockActivity().setSupportProgressBarIndeterminateVisibility(true);
             Tools.startSvcIntent(getActivity().getBaseContext(), MainService.ACTION_CONNECT);
        }
        
        mStartStopButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mSettingsMgr.setUseDifferentAccount(true);
                mSettingsMgr.setLogin(mEditTextLogin.getText().toString());
                mSettingsMgr.setNotifiedAddress(mEditNotificationAddress.getText().toString());
                mSettingsMgr.setPassword(mEditTextPassword.getText().toString());
                if(mSwitchConnection != null) mSettingsMgr.setConnectOnMainScreenStartup(mSwitchConnection.isChecked());
                
                Tools.startSvcIntent(getActivity().getBaseContext(), mCurrentAction);
            }
        });
        
        return view;
    }
    
    public void updateStatus(int status) {
        mStartStopButton.setActivated(true);
        switch (status) {
            case XmppManager.CONNECTED:
                mCurrentAction = MainService.ACTION_DISCONNECT;
                mStartStopButton.setText(R.string.panel_connection_button_disconnect);
                break;
            case XmppManager.DISCONNECTED:
                mCurrentAction = MainService.ACTION_CONNECT;
                mStartStopButton.setText(R.string.panel_connection_button_connect);
                break;
            case XmppManager.CONNECTING:
                mStartStopButton.setActivated(false);
                mStartStopButton.setText(R.string.panel_connection_button_connecting);
                break;
            case XmppManager.DISCONNECTING:
                mStartStopButton.setActivated(false);
                mStartStopButton.setText(R.string.panel_connection_button_disconnecting);
                break;
            case XmppManager.WAITING_TO_CONNECT:
                mCurrentAction = MainService.ACTION_CONNECT;
                mStartStopButton.setText(R.string.panel_connection_button_connect);
                break;
            case XmppManager.WAITING_FOR_NETWORK:
                mCurrentAction = MainService.ACTION_CONNECT;
                mStartStopButton.setText(R.string.panel_connection_button_connect);
                break;
            default:
                throw new IllegalStateException();
        }
    }
}