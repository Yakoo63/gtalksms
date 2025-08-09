package com.googlecode.gtalksms.databases;

import com.googlecode.gtalksms.cmd.smsCmd.Sms;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

/**
 * Backend Class for the SMSDatabase
 * allows manipulation of the database
 * 
 * @author Florian Schmaus fschmaus@gmail.com - on behalf of the GTalkSMS Team
 *
 */
class SMSDatabase extends Database {
    private static final long OLD_SMS_THRESHOLD = 1000 * 60 * 60 * 24 * 5; // 5 days
    
    public SMSDatabase(Context ctx) {
        super(ctx); 
    }
    
    public static boolean addSMS(ContentValues values) {
        long ret = database.insert(DatabaseOpenHelper.SMS_TABLE_NAME, null, values);
        return ret != -1;
    }
    
    public static boolean updateSMS(ContentValues values, int smsID) {
        int ret = database.update(DatabaseOpenHelper.SMS_TABLE_NAME, values, "smsID='" + smsID + "'", null);
        return ret == 1;
    }
    
    public static boolean deleteSMS(int id) {
        int ret = database.delete(DatabaseOpenHelper.SMS_TABLE_NAME, "smsID='" + id + "'", null);
        return ret == 1;
    }    
    
    public static boolean containsSMS(int id) {        
        Cursor c = databaseRO.query(DatabaseOpenHelper.SMS_TABLE_NAME, new String[] { "smsID" }, "smsID='" + id + "'", null, null , null, null);
        boolean ret = c.getCount() == 1;
        c.close();
        return ret;
    }
    
    public static Sms[] getFullDatabase() {
        Cursor c = databaseRO.query(DatabaseOpenHelper.SMS_TABLE_NAME, 
                new String[] { "smsID", "phoneNumber", "name", "shortenedMessage", "answerTo",
                    "dIntents", "sIntents", "numParts", "resSIntent", "resDIntent", "date"}, null, null, null , null, null);
        int rowCount = c.getCount();
        c.moveToFirst();
        Sms[] res = new Sms[rowCount];
        for (int i = 0; i < rowCount; i++) {
            res[i] = new Sms(c.getInt(0), 
                    c.getString(1), 
                    c.getString(2),
                    c.getString(3),
                    c.getString(4),
                    c.getString(5),
                    c.getString(6),
                    c.getInt(7),
                    c.getInt(8),
                    c.getLong(9));     
            c.moveToNext();
        }
        c.close();
        return res;
    }
    
    /**
     * Deletes SMS from the Database that are older then 5 days
     * 
     */
    public static int deleteOldSMS() {
        long olderthan = System.currentTimeMillis() - OLD_SMS_THRESHOLD;
        return database.delete(DatabaseOpenHelper.SMS_TABLE_NAME, "date < " + olderthan, null);
    }
    
    /**
     * 
     * @param smsID
     * @return the result if there was one, otherwise null
     */
    public static String getSentIntent(int smsID) {
        String res = null;
        Cursor c = databaseRO.query(DatabaseOpenHelper.SMS_TABLE_NAME, new String[] { "sIntents" }, "smsID=" + smsID, null, null, null, null);
        if (c.moveToFirst()) {
            res = c.getString(0);   
        }        
        c.close();
        return res;
    }

    public static boolean putSentIntent(int smsID, String string) {
        boolean res = false;
        if (containsSMS(smsID)) {
            ContentValues value = new ContentValues();
            value.put("sIntents", string);
            int ret = (database.update(DatabaseOpenHelper.SMS_TABLE_NAME, value, "smsID=" + smsID, null));
            if (ret == 1) {
                res = true;
            }
        }
        return res;
    }
    
    /**
     * 
     * @param smsID
     * @return the result if there was one, otherwise null
     */
    public static String getDelIntent(int smsID) {
        String res = null;
        Cursor c = databaseRO.query(DatabaseOpenHelper.SMS_TABLE_NAME, new String[] { "sIntents" }, "smsID=" + smsID, null, null, null, null);
        if (c.moveToFirst()) {
            res = c.getString(0);   
        }        
        c.close();
        return res;
    }

    public static boolean putDelIntent(int smsID, String string) {
        boolean res = false;
        if (containsSMS(smsID)) {
            ContentValues value = new ContentValues();
            value.put("dIntents", string);
            int ret = (database.update(DatabaseOpenHelper.SMS_TABLE_NAME, value, "smsID=" + smsID, null));
            if (ret == 1) {
                res = true;
            }
        }
        return res;
    }
}
