package com.googlecode.gtalksms.cmd;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.cmd.recipientCmd.SetLastRecipientRunnable;
import com.googlecode.gtalksms.data.contacts.ContactsManager;
import com.googlecode.gtalksms.databases.KeyValueHelper;

public class RecipientCmd extends CommandHandlerBase {

    private static String sLastRecipientNumber = null;
    private static String sLastRecipientName = null;
    private static RecipientCmd sRecipientCmd;

    private SetLastRecipientRunnable setLastRecipientRunnable;
    private KeyValueHelper mKeyValueHelper;

    public RecipientCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_MESSAGE, "Recipient", new Cmd("recipient", "re"));
    }

    @Override
    protected void onCommandActivated() {
        mKeyValueHelper = KeyValueHelper.getKeyValueHelper(sContext);
        restoreLastRecipient();
        sRecipientCmd = this;
    }

    @Override
    protected void onCommandDeactivated() {
        mKeyValueHelper = null;
        sRecipientCmd = null;
    }

    @Override
    protected void execute(Command cmd) {
        if (isMatchingCmd(cmd, "recipient")) {
            displayLastRecipient(true);
        }
    }
    
    public static String getLastRecipientNumber() {
        return sLastRecipientNumber;
    }
    
    public static String getLastRecipientName() {
        return sLastRecipientName;
    }
    
    public static void setLastRecipient(String phoneNumber) {
        if (sRecipientCmd != null) {
            sRecipientCmd.setLastRecipientInternal(phoneNumber);
        }
    }

    private void setLastRecipientInternal(String phoneNumber) {
		SetLastRecipientRunnable slrRunnable = new SetLastRecipientRunnable(this, phoneNumber, sSettingsMgr, sContext);
        if (setLastRecipientRunnable != null) {
            setLastRecipientRunnable.setOutdated();
        }
        setLastRecipientRunnable = slrRunnable;
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
     * @param silentAndUpdate If true, don't sent a message to the user and don't update the KV-DB
     */
    public synchronized void setLastRecipientNow(String phoneNumber, boolean silentAndUpdate) {
        mAnswerTo = null;
        if (sLastRecipientNumber == null || !phoneNumber.equals(sLastRecipientNumber)) {
            sLastRecipientNumber = phoneNumber;
            sLastRecipientName = ContactsManager.getContactName(sContext, phoneNumber);
            if (!silentAndUpdate) { 
                displayLastRecipient(false);
                mKeyValueHelper.addKey(KeyValueHelper.KEY_LAST_RECIPIENT, phoneNumber);
            }
        }
    }

    private void displayLastRecipient(boolean useAnswerTo) {
        if (sLastRecipientNumber == null) {
            send(R.string.chat_error_no_recipient);
        } else {
            String msg = getString(R.string.chat_reply_contact, ContactsManager.getContactName(sContext, sLastRecipientNumber));
            if (useAnswerTo) {
                send(msg);
            } else {
                send(msg, null);
            }
        }
    }

    /**
     * restores the lastRecipient from the database if possible
     */
    private void restoreLastRecipient() {
        String phoneNumber = mKeyValueHelper.getValue(KeyValueHelper.KEY_LAST_RECIPIENT);
        if (phoneNumber != null) {
            setLastRecipientNow(phoneNumber, true);
        }
    }

    @Override
    protected void initializeSubCommands() {
        Cmd re = mCommandMap.get("recipient");
        re.setHelp(R.string.chat_help_recipient, null);
    }
}
