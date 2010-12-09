package com.googlecode.gtalksms.phone;

import com.googlecode.gtalksms.XmppService;

import android.content.Intent;
import android.net.Uri;

public class PhoneManager {

    /** Dial a phone number */
    public static Boolean Dial(String number) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tel:" + number));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            XmppService.getInstance().startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
