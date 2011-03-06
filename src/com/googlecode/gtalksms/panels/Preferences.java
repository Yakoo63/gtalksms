package com.googlecode.gtalksms.panels;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.tools.Tools;

public class Preferences extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.setLocale(new SettingsManager(this), this);
        
        getPreferenceManager().setSharedPreferencesName("GTalkSMS");
        Intent intent = getIntent();
        int prefs_id = intent.getIntExtra("panel", 0);
        addPreferencesFromResource(prefs_id);
    }
}
