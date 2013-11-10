package com.googlecode.gtalksms.data.phone;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;

import com.googlecode.gtalksms.tools.Tools;

public class PhoneManager {

    private final Context _context;
    
    public PhoneManager(Context baseContext) {
        _context = baseContext;
    }

    /** Dial a phone number */
    public Boolean Dial(String number, boolean makeTheCall) {
        try {
            Intent intent = new Intent(makeTheCall ? Intent.ACTION_CALL : Intent.ACTION_VIEW, Uri.parse("tel:" + number));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            _context.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    
    public ArrayList<Call> getPhoneLogs() {
        ArrayList<Call> res = new ArrayList<Call>();

        ContentResolver resolver = _context.getContentResolver();
        
        String[] projection = new String[] { CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DURATION, CallLog.Calls.DATE};
        String sortOrder = CallLog.Calls.DATE + " ASC";

        Cursor c = resolver.query(CallLog.Calls.CONTENT_URI, projection, null, null, sortOrder);
        
        if (c != null) {
            for (boolean hasData = c.moveToFirst() ; hasData ; hasData = c.moveToNext()) {
                Call call = new Call();
                call.phoneNumber = Tools.getString(c, CallLog.Calls.NUMBER);
                if (call.phoneNumber.equals("-1") || call.phoneNumber.equals("-2")) {
                    call.phoneNumber = null;
                }
                call.duration = Tools.getLong(c, CallLog.Calls.DURATION);
                call.date = Tools.getDateMilliSeconds(c, CallLog.Calls.DATE);
                call.type = Tools.getInt(c,CallLog.Calls.TYPE);
                
                res.add(call);
            }
            c.close();
        }
        return res;
    }
}
