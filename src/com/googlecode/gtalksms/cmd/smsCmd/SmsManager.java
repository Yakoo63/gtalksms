package com.googlecode.gtalksms.cmd.smsCmd;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.googlecode.gtalksms.Log;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.data.contacts.ContactsManager;
import com.googlecode.gtalksms.data.phone.Phone;
import com.googlecode.gtalksms.tools.StringFmt;
import com.googlecode.gtalksms.tools.Tools;

public class SmsManager {

    private Context _context;
    private SettingsManager _settings;
    
    private static final Uri SMS_CONTENT_URI = Uri.parse("content://sms");
    private static final Uri SMS_INBOX_CONTENT_URI = Uri.withAppendedPath(SMS_CONTENT_URI, "inbox");
    private static final Uri SMS_SENTBOX_CONTENT_URI = Uri.withAppendedPath(SMS_CONTENT_URI, "sent");
    private static final String COLUMNS[] = new String[] { "person", "address", "body", "date", "status" };
    private static final String SORT_ORDER = "date DESC";

    public SmsManager(SettingsManager settings, Context baseContext) {
        _settings = settings;
        _context = baseContext;
        Log.initialize(_settings);
    }

    /**
     * Returns a ArrayList of <Sms> with count sms where the contactId match the
     * argument
     */
    public ArrayList<Sms> getSms(ArrayList<Long> rawIds, String contactName) {
        if (rawIds.size() > 0) {
            return getAllSms(false, contactName, "person IN (" + TextUtils.join(", ", rawIds) + ") or person is null", false);
        }
        return new ArrayList<Sms>();
    }
    
    public ArrayList<Sms> getSms(ArrayList<Long> rawIds, String contactName, String message) {
        if (rawIds.size() > 0) {
            return getAllSms(false, contactName, "(person IN (" + TextUtils.join(", ", rawIds) + ") or person is null) and body LIKE '%" + StringFmt.encodeSQL(message) + "%'", false);
        }
        return new ArrayList<Sms>();
    }

    /**
     * Returns a ArrayList of <Sms> with count sms where the contactId match the
     * argument
     */
    public ArrayList<Sms> getAllSentSms() {
        return getAllSms(true, null, null, true);
    }
    
    public ArrayList<Sms> getAllSentSms(String message) {
        return getAllSms(true, null, "body LIKE '%" + StringFmt.encodeSQL(message) + "%'", true);
    }

    public ArrayList<Sms> getAllReceivedSms() {
        return getAllSms(false, null, null, false);
    }

    public ArrayList<Sms> getAllUnreadSms() {
        return getAllSms(false, null, "read = 0", false);
    }
    
    /**
     * Returns an ArrayList with all SMS that match the given criteria.
     * 
     * @param outgoingSms
     * @param sender
     * @param where
     * @param getMax
     * @return
     */
    private ArrayList<Sms> getAllSms(boolean outgoingSms, String sender, String where, boolean getMax) {
        ArrayList<Sms> res = new ArrayList<Sms>();
       
        Uri mSmsQueryUri = outgoingSms ? SMS_SENTBOX_CONTENT_URI : SMS_INBOX_CONTENT_URI;
        Cursor c = _context.getContentResolver().query(mSmsQueryUri, COLUMNS, where, null, SORT_ORDER);
        final int maxSms = _settings.smsNumber;
        int smsCount = 0;

        if (c != null) {
            for (boolean hasData = c.moveToFirst(); hasData && (getMax || smsCount < maxSms); hasData = c.moveToNext(), ++smsCount) {
                Long rawId = Tools.getLong(c, "person");
                String address = Tools.getString(c, "address");
                String addressName = ContactsManager.getContactName(_context, address);
                
                // Sometime, if it's only an external contact to gmail (exchange by instance)
                // the rawId is not set and with have to check the address (phone number)
                if (!outgoingSms && rawId == 0 && sender != null && addressName.compareTo(sender) != 0) {
                    smsCount--;
                    continue;
                }
                
                String receiver = outgoingSms ? addressName : _context.getString(R.string.chat_me);
                String body =     Tools.getString(c, "body");
                Sms sms = new Sms(address, body,  Tools.getDateMilliSeconds(c, "date"), receiver);
                String currentSender = sender;   
                
                if (currentSender == null) {
                    currentSender = outgoingSms ? _context.getString(R.string.chat_me) : addressName;
                }
                
                // Setting sender to null here is OK, because it just means
                // that the name of the sender could not be determined
                // and therefore the number will be used
                sms.setSender(currentSender);
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
                if (phone.phoneMatch(aSms.getNumber())) {
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
        ContentValues values = new ContentValues();
        values.put("address", phoneNumber);
        values.put("date", System.currentTimeMillis());
        values.put("body", message);
        _context.getContentResolver().insert(Uri.parse("content://sms/sent"), values);
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
    
    public int deleteLastSms(Uri deleteUri, int number) {
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