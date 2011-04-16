package com.googlecode.gtalksms.receivers;

import java.net.URL;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.tools.Tools;

public class UrlActivity extends Activity {
    Handler _handler = new Handler();
    Runnable _exitRunnable = new Runnable() {
        public void run() {
            onDestroy();
        }
    };
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        
        // To get the data use
        Uri data = intent.getData();
        URL url;
        try {
            url = new URL(data.getScheme(), data.getHost(), data.getPath());

            Tools.send(url.toString(), null, this);
        } catch (Exception e) {
            Tools.send(getString(R.string.chat_error, e.toString()), null, this);
        }
        finish();
    }
}
