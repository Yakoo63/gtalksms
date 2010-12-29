package com.googlecode.gtalksms.panels;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.widget.TextView;

import com.googlecode.gtalksms.tools.StringFmt;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.tools.Web;
import com.googlecode.gtalksms.R;

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
        label.setText(StringFmt.Style("GTalkSMS " + Tools.getVersionName(getBaseContext(), getClass()), Typeface.BOLD));

        updateConsole();
    }
    
    public void updateConsole() {
      TextView console = (TextView) findViewById(R.id.Text);
      console.setText("");
      console.append(StringFmt.Fmt("Website\n", 0xFFFF0000, 1.5, Typeface.BOLD));
      console.append(StringFmt.Url("http://code.google.com/p/gtalksms"));
      console.append(StringFmt.Fmt("\n\nAuthors\n", 0xFFFF0000, 1.5, Typeface.BOLD));
      console.append(Web.DownloadFromUrl("http://gtalksms.googlecode.com/hg/AUTHORS"));
      console.append(StringFmt.Fmt("\nDonors\n", 0xFFFF0000, 1.5, Typeface.BOLD));
      console.append(Web.DownloadFromUrl("http://gtalksms.googlecode.com/hg/Donors"));
      console.append(StringFmt.Fmt("\nChange log\n", 0xFFFF0000, 1.5, Typeface.BOLD));
      console.append(Web.DownloadFromUrl("http://gtalksms.googlecode.com/hg/Changelog"));
      
      MovementMethod m = console.getMovementMethod();
      if ((m == null) || !(m instanceof LinkMovementMethod))
      {
          console.setMovementMethod(LinkMovementMethod.getInstance());
      }
    }
}
