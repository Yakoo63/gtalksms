package com.googlecode.gtalksms.receivers;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppMsg;

/**
 * This is the 
 * @author Florian Schmaus fschmaus@gmail.com
 *
 */
public class SendIntentActivity extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent intent = getIntent();
        String extraText = intent.getStringExtra(Intent.EXTRA_TEXT);
        XmppMsg msg = new XmppMsg();
        // msg.appendBoldLine("Received shared text");
        msg.appendLine(extraText);
        Tools.send(msg, null, getBaseContext());

        finish();
    }
}
