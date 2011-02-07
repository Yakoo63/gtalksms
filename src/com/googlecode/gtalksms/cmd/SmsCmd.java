package com.googlecode.gtalksms.cmd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import android.util.Log;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.data.contacts.Contact;
import com.googlecode.gtalksms.data.contacts.ContactsManager;
import com.googlecode.gtalksms.data.phone.Phone;
import com.googlecode.gtalksms.data.sms.Sms;
import com.googlecode.gtalksms.data.sms.SmsMmsManager;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class SmsCmd extends Command {

    private SmsMmsManager _smsMgr;
    private String _lastRecipient = null;
    
    public BroadcastReceiver _sentSmsReceiver = null;
    public BroadcastReceiver _deliveredSmsReceiver = null;

    private int _smsCount;
    
    // Id used to distinguish the PendingIntents
    private int _penSIntentCount;
    private int _penDIntentCount;
    private Map<Integer, Sms> _smsMap = Collections.synchronizedMap(new HashMap<Integer, Sms>());
    
    public SmsCmd(MainService mainService) {
        super(mainService);
        _smsMgr = new SmsMmsManager(_settingsMgr, _context);

        if (_settingsMgr.notifySmsSent) {
            _sentSmsReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int smsID = intent.getIntExtra("smsID", -1);
                    int partNum = intent.getIntExtra("partNum", -1);
                    int res = getResultCode();
                    Integer SmsID = new Integer(smsID);
                    Sms s = _smsMap.get(SmsID);

                    if (s != null) {  // we could find the sms in the smsMap
                        s.sentIntents[partNum] = true;
                        boolean sentIntComplete = s.sentIntentsComplete();
                        String to;
                        if (s.to != null) { // prefer a name over a number in the to field
                            to = s.to;
                        } else {
                            to = s.number;
                        }

                        if (res == Activity.RESULT_OK && sentIntComplete) {
                            send(_context.getString(R.string.chat_sms_sent_to, s.shortendMessage, to));
                        } else if (s.resSentIntent == -1) {
                            switch (res) {
                            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                                send(_context.getString(R.string.chat_sms_failure_to, s.shortendMessage, to));
                                break;
                            case SmsManager.RESULT_ERROR_NO_SERVICE:
                                send(_context.getString(R.string.chat_sms_no_service_to, s.shortendMessage, to));
                                break;
                            case SmsManager.RESULT_ERROR_NULL_PDU:
                                send(_context.getString(R.string.chat_sms_null_pdu_to, s.shortendMessage, to));
                                break;
                            case SmsManager.RESULT_ERROR_RADIO_OFF:
                                send(_context.getString(R.string.chat_sms_radio_off_to, s.shortendMessage, to));
                                break;
                            }
                            s.resSentIntent = res;
                        }
                        if (_settingsMgr.notifySmsDelivered == false && sentIntComplete) {
                            _smsMap.remove(SmsID);  
                        }
                        
                    } else { // we could NOT find the sms in the smsMap - fall back to old behavior
                        Log.d(Tools.LOG_TAG, "sms in smsMap missing");
                        switch (res) {
                        case Activity.RESULT_OK:
                            send(_context.getString(R.string.chat_sms_sent));
                            break;
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                            send(_context.getString(R.string.chat_sms_failure));
                            break;
                        case SmsManager.RESULT_ERROR_NO_SERVICE:
                            send(_context.getString(R.string.chat_sms_no_service));
                            break;
                        case SmsManager.RESULT_ERROR_NULL_PDU:
                            send(_context.getString(R.string.chat_sms_null_pdu));
                            break;
                        case SmsManager.RESULT_ERROR_RADIO_OFF:
                            send(_context.getString(R.string.chat_sms_radio_off));
                            break;
                        }
                    }
                }
            };
            mainService.registerReceiver(_sentSmsReceiver, new IntentFilter(MainService.ACTION_SMS_SENT));
        }

        if (_settingsMgr.notifySmsDelivered) {
            _deliveredSmsReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int smsID = intent.getIntExtra("smsID", -1);
                    int partNum = intent.getIntExtra("partNum", -1);
                    int res = getResultCode();
                    Integer SmsID = new Integer(smsID);
                    Sms s = _smsMap.get(SmsID);

                    if (s != null) { // we could find the sms in the smsMap
                        s.delIntents[partNum] = true;
                        boolean delIntComplete = s.delIntentsComplete();
                        String to;
                        if (s.to != null) { // prefer a name over a number in the to field
                            to = s.to;
                        } else {
                            to = s.number;
                        }

                        if (res == Activity.RESULT_OK && delIntComplete) {
                            send(_context.getString(R.string.chat_sms_delivered_to, s.shortendMessage, to));
                        } else if (s.resSentIntent == -1) {
                            if(res == Activity.RESULT_CANCELED) {
                                send(_context.getString(R.string.chat_sms_not_delivered_to, s.shortendMessage, to));
                            }
                            s.resSentIntent = res;
                        }
                        if (delIntComplete) {
                            _smsMap.remove(SmsID);
                        }

                    } else { // we could NOT find the sms in the smsMap - fall back to old the behavior
                        Log.d(Tools.LOG_TAG, "sms in smsMap missing");
                        switch (res) {
                        case Activity.RESULT_OK:
                            send(_context.getString(R.string.chat_sms_delivered));
                            break;
                        case Activity.RESULT_CANCELED:
                            send(_context.getString(R.string.chat_sms_not_delivered));
                            break;
                        }
                    }
                }
            };
            mainService.registerReceiver(_deliveredSmsReceiver, new IntentFilter(MainService.ACTION_SMS_DELIVERED));
        }
    }

    @Override
    public void execute(String command, String args) {
        if (command.equals("sms")) {
            int separatorPos = args.indexOf(":");
            String contact = null;
            String message = null;
            if (-1 != separatorPos) {
                contact = args.substring(0, separatorPos);
                message = args.substring(separatorPos + 1);
                sendSMS(message, contact);
            } else if (args.length() > 0) {
                if (args.equals("unread")) {
                    readUnreadSMS();
                } else {
                    readSMS(args);
                }
            } else {
                readLastSMS();
            }
        } else if (command.equals("reply")) {
            if (args.length() == 0) {
                displayLastRecipient();
            } else if (_lastRecipient == null) {
                send(getString(R.string.chat_error_no_recipient));
            } else {
                _smsMgr.markAsRead(_lastRecipient);
                sendSMS(args, _lastRecipient);
            }
        } else if (command.equals("findsms") || command.equals("fs")) {
            int separatorPos = args.indexOf(":");
            String contact = null;
            String message = null;
            if (-1 != separatorPos) {
                contact = args.substring(0, separatorPos);
                message = args.substring(separatorPos + 1);
                searchSMS(message, contact);
            } else if (args.length() > 0) {
                searchSMS(args, null);
            }
        } else if (command.equals("markasread") || command.equals("mar")) {
            if (args.length() > 0) {
                markSmsAsRead(args);
            } else if (_lastRecipient == null) {
                send(getString(R.string.chat_error_no_recipient));
            } else {
                markSmsAsRead(_lastRecipient);
            }
        } else if (command.equals("delsms")) {
            if (args.length() == 0) {
                send(getString(R.string.chat_del_sms_syntax));
            } else {
                int separatorPos = args.indexOf(":");
                String subCommand = null;
                String search = null;
                if (-1 != separatorPos) {
                    subCommand = args.substring(0, separatorPos);
                    search = args.substring(separatorPos + 1);
                } else if (args.length() > 0) {
                    subCommand = args;
                }
                deleteSMS(subCommand, search);
            }
        }
    }

    /** delete SMS */
    private void deleteSMS(String cmd, String search) {
        
        int nbDeleted = -2;
        if (cmd.equals("all")) {
            nbDeleted = _smsMgr.deleteAllSms();
        } else if (cmd.equals("sent")) {
            nbDeleted = _smsMgr.deleteSentSms();
        } else if (cmd.equals("contact") && search != null) {
            ArrayList<Contact> contacts = ContactsManager.getMatchingContacts(_context, search);
            if (contacts.size() > 1) {
                StringBuilder sb = new StringBuilder(getString(R.string.chat_specify_details));
                sb.append(Tools.LineSep);
                for (Contact contact : contacts) {
                    sb.append(contact.name);
                    sb.append(Tools.LineSep);
                }
                send(sb.toString());
            } else if (contacts.size() == 1) {
                Contact contact = contacts.get(0);
                send(getString(R.string.chat_del_sms_from, contact.name));
                nbDeleted = _smsMgr.deleteSmsByContact(contact.rawIds);
            } else {
                send(getString(R.string.chat_no_match_for, search));
            }
        } else {
            send(getString(R.string.chat_del_sms_syntax));
        }
        
        if (nbDeleted >= 0) {
            send(getString(R.string.chat_del_sms_nb, nbDeleted));
        } else if (nbDeleted == -1) {
            send(getString(R.string.chat_del_sms_error));
        }
    }
    
    /** search SMS */
    private void searchSMS(String message, String contactName) {
        ArrayList<Contact> contacts = new ArrayList<Contact>();
        ArrayList<Sms> sentSms = null;
        
        send(getString(R.string.chat_sms_search_start));
        
        contacts = ContactsManager.getMatchingContacts(_context, contactName != null ? contactName : "*");
        
        if (_settingsMgr.displaySentSms) {
            sentSms = _smsMgr.getAllSentSms(message);
        }
        
        int nbResults = 0;
        if (contacts.size() > 0) {
            send(getString(R.string.chat_sms_search, message, contacts.size()));
            
            for (Contact contact : contacts) {
                ArrayList<Sms> smsArrayList = _smsMgr.getSms(contact.rawIds, contact.name, message);
                if (_settingsMgr.displaySentSms) {
                    smsArrayList.addAll(_smsMgr.getSentSms(ContactsManager.getPhones(_context, contact.id), sentSms));
                }
                Collections.sort(smsArrayList);

                if (smsArrayList.size() > 0) {
                    XmppMsg smsContact = new XmppMsg();
                    smsContact.appendBold(contact.name);
                    smsContact.append(" - ");
                    smsContact.appendItalicLine(getString(R.string.chat_sms_search_results, smsArrayList.size()));
                    
                    for (Sms sms : smsArrayList) {
                        smsContact.appendItalicLine(sms.date.toLocaleString() + " - " + sms.sender);
                        smsContact.appendLine(sms.message);
                        nbResults++;
                    }
                    
                    send(smsContact);
                }
            }
        } else if (sentSms.size() > 0) {
            XmppMsg smsContact = new XmppMsg();
            smsContact.appendBold(getString(R.string.chat_me));
            smsContact.append(" - ");
            smsContact.appendItalicLine(getString(R.string.chat_sms_search_results, sentSms.size()));
            
            for (Sms sms : sentSms) {
                smsContact.appendItalicLine(sms.date.toLocaleString() + " - " + sms.sender);
                smsContact.appendLine(sms.message);
                nbResults++;
            }
            
            send(smsContact);
        } 
        
        if (nbResults > 0) {
            send(getString(R.string.chat_sms_search_results, nbResults));
        } else {
            send(getString(R.string.chat_no_match_for, message));
        }
    }


    /** sends a SMS to the specified contact */
    private void sendSMS(String message, String contact) {
        if (Phone.isCellPhoneNumber(contact)) {
            send(getString(R.string.chat_send_sms, ContactsManager.getContactName(_context, contact)));
            sendSMSByPhoneNumber(message, contact, null);
        } else {
            ArrayList<Phone> mobilePhones = ContactsManager.getMobilePhones(_context, contact);
            if (mobilePhones.size() > 1) {
                send(getString(R.string.chat_specify_details));

                for (Phone phone : mobilePhones) {
                    send(phone.contactName + " - " + phone.cleanNumber);
                }
            } else if (mobilePhones.size() == 1) {
                Phone phone = mobilePhones.get(0);
                send(getString(R.string.chat_send_sms, phone.contactName + " (" + phone.cleanNumber + ")"));
                setLastRecipient(phone.cleanNumber);
                sendSMSByPhoneNumber(message, phone.cleanNumber, phone.contactName);
            } else {
                send(getString(R.string.chat_no_match_for, contact));
            }
        }
    }

    private void markSmsAsRead(String contact) {

        if (Phone.isCellPhoneNumber(contact)) {
            send(getString(R.string.chat_mark_as_read, ContactsManager.getContactName(_context, contact)));
            _smsMgr.markAsRead(contact);
        } else {
            ArrayList<Phone> mobilePhones = ContactsManager.getMobilePhones(_context, contact);
            if (mobilePhones.size() > 0) {
                send(getString(R.string.chat_mark_as_read, mobilePhones.get(0).contactName));

                for (Phone phone : mobilePhones) {
                    _smsMgr.markAsRead(phone.number);
                }
            } else {
                send(getString(R.string.chat_no_match_for, contact));
            }
        }
    }

    /** reads (count) SMS from all contacts matching pattern */
    private void readSMS(String searchedText) {

        ArrayList<Contact> contacts = ContactsManager.getMatchingContacts(_context, searchedText);
        ArrayList<Sms> sentSms = new ArrayList<Sms>();
        if (_settingsMgr.displaySentSms) {
            sentSms = _smsMgr.getAllSentSms();
        }

        if (contacts.size() > 0) {

            XmppMsg noSms = new XmppMsg();
            Boolean hasMatch = false;
            for (Contact contact : contacts) {
                ArrayList<Sms> smsArrayList = _smsMgr.getSms(contact.rawIds, contact.name);
                if (_settingsMgr.displaySentSms) {
                    smsArrayList.addAll(_smsMgr.getSentSms(ContactsManager.getPhones(_context, contact.id), sentSms));
                }
                Collections.sort(smsArrayList);

                List<Sms> smsList = Tools.getLastElements(smsArrayList, _settingsMgr.smsNumber);
                if (smsList.size() > 0) {
                    hasMatch = true;
                    XmppMsg smsContact = new XmppMsg();
                    smsContact.append(contact.name);
                    smsContact.append(" - ");
                    smsContact.appendItalicLine(getString(R.string.chat_sms_search_results, smsArrayList.size()));
                    
                    for (Sms sms : smsList) {
                        smsContact.appendItalicLine(sms.date.toLocaleString() + " - " + sms.sender);
                        smsContact.appendLine(sms.message);
                    }
                    if (smsList.size() < _settingsMgr.smsNumber) {
                        smsContact.appendItalicLine(getString(R.string.chat_only_got_n_sms, smsList.size()));
                    }
                    send(smsContact);
                } else {
                    noSms.appendBold(contact.name);
                    noSms.appendLine(getString(R.string.chat_no_sms));
                }
            }
            if (!hasMatch) {
                send(noSms);
            }
        } else {
            send(getString(R.string.chat_no_match_for, searchedText));
        }
    }

    /** reads unread SMS from all contacts */
    private void readUnreadSMS() {

        ArrayList<Sms> smsArrayList = _smsMgr.getAllUnreadSms();
        XmppMsg allSms = new XmppMsg();

        List<Sms> smsList = Tools.getLastElements(smsArrayList, _settingsMgr.smsNumber);
        if (smsList.size() > 0) {
            for (Sms sms : smsList) {
                allSms.appendItalicLine(sms.date.toLocaleString() + " - " + sms.sender);
                allSms.appendLine(sms.message);
            }
        } else {
            allSms.appendLine(getString(R.string.chat_no_sms));
        }
        send(allSms);
    }
    
    /** reads last (count) SMS from all contacts */
    private void readLastSMS() {

        ArrayList<Sms> smsArrayList = _smsMgr.getAllReceivedSms();
        XmppMsg allSms = new XmppMsg();

        if (_settingsMgr.displaySentSms) {
            smsArrayList.addAll(_smsMgr.getAllSentSms());
        }
        Collections.sort(smsArrayList);

        List<Sms> smsList = Tools.getLastElements(smsArrayList, _settingsMgr.smsNumber);
        if (smsList.size() > 0) {
            for (Sms sms : smsList) {
                allSms.appendItalicLine(sms.date.toLocaleString() + " - " + sms.sender);
                allSms.appendLine(sms.message);
            }
        } else {
            allSms.appendLine(getString(R.string.chat_no_sms));
        }
        send(allSms);
    }
    
    public void setLastRecipient(String phoneNumber) {
        if (_lastRecipient == null || !phoneNumber.equals(_lastRecipient)) {
            _lastRecipient = phoneNumber;
            displayLastRecipient();
        }
    }
    
    public void displayLastRecipient() {
        if (_lastRecipient == null) {
            send(getString(R.string.chat_error_no_recipient));
        } else {
            String contact = ContactsManager.getContactName(_context, _lastRecipient);
            if (Phone.isCellPhoneNumber(_lastRecipient) && contact.compareTo(_lastRecipient) != 0) {
                contact += " (" + _lastRecipient + ")";
            }
            send(getString(R.string.chat_reply_contact, contact));
        }
    }

    /** Sends a sms to the specified phone number with a receiver name */
    private void sendSMSByPhoneNumber(String message, String phoneNumber, String toName) {
        ArrayList<PendingIntent> SentPenIntents = null;
        ArrayList<PendingIntent> DelPenIntents = null;
        SmsManager sms = SmsManager.getDefault();
        ArrayList<String> messages = sms.divideMessage(message);

        if(_settingsMgr.notifySmsSentDelivered) {
            String shortendMessage = shortenMessage(message);
            int smsID = _smsCount++;
            Sms s = new Sms(phoneNumber, toName, shortendMessage, messages.size());
            _smsMap.put(new Integer(smsID), s);
            if(_settingsMgr.notifySmsSent) {
                SentPenIntents = createSPendingIntents(messages.size(), smsID);
            }
            if(_settingsMgr.notifySmsDelivered) {
                DelPenIntents = createDPendingIntents(messages.size(), smsID);
            }
        }

        sms.sendMultipartTextMessage(phoneNumber, null, messages, SentPenIntents, DelPenIntents);
        _smsMgr.addSmsToSentBox(message, phoneNumber);
    }

    /** clear the sms monitoring related stuff */
    private void clearSmsMonitor() {
        if (_sentSmsReceiver != null) {
            _context.unregisterReceiver(_sentSmsReceiver);
        }
        if (_deliveredSmsReceiver != null) {
            _context.unregisterReceiver(_deliveredSmsReceiver);
        }
        _sentSmsReceiver = null;
        _deliveredSmsReceiver = null;
    }
    
    private ArrayList<PendingIntent> createSPendingIntents(int size, int smsID) {
        ArrayList<PendingIntent> SentPenIntents = new ArrayList<PendingIntent>();
        for (int i = 0; i < size; i++) {
            int p = _penSIntentCount++;
                Intent sentIntent = new Intent(MainService.ACTION_SMS_SENT);
                sentIntent.putExtra("partNum", i);
                sentIntent.putExtra("smsID", smsID);
                PendingIntent sentPenIntent = PendingIntent.getBroadcast(_context, p, sentIntent, PendingIntent.FLAG_ONE_SHOT);
                SentPenIntents.add(sentPenIntent);
        }
        return SentPenIntents;
    }
    
    private ArrayList<PendingIntent> createDPendingIntents(int size, int smsID) {
        ArrayList<PendingIntent> DelPenIntents = new ArrayList<PendingIntent>();
        for (int i = 0; i < size; i++) {
            int p = _penDIntentCount++;
            Intent deliveredIntent = new Intent(MainService.ACTION_SMS_DELIVERED);
            deliveredIntent.putExtra("partNum", i);
            deliveredIntent.putExtra("smsID", smsID);
            PendingIntent deliveredPenIntent = PendingIntent.getBroadcast(_context, p, deliveredIntent, PendingIntent.FLAG_ONE_SHOT);
            DelPenIntents.add(deliveredPenIntent);
        }
        return DelPenIntents;
    }
    
    private static String shortenMessage(String message) {
        final int shortenTo = 20;
        String shortendMessage;
        if (message.length() < shortenTo) {
            shortendMessage = message;
        } else {
            shortendMessage = message.substring(0, shortenTo) + "...";
        }
        return shortendMessage;
    }
    
    @Override
    public void cleanUp() {
        clearSmsMonitor();
    }
}
