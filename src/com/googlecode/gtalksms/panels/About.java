package com.googlecode.gtalksms.panels;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.widget.TextView;

import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.tools.StringFmt;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.tools.UrlToStringDownloader;

public class About extends Activity {
    private static final String AUTHORS_URL = "http://gtalksms.googlecode.com/hg/AUTHORS";
    private static final String DONORS_URL = "http://gtalksms.googlecode.com/hg/Donors";
    private static final String CHANGELOG_URL = "http://gtalksms.googlecode.com/hg/Changelog";
    
    private static URL[] sUrls;
    
    static {
        sUrls = new URL[3];
        try {
            sUrls[0] = new URL(AUTHORS_URL);
            sUrls[1] = new URL(DONORS_URL);
            sUrls[2] = new URL(CHANGELOG_URL);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private HashMap<URL, String> mUrlMap;
    
    private class AboutUrlToStringDownloader extends UrlToStringDownloader {
        protected void onPostExecute(HashMap<URL, String> result) {
            mUrlMap = result;
            updateConsole();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }
   
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        TextView label = (TextView) findViewById(R.id.VersionLabel);
        label.setText(StringFmt.Style(Tools.APP_NAME + " " + Tools.getVersionName(this), Typeface.BOLD));

        updateConsole();
        
        if (mUrlMap == null) {
            AboutUrlToStringDownloader ausd = new AboutUrlToStringDownloader();
            ausd.execute(sUrls);
        }
    }
    
    public void updateConsole() {
      TextView console = (TextView) findViewById(R.id.Text);
      console.setText("");
      console.append(StringFmt.Fmt(getString(R.string.about_website) + "\n", 0xFFFF0000, 1.5, Typeface.BOLD));
      console.append(StringFmt.Url("http://code.google.com/p/gtalksms"));
      console.append(StringFmt.Fmt("\n\n" + getString(R.string.about_authors) + "\n", 0xFFFF0000, 1.5, Typeface.BOLD));
      console.append(appendURL(AUTHORS_URL));
      console.append(StringFmt.Fmt("\n" + getString(R.string.about_donors) + "\n", 0xFFFF0000, 1.5, Typeface.BOLD));
      console.append(appendURL(DONORS_URL));
      console.append(StringFmt.Fmt("\n" + getString(R.string.about_change_log) + "\n", 0xFFFF0000, 1.5, Typeface.BOLD));
      console.append(appendURL(CHANGELOG_URL));
      
      MovementMethod m = console.getMovementMethod();
      if ((m == null) || !(m instanceof LinkMovementMethod))
      {
          console.setMovementMethod(LinkMovementMethod.getInstance());
      }
    }
    
    private String appendURL(String urlString) {
        try {
            URL url = new URL(urlString);
            if (mUrlMap != null && mUrlMap.containsKey(url)) {
                return mUrlMap.get(url);
            } else {
                // TODO l18n
                return "...loading...";
            }
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }
}
