package com.googlecode.gtalksms.panels;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.MainService.LocalBinder;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.panels.tabs.BuddiesTabFragment;
import com.googlecode.gtalksms.panels.tabs.CommandsTabFragment;
import com.googlecode.gtalksms.panels.tabs.ConnectionStatusTabFragment;
import com.googlecode.gtalksms.panels.tabs.ConnectionTabFragment;
import com.googlecode.gtalksms.panels.tabs.HelpTabFragment;
import com.googlecode.gtalksms.tools.StringFmt;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppFriend;

import com.google.android.gms.ads.*;

public class MainActivity extends SherlockFragmentActivity {
    
    public class TabListener implements ActionBar.TabListener {
        private final ViewPager mPager;
        private final int mIndex;

        public TabListener(ViewPager pager, int index) {
            mPager = pager;
            mIndex = index;
        }

        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            mPager.setCurrentItem(mIndex);
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft) {}
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {}
    }
    
    public static class TabAdapter extends FragmentPagerAdapter {
        final ActionBar mActionBar;
        final ArrayList<SherlockFragment> mFragments;
        
        public TabAdapter(FragmentManager fm, ActionBar actionBar, ArrayList<SherlockFragment> fragments) {
            super(fm);
            mActionBar = actionBar;
            mFragments = fragments;
        }

        @Override
        public int getCount() {
            return mActionBar.getTabCount();
        }

        public void update() {
            notifyDataSetChanged();
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            if (position >= mFragments.size()) {
                return mFragments.get(mFragments.size() - 1);
            } 
            if (position < 0) {
                return mFragments.get(0);
            }
            
            return mFragments.get(position);
        }
    }

    private SettingsManager mSettingsManager;
    private MainService mMainService;
    private ActionBar mActionBar;
    private ViewPager mPager;
    private AdView mAdView;
    private final ConnectionTabFragment mConnectionTabFragment = new ConnectionTabFragment();
    private final BuddiesTabFragment mBuddiesTabFragment = new BuddiesTabFragment();
    private final CommandsTabFragment mCommandsTabFragment = new CommandsTabFragment();
    private final HelpTabFragment mHelpTabFragment = new HelpTabFragment();
    private final ConnectionStatusTabFragment mConnectionStatusTabFragment = new ConnectionStatusTabFragment();
    private final ArrayList<SherlockFragment> mFragments = new ArrayList<SherlockFragment>();
    
    private final BroadcastReceiver mXmppreceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MainService.ACTION_XMPP_PRESENCE_CHANGED)) {
                int stateInt = intent.getIntExtra("state", XmppFriend.OFFLINE);
                String userId = intent.getStringExtra("userid");
                String userFullId = intent.getStringExtra("fullid");
                String name = intent.getStringExtra("name");
                String status = intent.getStringExtra("status");

                mBuddiesTabFragment.updateBuddy(userId, userFullId, name, status, stateInt);
            } else if (action.equals(MainService.ACTION_XMPP_CONNECTION_CHANGED)) {
                updateStatus(intent.getIntExtra("new_state", 0), intent.getStringExtra("current_action"));
            }
        }
    };
    
    private final ServiceConnection mMainServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d("MainActivity: MainService connected");
            LocalBinder binder = (LocalBinder) service;
            MainService mainService = binder.getService();
            mMainService = mainService;
            mMainService.updateBuddies();
            updateStatus(mMainService.getConnectionStatus(), mMainService.getConnectionStatusAction());
            mConnectionStatusTabFragment.setMainService(mainService);
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d("mainActivity: MainService disconnected");
            mMainService = null;
            mConnectionStatusTabFragment.unsetMainService();
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        this.setTheme(com.actionbarsherlock.R.style.Theme_Sherlock);
        super.onCreate(savedInstanceState);

        mSettingsManager = SettingsManager.getSettingsManager(getApplicationContext());
        Log.initialize(mSettingsManager);

        setTitle(StringFmt.Style("GTalkSMS " + Tools.getVersionName(getBaseContext()), Typeface.BOLD));
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
 
        setSupportProgressBarIndeterminateVisibility(false);
        setContentView(R.layout.tab_container);
        
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowTitleEnabled(true);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mPager = (ViewPager)findViewById(R.id.fragment_container);
        
        mActionBar.addTab(mActionBar.newTab().setText(getString(R.string.panel_connection)).setTabListener(new TabListener(mPager, 0)));
        mActionBar.addTab(mActionBar.newTab().setText(getString(R.string.panel_help)).setTabListener(new TabListener(mPager, 1)));

        
        if (Tools.isDonateAppInstalled(getBaseContext())) {
            findViewById(R.id.StatusBar).setVisibility(View.GONE);
        } else {
            TextView marketLink = (TextView) findViewById(R.id.MarketLink);
            marketLink.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    Tools.openLink(MainActivity.this, "market://details?id=com.googlecode.gtalksmsdonate");
                }
            });
            
            TextView donateLink = (TextView) findViewById(R.id.DonateLink);
            donateLink.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    Tools.openLink(MainActivity.this, "https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=WQDV6S67WAC7A&lc=US&item_name=GTalkSMS&item_number=WEB&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted");
                }
            });

            // Create the adView
            mAdView = new AdView(this);
            mAdView.setAdUnitId("ca-app-pub-1255069456864059/8040023329");
            mAdView.setAdSize(AdSize.BANNER);
            LinearLayout  layout = (LinearLayout) findViewById(R.id.StatusBar);
            layout.addView(mAdView, 0);
            mAdView.loadAd(new AdRequest.Builder().build());
        }
        
        mFragments.add(mConnectionTabFragment);
        mFragments.add(mHelpTabFragment);
        mFragments.add(mBuddiesTabFragment);
        mFragments.add(mCommandsTabFragment);
        mFragments.add(mConnectionStatusTabFragment);

        mPager.setAdapter(new TabAdapter(getSupportFragmentManager(), mActionBar, mFragments));

        mPager.setOnPageChangeListener(new OnPageChangeListener() {
            public void onPageScrollStateChanged(int arg0) {}
            public void onPageScrolled(int arg0, float arg1, int arg2) {}
            public void onPageSelected(int index) {
                mActionBar.getTabAt(index).select();
            }
        }); 
    }

    @Override
    public void onDestroy() {
        if (mAdView != null) {
            mAdView.removeAllViews();
            mAdView.destroy();
            mAdView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        Log.d("MainActivity: onStop()");
        unbindService(mMainServiceConnection);
        unregisterReceiver(mXmppreceiver);
    }
    
    @Override
    public void onStart() {
        super.onStart();

        Log.d("MainActivity: onSart()");
        IntentFilter intentFilter = new IntentFilter(MainService.ACTION_XMPP_PRESENCE_CHANGED);
        intentFilter.addAction(MainService.ACTION_XMPP_CONNECTION_CHANGED);
        registerReceiver(mXmppreceiver, intentFilter);
        bindService(new Intent(this, MainService.class), mMainServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Settings").setIcon(R.drawable.ic_menu_preferences).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getTitle().equals("Settings")) {
            Intent intent = new Intent(MainActivity.this, Preferences.class);
            intent.putExtra("panel", R.xml.prefs_all);
            startActivity(intent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent i = new Intent(this, LogCollector.class);
            startActivity(i);
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }
    
    private void updateStatus(int status, String action) {
        mConnectionTabFragment.updateStatus(status, action);
        setSupportProgressBarIndeterminateVisibility(false);
        switch (status) {
            case XmppManager.CONNECTED:
                mActionBar.setIcon(R.drawable.icon_green);
                break;
            case XmppManager.DISCONNECTED:
                mActionBar.setIcon(R.drawable.icon_red);
                break;
            case XmppManager.CONNECTING:
            case XmppManager.DISCONNECTING:
                setSupportProgressBarIndeterminateVisibility(true);
                mActionBar.setIcon(R.drawable.icon_orange);
                break;
            case XmppManager.WAITING_TO_CONNECT:
            case XmppManager.WAITING_FOR_NETWORK:
                mActionBar.setIcon(R.drawable.icon_blue);
                break;
            default:
                throw new IllegalStateException();
        }

        if (status == XmppManager.CONNECTED) {
            mCommandsTabFragment.updateCommands();
            addTab(getString(R.string.panel_buddies), 2);
            addTab(getString(R.string.panel_commands), 3);
            if (mSettingsManager.debugLog) {
                addTab(getString(R.string.panel_connection_status), 4);
            }
        } else {
            if (isTabExists(getString(R.string.panel_buddies))) {
                mActionBar.setSelectedNavigationItem(0);
                mPager.setCurrentItem(0);
            }
            removeTab(getString(R.string.panel_buddies));
            removeTab(getString(R.string.panel_commands));
            removeTab(getString(R.string.panel_connection_status));
        }
    }

    private void addTab(String name, int index) {
        if (!isTabExists(name)) {
            mActionBar.addTab(mActionBar.newTab().setText(name).setTabListener(new TabListener(mPager, index)));
            mPager.getAdapter().notifyDataSetChanged();
        }
    }

    private boolean isTabExists(String name) {
        for (int i = 0 ; i < mActionBar.getTabCount() ; ++i) {
            if (mActionBar.getTabAt(i).getText().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private void removeTab(String name) {
        for (int i = 0 ; i < mActionBar.getTabCount() ; ++i) {
            if (mActionBar.getTabAt(i).getText().equals(name)) {
                mActionBar.removeTabAt(i);
                mPager.getAdapter().notifyDataSetChanged();
                i--;
            }
        }
    }
}
