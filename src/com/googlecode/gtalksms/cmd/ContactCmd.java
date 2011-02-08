package com.googlecode.gtalksms.cmd;

import java.util.ArrayList;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.data.contacts.Contact;
import com.googlecode.gtalksms.data.contacts.ContactAddress;
import com.googlecode.gtalksms.data.contacts.ContactsManager;
import com.googlecode.gtalksms.data.phone.Phone;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class ContactCmd extends Command {
    public ContactCmd(MainService mainService) {
        super(mainService, new String[] {"contact"});
    }
   
    @Override
    public void execute(String cmd, String searchedText) {
    
        ArrayList<Contact> contacts = ContactsManager.getMatchingContacts(_context, searchedText);

        if (contacts.size() > 0) {

            if (contacts.size() > 1) {
                send(getString(R.string.chat_contact_found, contacts.size(), searchedText));
            }

            for (Contact contact : contacts) {
                XmppMsg strContact = new XmppMsg();
                strContact.appendBoldLine(contact.name);

                // strContact.append(Tools.LineSep + "Id : " + contact.id);
                // strContact.append(Tools.LineSep + "Raw Ids : " + TextUtils.join(" ",
                // contact.rawIds));

                ArrayList<Phone> mobilePhones = ContactsManager.getPhones(_context, contact.id);
                if (mobilePhones.size() > 0) {
                    strContact.appendItalicLine(getString(R.string.chat_phones));
                    for (Phone phone : mobilePhones) {
                        strContact.appendLine(phone.label + " - " + phone.cleanNumber);
                    }
                }

                ArrayList<ContactAddress> emails = ContactsManager.getEmailAddresses(_context, contact.id);
                if (emails.size() > 0) {
                    strContact.appendItalicLine(getString(R.string.chat_emails));
                    for (ContactAddress email : emails) {
                        strContact.appendLine(email.label + " - " + email.address);
                    }
                }

                ArrayList<ContactAddress> addresses = ContactsManager.getPostalAddresses(_context, contact.id);
                if (addresses.size() > 0) {
                    strContact.appendItalicLine(getString(R.string.chat_addresses));
                    for (ContactAddress address : addresses) {
                        strContact.appendLine(address.label + " - " + address.address);
                    }
                }
                send(strContact);
            }
        } else {
            send(getString(R.string.chat_no_match_for, searchedText));
        }
    }
}
