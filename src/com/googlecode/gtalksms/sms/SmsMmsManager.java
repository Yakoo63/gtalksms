package com.googlecode.gtalksms.sms;

import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.gsm.SmsManager;

import com.googlecode.gtalksms.Tools;
import com.googlecode.gtalksms.XmppService;
import com.googlecode.gtalksms.contacts.Phone;

public class SmsMmsManager {
    // intents for sms sending
    public static PendingIntent sentPI = null;
    public static PendingIntent deliveredPI = null;
    public static BroadcastReceiver sentSmsReceiver = null;
    public static BroadcastReceiver deliveredSmsReceiver = null;
    public static boolean notifySmsSent;
    public static boolean notifySmsDelivered;

    /** clear the sms monitoring related stuff */
    public static void clearSmsMonitors() {
        if (sentSmsReceiver != null) {
            XmppService.getInstance().unregisterReceiver(sentSmsReceiver);
        }
        if (deliveredSmsReceiver != null) {
            XmppService.getInstance().unregisterReceiver(deliveredSmsReceiver);
        }
        sentPI = null;
        deliveredPI = null;
        sentSmsReceiver = null;
        deliveredSmsReceiver = null;
    }

    /** reinit sms monitors (that tell the user the status of the sms) */
    public static void initSmsMonitors() {
        if (notifySmsSent) {
            String SENT = "SMS_SENT";
            sentPI = PendingIntent.getBroadcast(XmppService.getInstance(), 0,
                new Intent(SENT), 0);
            sentSmsReceiver = new BroadcastReceiver(){
                @Override
                public void onReceive(Context arg0, Intent arg1) {
                    switch (getResultCode())
                    {
                        case Activity.RESULT_OK:
                            XmppService.getInstance().send("SMS sent");
                            break;
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                            XmppService.getInstance().send("Generic failure");
                            break;
                        case SmsManager.RESULT_ERROR_NO_SERVICE:
                            XmppService.getInstance().send("No service");
                            break;
                        case SmsManager.RESULT_ERROR_NULL_PDU:
                            XmppService.getInstance().send("Null PDU");
                            break;
                        case SmsManager.RESULT_ERROR_RADIO_OFF:
                            XmppService.getInstance().send("Radio off");
                            break;
                    }
                }
            };
            XmppService.getInstance().registerReceiver(sentSmsReceiver, new IntentFilter(SENT));
        }
        if (notifySmsDelivered) {
            String DELIVERED = "SMS_DELIVERED";
            deliveredPI = PendingIntent.getBroadcast(XmppService.getInstance(), 0,
                    new Intent(DELIVERED), 0);
            deliveredSmsReceiver = new BroadcastReceiver(){
                @Override
                public void onReceive(Context arg0, Intent arg1) {
                    switch (getResultCode())
                    {
                        case Activity.RESULT_OK:
                            XmppService.getInstance().send("SMS delivered");
                            break;
                        case Activity.RESULT_CANCELED:
                            XmppService.getInstance().send("SMS not delivered");
                            break;
                    }
                }
            };
            XmppService.getInstance().registerReceiver(deliveredSmsReceiver, new IntentFilter(DELIVERED));
        }
    }

    /** Sends a sms to the specified phone number */
//    public static void sendSMSByPhoneNumber(String message, String phoneNumber) {
//        SmsManager sms = SmsManager.getDefault();
//        ArrayList<String> messages = sms.divideMessage(message);
//        for (int i=0; i < messages.size(); i++) {
//            sms.sendTextMessage(phoneNumber, null, messages.get(i), sentPI, deliveredPI);
//            addSmsToSentBox(message, phoneNumber);
//        }
//    }
    
    public static void sendSMSByPhoneNumber(String message, String phoneNumber) {
        //send("Sending sms to " + getContactName(phoneNumber));
        SmsManager sms = SmsManager.getDefault();
        ArrayList<String> messages = sms.divideMessage(message);
        
        //création des liste d'instents
        ArrayList<PendingIntent> listOfSentIntents = new ArrayList<PendingIntent>();
        listOfSentIntents.add(sentPI);
        ArrayList<PendingIntent> listOfDelIntents = new ArrayList<PendingIntent>();
        listOfDelIntents.add(deliveredPI);
        for (int i=1; i < messages.size(); i++){
            listOfSentIntents.add(null);
            listOfDelIntents.add(null);
        }        
        
        sms.sendMultipartTextMessage(phoneNumber, null, messages, listOfSentIntents, listOfDelIntents);
        
        addSmsToSentBox(message, phoneNumber);
    }
    /**
     * Returns a ArrayList of <Sms> with count sms where the contactId match the argument
     */
    public static ArrayList<Sms> getSms(Long contactId, String contactName) {
        ArrayList<Sms> res = new ArrayList<Sms>();

        if(null != contactId) {
            Uri mSmsQueryUri = Uri.parse("content://sms/inbox");
            String columns[] = new String[] { "person", "address", "body", "date", "status"};
            Cursor c = XmppService.getInstance().getContentResolver().query(mSmsQueryUri, columns, "person = " + contactId, null, null);

            if (c.getCount() > 0) {
                for (boolean hasData = c.moveToFirst() ; hasData ; hasData = c.moveToNext()) {
                    Date date = new Date();
                    date.setTime(Long.parseLong(Tools.getString(c ,"date")));
                    Sms sms = new Sms();
                    sms.date = date;
                    sms.number = Tools.getString(c ,"address");
                    sms.message = Tools.getString(c ,"body");
                    sms.sender = contactName;
                    res.add( sms );
                }
            }
            c.close();
        }
        return res;
    }

    /**
     * Returns a ArrayList of <Sms> with count sms where the contactId match the argument
     */
    public static ArrayList<Sms> getAllSentSms() {
        ArrayList<Sms> res = new ArrayList<Sms>();

        Uri mSmsQueryUri = Uri.parse("content://sms/sent");
        String columns[] = new String[] { "address", "body", "date", "status"};
        Cursor c = XmppService.getInstance().getContentResolver().query(mSmsQueryUri, columns, null, null, null);

        if (c.getCount() > 0) {
            for (boolean hasData = c.moveToFirst() ; hasData ; hasData = c.moveToNext()) {
                Date date = new Date();
                date.setTime(Long.parseLong(Tools.getString(c ,"date")));
                Sms sms = new Sms();
                sms.date = date;
                sms.number = Tools.getString(c ,"address");
                sms.message = Tools.getString(c ,"body");
                sms.sender = "Me";
                res.add( sms );

            }
        }
        c.close();

        return res;
    }

    /**
     * Returns a ArrayList of <Sms> with count sms where the contactId match the argument
     */
    public static ArrayList<Sms> getSentSms(ArrayList<Phone> phones, ArrayList<Sms> sms) {
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
                res.add( aSms );
            }
        }

        return res;
    }

    /** Adds the text of the message to the sent box */
    public static void addSmsToSentBox(String message, String phoneNumber) {
        ContentValues values = new ContentValues();
        values.put("address", phoneNumber);
        values.put("date", System.currentTimeMillis());
        values.put("body", message);
        XmppService.getInstance().getContentResolver().insert(Uri.parse("content://sms/sent"), values);
    }
}
