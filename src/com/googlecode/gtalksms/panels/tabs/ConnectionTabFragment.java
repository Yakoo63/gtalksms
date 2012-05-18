package com.googlecode.gtalksms.panels.tabs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockFragment;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.widgets.SwitchCheckBoxCompat;

public class ConnectionTabFragment extends SherlockFragment {
    
    SettingsManager mSettingsMgr;
    EditText mEditTextLogin;
    EditText mEditNotificationAddress;
    EditText mEditTextPassword;
    SwitchCheckBoxCompat mSwitchConnection;
    Button mStartStopButton;
    String mCurrentAction = MainService.ACTION_CONNECT;
    int mCurrentStatus = XmppManager.DISCONNECTED;
    
    @Override
    public void onResume() {
        super.onResume();
        updateStatus(mCurrentStatus);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_connection, container, false);
        
        mSettingsMgr = SettingsManager.getSettingsManager(view.getContext());
        mEditTextLogin = (EditText) view.findViewById(R.id.editTextLogin);
        mEditNotificationAddress = (EditText) view.findViewById(R.id.editTextNotificationAddress);
        mEditTextPassword = (EditText) view.findViewById(R.id.editTextPassword);
        mStartStopButton = (Button)  view.findViewById(R.id.buttonConnect);
        mSwitchConnection = new SwitchCheckBoxCompat(view, R.id.switchConnection);

        mEditTextLogin.setText(mSettingsMgr.getLogin());
        mEditNotificationAddress.setText(mSettingsMgr.getNotifiedAddress());
        mEditTextPassword.setText(mSettingsMgr.getPassword());
        mSwitchConnection.setChecked(mSettingsMgr.getConnectOnMainScreenStartup());
        
        if (mSettingsMgr.getConnectOnMainScreenStartup()) {
             Tools.startSvcIntent(getActivity().getBaseContext(), MainService.ACTION_CONNECT);
        }
        
        mStartStopButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mSettingsMgr.setUseDifferentAccount(true);
                mSettingsMgr.setLogin(mEditTextLogin.getText().toString());
                mSettingsMgr.setNotifiedAddress(mEditNotificationAddress.getText().toString());
                mSettingsMgr.setPassword(mEditTextPassword.getText().toString());
                mSettingsMgr.setConnectOnMainScreenStartup(mSwitchConnection.isChecked());
                
                Tools.startSvcIntent(getActivity().getBaseContext(), mCurrentAction);
            }
        });
        return view;
    }
    
    public void updateStatus(int status) {
        mCurrentStatus = status;
        mCurrentAction = MainService.ACTION_DISCONNECT;
        
        switch (status) {
            case XmppManager.CONNECTED:
                mStartStopButton.setText(R.string.panel_connection_button_disconnect);
                break;
            case XmppManager.DISCONNECTED:
                mCurrentAction = MainService.ACTION_CONNECT;
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
        
        if(mCurrentAction.equals(MainService.ACTION_CONNECT)) {
            mStartStopButton.setEnabled(true);
            mSwitchConnection.setEnabled(true);
            mEditTextLogin.setEnabled(true);
            mEditNotificationAddress.setEnabled(true);
            mEditTextPassword.setEnabled(true);
        } else {
            mStartStopButton.setEnabled(mCurrentAction.equals(MainService.ACTION_DISCONNECT));
            mSwitchConnection.setEnabled(false);
            mEditTextLogin.setEnabled(false);
            mEditNotificationAddress.setEnabled(false);
            mEditTextPassword.setEnabled(false);
        }
    }
}