package com.googlecode.gtalksms.cmd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.cmd.smsCmd.DeliveredIntentReceiver;
import com.googlecode.gtalksms.cmd.smsCmd.SentIntentReceiver;
import com.googlecode.gtalksms.cmd.smsCmd.SetLastRecipientRunnable;
import com.googlecode.gtalksms.cmd.smsCmd.Sms;
import com.googlecode.gtalksms.cmd.smsCmd.SmsMmsManager;
import com.googlecode.gtalksms.data.contacts.Contact;
import com.googlecode.gtalksms.data.contacts.ContactsManager;
import com.googlecode.gtalksms.data.phone.Phone;
import com.googlecode.gtalksms.tools.AliasHelper;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class SmsCmd extends Command {
    private SmsMmsManager _smsMgr;
    private String _lastRecipient = null;
    private String _lastRecipientName = null;    
    private XmppManager _xmppMgr;
    
    public BroadcastReceiver _sentSmsReceiver = null;
    public BroadcastReceiver _deliveredSmsReceiver = null;

    private int _smsCount;
    private SetLastRecipientRunnable _setLastrecipientRunnable;
    
    // int counter used to distinguish the PendingIntents
    private int _penSIntentCount;
    private int _penDIntentCount;
    // synchronizedMap because the worker thread and the intent receivers work with this map
    private Map<Integer, Sms> _smsMap = Collections.synchronizedMap(new HashMap<Integer, Sms>()); 
    
    private AliasHelper _aliasHelper;
          
    public SmsCmd(MainService mainService) {
        super(mainService, new String[] {"sms", "reply", "findsms", "fs", "markasread", "mar", "chat", "delsms"});
        _smsMgr = new SmsMmsManager(_settingsMgr, _context);

        if (_settingsMgr.notifySmsSent) {
            _sentSmsReceiver = new SentIntentReceiver(mainService, _smsMap);
            mainService.registerReceiver(_sentSmsReceiver, new IntentFilter(MainService.ACTION_SMS_SENT));
        }

        if (_settingsMgr.notifySmsDelivered) {
            _deliveredSmsReceiver = new DeliveredIntentReceiver(mainService, _smsMap);
            mainService.registerReceiver(_deliveredSmsReceiver, new IntentFilter(MainService.ACTION_SMS_DELIVERED));
        }
        _xmppMgr = _mainService.getXmppmanager();
        _aliasHelper = mainService.createAndGetAliasHelper();
    }

    @Override
    protected void execute(String command, String args) {
    	String contact;
        if (command.equals("sms")) {
            int separatorPos = args.indexOf(":");
            contact = null;
            String message = null;
            
            if (-1 != separatorPos) {
                contact = args.substring(0, separatorPos);
                contact = _aliasHelper.convertAliasToNumber(contact);
                message = args.substring(separatorPos + 1);
            }
            
            if (message != null && message.length() > 0) {
                sendSMS(message, contact);
            } else if (args.length() > 0) {
                if (args.equals("unread")) {
                    readUnreadSMS();
                } else {
                    readSMS(_aliasHelper.convertAliasToNumber(args));
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
            contact = null;
            String message = null;
            if (-1 != separatorPos) {
                contact = args.substring(0, separatorPos);
                contact = _aliasHelper.convertAliasToNumber(contact);
                message = args.substring(separatorPos + 1);
                searchSMS(message, contact);
            } else if (args.length() > 0) {
                searchSMS(args, null);
            }
        } else if (command.equals("markasread") || command.equals("mar")) {
            if (args.length() > 0) {
                markSmsAsRead(_aliasHelper.convertAliasToNumber(args));
            } else if (_lastRecipient == null) {
                send(getString(R.string.chat_error_no_recipient));
            } else {
                markSmsAsRead(_lastRecipient);
            }
        } else if (command.equals("chat")) {
        	if (args.length() > 0) {
                inviteRoom(_aliasHelper.convertAliasToNumber(args));
        	} else if (_lastRecipient != null) {
        	    _xmppMgr.getXmppMuc().inviteRoom(_lastRecipient, _lastRecipientName);
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
                    search = _aliasHelper.convertAliasToNumber(search);
                } else if (args.length() > 0) {
                    subCommand = args;
                }
                deleteSMS(subCommand, search);
            }
        }
    }
    
    @Override
    public String[] help() {
        String[] s = { 
                getString(R.string.chat_help_sms_reply, makeBold("\"reply:#message#\"")),
                getString(R.string.chat_help_sms_show_all, makeBold("\"sms\"")),
                getString(R.string.chat_help_sms_show_unread, makeBold("\"sms:unread\"")),
                getString(R.string.chat_help_sms_show_contact, makeBold("\"sms:#contact#\"")),
                getString(R.string.chat_help_sms_send, makeBold("\"sms:#contact#:#message#\"")),
                getString(R.string.chat_help_sms_chat, makeBold("\"chat:#contact#")),
                getString(R.string.chat_help_find_sms_all, makeBold("\"findsms:#message#\""), makeBold("\"fs:#message#\"")),
                getString(R.string.chat_help_find_sms, makeBold("\"findsms:#contact#:#message#\""), makeBold("\"fs:#contact#:#message#\"")),
                getString(R.string.chat_help_mark_as_read, makeBold("\"markAsRead:#contact#\""), makeBold("\"mar\"")),
                getString(R.string.chat_help_del_sms_all, makeBold("\"delsms:all\"")),
                getString(R.string.chat_help_del_sms_sent, makeBold("\"delsms:sent\"")),
                getString(R.string.chat_help_del_sms_last, makeBold("\"delsms:last:#number#\""), makeBold("\"delsms:lastin:#number#\""), makeBold("\"delsms:lastout:#number#\"")),
                getString(R.string.chat_help_del_sms_contact, makeBold("\"delsms:contact:#contact#\""))
                };
        return s;
    }
    
    public void setLastRecipient(String phoneNumber) {
        SetLastRecipientRunnable slrRunnable = new SetLastRecipientRunnable(this, phoneNumber);
        if (_setLastrecipientRunnable != null) {
            _setLastrecipientRunnable.setOutdated();
        }
        _setLastrecipientRunnable = slrRunnable;
        Thread t = new Thread(slrRunnable);
        t.setDaemon(true);
        t.start();
    }
    
    /**
     * Sets the last Recipient/Reply contact
     * if the contact has changed
     * and calls displayLastRecipient()
     * 
     * @param phoneNumber
     */
    public synchronized void setLastRecipientNow(String phoneNumber) {
        if (_lastRecipient == null || !phoneNumber.equals(_lastRecipient)) {
            _lastRecipient = phoneNumber;
            _lastRecipientName = ContactsManager.getContactName(_context, phoneNumber);
            displayLastRecipient();
        }
    }
    
    /**
     * "delsms" cmd - deletes sms, either
     * - all sms
     * - all sent sms
     * - sms from specified contact
     * 
     * @param cmd - all, sent, contact
     * @param search - if cmd == contact the name of the contact
     */
    private void deleteSMS(String cmd, String search) {    
        int nbDeleted = -2;
        if (cmd.equals("all")) {
            nbDeleted = _smsMgr.deleteAllSms();
        } else if (cmd.equals("sent")) {
            nbDeleted = _smsMgr.deleteSentSms();
        } else if (cmd.startsWith("last")) {
            Integer number = Tools.parseInt(search);
            if (number == null) {
                number = 1;
            }

            if (cmd.equals("last")) { 
                nbDeleted = _smsMgr.deleteLastSms(number);
            } else if (cmd.equals("lastin")) { 
                nbDeleted = _smsMgr.deleteLastInSms(number);
            } else if (cmd.equals("lastout")) { 
                nbDeleted = _smsMgr.deleteLastOutSms(number);
            } else {
                send(getString(R.string.chat_del_sms_error));
            }
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
    
    /**
     * create a MUC with the specified contact
     * and invites the user
     * in case the contact isn't distinct
     * the user is informed
     * 
     * @param contact
     */
    private void inviteRoom(String contact) {
        String name, number;
        if (Phone.isCellPhoneNumber(contact)) {
                number = contact;
                name = ContactsManager.getContactName(_context, contact);
                _xmppMgr.getXmppMuc().inviteRoom(number, name);
        } else {
            ArrayList<Phone> mobilePhones = ContactsManager.getMobilePhones(_context, contact);
            if (mobilePhones.size() > 1) {
                send(getString(R.string.chat_specify_details));
                for (Phone phone : mobilePhones) {
                    send(phone.contactName + " - " + phone.cleanNumber);
                }
            } else if (mobilePhones.size() == 1) {
                Phone phone = mobilePhones.get(0);
                _xmppMgr.getXmppMuc().inviteRoom(phone.cleanNumber, phone.contactName);
//                setLastRecipient(phone.cleanNumber); // issue 117
            } else {
                send( getString(R.string.chat_no_match_for, contact) );
            }
        }
    }
    
    /** search SMS */
    private void searchSMS(String message, String contactName) {
        ArrayList<Contact> contacts = new ArrayList<Contact>();
        ArrayList<Sms> sentSms = null;
        
        send(getString(R.string.chat_sms_search_start));
        
        contacts = ContactsManager.getMatchingContacts(_context, contactName != null ? contactName : "*");
        
        if (_settingsMgr.showSentSms) {
            sentSms = _smsMgr.getAllSentSms(message);
        }
        
        int nbResults = 0;
        if (contacts.size() > 0) {
            send(getString(R.string.chat_sms_search, message, contacts.size()));
            
            for (Contact contact : contacts) {
                ArrayList<Sms> smsArrayList = _smsMgr.getSms(contact.rawIds, contact.name, message);
                if (_settingsMgr.showSentSms) {
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

    /**
     * Sends an SMS Message
     * 
     * @param message the message to send, needed
     * @param contact the name or number
     */
    private void sendSMS(String message, String contact) {
        if (Phone.isCellPhoneNumber(contact)) {
            String resolvedName = ContactsManager.getContactName(_context, contact);
            if (_settingsMgr.notifySmsSent) {
                send(getString(R.string.chat_send_sms,  resolvedName + ": \"" + Tools.shortenMessage(message) + "\""));
            }
            sendSMSByPhoneNumber(message, contact, resolvedName);           
        } else {
            ArrayList<Phone> mobilePhones = ContactsManager.getMobilePhones(_context, contact);
            if (mobilePhones.size() > 1) {
                send(getString(R.string.chat_specify_details));

                for (Phone phone : mobilePhones) {
                    send(phone.contactName + " - " + phone.cleanNumber);
                }
            } else if (mobilePhones.size() == 1) {
                Phone phone = mobilePhones.get(0);
                if (_settingsMgr.notifySmsSent) {
                    send(getString(R.string.chat_send_sms, phone.contactName + " (" + phone.cleanNumber + ")")  + ": \"" + Tools.shortenMessage(message) + "\"");
                }
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
        if (_settingsMgr.showSentSms) {
            sentSms = _smsMgr.getAllSentSms();
        }

        if (contacts.size() > 0) {

            XmppMsg noSms = new XmppMsg();
            Boolean hasMatch = false;
            for (Contact contact : contacts) {
                ArrayList<Sms> smsArrayList = _smsMgr.getSms(contact.rawIds, contact.name);
                if (_settingsMgr.showSentSms) {
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
                    noSms.append(" - ");
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

        if (_settingsMgr.showSentSms) {
            smsArrayList.addAll(_smsMgr.getAllSentSms());
        }
        Collections.sort(smsArrayList);

        List<Sms> smsList = Tools.getLastElements(smsArrayList, _settingsMgr.smsNumber);
        if (smsList.size() > 0) {
            XmppMsg message = new XmppMsg();
            if (_settingsMgr.smsReplySeparate) {
                for (Sms sms : smsList) {
                    message.appendItalicLine(sms.date.toLocaleString() + " - " + sms.sender);
                    message.appendLine(sms.message);
                    send(message);
                    message = new XmppMsg();
                }   
            } else {
                for (Sms sms : smsList) {
                    message.appendItalicLine(sms.date.toLocaleString() + " - " + sms.sender);
                    message.appendLine(sms.message);
                } 
                send(message);
            }
        } else {
            send(getString(R.string.chat_no_sms));
        }
    }
    
    private void displayLastRecipient() {
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
            String shortendMessage = Tools.shortenMessage(message);
            int smsID = _smsCount++;
            Sms s = new Sms(phoneNumber, toName, shortendMessage, messages.size(), _answerTo);
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
    
    @Override
    public void cleanUp() {
        clearSmsMonitor();
    }
    
}
