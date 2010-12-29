package com.googlecode.gtalksms.data.sms;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.data.contacts.ContactsManager;
import com.googlecode.gtalksms.data.phone.Phone;
import com.googlecode.gtalksms.tools.Tools;

public class SmsMmsManager {

    Context _context;
    SettingsManager _settings;

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
            return getAllSms("content://sms/inbox", contactName, "person IN (" + TextUtils.join(", ", rawIds) + ")");
        }
        return new ArrayList<Sms>();
    }

    /**
     * Returns a ArrayList of <Sms> with count sms where the contactId match the
     * argument
     */
    public ArrayList<Sms> getAllSentSms() {
        return getAllSms("content://sms/sent", "Me", null);
    }

    public ArrayList<Sms> getAllReceivedSms() {
        return getAllSms("content://sms/inbox", null, null);
    }

    private ArrayList<Sms> getAllSms(String folder, String sender, String where) {
        ArrayList<Sms> res = new ArrayList<Sms>();

        Uri mSmsQueryUri = Uri.parse(folder);
        String columns[] = new String[] { "person", "address", "body", "date", "status" };
        String sortOrder = "date DESC";

        Cursor c = _context.getContentResolver().query(mSmsQueryUri, columns, where, null, sortOrder);
        int maxSms = _settings.smsNumber;
        int nbSms = 0;

        for (boolean hasData = c.moveToFirst(); hasData && nbSms < maxSms; hasData = c.moveToNext(), ++nbSms) {
            Sms sms = new Sms();
            sms.date = Tools.getDateMilliSeconds(c, "date");
            sms.number = Tools.getString(c, "address");
            sms.message = Tools.getString(c, "body");
            if (sender == null) {
                sms.sender = ContactsManager.getContactName(_context, Tools.getLong(c, "person"));
            } else {
                sms.sender = sender;
            }
            res.add(sms);

        }
        c.close();

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
}
