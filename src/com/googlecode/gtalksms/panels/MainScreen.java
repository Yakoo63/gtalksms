package com.googlecode.gtalksms.panels;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.XmppListener;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.R;

public class MainScreen extends Activity {

    @Override
    public void onPause() {
        super.onPause();
        
        if (MainService.getInstance() != null) {
            MainService.getInstance().setXmppListener(null);
        }  
    }
   
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        TextView label = (TextView) findViewById(R.id.VersionLabel);
        label.setText("GTalkSMS " + Tools.getVersionName(getBaseContext(), getClass()));

        registerListener();
        
        Button prefBtn = (Button) findViewById(R.id.Preferences);
        prefBtn.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    Intent settingsActivity = new Intent(getBaseContext(), Preferences.class);
                    startActivity(settingsActivity);
                }
        });

        Button startStopButton = (Button) findViewById(R.id.StartStop);
        startStopButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    Intent intent = new Intent(".GTalkSMS.ACTION");
                    if (MainService.getInstance() == null) {
                        startService(intent);
                        registerListener();
                    }
                    else {
                        stopService(intent);
                    }
                }
        });
    }
    
    public void updateConsole() {
//        TextView console = (TextView) findViewById(R.id.Console);
//        console.append("\n" + MainService.getInstance().getContactsList());
    }
    
    public void registerListener() {
        if (MainService.getInstance() != null) {
            MainService.getInstance().setXmppListener(new XmppListener() {
                @Override
                public void onMessageReceived(String message) {
                }
                
                @Override
                public void onConnectionStatusChanged(int oldStatus, int status) {
                    if (status == XmppManager.CONNECTED) {
                        updateConsole();
                    }
                }

                @Override
                public void onPresenceStatusChanged(String person, String status) {
//                    TextView console = (TextView) findViewById(R.id.Console);
//                    console.append("\n" + person + " : " + status);
                }
            });
            updateConsole();
        }
    }
}
