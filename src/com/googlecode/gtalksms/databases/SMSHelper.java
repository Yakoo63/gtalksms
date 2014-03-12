package com.googlecode.gtalksms.databases;

import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.cmd.smsCmd.Sms;

import android.content.ContentValues;
import android.content.Context;

/**
 * Middle-end helper. Adds and restores SMS to the database backend.
 * 
 * @author Florian Schmaus fschmaus@gmail.com - on behalf of the GTalkSMS Team
 *
 */
public class SMSHelper {    
    private static SMSHelper smsHelper = null;
    
    /**
     * This constructor ensures that the database is setup correctly
     * @param ctx
     */
    private SMSHelper(Context ctx) {
        new SMSDatabase(ctx);
    }
    
    public static SMSHelper getSMSHelper(Context ctx) {
        if (smsHelper == null) {
            smsHelper = new SMSHelper(ctx);
        }        
        return smsHelper;
    }
    
    public boolean addSMS(Sms sms) {
        int smsID = sms.getID();
        ContentValues values = new ContentValues();
        values.put("smsID", smsID);
        values.put("phoneNumber", sms.getNumber());
        values.put("name", sms.getTo().replace('\'', '\"'));
        values.put("shortenedMessage", sms.getShortenedMessage().replace('\'', '\"'));
        values.put("answerTo", sms.getAnswerTo() == null ? "unknown" : sms.getAnswerTo());
        values.put("dIntents", sms.getDelIntents());
        values.put("sIntents", sms.getSentIntents());
        values.put("numParts", sms.getNumParts());
        values.put("resSIntent", sms.getResSentIntent());
        values.put("resDIntent", sms.getDelIntents());
        values.put("date", sms.getCreatedDate().getTime());
        return addOrUpdate(values, smsID);
    }
    
    public boolean deleteSMS(int id) {
        if(SMSDatabase.containsSMS(id)) {
            return SMSDatabase.deleteSMS(id);
        } else {
            return false;
        }
    }
    
    public boolean containsSMS(int smsID) {
        return SMSDatabase.containsSMS(smsID);
    }
     
    public void deleteOldSMS() {
        SMSDatabase.deleteOldSMS();
    }
    
    public Sms[] getFullDatabase() {
        return SMSDatabase.getFullDatabase();
    }
    
    private boolean addOrUpdate(ContentValues values, int smsID) {
       if(SMSDatabase.containsSMS(smsID)) {
           return SMSDatabase.updateSMS(values, smsID);
       } else {
           return SMSDatabase.addSMS(values);
       }
    }

    public void setSentIntentTrue(int smsID, int partNum) {
        String sentIntentStr = SMSDatabase.getSentIntent(smsID);
        if (sentIntentStr != null) {
            char[] sentIntent = sentIntentStr.toCharArray();
            // OoB check, see issue 187
            if (partNum < sentIntent.length) {
                sentIntent[partNum] = 'X';
                SMSDatabase.putSentIntent(smsID, sentIntent.toString());
            } else {
                Log.e("SMSHelper.setSentIntent() OutOfBounds: " +
                        "partNum=" + partNum +
                        " length=" + sentIntent.length +
                        " sentIntentSTr= " + sentIntentStr);
            }
        } // TODO handle null case
    }

    public void setDelIntentTrue(int smsID, int partNum) {
        String delIntentStr = SMSDatabase.getDelIntent(smsID);
        if (delIntentStr != null) {
            char[] delIntent = delIntentStr.toCharArray();
            // OoB check, see issue 208
            if (partNum < delIntent.length) {
                delIntent[partNum] = 'X';
                SMSDatabase.putDelIntent(smsID, delIntent.toString());
            } else {
                Log.e("SMSHelper.setSentIntent() OutOfBounds: " +
                        "partNum=" + partNum +
                        " length=" + delIntent.length +
                        " sentIntentSTr= " + delIntentStr);
            }
        } // TODO handle null case
    }       
}
