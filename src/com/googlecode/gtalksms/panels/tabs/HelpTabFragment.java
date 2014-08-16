package com.googlecode.gtalksms.panels.tabs;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.tools.StringFmt;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.tools.UrlToStringDownloader;

public class HelpTabFragment extends SherlockFragment {
    private static final URL[] sUrls;
    private TextView mTextViewConsole;
    
    static {
        sUrls = new URL[3];
        try {
            sUrls[0] = new URL(Tools.AUTHORS_URL);
            sUrls[1] = new URL(Tools.DONORS_URL);
            sUrls[2] = new URL(Tools.CHANGELOG_URL);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private volatile HashMap<URL, String> mUrlMap;
    
    private class AboutUrlToStringDownloader extends UrlToStringDownloader {
        protected void onPostExecute(HashMap<URL, String> result) {
            mUrlMap = result;
            try {
                updateConsole();
            } catch (Exception e) {}
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_help, container, false);
        
        mTextViewConsole = (TextView) view.findViewById(R.id.Text);
        
        updateConsole();
        
        if (mUrlMap == null) {
            AboutUrlToStringDownloader ausd = new AboutUrlToStringDownloader();
            ausd.execute(sUrls);
        }
        
        return view;
    }
    
    void updateConsole() {
        mTextViewConsole.setText("");
        mTextViewConsole.append(StringFmt.Fmt(getString(R.string.about_website) + "\n", 0xFFFF0000, 1.5, Typeface.BOLD));
        mTextViewConsole.append(getString(R.string.about_website_help));
        mTextViewConsole.append(StringFmt.Url("\thttp://code.google.com/p/GTalkSMS\n", "http://code.google.com/p/gtalksms"));
        
        mTextViewConsole.append(getString(R.string.about_rate_help));
        mTextViewConsole.append(StringFmt.Url(getString(R.string.about_rate), "market://details?id=com.googlecode.gtalksms"));
        mTextViewConsole.append(StringFmt.Url(getString(R.string.about_rate_donate), "market://details?id=com.googlecode.gtalksmsdonate"));
        
        mTextViewConsole.append(getString(R.string.about_help));
        mTextViewConsole.append(StringFmt.Url("\tgtalksms-users for usage\n", "mailto:gtalksms-users@googlegroups.com"));
        mTextViewConsole.append(StringFmt.Url("\tgtalksms-dev for development\n", "mailto:gtalksms-dev@googlegroups.com"));
        
        mTextViewConsole.append(StringFmt.Fmt("\n" + getString(R.string.about_authors) + "\n", 0xFFFF0000, 1.5, Typeface.BOLD));
        mTextViewConsole.append(appendURL(Tools.AUTHORS_URL));
        
        mTextViewConsole.append(StringFmt.Fmt("\n" + getString(R.string.about_donors) + "\n", 0xFFFF0000, 1.5, Typeface.BOLD));
        mTextViewConsole.append(getString(R.string.about_donate_string));
        mTextViewConsole.append(StringFmt.Url(getString(R.string.about_donate_paypal), "https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=WQDV6S67WAC7A&lc=US&item_name=GTalkSMS&item_number=WEB&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted"));
        mTextViewConsole.append(StringFmt.Url(getString(R.string.about_donate_market), "market://details?id=com.googlecode.gtalksmsdonate"));

        try {
            mTextViewConsole.append(new String(appendURL(Tools.DONORS_URL).getBytes("ISO-8859-1"), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            mTextViewConsole.append(appendURL(Tools.DONORS_URL));
        }

        mTextViewConsole.append(StringFmt.Fmt("\n" + getString(R.string.about_change_log) + "\n", 0xFFFF0000, 1.5, Typeface.BOLD));
        mTextViewConsole.append(appendURL(Tools.CHANGELOG_URL));
        
        MovementMethod m = mTextViewConsole.getMovementMethod();
        if ((m == null) || !(m instanceof LinkMovementMethod))
        {
            mTextViewConsole.setMovementMethod(LinkMovementMethod.getInstance());
        }
      }
      
      private String appendURL(String urlString) {
          try {
              URL url = new URL(urlString);
              if (mUrlMap != null && mUrlMap.containsKey(url)) {
                  return mUrlMap.get(url);
              } else {
                  // TODO l18n
                  return "...loading...\n";
              }
          } catch (MalformedURLException e) {
              throw new IllegalStateException(e);
          }
      }
}