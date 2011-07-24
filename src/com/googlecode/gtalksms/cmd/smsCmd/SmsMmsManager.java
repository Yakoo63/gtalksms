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

public class SmsMmsManager {

    private Context _context;
    private SettingsManager _settings;
    
    private static final String INBOX = "content://sms/inbox";
    private static final String SENTBOX = "content://sms/sent";
    private static final String COLUMNS[] = new String[] { "person", "address", "body", "date", "status" };
    private static final String SORT_ORDER = "date DESC";

    public SmsMmsManager(SettingsManager settings, Context baseContext) {
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
            return getAllSms(false, contactName, "person IN (" + TextUtils.join(", ", rawIds) + ")", false);
        }
        return new ArrayList<Sms>();
    }
    
    public ArrayList<Sms> getSms(ArrayList<Long> rawIds, String contactName, String message) {
        if (rawIds.size() > 0) {
            return getAllSms(false, contactName, 
                    "person IN (" + TextUtils.join(", ", rawIds) + ") and body LIKE '%" + StringFmt.encodeSQL(message) + "%'", false);
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
        String folder;
        // Select the data resource based on the boolean flag
        if (outgoingSms) {
            folder = SENTBOX;
        } else {
            folder = INBOX;
        }
        
        Uri mSmsQueryUri = Uri.parse(folder);

        Cursor c = _context.getContentResolver().query(mSmsQueryUri, COLUMNS, where, null, SORT_ORDER);
        final int maxSms = _settings.smsNumber;
        int smsCount = 0;

        if (c != null) {
            for (boolean hasData = c.moveToFirst(); hasData && (getMax || smsCount < maxSms); hasData = c.moveToNext(), ++smsCount) {
                String address = Tools.getString(c, "address");
                String receiver = outgoingSms ? ContactsManager.getContactName(_context, address) : _context.getString(R.string.chat_me);
                Sms sms = new Sms(address, Tools.getString(c, "body"),  Tools.getDateMilliSeconds(c, "date"), receiver);
                String currentSender = sender;                
                if (currentSender == null) {
                    currentSender = outgoingSms ? _context.getString(R.string.chat_me) : ContactsManager.getContactName(_context, Tools.getLong(c, "person"));
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
     */
    public void markAsRead(String smsNumber) {
        try {
            ContentResolver cr = _context.getContentResolver();
            Uri smsUri = Uri.parse("content://sms/inbox");
           
            ContentValues values = new ContentValues();
            values.put("read", "1");
            
            cr.update(smsUri, values, " address='" + smsNumber + "'", null);
        } catch (Exception e) {
            Log.w("exception in setRead:", e);
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
            Log.e("exception in deleteSms:", e);
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
            Log.e("exception in deleteSms:", e);
            if (result == 0) {
                result = -1;
            }
        }

        return result;
    }

    public int deleteLastSms(int number) {
        return deleteLastSms("content://sms", number);
    }
    
    public int deleteLastInSms(int number) {
        return deleteLastSms("content://sms/inbox", number);
    }
    
    public int deleteLastOutSms(int number) {
        return deleteLastSms("content://sms/sent", number);
    }
    
    public int deleteLastSms(String url, int number) {
        int result = 0;

        ContentResolver cr = _context.getContentResolver();
        Uri deleteUri = Uri.parse(url);
        Cursor c = cr.query(deleteUri, new String[] { "_id" }, null, null, "date desc");
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
