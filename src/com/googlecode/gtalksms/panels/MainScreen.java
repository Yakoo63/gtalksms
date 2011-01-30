package com.googlecode.gtalksms.panels;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.gtalksms.LogCollectorActivity;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.tools.StringFmt;
import com.googlecode.gtalksms.tools.Tools;

public class MainScreen extends Activity {

    private MainService mainService;
    private SettingsManager _settingsMgr;
    
    private BroadcastReceiver xmppreceiver;
    
    private ServiceConnection mainServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mainService = ((MainService.LocalBinder)service).getService();
            updateStatus(mainService.getConnectionStatus());
        }

        public void onServiceDisconnected(ComponentName className) {
            mainService = null;
        }
    };
    
    @Override
    public void onPause() {
        super.onPause();
        unbindService(mainServiceConnection);
        unregisterReceiver(xmppreceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        xmppreceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(XmppManager.ACTION_PRESENCE_CHANGED)) {
                    // Presence notifications for next features
//                  TextView console = (TextView) findViewById(R.id.Console);
//                  String person = intent.getStringExtra("person");
//                  String status = intent.getStringExtra("status");
//                  console.append("\n" + person + " : " + status);
                } else if (action.equals(XmppManager.ACTION_CONNECTION_CHANGED)) {
                        updateStatus(intent.getIntExtra("new_state", 0));
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter(XmppManager.ACTION_PRESENCE_CHANGED);
        intentFilter.addAction(XmppManager.ACTION_CONNECTION_CHANGED);
        registerReceiver(xmppreceiver, intentFilter);
        Intent intent = new Intent(MainService.ACTION_CONNECT);
        bindService(intent, mainServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        _settingsMgr = new SettingsManager(this) {
            @Override  public void OnPreferencesUpdated() {
                createView();
            }
        };
        
        createView();
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onDestroy() {
        _settingsMgr.Destroy();
        super.onDestroy();
    }
    
    private void createView() {
        Tools.setLocale(_settingsMgr, this);
        
        setContentView(R.layout.main);

        TextView label = (TextView) findViewById(R.id.VersionLabel);
        label.setText(StringFmt.Style("GTalkSMS " + Tools.getVersionName(getBaseContext(), getClass()), Typeface.BOLD));

        Button prefBtn = (Button) findViewById(R.id.Preferences);
        prefBtn.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                openOptionsMenu();
            }
        });
        
        Button aboutBtn = (Button) findViewById(R.id.About);
        aboutBtn.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                startActivity(new Intent(getBaseContext(), About.class));
            }
        });
        
        Button donateBtn = (Button) findViewById(R.id.Donate);
        donateBtn.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                openLink("https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=WQDV6S67WAC7A&lc=US&item_name=GTalkSMS&item_number=WEB&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted");
            }
        });
        
        // Set FREE label for not donate version
        if (getPackageName().endsWith("donate")) {
            donateBtn.setVisibility(View.GONE);
        } else {
            donateBtn.setVisibility(View.VISIBLE);
        }
        
        Button clipboardBtn = (Button) findViewById(R.id.Clipboard);
        clipboardBtn.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                if (mainService != null ) {
                    mainService.sendClipboard();
                }
            }
        });

        Button startStopButton = (Button) findViewById(R.id.StartStop);
        startStopButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                startService(MainService.newSvcIntent(MainScreen.this, MainService.ACTION_TOGGLE));
            }
        });
        
        updateConsole();
    }
    
    /** lets the user choose an activity compatible with the url */
    private void openLink(String url) {
        Intent target = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        Intent intent = Intent.createChooser(target, getString(R.string.chat_choose_activity));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void updateConsole() {
//      TextView console = (TextView) findViewById(R.id.Console);
    }
  
    public void updateStatus(int status) {
        ImageView statusImg = (ImageView) findViewById(R.id.StatusImage);
        switch (status) {
            case XmppManager.CONNECTED:
                statusImg.setImageResource(R.drawable.led_green);
                break;
            case XmppManager.DISCONNECTED:
                statusImg.setImageResource(R.drawable.led_red);
                break;
            case XmppManager.CONNECTING:
            case XmppManager.DISCONNECTING:
            case XmppManager.WAITING_TO_CONNECT:
                statusImg.setImageResource(R.drawable.led_orange);
                break;
            default:
                break;
        }
    }
  
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Force menu update on each opening for localization issue
        menu.clear();
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.preferences_menu, menu);
        
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        int prefs_id;
        switch (item.getItemId()) {
        case R.id.connection_settings:
            prefs_id = R.xml.prefs_connection;
            break;
        case R.id.notification_settings:
            prefs_id = R.xml.prefs_notifications;
            break;
        case R.id.application_settings:
            prefs_id = R.xml.prefs_application;
            break;
        default:
            return super.onOptionsItemSelected(item);
        }
        Intent intent = new Intent(MainScreen.this, Preferences.class);
        intent.putExtra("panel", prefs_id);
        startActivity(intent);
        return true;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        // Note: KEYCODE_MENU was my first preference, but I failed to make that work.
        // Also: once we have more diagnostic features, it probably makes
        // sense to create a new panel and have the log collector called from
        // there - but now, just jumping directly to the collector should be fine.
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent i = new Intent(this, LogCollectorActivity.class);
            startActivity(i);            
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }
}
