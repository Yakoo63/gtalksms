package com.googlecode.gtalksms;

import com.googlecode.gtalksms.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName("GTalkSMS");
            addPreferencesFromResource(R.xml.preferences);
    }

}
