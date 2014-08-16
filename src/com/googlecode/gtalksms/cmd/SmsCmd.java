package com.googlecode.gtalksms.cmd;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.XMPPException;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.cmd.smsCmd.DeliveredIntentReceiver;
import com.googlecode.gtalksms.cmd.smsCmd.SentIntentReceiver;
import com.googlecode.gtalksms.cmd.smsCmd.Sms;
import com.googlecode.gtalksms.cmd.smsCmd.SmsManager;
import com.googlecode.gtalksms.data.contacts.Contact;
import com.googlecode.gtalksms.data.contacts.ContactsManager;
import com.googlecode.gtalksms.data.contacts.ContactsResolver;
import com.googlecode.gtalksms.data.contacts.ResolvedContact;
import com.googlecode.gtalksms.databases.AliasHelper;
import com.googlecode.gtalksms.databases.KeyValueHelper;
import com.googlecode.gtalksms.databases.SMSHelper;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppMsg;
import com.googlecode.gtalksms.xmpp.XmppMuc;

public class SmsCmd extends CommandHandlerBase {
    private static boolean sSentIntentReceiverRegistered = false;
    private static boolean sDelIntentReceiverRegistered = false;
    private static BroadcastReceiver sSentSmsReceiver = null;
    private static BroadcastReceiver sDeliveredSmsReceiver = null;
    private static Integer sSmsID;

    // synchronizedMap because the worker thread and the intent receivers work with this map
    private static Map<Integer, Sms> mSmsMap;

    private ContactsResolver mContactsResolver;
    private SmsManager mSmsManager;

    private AliasHelper mAliasHelper;
    private KeyValueHelper mKeyValueHelper;
    private SMSHelper mSmsHelper;

    public SmsCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_MESSAGE, "SMS", new Cmd("sms", "s"), new Cmd("reply", "r"), 
                new Cmd("findsms", "fs"), new Cmd("markasread", "mar"), new Cmd("chat", "c"), new Cmd("delsms"));
    }

    @Override
    protected void onCommandActivated() {
        mSmsManager = new SmsManager(sSettingsMgr, sContext);
        mSmsHelper = SMSHelper.getSMSHelper(sContext);
        mAliasHelper = AliasHelper.getAliasHelper(sContext);
        mKeyValueHelper = KeyValueHelper.getKeyValueHelper(sContext);
        mContactsResolver = ContactsResolver.getInstance(sContext);

        restoreSmsInformation();

        if (sSettingsMgr.notifySmsSent && !sSentIntentReceiverRegistered) {
            if (sSentSmsReceiver == null) {
                sSentSmsReceiver = new SentIntentReceiver(sMainService, mSmsMap, mSmsHelper);
            }
            sMainService.registerReceiver(sSentSmsReceiver, new IntentFilter(MainService.ACTION_SMS_SENT));
            sSentIntentReceiverRegistered = true;
        }
        if (sSettingsMgr.notifySmsDelivered && !sDelIntentReceiverRegistered) {
            if (sDeliveredSmsReceiver == null) {
                sDeliveredSmsReceiver = new DeliveredIntentReceiver(sMainService, mSmsMap, mSmsHelper);
            }
            sMainService.registerReceiver(sDeliveredSmsReceiver, new IntentFilter(MainService.ACTION_SMS_DELIVERED));
            sDelIntentReceiverRegistered = true;
        }
    }

    @Override
    protected void onCommandDeactivated() {
        mSmsManager = null;
        mSmsHelper = null;
        mAliasHelper = null;
        mKeyValueHelper = null;
        mContactsResolver = null;

        if (sSentSmsReceiver != null && sSentIntentReceiverRegistered) {
            sContext.unregisterReceiver(sSentSmsReceiver);
            sSentIntentReceiverRegistered = false;
        }
        if (sDeliveredSmsReceiver != null && sDelIntentReceiverRegistered) {
            sContext.unregisterReceiver(sDeliveredSmsReceiver);
            sDelIntentReceiverRegistered = false;
        }
    }

    @Override
    protected void execute(Command c) {
        String arg1 = c.getArg1();
        String arg2 = c.getAllArg2();

        if (isMatchingCmd(c, "sms")) {
            // If there is a message, send it.
            if (arg2.length() > 0) {
                sendSMS(arg2, arg1);
            } else if (c.getArg1().equals("unread")) {
                readUnreadSMS();
            } else if (arg1.length() > 0) {
                readSMS(arg1);
            } else {
                readLastSMS();
            }
        } else if (isMatchingCmd(c, "findsms")) {
            searchSMS(arg2, mAliasHelper.convertAliasToNumber(arg1));
        } else if (isMatchingCmd(c, "markasread")) {
            if (arg1.length() > 0) {
                markSmsAsRead(arg1);
            } else if (RecipientCmd.getLastRecipientNumber() == null) {
                send(R.string.chat_error_no_recipient);
            } else {
                markSmsAsReadByNumber(RecipientCmd.getLastRecipientNumber(), RecipientCmd.getLastRecipientName());
            }
        } else if (isMatchingCmd(c, "chat")) {
            if (arg1.length() > 0) {
                inviteRoom(arg1);
            } else if (RecipientCmd.getLastRecipientNumber() != null) {
                try {
                    XmppMuc.getInstance(sContext).inviteRoom(RecipientCmd.getLastRecipientNumber(),
                            RecipientCmd.getLastRecipientName(), XmppMuc.MODE_SMS);
                } catch (Exception e) {
                    // Creation of chat with last recipient failed
                    send(R.string.chat_error, e.getLocalizedMessage());
                }
            }
        } else if (isMatchingCmd(c, "delsms")) {
            if (arg1.length() == 0) {
                send(R.string.chat_del_sms_syntax);
            } else {
                String search = null;
                if (arg2.length() > 0) {
                    search = mAliasHelper.convertAliasToNumber(arg2);
                }
                deleteSMS(arg1, search);
            }
        } else if (isMatchingCmd(c, "reply")) {
            arg1 = c.getAllArg1();
            if (arg1.length() == 0) {
                executeNewCmd("recipient");
            } else if (RecipientCmd.getLastRecipientNumber() == null) {
                send(R.string.chat_error_no_recipient);
            } else {
                if (sSettingsMgr.markSmsReadOnReply) {
                    mSmsManager.markAsRead(RecipientCmd.getLastRecipientNumber());
                }
                sendSMS(arg1, RecipientCmd.getLastRecipientNumber());
            }
        }
    }

    /**
     * "delsms" cmd deletes sms, either 
     * - all sms 
     * - all sent sms 
     * - sms from specified contact 
     * - #last messages 
     * - #last incoming/outgoing messages
     * 
     * @param cmd
     *            - all, sent, contact
     * @param search
     *            - if cmd == contact the name of the contact
     */
    private void deleteSMS(String cmd, String search) {
        int nbDeleted = -2;
        if (cmd.equals("all")) {
            nbDeleted = mSmsManager.deleteAllSms();
        } else if (cmd.equals("sent")) {
            nbDeleted = mSmsManager.deleteSentSms();
        } else if (cmd.startsWith("last")) {
            Integer number = Tools.parseInt(search);
            if (number == null) {
                number = 1;
            }

            if (cmd.equals("last")) {
                nbDeleted = mSmsManager.deleteLastSms(number);
            } else if (cmd.equals("lastin")) {
                nbDeleted = mSmsManager.deleteLastInSms(number);
            } else if (cmd.equals("lastout")) {
                nbDeleted = mSmsManager.deleteLastOutSms(number);
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
                nbDeleted = mSmsManager.deleteSmsByContact(contact.rawIds);
            } else {
                send(R.string.chat_no_match_for, search);
            }
        } else if (cmd.equals("number") && search != null) {
            send(R.string.chat_del_sms_from, search);
            nbDeleted = mSmsManager.deleteSmsByNumber(search);
            if (nbDeleted <= 0) {
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
     * create a MUC with the specified contact and invites the user in case the contact isn't distinct the user is
     * informed
     * 
     * @param contactInformation
     */
    private void inviteRoom(String contactInformation) {
        ResolvedContact res = mContactsResolver.resolveContact(contactInformation, ContactsResolver.TYPE_CELL);

        if (res == null) {
            send(R.string.chat_no_match_for, contactInformation);
        } else if (res.isDistinct()) {
            try {
                XmppMuc.getInstance(sContext).inviteRoom(res.getNumber(), res.getName(), XmppMuc.MODE_SMS);
            } catch (Exception e) {
                send(R.string.chat_error, e.getLocalizedMessage());
            }
        } else if (!res.isDistinct()) {
            askForMoreDetails(res.getCandidates());
        }
    }

    /**
     * Search for SMS Mesages and sends them back to the user
     * 
     * @param message
     * @param contactName
     *            - optional, may be null
     */
    private void searchSMS(String message, String contactName) {
        ArrayList<Contact> contacts;

        send(R.string.chat_sms_search_start);
        contacts = ContactsManager.getMatchingContacts(sContext, contactName);

        if (contacts.size() > 0) {
            send(R.string.chat_sms_search, message, contacts.size());

            for (Contact contact : contacts) {
                ArrayList<Sms> smsList = mSmsManager.getSms(ContactsManager.getPhones(sContext, contact.ids), message);
                Collections.sort(smsList);

                if (smsList.size() > 0) {
                    sendSmsListOnXmpp(smsList, contact.name, null);
                }
            }
            send("Done");

        } else {
            send(R.string.chat_no_match_for, message);
        }
    }

    /**
     * Appends an SMS to an XmppMsg with formatting does not send the XmppMsg!
     * 
     * @param msg
     * @param sms
     */
    private static void appendSMS(XmppMsg msg, Sms sms) {
        msg.append(DateFormat.getDateTimeInstance().format(sms.getDate()) + " - ");
        msg.appendBold(sms.getSender());
        msg.append(" --> ");
        msg.appendBoldLine(sms.getReceiver());
        msg.appendLine(sms.getMessage());
        msg.appendLine("");
    }

    /**
     * Sends an SMS Message returns an error to the user if the contact could not be found
     * 
     * @param message
     *            the message to send
     * @param contactInformation
     */
    private void sendSMS(String message, String contactInformation) {
        ResolvedContact rc = mContactsResolver.resolveContact(contactInformation, ContactsResolver.TYPE_CELL);

        if (rc == null) {
            send(R.string.chat_no_match_for, contactInformation);
        } else if (rc.isDistinct()) {
            sendSMSByPhoneNumber(message, rc.getNumber(), rc.getName());
        } else if (!rc.isDistinct()) {
            askForMoreDetails(rc.getCandidates());
        }
    }

    /**
     * Marks all SMS from a given contact or number read
     *
     */
    private void markSmsAsRead(String contactInformation) {
        ResolvedContact rc = mContactsResolver.resolveContact(contactInformation, ContactsResolver.TYPE_CELL);
        if (rc == null) {
            // this is a special case, where the user wants to mark a message
            // with a named-operator string as read, see issue 149
            if (mSmsManager.markAsRead(contactInformation)) {
                send(R.string.chat_mark_as_read, contactInformation);
            } else {
                send(R.string.chat_no_match_for, contactInformation);
            }
        } else if (rc.isDistinct()) {
            markSmsAsReadByNumber(rc.getNumber(), rc.getName());
        } else {
            askForMoreDetails(rc.getCandidates());
        }
    }

    /**
     * Marks all SMS from the given number a read.
     * 
     * @param number
     *            The (cell) phone number, do not provide a contact name here!
     */
    private void markSmsAsReadByNumber(String number, String name) {
        if (!mSmsManager.markAsRead(number)) {
            send("Error marking SMS as read from number: " + number);
        } else {
            // Inform the user about the success
            if (name != null) {
                send(R.string.chat_mark_as_read, name);
            } else {
                send(R.string.chat_mark_as_read, number);
            }
        }
    }

    /**
     * reads (count) SMS from all contacts matching pattern
     * 
     * @param namePattern
     */
    private void readSMS(String namePattern) {
        // We do not use the ContactsResolver here because readSMS() has a slightly different behavior when searching for contacts
        namePattern = mAliasHelper.convertAliasToNumber(namePattern);
        ArrayList<Contact> contacts = ContactsManager.getMatchingContacts(sContext, namePattern);

        if (contacts.size() > 0) {
            XmppMsg noSms = new XmppMsg();
            boolean hasMatch = false;
            for (Contact contact : contacts) {
                // Looking for conversations for all phone numbers of the contact
                ArrayList<Sms> smsList = mSmsManager.getSms(ContactsManager.getPhones(sContext, contact.ids));
                Collections.sort(smsList);

                if (smsList.size() > 0) {
                    hasMatch = true;
                    sendSmsListOnXmpp(smsList, contact.name, smsList.size() < sSettingsMgr.smsNumber ? getString(R.string.chat_only_got_n_sms, smsList.size()) : null);
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
            send(R.string.chat_no_match_for, namePattern);
        }
    }

    /** reads unread SMS from all contacts */
    private void readUnreadSMS() {
        ArrayList<Sms> smsList = mSmsManager.getLastUnreadSms();
        Collections.sort(smsList);

        XmppMsg allSms = new XmppMsg();

        if (smsList.size() > 0) {
            sendSmsListOnXmpp(smsList, null, null);
        } else {
            allSms.appendLine(getString(R.string.chat_no_sms));
        }
        send(allSms);
    }

    /** reads last (count) SMS from all contacts */
    private void readLastSMS() {
        ArrayList<Sms> smsList = mSmsManager.getLastSms();
        Collections.sort(smsList);

        if (smsList.size() > 0) {
            sendSmsListOnXmpp(smsList, null, null);
        } else {
            send(R.string.chat_no_sms);
        }
    }
    
    /** Helper to send messages via xmpp according to the settings */
    private void sendSmsListOnXmpp(ArrayList<Sms> smsList, String preMsg, String postMsg) {
        XmppMsg message = new XmppMsg();
        if (sSettingsMgr.smsReplySeparate) {
            if (preMsg != null) {
                message.appendBold(preMsg);
                sendAndClear(message);
            }
            for (Sms sms : smsList) {
                appendSMS(message, sms);
                sendAndClear(message);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {}
            }
            if (postMsg != null) {
                message.appendItalicLine(postMsg);
                sendAndClear(message);
            }
        } else {
            if (preMsg != null) {
                message.appendBoldLine(preMsg);
            }
            for (Sms sms : smsList) {
                appendSMS(message, sms);
            }
            if (postMsg != null) {
                message.appendItalicLine(postMsg);
            }
            send(message);
        }
    }

    /**
     * Sends a sms to the specified phone number with a custom receiver name Creates the pendingIntents for
     * send/delivery notifications, if needed. Adds the sent SMS to the systems SentBox
     * 
     * @param message
     * @param phoneNumber
     * @param toName
     */
    private void sendSMSByPhoneNumber(String message, String phoneNumber, String toName) {
        if (sSettingsMgr.notifySmsSent) {
            send(R.string.chat_send_sms, toName + " (" + phoneNumber + ")" + ": \"" + Tools.shortenMessage(message) + "\"");
        }

        if (sSettingsMgr.markSmsReadOnReply) {
            mSmsManager.markAsRead(phoneNumber);
        }

        ArrayList<PendingIntent> sentPendingIntents = new ArrayList<PendingIntent>();
        ArrayList<PendingIntent> deliveredPendingIntents = new ArrayList<PendingIntent>();
        android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
        ArrayList<String> messages = smsManager.divideMessage(message);

        if (sSettingsMgr.notifySmsSentDelivered) {
            String shortenedMessage = Tools.shortenMessage(message);
            Integer smsID = getSmsID();
            Sms s = new Sms(phoneNumber, toName, shortenedMessage, messages.size(), mAnswerTo, smsID);
            mSmsMap.put(smsID, s);
            mSmsHelper.addSMS(s);
            if (sSettingsMgr.notifySmsSent) {
                Log.i("SmsCmd sendSMSByPhoneNumber() - creating SentPendingIntents");
                sentPendingIntents = createSPendingIntents(messages.size(), smsID);
            }
            if (sSettingsMgr.notifySmsDelivered) {
                Log.i("SmsCmd sendSMSByPhoneNumber() - creating DeliveredPendingIntents");
                deliveredPendingIntents = createDPendingIntents(messages.size(), smsID);
            }
        }

        smsManager.sendMultipartTextMessage(phoneNumber, null, messages, sentPendingIntents, deliveredPendingIntents);
        RecipientCmd.setLastRecipient(phoneNumber);
        mSmsManager.addSmsToSentBox(message, phoneNumber);
    }

    private ArrayList<PendingIntent> createSPendingIntents(int size, int smsID) {
        ArrayList<PendingIntent> pendingIntents = new ArrayList<PendingIntent>();
        int startSIntentNumber = getSIntentStart(size);
        for (int i = 0; i < size; i++) {
            int p = startSIntentNumber++;
            Intent sentIntent = new Intent(MainService.ACTION_SMS_SENT);
            sentIntent.putExtra("partNum", i);
            sentIntent.putExtra("smsID", smsID);
            PendingIntent sentPenIntent = PendingIntent.getBroadcast(sContext, p, sentIntent, PendingIntent.FLAG_ONE_SHOT);
            pendingIntents.add(sentPenIntent);
        }
        return pendingIntents;
    }

    private ArrayList<PendingIntent> createDPendingIntents(int size, int smsID) {
        ArrayList<PendingIntent> pendingIntents = new ArrayList<PendingIntent>();
        int startDIntentNumber = getDIntentStart(size);
        for (int i = 0; i < size; i++) {
            int p = startDIntentNumber++;
            Intent deliveredIntent = new Intent(MainService.ACTION_SMS_DELIVERED);
            deliveredIntent.putExtra("partNum", i);
            deliveredIntent.putExtra("smsID", smsID);
            PendingIntent deliveredPenIntent = PendingIntent.getBroadcast(sContext, p, deliveredIntent, PendingIntent.FLAG_ONE_SHOT);
            pendingIntents.add(deliveredPenIntent);
        }
        return pendingIntents;
    }

    /**
     * Restores the SMS information from the database Creates the smsMap object and fills it if there are any old SMS
     * from the database
     */
    private void restoreSmsInformation() {
        if (sSmsID == null) {
            sSmsID = mKeyValueHelper.getIntegerValue(KeyValueHelper.KEY_SMS_ID);
            // This is the first time the method was called, init the values
            if (sSmsID == null) {
                mKeyValueHelper.addKey(KeyValueHelper.KEY_SMS_ID, "0");
                mKeyValueHelper.addKey(KeyValueHelper.KEY_SINTENT, "0");
                mKeyValueHelper.addKey(KeyValueHelper.KEY_DINTENT, "0");
                sSmsID = 0;
            }
            mSmsMap = Collections.synchronizedMap(new HashMap<Integer, Sms>());
            mSmsHelper.deleteOldSMS();
            Sms[] toAdd = mSmsHelper.getFullDatabase();
            for (Sms s : toAdd) {
                mSmsMap.put(s.getID(), s);
            }
        }
    }

    private Integer getSmsID() {
        int res = sSmsID;
        sSmsID++;
        mKeyValueHelper.addKey(KeyValueHelper.KEY_SMS_ID, sSmsID.toString());
        return res;
    }

    private int getSIntentStart(int size) {
        Integer res = mKeyValueHelper.getIntegerValue(KeyValueHelper.KEY_SINTENT);
        Integer newValue = res + size;
        mKeyValueHelper.addKey(KeyValueHelper.KEY_SINTENT, newValue.toString());
        return res;
    }

    private int getDIntentStart(int size) {
        Integer res = mKeyValueHelper.getIntegerValue(KeyValueHelper.KEY_DINTENT);
        Integer newValue = res + size;
        mKeyValueHelper.addKey(KeyValueHelper.KEY_DINTENT, newValue.toString());
        return res;
    }

    @Override
    protected void initializeSubCommands() {
        Cmd sms = mCommandMap.get("sms");
        sms.setHelp(R.string.chat_help_sms_show_all, null);
        sms.AddSubCmd("unread", R.string.chat_help_sms_show_unread, null);
        sms.AddSubCmd("#contact#", R.string.chat_help_sms_show_contact, null);
        sms.AddSubCmd("#contact#", R.string.chat_help_sms_send, "#message#");

        Cmd fs = mCommandMap.get("findsms");
        fs.setHelp(R.string.chat_help_find_sms_all, "#message#");
        fs.AddSubCmd("#contact#", R.string.chat_help_find_sms, "#message#");

        Cmd del = mCommandMap.get("delsms");
        del.setHelp(R.string.chat_help_del_sms, null);
        del.AddSubCmd("all", R.string.chat_help_del_sms_all, null);
        del.AddSubCmd("sent", R.string.chat_help_del_sms_sent, null);
        del.AddSubCmd("last", R.string.chat_help_del_sms_last, "#number#");
        del.AddSubCmd("lastin", R.string.chat_help_del_sms_last_in, "#number#");
        del.AddSubCmd("lastout", R.string.chat_help_del_sms_last_out, "#number#");
        del.AddSubCmd("contact", R.string.chat_help_del_sms_contact, "#contact#");
        del.AddSubCmd("number", R.string.chat_help_del_sms_number, "#phone_number#");

        mCommandMap.get("reply").setHelp(R.string.chat_help_sms_reply, "#message#");
        mCommandMap.get("chat").setHelp(R.string.chat_help_sms_chat, "#contact#");
        mCommandMap.get("markasread").setHelp(R.string.chat_help_mark_as_read, "#contact#");
    }
}
