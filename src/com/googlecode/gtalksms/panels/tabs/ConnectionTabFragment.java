package com.googlecode.gtalksms.panels.tabs;

import android.content.Intent;
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
import com.googlecode.gtalksms.tools.Tools;

public class ConnectionTabFragment extends SherlockFragment {
    SettingsManager _settingsMgr;
    EditText _editTextLogin;
    EditText _editTextPassword;
    Switch _switchConnection;
    Button _startStopButton;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_connection, container, false);
        
        _settingsMgr = SettingsManager.getSettingsManager(view.getContext());
        _editTextLogin = (EditText) view.findViewById(R.id.editTextLogin);
        _editTextPassword = (EditText) view.findViewById(R.id.editTextPassword);
        _switchConnection = (Switch) view.findViewById(R.id.switchConnection);
        _startStopButton = (Button)  view.findViewById(R.id.buttonConnect);
        
        _editTextLogin.setText(_settingsMgr.getLogin());
        _editTextPassword.setText(_settingsMgr.getPassword());
        getSherlockActivity().setSupportProgressBarIndeterminateVisibility(false);

        if (_settingsMgr.getConnectOnMainScreenStartup()) {
             _switchConnection.setChecked(true);
             getSherlockActivity().setSupportProgressBarIndeterminateVisibility(true);
             Tools.startSvcIntent(getActivity().getBaseContext(), MainService.ACTION_CONNECT);
        }
        
        _startStopButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                _startStopButton.setText("Connecting...");
                _settingsMgr.setUseDifferentAccount(true);
                _settingsMgr.setLogin(_editTextLogin.getText().toString());
                _settingsMgr.setPassword(_editTextPassword.getText().toString());
                _settingsMgr.setConnectOnMainScreenStartup(_switchConnection.isChecked());
                
                getSherlockActivity().setSupportProgressBarIndeterminateVisibility(true);
                Tools.startSvcIntent(getActivity().getBaseContext(), MainService.ACTION_CONNECT);
            }
        });
        
        return view;
    }
}