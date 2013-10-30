package com.googlecode.gtalksms.cmd;

import java.util.ArrayList;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.data.contacts.Contact;
import com.googlecode.gtalksms.data.contacts.ContactAddress;
import com.googlecode.gtalksms.data.contacts.ContactsManager;
import com.googlecode.gtalksms.data.phone.Phone;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class ContactCmd extends CommandHandlerBase {
    public ContactCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_CONTACTS, "Contact", new Cmd("contact"));
    }
   
    @Override
    protected void execute(Command cmd) {
        String searchedText = cmd.getAllArg1();
        ArrayList<Contact> contacts = ContactsManager.getMatchingContacts(sContext, searchedText);

        if (contacts.size() > 0) {
            XmppMsg strContact = new XmppMsg();

            if (contacts.size() > 1) {
                strContact.appendLine(getString(R.string.chat_contact_found, contacts.size(), searchedText));
            }
            
            for (Contact contact : contacts) {
                strContact.appendBoldLine(contact.name);

                ArrayList<Phone> mobilePhones = ContactsManager.getPhones(sContext, contact.ids);
                if (mobilePhones.size() > 0) {
                    strContact.appendItalicLine(getString(R.string.chat_phones));
                    for (Phone phone : mobilePhones) {
                        strContact.append(phone.getLabel() + " - " + phone.getCleanNumber());
                        // append an astrix to mark the default number
                        if (phone.isDefaultNumber()) {
                            strContact.appendBold(" *");
                        }
                        strContact.newLine();
                    }
                }

                ArrayList<ContactAddress> emails = ContactsManager.getEmailAddresses(sContext, contact.ids);
                if (emails.size() > 0) {
                    strContact.appendItalicLine(getString(R.string.chat_emails));
                    for (ContactAddress email : emails) {
                        strContact.appendLine((email.label != null ? email.label + " - " : "") + email.address);
                    }
                }

                ArrayList<ContactAddress> addresses = ContactsManager.getPostalAddresses(sContext, contact.ids);
                if (addresses.size() > 0) {
                    strContact.appendItalicLine(getString(R.string.chat_addresses));
                    for (ContactAddress address : addresses) {
                        strContact.appendLine((address.label != null ? address.label + " - " : "") + address.address);
                    }
                }
            }
            send(strContact);
        } else {
            send(R.string.chat_no_match_for, searchedText);
        }
    }

    @Override
    protected void onCommandActivated() {
    }

    @Override
    protected void onCommandDeactivated() {
    }

    @Override
    protected void initializeSubCommands() {
        mCommandMap.get("contact").setHelp(R.string.chat_help_contact, "#contact#");        
    }
}
