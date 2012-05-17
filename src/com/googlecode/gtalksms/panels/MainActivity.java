package com.googlecode.gtalksms.panels;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.panels.tabs.BuddiesTabFragment;
import com.googlecode.gtalksms.panels.tabs.ConnectionTabFragment;
import com.googlecode.gtalksms.tools.StringFmt;
import com.googlecode.gtalksms.tools.Tools;

public class MainActivity extends SherlockFragmentActivity {
    
    class TabListener implements ActionBar.TabListener {

        private SherlockFragment fragment;

        public TabListener(SherlockFragment fragment) {
            this.fragment = fragment;
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            ft.add(R.id.fragment_container, fragment);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        }

        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            ft.add(R.id.fragment_container, fragment);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            ft.remove(fragment);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        }
    }
    
    private AdView _adView;
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tab_container);
        final ActionBar ab = getSupportActionBar();
        
        ab.setDisplayShowTitleEnabled(true);
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        ab.addTab(ab.newTab().setText("Connection").setTabListener(new TabListener(new ConnectionTabFragment())));
        ab.addTab(ab.newTab().setText("Buddies").setTabListener(new TabListener(new BuddiesTabFragment())));
        ab.addTab(ab.newTab().setText("Commands").setTabListener(new TabListener(new BuddiesTabFragment()))); // TO UPDATE!!!
        ab.addTab(ab.newTab().setText("About").setTabListener(new TabListener(new BuddiesTabFragment()))); // TO UPDATE!!!
        
        setTitle(StringFmt.Style("GTalkSMS " + Tools.getVersionName(getBaseContext()), Typeface.BOLD));

        TextView marketLink = (TextView) findViewById(R.id.MarketLink);
        LinearLayout mainLayout = (LinearLayout) findViewById(R.id.MainLayout);
        
        if (Tools.isDonateAppInstalled(getBaseContext())) {
            marketLink.setVisibility(View.GONE);
        } else {
            _adView = new AdView(this, AdSize.BANNER, "a14e5a583244738");
            _adView.loadAd(new AdRequest());
            _adView.setBackgroundColor(Color.TRANSPARENT);
            mainLayout.addView(_adView, 1);

            marketLink.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    Tools.openLink(MainActivity.this, "market://details?id=com.googlecode.gtalksmsdonate");
                }
            });
        }
    }
}
