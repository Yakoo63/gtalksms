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
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.panels.wizard.Wizard;
import com.googlecode.gtalksms.tools.StringFmt;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppFriend;

public class MainScreen extends Activity  {

    
    private MainService mMainService;
    private SettingsManager mSettingsMgr;
    private BroadcastReceiver mXmppreceiver;
    private ArrayList<HashMap<String, String>> mFriends = new ArrayList<HashMap<String, String>>();
    ListView mBuddiesListView;

    private ServiceConnection _mainServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mMainService = ((MainService.LocalBinder) service).getService();
            updateStatus(mMainService.getConnectionStatus(), mMainService.getTLSStatus(), mMainService.getCompressionStatus());
            mMainService.updateBuddies();
        }

        public void onServiceDisconnected(ComponentName className) {
            mMainService = null;
        }
    };

    private void updateStatus(int status, boolean tls, boolean compression) {
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
                statusImg.setImageResource(R.drawable.led_orange_con);
                break;
            case XmppManager.DISCONNECTING:
                statusImg.setImageResource(R.drawable.led_orange_discon);
                break;
            case XmppManager.WAITING_TO_CONNECT:
                statusImg.setImageResource(R.drawable.led_orange_timewait);
                break;
            case XmppManager.WAITING_FOR_NETWORK:
                statusImg.setImageResource(R.drawable.no_network);
                break;
            default:
                throw new IllegalStateException();
        }

        tlsStatus.setVisibility(tls ? View.VISIBLE : View.INVISIBLE);
        compressionStatus.setVisibility(compression ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onPause() {
        super.onPause();
        unbindService(_mainServiceConnection);
        unregisterReceiver(mXmppreceiver);
    }

    private static String getStateImg(int stateType) {
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
        mXmppreceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(MainService.ACTION_XMPP_PRESENCE_CHANGED)) {
                    int stateInt = intent.getIntExtra("state", XmppFriend.OFFLINE);
                    String userId = intent.getStringExtra("userid");
                    String userFullId = intent.getStringExtra("fullid");  // TODO check if fullid contains only the bare resource
                    String name = intent.getStringExtra("name");
                    String status = intent.getStringExtra("status");
                    String stateImg = getStateImg(stateInt);

                    boolean exist = false;
                    for (HashMap<String, String> map : mFriends) {
                        if (map.get("userid").equals(userId)) {
                            exist = true;                          
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
                        
                        mFriends.add(map);
                    }
                    if (mSettingsMgr.debugLog) Log.i(Tools.LOG_TAG, "Update presence: " + userId + " - " + XmppFriend.stateToString(stateInt));
                    updateBuddiesList();

                } else if (action.equals(MainService.ACTION_XMPP_CONNECTION_CHANGED)) {
                    updateStatus(intent.getIntExtra("new_state", 0), intent.getBooleanExtra("TLS", false), intent.getBooleanExtra("Compression", false));
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter(MainService.ACTION_XMPP_PRESENCE_CHANGED);
        intentFilter.addAction(MainService.ACTION_XMPP_CONNECTION_CHANGED);
        registerReceiver(mXmppreceiver, intentFilter);
        Intent intent = new Intent(MainService.ACTION_CONNECT);
        bindService(intent, _mainServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSettingsMgr = SettingsManager.getSettingsManager(this);
        createView();  
    }

    /** Called when the activity is first created. */
    @Override
    public void onDestroy() {
        mSettingsMgr.Destroy();
        super.onDestroy();
    }

    private void createView() {
    	if (mSettingsMgr.getConnectOnMainScreenStartup()) {
    	    Tools.startSvcIntent(this, MainService.ACTION_CONNECT);
    	}
    	
    	boolean isDonate = Tools.isDonateAppInstalled(getBaseContext());
    	
        Tools.setLocale(mSettingsMgr, this);
        
        setContentView(R.layout.main);

        TextView label = (TextView) findViewById(R.id.VersionLabel);
        label.setText(StringFmt.Style(Tools.APP_NAME + " " + Tools.getVersionName(getBaseContext()), Typeface.BOLD));

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
        
        Button marketBtn = (Button) findViewById(R.id.Market);
        marketBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                openLink("market://details?id=com.googlecode.gtalksmsdonate");
            }
        });

        // Set FREE label for not donate version
        if (isDonate) {
            donateBtn.setVisibility(View.GONE);
            marketBtn.setVisibility(View.GONE);
        } else {
            donateBtn.setVisibility(View.VISIBLE);
            marketBtn.setVisibility(View.VISIBLE);
        }

        Button clipboardBtn = (Button) findViewById(R.id.Clipboard);
        clipboardBtn.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                if (MainService.IsRunning) {
                    Intent intent = new Intent(MainService.ACTION_COMMAND);
                    intent.putExtra("cmd", "copy");
                    intent.setClassName("com.googlecode.gtalksms", "com.googlecode.gtalksms.MainService");
                    startService(intent);
                }
            }
        });

        Button startStopButton = (Button) findViewById(R.id.StartStop);
        startStopButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                MainService.sendToServiceHandler(new Intent(MainService.ACTION_TOGGLE));
            }
        });

        mBuddiesListView = (ListView) findViewById(R.id.ListViewBuddies);

        mBuddiesListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            @SuppressWarnings("unchecked")
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                HashMap<String, String> map = (HashMap<String, String>) mBuddiesListView.getItemAtPosition(position);
                AlertDialog.Builder adb = new AlertDialog.Builder(MainScreen.this);
                adb.setTitle(map.get("name"));
                
                String user = map.get("userid");
                StringBuilder sb = new StringBuilder(user);
                sb.append(Tools.LineSep);
                sb.append(Tools.LineSep);
                for (String key : map.keySet()) {
                    try {
                        if (key.startsWith("location_")) {
                            sb.append(key.substring(10 + user.length()));
                            sb.append(": ");
                            sb.append(map.get(key));
                            sb.append(Tools.LineSep);
                        }
                    } catch(Exception e) {
                        Log.e(Tools.LOG_TAG, "Failed to decode buddy name", e);
                    }
                }
                adb.setMessage(sb.toString());
                adb.setPositiveButton("Ok", null);
                adb.show();
            }
        });
    }

    private void updateBuddiesList() {
        Collections.sort(mFriends, new Comparator<HashMap<String, String>> () {
            public int compare(HashMap<String, String> object1, HashMap<String, String> object2) {
                if (object1.get("name") != null && object2.get("name") != null) {
                    return object1.get("name").compareTo(object2.get("name"));
                }
                return object1.get("userid").compareTo(object2.get("userid"));
            }});
        
        SimpleAdapter mSchedule = new SimpleAdapter(getBaseContext(), mFriends, R.layout.buddyitem, new String[] { "state", "name", "status" }, new int[] {
                R.id.buddyState, R.id.buddyName, R.id.buddyStatus });

        mBuddiesListView.setAdapter(mSchedule);
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
        Intent intent;
        
        if (item.getItemId() == R.id.connection_settings) {
            prefs_id = R.xml.prefs_connection;
        } else if (item.getItemId() == R.id.notification_settings) {
            prefs_id = R.xml.prefs_notifications;
        } else if (item.getItemId() == R.id.application_settings) {
            prefs_id = R.xml.prefs_application;
        } else if (item.getItemId() == R.id.wizard) {
            intent = new Intent(MainScreen.this, Wizard.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.cmd_manager) {
            intent = new Intent(MainScreen.this, CmdManager.class);
            startActivity(intent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
        intent = new Intent(MainScreen.this, Preferences.class);
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
