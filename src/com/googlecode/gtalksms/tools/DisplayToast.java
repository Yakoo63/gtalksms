package com.googlecode.gtalksms.tools;

import android.content.Context;
import android.widget.Toast;

public final class DisplayToast implements Runnable {
    private final String text;
    private final Context ctx;
    
    public DisplayToast(String text, Context ctx) {
        this.text = text;
        this.ctx = ctx;
    }
    
    public void run() {
        String toastMsg  =  Tools.APP_NAME + ": " + text;
        Toast.makeText(ctx, toastMsg, Toast.LENGTH_SHORT).show();
    }
}