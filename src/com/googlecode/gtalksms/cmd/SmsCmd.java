package com.googlecode.gtalksms.cmd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.XMPPException;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.cmd.smsCmd.DeliveredIntentReceiver;
import com.googlecode.gtalksms.cmd.smsCmd.SentIntentReceiver;
import com.googlecode.gtalksms.cmd.smsCmd.SetLastRecipientRunnable;
import com.googlecode.gtalksms.cmd.smsCmd.Sms;
import com.googlecode.gtalksms.cmd.smsCmd.SmsMmsManager;
import com.googlecode.gtalksms.data.contacts.Contact;
import com.googlecode.gtalksms.data.contacts.ContactsManager;
import com.googlecode.gtalksms.data.phone.Phone;
import com.googlecode.gtalksms.databases.AliasHelper;
import com.googlecode.gtalksms.databases.KeyValueHelper;
import com.googlecode.gtalksms.databases.SMSHelper;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppMsg;
import com.googlecode.gtalksms.xmpp.XmppMuc;

public class SmsCmd extends CommandHandlerBase {
    private static boolean sSentIntentReceiverRegistered = false;
    private static boolean sDelIntentReceiverRegistered = false;
    private SmsMmsManager _smsMgr;
    private String _lastRecipient = null;
    private String _lastRecipientName = null;    
    
    public BroadcastReceiver _sentSmsReceiver = null;
    public BroadcastReceiver _deliveredSmsReceiver = null;
    
    private static Integer smsID;
    private SetLastRecipientRunnable _setLastrecipientRunnable;
    
    // synchronizedMap because the worker thread and the intent receivers work with this map
    private static Map<Integer, Sms> _smsMap; 
    
    private AliasHelper _aliasHelper;
    private KeyValueHelper _keyValueHelper;
    private SMSHelper _smsHelper;
          
    public SmsCmd(MainService mainService) {
        super(mainService, new String[] {"sms", "reply", "findsms", "fs", "markasread", "mar", "chat", "delsms"}, CommandHandlerBase.TYPE_MESSAGE);
        _smsMgr = new SmsMmsManager(sSettingsMgr, sContext);
        _smsHelper = SMSHelper.getSMSHelper(sContext);
        _aliasHelper = AliasHelper.getAliasHelper(mainService.getBaseContext());
        _keyValueHelper = KeyValueHelper.getKeyValueHelper(mainService.getBaseContext());
        
        restoreSmsInformation();
        setup();
        restoreLastRecipient();
    }
    
    public void setup() {
        if (sSettingsMgr.notifySmsSent && !sSentIntentReceiverRegistered) {
            if (_sentSmsReceiver == null) {
                _sentSmsReceiver = new SentIntentReceiver(sMainService, _smsMap, _smsHelper);
            }
            sMainService.registerReceiver(_sentSmsReceiver, new IntentFilter(MainService.ACTION_SMS_SENT));
            sSentIntentReceiverRegistered = true;
        }
        if (sSettingsMgr.notifySmsDelivered && !sDelIntentReceiverRegistered) {
            if (_deliveredSmsReceiver == null) {
                _deliveredSmsReceiver = new DeliveredIntentReceiver(sMainService, _smsMap, _smsHelper);
            }
            sMainService.registerReceiver(_deliveredSmsReceiver, new IntentFilter(MainService.ACTION_SMS_DELIVERED));
            sDelIntentReceiverRegistered = true;
        }
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
                send(R.string.chat_error_no_recipient);
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
                send(R.string.chat_error_no_recipient);
            } else {
                markSmsAsRead(_lastRecipient);
            }
        } else if (command.equals("chat")) {
        	if (args.length() > 0) {
                inviteRoom(_aliasHelper.convertAliasToNumber(args));
        	} else if (_lastRecipient != null) {
        	    try {
					XmppMuc.getInstance(sContext).inviteRoom(_lastRecipient, _lastRecipientName, XmppMuc.MODE_SMS);
				} catch (XMPPException e) {
					// TODO Auto-generated catch block
				}
        	}
        } else if (command.equals("delsms")) {
            if (args.length() == 0) {
                send(R.string.chat_del_sms_syntax);
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
    public synchronized void setLastRecipientNow(String phoneNumber, boolean silentAndUpdate) {
        if (_lastRecipient == null || !phoneNumber.equals(_lastRecipient)) {
            _lastRecipient = phoneNumber;
            _lastRecipientName = ContactsManager.getContactName(sContext, phoneNumber);
            if (!silentAndUpdate) { 
            	displayLastRecipient();
            	_keyValueHelper.addKey(KeyValueHelper.KEY_LAST_RECIPIENT, phoneNumber);
            }
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
                send(R.string.chat_del_sms_error);
            }
        } else if (cmd.equals("contact") && search != null) {
            ArrayList<Contact> contacts = ContactsManager.getMatchingContacts(sContext, search);
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
                send(R.string.chat_del_sms_from, contact.name);
                nbDeleted = _smsMgr.deleteSmsByContact(contact.rawIds);
            } else {
                send(R.string.chat_no_match_for, search);
            }
        } else {
            send(R.string.chat_del_sms_syntax);
        }
        
        if (nbDeleted >= 0) {
            send(R.string.chat_del_sms_nb, nbDeleted);
        } else if (nbDeleted == -1) {
            send(R.string.chat_del_sms_error);
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
                name = ContactsManager.getContactName(sContext, contact);                
                try {
					XmppMuc.getInstance(sContext).inviteRoom(number, name, XmppMuc.MODE_SMS);
				} catch (XMPPException e) {
					// TODO Auto-generated catch block
				}
        } else {
            ArrayList<Phone> mobilePhones = ContactsManager.getMobilePhones(sContext, contact);
            if (mobilePhones.size() > 1) {
                send(R.string.chat_specify_details);
                for (Phone phone : mobilePhones) {
                    send(phone.getContactName() + " - " + phone.getCleanNumber());
                }
            } else if (mobilePhones.size() == 1) {
                Phone phone = mobilePhones.get(0);
                try {
                    XmppMuc.getInstance(sContext).inviteRoom(phone.getCleanNumber(), phone.getContactName(), XmppMuc.MODE_SMS);
				} catch (XMPPException e) {
					// TODO Auto-generated catch block
				}
//                setLastRecipient(phone.cleanNumber); // issue 117
            } else {
                send(R.string.chat_no_match_for, contact);
            }
        }
    }
    
    /**
     * Search for SMS Mesages 
     * and sends them back to the user
     * 
     * @param message
     * @param contactName - optional, may be null
     */
    private void searchSMS(String message, String contactName) {
        ArrayList<Contact> contacts;
        ArrayList<Sms> sentSms = null;
        
        send(R.string.chat_sms_search_start);
        
        contacts = ContactsManager.getMatchingContacts(sContext, contactName != null ? contactName : "*");
        
        if (sSettingsMgr.showSentSms) {
            sentSms = _smsMgr.getAllSentSms(message);
        }
        
        if (contacts.size() > 0) {
            send(R.string.chat_sms_search, message, contacts.size());
            
            for (Contact contact : contacts) {
                ArrayList<Sms> smsArrayList = _smsMgr.getSms(contact.rawIds, contact.name, message);
                if (sentSms != null) {
                    smsArrayList.addAll(_smsMgr.getSentSms(ContactsManager.getPhones(sContext, contact.id), sentSms));
                }
                Collections.sort(smsArrayList);

                if (smsArrayList.size() > 0) {
                    XmppMsg smsContact = new XmppMsg();
                    smsContact.appendBold(contact.name);
                    smsContact.append(" - ");
                    smsContact.appendItalicLine(getString(R.string.chat_sms_search_results, smsArrayList.size()));
                    if (sSettingsMgr.smsReplySeparate) {
                        send(smsContact);
                        for (Sms sms : smsArrayList) {
                            smsContact = new XmppMsg();
                            appendSMS(smsContact, sms);
                            send(smsContact);
                        }
                    } else {
                        for (Sms sms : smsArrayList) {
                            appendSMS(smsContact, sms);
                        }
                        send(smsContact);
                    }
                }
            }
        } else if (sentSms.size() > 0) {
            XmppMsg smsContact = new XmppMsg();
            smsContact.appendBold(getString(R.string.chat_me));
            smsContact.append(" - ");
            smsContact.appendItalicLine(getString(R.string.chat_sms_search_results, sentSms.size()));
            if (sSettingsMgr.smsReplySeparate) {
                send(smsContact);
                for (Sms sms : sentSms) {
                    smsContact = new XmppMsg();
                    appendSMS(smsContact, sms);
                    send(smsContact);
                }
            } else {
                for (Sms sms : sentSms) {
                    appendSMS(smsContact, sms);
                }
                send(smsContact);
            }
        } else {
                send(R.string.chat_no_match_for, message);
        }
    }
    
    /**
     * Appends an SMS to an XmppMsg with formating
     * does not send the XmppMsg!
     * 
     * @param msg
     * @param sms
     */
    private static void appendSMS(XmppMsg msg, Sms sms) {
        msg.appendItalicLine(sms.getDate().toLocaleString() + " - " + sms.getSender());
        msg.appendLine(sms.getMessage());
    }

    /**
     * Sends an SMS Message
     * returns an error to the user if the contact could not be found
     * 
     * @param message the message to send
     * @param contact the name or number
     */
    private void sendSMS(String message, String contact) {
        if (Phone.isCellPhoneNumber(contact)) {
            String resolvedName = ContactsManager.getContactName(sContext, contact);
            if (sSettingsMgr.notifySmsSent) {
                send(R.string.chat_send_sms,  resolvedName + ": \"" + Tools.shortenMessage(message) + "\"");
            }
            sendSMSByPhoneNumber(message, contact, resolvedName);           
        } else {
            ArrayList<Phone> mobilePhones = ContactsManager.getMobilePhones(sContext, contact);
            if (mobilePhones.size() > 1) {
                // start searching for a default mobile number
                for (Phone phone : mobilePhones) {
                    if (phone.isDefaultNumber()) {
                        sendSMSByPhoneNumber(message, phone.getCleanNumber(), phone.getContactName());
                        return;
                    }
                }
                // there are more then 1 mobile phones for this contact
                // and none of them is marked as default
                send(R.string.chat_specify_details);

                for (Phone phone : mobilePhones) {
                    send(phone.getContactName() + " - " + phone.getCleanNumber());
                }
            } else if (mobilePhones.size() == 1) {
                Phone phone = mobilePhones.get(0);
                if (sSettingsMgr.notifySmsSent) {
                    send(R.string.chat_send_sms, phone.getContactName() + " (" + phone.getCleanNumber() + ")"  + ": \"" + Tools.shortenMessage(message) + "\"");
                }
                sendSMSByPhoneNumber(message, phone.getCleanNumber(), phone.getContactName());
            } else {
                send(R.string.chat_no_match_for, contact);
            }
        }
    }

    private void markSmsAsRead(String contact) {

        if (Phone.isCellPhoneNumber(contact)) {
            send(R.string.chat_mark_as_read, ContactsManager.getContactName(sContext, contact));
            _smsMgr.markAsRead(contact);
        } else {
            ArrayList<Phone> mobilePhones = ContactsManager.getMobilePhones(sContext, contact);
            if (mobilePhones.size() > 0) {
                send(R.string.chat_mark_as_read, mobilePhones.get(0).getContactName());

                for (Phone phone : mobilePhones) {
                    _smsMgr.markAsRead(phone.getNumber());
                }
            } else {
                send(R.string.chat_no_match_for, contact);
            }
        }
    }

    /**
     * reads (count) SMS from all contacts matching pattern
     * 
     *  @param searchedText 
     */
    private void readSMS(String searchedText) {

        ArrayList<Contact> contacts = ContactsManager.getMatchingContacts(sContext, searchedText);
        ArrayList<Sms> sentSms = new ArrayList<Sms>();
        if (sSettingsMgr.showSentSms) {
            sentSms = _smsMgr.getAllSentSms();
        }

        if (contacts.size() > 0) {

            XmppMsg noSms = new XmppMsg();
            boolean hasMatch = false;
            for (Contact contact : contacts) {
                ArrayList<Sms> smsArrayList = _smsMgr.getSms(contact.rawIds, contact.name);
                if (sSettingsMgr.showSentSms) {
                    smsArrayList.addAll(_smsMgr.getSentSms(ContactsManager.getPhones(sContext, contact.id), sentSms));
                }
                Collections.sort(smsArrayList);

                List<Sms> smsList = Tools.getLastElements(smsArrayList, sSettingsMgr.smsNumber);
                if (smsList.size() > 0) {
                    hasMatch = true;
                    XmppMsg smsContact = new XmppMsg();
                    smsContact.append(contact.name);
                    smsContact.append(" - ");
                    smsContact.appendItalicLine(getString(R.string.chat_sms_search_results, smsArrayList.size()));
                    
                    for (Sms sms : smsList) {
                        appendSMS(smsContact, sms);
                    }
                    if (smsList.size() < sSettingsMgr.smsNumber) {
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
            send(R.string.chat_no_match_for, searchedText);
        }
    }

    /** reads unread SMS from all contacts */
    private void readUnreadSMS() {

        ArrayList<Sms> smsArrayList = _smsMgr.getAllUnreadSms();
        XmppMsg allSms = new XmppMsg();

        List<Sms> smsList = Tools.getLastElements(smsArrayList, sSettingsMgr.smsNumber);
        if (smsList.size() > 0) {
            for (Sms sms : smsList) {
                appendSMS(allSms, sms);
            }
        } else {
            allSms.appendLine(getString(R.string.chat_no_sms));
        }
        send(allSms);
    }
    
    /** reads last (count) SMS from all contacts */
    private void readLastSMS() {

        ArrayList<Sms> smsArrayList = _smsMgr.getAllReceivedSms();

        if (sSettingsMgr.showSentSms) {
            smsArrayList.addAll(_smsMgr.getAllSentSms());
        }
        Collections.sort(smsArrayList);

        List<Sms> smsList = Tools.getLastElements(smsArrayList, sSettingsMgr.smsNumber);
        if (smsList.size() > 0) {
            XmppMsg message = new XmppMsg();
            if (sSettingsMgr.smsReplySeparate) {
                for (Sms sms : smsList) {
                    appendSMS(message, sms);
                    send(message);
                    message = new XmppMsg();
                }   
            } else {
                for (Sms sms : smsList) {
                    appendSMS(message, sms);
                } 
                send(message);
            }
        } else {
            send(R.string.chat_no_sms);
        }
    }
    
    private void displayLastRecipient() {
        if (_lastRecipient == null) {
            send(R.string.chat_error_no_recipient);
        } else {
            String contact = ContactsManager.getContactName(sContext, _lastRecipient);
            if (Phone.isCellPhoneNumber(_lastRecipient) && contact.compareTo(_lastRecipient) != 0) {
                contact += " (" + _lastRecipient + ")";
            }
            send(R.string.chat_reply_contact, contact);
        }
    }

    /** Sends a sms to the specified phone number with a receiver name */
    private void sendSMSByPhoneNumber(String message, String phoneNumber, String toName) {
        ArrayList<PendingIntent> SentPenIntents = null;
        ArrayList<PendingIntent> DelPenIntents = null;
        SmsManager sms = SmsManager.getDefault();
        ArrayList<String> messages = sms.divideMessage(message);

        if(sSettingsMgr.notifySmsSentDelivered) {
            String shortendMessage = Tools.shortenMessage(message);
            Integer smsID = getSmsID();
            Sms s = new Sms(phoneNumber, toName, shortendMessage, messages.size(), mAnswerTo, smsID);          
            _smsMap.put(smsID, s);
            _smsHelper.addSMS(s);
            if(sSettingsMgr.notifySmsSent) {
                SentPenIntents = createSPendingIntents(messages.size(), smsID);
            }
            if(sSettingsMgr.notifySmsDelivered) {
                DelPenIntents = createDPendingIntents(messages.size(), smsID);
            }
        }

        sms.sendMultipartTextMessage(phoneNumber, null, messages, SentPenIntents, DelPenIntents);
        setLastRecipient(phoneNumber);
        _smsMgr.addSmsToSentBox(message, phoneNumber);
    }
    
    private ArrayList<PendingIntent> createSPendingIntents(int size, int smsID) {
        ArrayList<PendingIntent> SentPenIntents = new ArrayList<PendingIntent>();
        int startSIntentNumber = getSIntentStart(size);
        for (int i = 0; i < size; i++) {
            int p = startSIntentNumber++;
            Intent sentIntent = new Intent(MainService.ACTION_SMS_SENT);
            sentIntent.putExtra("partNum", i);
            sentIntent.putExtra("smsID", smsID);
            PendingIntent sentPenIntent = PendingIntent.getBroadcast(sContext, p, sentIntent, PendingIntent.FLAG_ONE_SHOT);
            SentPenIntents.add(sentPenIntent);
        }
        return SentPenIntents;
    }
    
    private ArrayList<PendingIntent> createDPendingIntents(int size, int smsID) {
        ArrayList<PendingIntent> DelPenIntents = new ArrayList<PendingIntent>();
        int startDIntentNumber = getDIntentStart(size);
        for (int i = 0; i < size; i++) {
            int p = startDIntentNumber++;
            Intent deliveredIntent = new Intent(MainService.ACTION_SMS_DELIVERED);
            deliveredIntent.putExtra("partNum", i);
            deliveredIntent.putExtra("smsID", smsID);
            PendingIntent deliveredPenIntent = PendingIntent.getBroadcast(sContext, p, deliveredIntent, PendingIntent.FLAG_ONE_SHOT);
            DelPenIntents.add(deliveredPenIntent);
        }
        return DelPenIntents;
    }
    
    /**
     * restores the lastRecipient from the database if possible
     */
    private void restoreLastRecipient() {
    	String phoneNumber = _keyValueHelper.getValue(KeyValueHelper.KEY_LAST_RECIPIENT);
    	if (phoneNumber != null) {
    		setLastRecipientNow(phoneNumber, true);
    	}
    }
    
    private void restoreSmsInformation() {
        if (smsID == null) {
            smsID = _keyValueHelper.getIntegerValue(KeyValueHelper.KEY_SMS_ID);
            // this is the first time, init the values
            if (smsID == null) {
                _keyValueHelper.addKey(KeyValueHelper.KEY_SMS_ID, "0");
                _keyValueHelper.addKey(KeyValueHelper.KEY_SINTENT, "0");
                _keyValueHelper.addKey(KeyValueHelper.KEY_DINTENT, "0");
                smsID = 0;
            }
            _smsMap = Collections.synchronizedMap(new HashMap<Integer, Sms>());
            _smsHelper.deleteOldSMS();
            Sms[] toAdd = _smsHelper.getFullDatabase();
            for (Sms s : toAdd) {
                _smsMap.put(s.getID(), s);
            }
        }
    }
    
    private Integer getSmsID() {
        int res = smsID;
        smsID++;
        _keyValueHelper.addKey(KeyValueHelper.KEY_SMS_ID, smsID.toString());        
        return new Integer(res);
    }
    
    private int getSIntentStart(int size) {
        Integer res = _keyValueHelper.getIntegerValue(KeyValueHelper.KEY_SINTENT);
        Integer newValue = res + size;
        _keyValueHelper.addKey(KeyValueHelper.KEY_SINTENT, newValue.toString());
        return res;
    }
    
    private int getDIntentStart(int size) {
        Integer res = _keyValueHelper.getIntegerValue(KeyValueHelper.KEY_DINTENT);
        Integer newValue = res + size;
        _keyValueHelper.addKey(KeyValueHelper.KEY_DINTENT, newValue.toString());
        return res;
    }
    
    @Override
    public void cleanUp() {
        if (_sentSmsReceiver != null && sSentIntentReceiverRegistered) {
            sContext.unregisterReceiver(_sentSmsReceiver);
            sSentIntentReceiverRegistered = false;
        }
        if (_deliveredSmsReceiver != null && sDelIntentReceiverRegistered) {
            sContext.unregisterReceiver(_deliveredSmsReceiver);
            sDelIntentReceiverRegistered = false;
        }
    }    
}
