package com.googlecode.gtalksms.tools;

import android.content.Context;
import android.widget.Toast;

public final class DisplayToast implements Runnable {
    private final String text;
    private final Context ctx;
    private final String extraInfo;
    
    public DisplayToast(String text, String extraInfo, Context ctx) {
        this.text = text;
        this.ctx = ctx;
        this.extraInfo = extraInfo;
    }
    
    public void run() {
        Toast toast;
        int duration;
        String toastMsg;
        
        if (extraInfo == null) {
            toastMsg = Tools.APP_NAME + ": " + text;
            duration = Toast.LENGTH_SHORT;
        } else {
            toastMsg = Tools.APP_NAME + ": " + text + "\n" + extraInfo;
            duration = Toast.LENGTH_LONG;
        }
        
        toast = Toast.makeText(ctx, toastMsg, duration);
        toast.show();
    }
}