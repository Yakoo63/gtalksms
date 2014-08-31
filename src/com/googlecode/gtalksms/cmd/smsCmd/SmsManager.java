package com.googlecode.gtalksms.cmd.smsCmd;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.data.contacts.ContactsManager;
import com.googlecode.gtalksms.data.phone.Phone;
import com.googlecode.gtalksms.tools.StringFmt;
import com.googlecode.gtalksms.tools.Tools;

public class SmsManager {
    private final Context _context;
    private final SettingsManager _settings;
    
    private static final Uri THREADS_CONTENT_URI = Uri.parse("content://mms-sms/threadID");
    private static final Uri SMS_CONTENT_URI = Uri.parse("content://sms");
    private static final Uri SMS_INBOX_CONTENT_URI = Uri.withAppendedPath(SMS_CONTENT_URI, "inbox");
    private static final Uri SMS_SENTBOX_CONTENT_URI = Uri.withAppendedPath(SMS_CONTENT_URI, "sent");
    private static final String COLUMNS[] = new String[] { "person", "address", "body", "date", "type" };
    private static final String SORT_ORDER = "date DESC";
    private static final String SORT_ORDER_LIMIT = "date DESC limit ";

    public SmsManager(SettingsManager settings, Context baseContext) {
        _settings = settings;
        _context = baseContext;
        Log.initialize(_settings);
    }

    public ArrayList<Sms> getSms(ArrayList<Phone> phones) {
        return getSms(phones, null);
    }

    public ArrayList<Sms> getSms(ArrayList<Phone> phones, String search) {
        ArrayList<Sms> res = new ArrayList<Sms>();

        for (Phone phone: phones) {
            Cursor c = _context.getContentResolver().query(THREADS_CONTENT_URI.buildUpon().appendQueryParameter("recipient", phone.getCleanNumber()).build(), null, null, null, null);
            if (c != null) {
                for (boolean hasData = c.moveToFirst(); hasData; hasData = c.moveToNext()) {
                    res.addAll(getSmsByThreadId(Tools.getInt(c, "_id"), search));
                }
                c.close();
            }
        }
        
        return res;
    }
    
//    private ArrayList<Sms> getSmsMmsByThreadId(int threadId) {
//        ArrayList<Sms> res = new ArrayList<Sms>();
//
//        final String[] projection = new String[]{"_id", "ct_t"};
//        Uri uri = Uri.parse("content://mms-sms/conversations/" + threadId);
//        Cursor c = _context.getContentResolver().query(uri, projection, null, null, SORT_ORDER_LIMIT + _settings.smsNumber);
//        if (c != null) {
//            for (boolean hasData = c.moveToFirst(); hasData; hasData = c.moveToNext()) {
//                if ("application/vnd.wap.multipart.related".equals(Tools.getString(c, "ct_t"))) {
//                    // it's MMS
//                } else {
//                    // it's SMS
//                }
//            }
//            c.close();
//        }
//        return res;
//    }

    private ArrayList<Sms> getSmsByThreadId(int threadId, String search) {
        String where = "thread_id = " + threadId;
        if (search != null) {
            where += " and body LIKE '%" + StringFmt.encodeSQL(search) + "%'";
        }
        return getAllSms(where);
    }

    public ArrayList<Sms> getLastUnreadSms() {
        return getAllSms("read = 0");
    }
    
    public ArrayList<Sms> getLastSms() {
        return getAllSms(null);
    }
    
    public ArrayList<Sms> getLastSms(String search) {
        return getAllSms("body LIKE '%" + StringFmt.encodeSQL(search) + "%'");
    }
    
    private ArrayList<Sms> getAllSms(String where) {
        ArrayList<Sms> res = new ArrayList<Sms>();

        Cursor c = _context.getContentResolver().query(SMS_CONTENT_URI, COLUMNS, where, null, SORT_ORDER_LIMIT + _settings.smsNumber);
        if (c != null) {
            for (boolean hasData = c.moveToFirst(); hasData; hasData = c.moveToNext()) {
                boolean isSent = Tools.getInt(c, "type") == 2;
                String address = Tools.getString(c, "address");
                
                String sender = ContactsManager.getContactName(_context, address);
                String receiver = _context.getString(R.string.chat_me);
                
                Sms sms = new Sms(address,  Tools.getString(c, "body"),  Tools.getDateMilliSeconds(c, "date"), isSent ? sender : receiver);
                sms.setSender(isSent ? receiver : sender);
                res.add(sms);
            }
            c.close();
        }
        return res;
    }
    
    
    /**
     * Marks all SMS from a given phone number as read
     * 
     * @param smsNumber The phone number
     * @return true if successfully, otherwise false
     */
    public boolean markAsRead(String smsNumber) {
        try {
            ContentResolver cr = _context.getContentResolver();  
            ContentValues values = new ContentValues();
            values.put("read", "1");
            cr.update(SMS_INBOX_CONTENT_URI, values, " address='" + smsNumber + "'", null);
            return true;
        } catch (Exception e) {
            Log.w("markAsRead() exception:", e);
            return false;
        }
    }

    /** Adds the text of the message to the sent box */
    public void addSmsToSentBox(String message, String phoneNumber) {
        // Starting KITKAT, the sent sms are logged by the default application
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            ContentValues values = new ContentValues();
            values.put("address", phoneNumber);
            values.put("date", System.currentTimeMillis());
            values.put("body", message);
            _context.getContentResolver().insert(Uri.parse("content://sms/sent"), values);
        }
    }

    public int deleteAllSms() {
        return deleteSms(SMS_CONTENT_URI, null);
    }

    public int deleteSentSms() {
        return deleteSms(SMS_SENTBOX_CONTENT_URI, null);
    }
    
    public int deleteSmsByContact(ArrayList<Long> rawIds) {
        int result = -1;
        if (rawIds.size() > 0) {
            return deleteThreads(SMS_INBOX_CONTENT_URI, "person IN (" + TextUtils.join(", ", rawIds) + ")");
        }
        return result;
    }
    
    public int deleteSmsByNumber(String smsNumber) {
        return deleteThreads(SMS_INBOX_CONTENT_URI, "address = '" + smsNumber + "'");
    }
    
    private int deleteThreads(Uri deleteUri, String where) {
        int result = 0;

        ContentResolver cr = _context.getContentResolver();
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
            Log.e("exception in deleteSms:", e);
            if (result == 0) {
                result = -1;
            }
        }
        return result;
    }

    private int deleteSms(Uri deleteUri, String where) {
        int result = 0;

        ContentResolver cr = _context.getContentResolver();
        Cursor c = cr.query(deleteUri, new String[] { "_id" }, where, null, null);
        try {
            while (c.moveToNext()) {
                // Delete the SMS
                String uri = "content://sms/" + c.getString(0);
                result += cr.delete(Uri.parse(uri), null, null);
            }
        } catch (Exception e) {
            Log.e("exception in deleteSms:", e);
            if (result == 0) {
                result = -1;
            }
        }

        return result;
    }

    public int deleteLastSms(int number) {
        return deleteLastSms(SMS_CONTENT_URI, number);
    }
    
    public int deleteLastInSms(int number) {
        return deleteLastSms(SMS_INBOX_CONTENT_URI, number);
    }
    
    public int deleteLastOutSms(int number) {
        return deleteLastSms(SMS_SENTBOX_CONTENT_URI, number);
    }
    
    int deleteLastSms(Uri deleteUri, int number) {
        int result = 0;

        ContentResolver cr = _context.getContentResolver();
        Cursor c = cr.query(deleteUri, new String[] { "_id" }, null, null, SORT_ORDER);
        try {
            for (int i = 0 ; i < number && c.moveToNext() ; ++i) {
                // Delete the SMS
                String uri = "content://sms/" + c.getString(0);
                result += cr.delete(Uri.parse(uri), null, null);
            }
        } catch (Exception e) {
            Log.e("exception in deleteSms:", e);
            if (result == 0) {
                result = -1;
            }
        }

        return result;
    }
}