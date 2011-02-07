package com.googlecode.gtalksms.data.sms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.data.contacts.ContactsManager;
import com.googlecode.gtalksms.data.phone.Phone;
import com.googlecode.gtalksms.tools.StringFmt;
import com.googlecode.gtalksms.tools.Tools;

public class SmsMmsManager {

    private Context _context;
    private SettingsManager _settings;

    public SmsMmsManager(SettingsManager settings, Context baseContext) {
        _settings = settings;
        _context = baseContext;
    }

    /**
     * Returns a ArrayList of <Sms> with count sms where the contactId match the
     * argument
     */
    public ArrayList<Sms> getSms(ArrayList<Long> rawIds, String contactName) {
        if (rawIds.size() > 0) {
            return getAllSms("content://sms/inbox", contactName, "person IN (" + TextUtils.join(", ", rawIds) + ")", false);
        }
        return new ArrayList<Sms>();
    }
    
    public ArrayList<Sms> getSms(ArrayList<Long> rawIds, String contactName, String message) {
        if (rawIds.size() > 0) {
            return getAllSms("content://sms/inbox", contactName, 
                    "person IN (" + TextUtils.join(", ", rawIds) + ") and body LIKE '%" + StringFmt.encodeSQL(message) + "%'", false);
        }
        return new ArrayList<Sms>();
    }

    /**
     * Returns a ArrayList of <Sms> with count sms where the contactId match the
     * argument
     */
    public ArrayList<Sms> getAllSentSms() {
        return getAllSms("content://sms/sent", _context.getString(R.string.chat_me), null, true);
    }
    
    public ArrayList<Sms> getAllSentSms(String message) {
        return getAllSms("content://sms/sent", _context.getString(R.string.chat_me), 
                         "body LIKE '%" + StringFmt.encodeSQL(message) + "%'", true);
    }

    public ArrayList<Sms> getAllReceivedSms() {
        return getAllSms("content://sms/inbox", null, null, false);
    }

    public ArrayList<Sms> getAllUnreadSms() {
        return getAllSms("content://sms/inbox", null, "read = 0", false);
    }
    
    private ArrayList<Sms> getAllSms(String folder, String sender, String where, Boolean getMax) {
        ArrayList<Sms> res = new ArrayList<Sms>();

        Uri mSmsQueryUri = Uri.parse(folder);
        String columns[] = new String[] { "person", "address", "body", "date", "status" };
        String sortOrder = "date DESC";

        Cursor c = _context.getContentResolver().query(mSmsQueryUri, columns, where, null, sortOrder);
        int maxSms = _settings.smsNumber;
        int nbSms = 0;

        if (c != null) {
            for (boolean hasData = c.moveToFirst(); hasData && (getMax || nbSms < maxSms); hasData = c.moveToNext(), ++nbSms) {
                Sms sms = new Sms(Tools.getString(c, "address"), Tools.getString(c, "body"),  Tools.getDateMilliSeconds(c, "date"));
                if (sender == null) {
                    sms.sender = ContactsManager.getContactName(_context, Tools.getLong(c, "person"));
                } else {
                    sms.sender = sender;
                }
                res.add(sms);
    
            }
            
            c.close();
        }
        
        return res;
    }

    /**
     * Returns a ArrayList of <Sms> with count sms where the contactId match the
     * argument
     */
    public ArrayList<Sms> getSentSms(ArrayList<Phone> phones, ArrayList<Sms> sms) {
        ArrayList<Sms> res = new ArrayList<Sms>();

        for (Sms aSms : sms) {
            Boolean phoneMatch = false;

            for (Phone phone : phones) {
                if (phone.phoneMatch(aSms.number)) {
                    phoneMatch = true;
                    break;
                }
            }

            if (phoneMatch) {
                res.add(aSms);
            }
        }

        return res;
    }

    public void markAsRead(String smsNumber) {
        try {
            ContentResolver cr = _context.getContentResolver();
            Uri smsUri = Uri.parse("content://sms/inbox");
           
            ContentValues values = new ContentValues();
            values.put("read", "1");
            
            cr.update(smsUri, values, " address='" + smsNumber + "'", null);
        } catch (Exception e) {
            Log.i("exception in setRead:", e.getMessage());
        }
    }

    /** Adds the text of the message to the sent box */
    public void addSmsToSentBox(String message, String phoneNumber) {
        ContentValues values = new ContentValues();
        values.put("address", phoneNumber);
        values.put("date", System.currentTimeMillis());
        values.put("body", message);
        _context.getContentResolver().insert(Uri.parse("content://sms/sent"), values);
    }

    public int deleteAllSms() {
        return deleteSms("content://sms", null);
    }

    public int deleteSentSms() {
        return deleteSms("content://sms/sent", null);
    }
    
    public int deleteSmsByContact(ArrayList<Long> rawIds) {
        int result = -1;
        if (rawIds.size() > 0) {
            return deleteThreads("content://sms/inbox", "person IN (" + TextUtils.join(", ", rawIds) + ")");
        }
        return result;
    }
    
    private int deleteThreads(String url, String where) {
        int result = 0;

        ContentResolver cr = _context.getContentResolver();
        Uri deleteUri = Uri.parse(url);
        Cursor c = cr.query(deleteUri, new String[] { "thread_id" }, where, null, null);
        try {
            Set<String> threads = new HashSet<String>();
            
            while (c.moveToNext()) {
                threads.add(c.getString(0));
            }
            
            for (String thread : threads) {
                // Delete the SMS
                String uri = "content://sms/conversations/" + thread;
                result += cr.delete(Uri.parse(uri), null, null);
            }
        } catch (Exception e) {
            Log.e(Tools.LOG_TAG, "exception in deleteSms:", e);
            if (result == 0) {
                result = -1;
            }
        }

        return result;
    }

    private int deleteSms(String url, String where) {
        int result = 0;

        ContentResolver cr = _context.getContentResolver();
        Uri deleteUri = Uri.parse(url);
        Cursor c = cr.query(deleteUri, new String[] { "_id" }, where, null, null);
        try {
            while (c.moveToNext()) {
                // Delete the SMS
                String uri = "content://sms/" + c.getString(0);
                result += cr.delete(Uri.parse(uri), null, null);
            }
        } catch (Exception e) {
            Log.e(Tools.LOG_TAG, "exception in deleteSms:", e);
            if (result == 0) {
                result = -1;
            }
        }

        return result;
    }

}
