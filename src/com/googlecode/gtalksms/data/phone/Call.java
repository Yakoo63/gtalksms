package com.googlecode.gtalksms.data.phone;

import java.util.Date;

import android.content.Context;

import com.googlecode.gtalksms.R;

public class Call {
    public String phoneNumber;
    public int type;
    public long duration;
    public Date date;

    public String duration() {
        long minutes = duration / 60;
        long seconds = duration % 60;
        String res = "";
        
        if (minutes > 0) {
            res = minutes + "min ";
        }
        
        return res + seconds + "s";
    }
    
    public String type(Context context) {
        int key;
        switch (type) {
            case 1:
                key = R.string.chat_call_incoming;
                break;
            case 2:
                key = R.string.chat_call_outgoing;
                break;
            case 3:
                key = R.string.chat_call_missed;
                break;
            default:
                key = R.string.chat_call_unknown;
        }
        
        return context.getString(key);
    }
}
