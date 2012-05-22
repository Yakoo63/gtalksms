package com.googlecode.gtalksms.panels;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Build;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.panels.tabs.BuddiesTabFragment;
import com.googlecode.gtalksms.panels.tabs.CommandsTabFragment;
import com.googlecode.gtalksms.panels.tabs.ConnectionTabFragment;
import com.googlecode.gtalksms.panels.tabs.HelpTabFragment;
import com.googlecode.gtalksms.tools.StringFmt;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppFriend;

public class MainActivity extends SherlockFragmentActivity {
    
    public class TabListener implements ActionBar.TabListener {
        private ViewPager mPager;
        private int mIndex;

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
        ActionBar mActionBar;
        ArrayList<SherlockFragment> mFragments;
        
        public TabAdapter(FragmentManager fm, ActionBar actionBar, ArrayList<SherlockFragment> fragments) {
            super(fm);
            mActionBar = actionBar;
            mFragments = fragments;
        }

        @Override
        public int getCount() {
            return mActionBar.getTabCount();
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
    
    private MainService mMainService;
    private ActionBar mActionBar;
    private ViewPager mPager;
    private ConnectionTabFragment mConnectionTabFragment = new ConnectionTabFragment();
    private BuddiesTabFragment mBuddiesTabFragment = new BuddiesTabFragment();
    private CommandsTabFragment mCommandsTabFragment = new CommandsTabFragment();
    private HelpTabFragment mHelpTabFragment = new HelpTabFragment();
    private ArrayList<SherlockFragment> mFragments = new ArrayList<SherlockFragment>();
    
    private BroadcastReceiver mXmppreceiver = new BroadcastReceiver() {
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
                updateStatus(intent.getIntExtra("new_state", 0));
            }
        }
    };;
    
    private ServiceConnection mMainServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mMainService = ((MainService.LocalBinder) service).getService();
            mMainService.updateBuddies();
            updateStatus(mMainService.getConnectionStatus());
        }

        public void onServiceDisconnected(ComponentName className) {
            mMainService = null;
        }
    };

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LinearLayout statusBar = (LinearLayout) findViewById(R.id.StatusBar);
        LinearLayout linksBar = (LinearLayout) findViewById(R.id.LinksBar);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            statusBar.setOrientation(LinearLayout.HORIZONTAL);
            linksBar.setOrientation(LinearLayout.VERTICAL);
        } else {
            statusBar.setOrientation(LinearLayout.VERTICAL);
            linksBar.setOrientation(LinearLayout.HORIZONTAL);
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        this.setTheme(com.actionbarsherlock.R.style.Theme_Sherlock);
        super.onCreate(savedInstanceState);
        
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
            AdView adView = new AdView(this, AdSize.BANNER, "a14e5a583244738");
            adView.loadAd(new AdRequest());
            
            LinearLayout adsLayout = (LinearLayout) findViewById(R.id.AdsLayout);
            adsLayout.addView(adView);

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
        }
        
        mFragments.add(mConnectionTabFragment);
        mFragments.add(mHelpTabFragment);
        mFragments.add(mBuddiesTabFragment);
        mFragments.add(mCommandsTabFragment);
        
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
    public void onPause() {
        super.onPause();
        
        unbindService(mMainServiceConnection);
        unregisterReceiver(mXmppreceiver);
    }
    
    @Override
    public void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter(MainService.ACTION_XMPP_PRESENCE_CHANGED);
        intentFilter.addAction(MainService.ACTION_XMPP_CONNECTION_CHANGED);
        registerReceiver(mXmppreceiver, intentFilter);
        Intent intent = new Intent(MainService.ACTION_CONNECT);
        bindService(intent, mMainServiceConnection, Context.BIND_AUTO_CREATE);
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
    
    private void updateStatus(int status) {
        mConnectionTabFragment.updateStatus(status);
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
        
        boolean b1 = removeTab(getString(R.string.panel_buddies));
        boolean b2 = removeTab(getString(R.string.panel_commands));
        
        if (status == XmppManager.CONNECTED) {
            mCommandsTabFragment.updateCommands(mMainService.getCommandSet());
            mActionBar.addTab(mActionBar.newTab().setText(getString(R.string.panel_buddies)).setTabListener(new TabListener(mPager, 2)));
            mActionBar.addTab(mActionBar.newTab().setText(getString(R.string.panel_commands)).setTabListener(new TabListener(mPager, 3)));
        } else if (b1 || b2) {
            mActionBar.setSelectedNavigationItem(0);
            mPager.setCurrentItem(0);
        }
    }
    
    private boolean removeTab(String name) {
        boolean result = false;
        for (int i = 0 ; i < mActionBar.getTabCount() ; ++i) {
            if (mActionBar.getTabAt(i).getText().equals(name)) {
                if (mActionBar.getSelectedNavigationIndex() == i) {
                    result = true;
                }
                
                mActionBar.removeTabAt(i);
                i--;
            }
        }
        return result;
    }
}
