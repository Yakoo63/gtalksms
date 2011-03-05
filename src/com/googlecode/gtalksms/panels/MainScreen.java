package com.googlecode.gtalksms.panels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.tools.StringFmt;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppFriend;

public class MainScreen extends Activity {

    private MainService mainService;
    private SettingsManager _settingsMgr;
    private BroadcastReceiver _xmppreceiver;
    private ArrayList<HashMap<String, String>> _friends = new ArrayList<HashMap<String, String>>();
    ListView _buddiesListView;

    private ServiceConnection _mainServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mainService = ((MainService.LocalBinder) service).getService();
            MainScreen.this.updateStatus(mainService.getConnectionStatus(), mainService.getTLSStatus(), mainService.getCompressionStatus());
            mainService.updateBuddies();
        }

        public void onServiceDisconnected(ComponentName className) {
            //TODO should we call updateBuddies() here, as sometimes the service if offline (red)
            //but the buddy is still shown as online
            mainService = null;
        }
    };

    public void updateStatus(int status, boolean tls, boolean compression) {
        ImageView statusImg = (ImageView) findViewById(R.id.StatusImage);
        ImageView tlsStatus = (ImageView) findViewById(R.id.TLSsecured);
        ImageView compressionStatus = (ImageView) findViewById(R.id.compression);

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

        tlsStatus.setVisibility(tls ? View.VISIBLE : View.INVISIBLE);
        compressionStatus.setVisibility(compression ? View.VISIBLE : View.INVISIBLE);
        
    }

    @Override
    public void onPause() {
        super.onPause();
        unbindService(_mainServiceConnection);
        unregisterReceiver(_xmppreceiver);
    }

    public String getStateImg(int stateType) {
        String state = String.valueOf(R.drawable.buddy_offline);
        switch (stateType) {
            case XmppFriend.AWAY:
            case XmppFriend.EXAWAY:
                state = String.valueOf(R.drawable.buddy_away);
                break;
            case XmppFriend.BUSY:
                state = String.valueOf(R.drawable.buddy_busy);
                break;
            case XmppFriend.ONLINE:
                state = String.valueOf(R.drawable.buddy_available);
                break;
        }
        
        return state;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        _xmppreceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(XmppManager.ACTION_PRESENCE_CHANGED)) {
                    int stateInt = intent.getIntExtra("state", XmppFriend.OFFLINE);
                    String userId = intent.getStringExtra("userid");
                    String userFullId = intent.getStringExtra("fullid");
                    String name = intent.getStringExtra("name");
                    String status = intent.getStringExtra("status");
                    String stateImg = getStateImg(stateInt);

                    boolean exist = false;
                    for (HashMap<String, String> map : _friends) {
                        if (map.get("userid").equals(userId)) {
                            
                            if (stateInt == XmppFriend.OFFLINE) {
                                map.remove("location_" + userFullId);
                                
                                for (String key : map.keySet()) {
                                    if (key.startsWith("location_")) {
                                        try {
                                            stateImg = getStateImg(stateInt);
                                            break; 
                                        } catch (Exception e) {}
                                    }
                                }
                            } else if (userFullId != null) {
                                map.put("location_" + userFullId, XmppFriend.stateToString(stateInt));
                            }
                            map.put("state", stateImg);
                            map.put("status", status);
                            exist = true;                          
                            break;
                        }
                    }

                    if (!exist) {
                        HashMap<String, String> map = new HashMap<String, String>();
                        map.put("name", name);
                        map.put("status", status);
                        map.put("userid", userId);
                        map.put("state", stateImg);
                        if (userFullId != null && stateInt != XmppFriend.OFFLINE) {
                            map.put("location_" + userFullId, XmppFriend.stateToString(stateInt)+ "\n");
                        }
                        
                        _friends.add(map);
                    }
                    if (_settingsMgr.debugLog) Log.d(Tools.LOG_TAG, "Update presence: " + userId + " - " + XmppFriend.stateToString(stateInt));
                    updateBuddiesList();

                } else if (action.equals(XmppManager.ACTION_CONNECTION_CHANGED)) {
                    updateStatus(intent.getIntExtra("new_state", 0), intent.getBooleanExtra("TLS", false), intent.getBooleanExtra("Compression", false));
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter(XmppManager.ACTION_PRESENCE_CHANGED);
        intentFilter.addAction(XmppManager.ACTION_CONNECTION_CHANGED);
        registerReceiver(_xmppreceiver, intentFilter);
        Intent intent = new Intent(MainService.ACTION_CONNECT);
        bindService(intent, _mainServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _settingsMgr = new SettingsManager(this);
//        _settingsMgr = new SettingsManager(this) {
//            @Override
//            public void OnPreferencesUpdated() {
//            	super.OnPreferencesUpdated();
//                createView();
//            }
//        };

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
                if (mainService != null) {
                    mainService.executeCommand("copy", "");
                }
            }
        });

        Button startStopButton = (Button) findViewById(R.id.StartStop);
        startStopButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                startService(MainService.newSvcIntent(MainScreen.this, MainService.ACTION_TOGGLE));
            }
        });

        _buddiesListView = (ListView) findViewById(R.id.ListViewBuddies);

        _buddiesListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            @SuppressWarnings("unchecked")
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                HashMap<String, String> map = (HashMap<String, String>) _buddiesListView.getItemAtPosition(position);
                AlertDialog.Builder adb = new AlertDialog.Builder(MainScreen.this);
                adb.setTitle(map.get("name"));
                
                String user = map.get("userid");
                StringBuilder sb = new StringBuilder(user);
                sb.append(Tools.LineSep);
                sb.append(Tools.LineSep);
                for (String key : map.keySet()) {
                    if (key.startsWith("location_")) {
                        sb.append(key.substring(10 + user.length()));
                        sb.append(": ");
                        sb.append(map.get(key));
                        sb.append(Tools.LineSep);
                    }
                }
                adb.setMessage(sb.toString());
                adb.setPositiveButton("Ok", null);
                adb.show();
            }
        });
    }

    private void updateBuddiesList() {
        Collections.sort(_friends, new Comparator<HashMap<String, String>> () {
            @Override
            public int compare(HashMap<String, String> object1, HashMap<String, String> object2) {
                if (object1.get("name") != null && object2.get("name") != null) {
                    return object1.get("name").compareTo(object2.get("name"));
                }
                return object1.get("userid").compareTo(object2.get("userid"));
            }});
        
        
        SimpleAdapter mSchedule = new SimpleAdapter(getBaseContext(), _friends, 
                R.layout.buddyitem, new String[] { "state", "name", "status" }, 
                new int[] { R.id.buddyState, R.id.buddyName, R.id.buddyStatus });

        _buddiesListView.setAdapter(mSchedule);
    }

    /** lets the user choose an activity compatible with the url */
    private void openLink(String url) {
        Intent target = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        Intent intent = Intent.createChooser(target, getString(R.string.chat_choose_activity));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
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
            Intent i = new Intent(this, LogCollector.class);
            startActivity(i);
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }
}
