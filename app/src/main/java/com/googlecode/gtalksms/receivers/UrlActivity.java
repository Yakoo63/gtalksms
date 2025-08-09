package com.googlecode.gtalksms.receivers;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.googlecode.gtalksms.tools.Tools;

public class UrlActivity extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent intent = getIntent();       
        Uri uri = intent.getData();
        if (uri != null) {
            Tools.send(uri.toString(), null, this);
        }
        
        finish();
    }
}
