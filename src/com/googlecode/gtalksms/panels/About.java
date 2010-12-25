package com.googlecode.gtalksms.panels;

import android.app.Activity;
import android.os.Bundle;
import android.text.util.Linkify;
import android.widget.TextView;

import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.tools.Web;

public class About extends Activity {

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
        label.setText("GTalkSMS " + Tools.getVersionName(getBaseContext(), getClass()));

        updateConsole();
    }
    
    public void updateConsole() {
      TextView console = (TextView) findViewById(R.id.Text);
      console.setAutoLinkMask(Linkify.ALL);
      console.setText("Website: http://code.google.com/p/gtalksms");
      console.append("\n\nDonors\n");
      console.append(Web.DownloadFromUrl("http://gtalksms.googlecode.com/hg/Donors"));
      console.append("\n\nChange log\n");
      console.append(Web.DownloadFromUrl("http://gtalksms.googlecode.com/hg/Changelog"));
    }
}
