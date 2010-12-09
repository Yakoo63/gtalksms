package com.googlecode.gtalksms.contacts;

import java.util.ArrayList;
import java.util.Collections;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts;
import android.provider.Contacts.People;

import com.googlecode.gtalksms.Tools;
import com.googlecode.gtalksms.XmppService;

public class ContactsManager {

    /**
     * Tries to get the contact display name of the specified phone number.
     * If not found, returns the argument.
     */
    public static String getContactName (String phoneNumber) {
        String res;
        if (phoneNumber != null) {
            res = phoneNumber;
            ContentResolver resolver = XmppService.getInstance().getContentResolver();
            String[] projection = new String[] {
                    Contacts.Phones.DISPLAY_NAME,
                    Contacts.Phones.NUMBER };
            Uri contactUri = Uri.withAppendedPath(Contacts.Phones.CONTENT_FILTER_URL, Uri.encode(phoneNumber));
            Cursor c = resolver.query(contactUri, projection, null, null, null);
            if (c.moveToFirst()) {
                String name = c.getString(c.getColumnIndex(Contacts.Phones.DISPLAY_NAME));
                res = name;
            }
        } else {
            res = "[hidden number]";
        }
        return res;
    }

    /**
     * Returns a ArrayList of <Contact> where the names/company match the argument
     */
    public static ArrayList<Contact> getMatchingContacts(String searchedName) {
        ArrayList<Contact> res = new ArrayList<Contact>();
        if (Phone.isCellPhoneNumber(searchedName)) {
            searchedName = getContactName(searchedName);
        }

        if (!searchedName.equals("")) {
            ContentResolver resolver = XmppService.getInstance().getContentResolver();
            String[] projection = new String[] {
                    Contacts.People._ID,
                    Contacts.People.NAME
                    };
            Uri contactUri = Uri.withAppendedPath(People.CONTENT_FILTER_URI, Uri.encode(searchedName));
            Cursor c = resolver.query(contactUri, projection, null, null, null);
            for (boolean hasData = c.moveToFirst() ; hasData ; hasData = c.moveToNext()) {
                Long id = Tools.getLong(c, People._ID);
                if (null != id) {
                    String contactName = Tools.getString(c, People.NAME);
                    if(null != contactName) {
                        Contact contact = new Contact();
                        contact.id = id;
                        contact.name = contactName;
                        res.add(contact);
                    }
                }
            }
            c.close();
        }
        Collections.sort(res);
        return res;
    }

    /**
     * Returns a ArrayList of <ContactAddress> containing postal addresses which match to contact id
     */
    public static ArrayList<ContactAddress> getPostalAddresses(Long contactId) {
        return getAddresses(contactId, Contacts.KIND_POSTAL);
    }

    /**
     * Returns a ArrayList of <ContactAddress> containing email addresses which match to contact id
     */
    public static ArrayList<ContactAddress> getEmailAddresses(Long contactId) {
        return getAddresses(contactId, Contacts.KIND_EMAIL);
    }

    /**
     * Returns a ArrayList of <ContactAddress> which match to contact id
     */
    public static ArrayList<ContactAddress> getAddresses(Long contactId, int kind) {
        ArrayList<ContactAddress> res = new ArrayList<ContactAddress>();
        XmppService xmpp = XmppService.getInstance();
        if(null != contactId) {

            String addrWhere = Contacts.ContactMethods.PERSON_ID + " = " + contactId + " and " +
                               Contacts.ContactMethodsColumns.KIND + " = " + kind;
            Cursor c = xmpp.getContentResolver().query(Contacts.ContactMethods.CONTENT_URI,
                        null, addrWhere, null, null);
            while(c.moveToNext()) {

                String label = Tools.getString(c,Contacts.ContactMethodsColumns.LABEL);
                int type = Tools.getLong(c,Contacts.ContactMethodsColumns.TYPE).intValue();

                if (label == null || label.compareTo("") != 0) {
                    label = Contacts.ContactMethods.getDisplayLabel(xmpp.getBaseContext(), kind, type, "").toString();
                }

                ContactAddress a = new ContactAddress();
                a.address = Tools.getString(c, Contacts.ContactMethodsColumns.DATA);
                a.label = label;
                res.add(a);
            }
            c.close();
        }
        return res;
    }

    /**
     * Returns a ArrayList < Phone > of a specific contact
     * ! phone.contactName not set
     */
    public static ArrayList<Phone> getPhones(Long contactId) {
        ArrayList<Phone> res = new ArrayList<Phone>();

        Uri personUri = ContentUris.withAppendedId(People.CONTENT_URI, contactId);
        Uri phonesUri = Uri.withAppendedPath(personUri, People.Phones.CONTENT_DIRECTORY);
        String[] proj = new String[] {Contacts.Phones.NUMBER, Contacts.Phones.LABEL, Contacts.Phones.TYPE};
        Cursor c = XmppService.getInstance().getContentResolver().query(phonesUri, proj, null, null, null);

        for (boolean hasData = c.moveToFirst() ; hasData ; hasData = c.moveToNext()) {
            String number = Tools.getString(c,Contacts.Phones.NUMBER);

            String label = Tools.getString(c,Contacts.Phones.LABEL);
            int type = Tools.getLong(c,Contacts.Phones.TYPE).intValue();

            if (label == null || label.compareTo("") != 0) {
                label = Contacts.Phones.getDisplayLabel(XmppService.getInstance().getBaseContext(), type, "").toString();
            }

            Phone phone = new Phone();
            phone.number = number;
            phone.cleanNumber = Phone.cleanPhoneNumber(phone.number);
            phone.isCellPhoneNumber = Phone.isCellPhoneNumber(phone.number);
            phone.label = label;
            phone.type = type;

            res.add(phone);
        }
        return res;
    }

    /**
     * Returns a ArrayList < Phone >
     * with all matching phones for the argument
     */
    public static ArrayList<Phone> getPhones(String searchedText) {
        ArrayList<Phone> res = new ArrayList<Phone>();
        if (Phone.isCellPhoneNumber(searchedText)) {
            Phone phone = new Phone();
            phone.number = searchedText;
            phone.cleanNumber = Phone.cleanPhoneNumber(phone.number);
            phone.contactName = getContactName(searchedText);
            phone.isCellPhoneNumber = true;
            phone.type = Contacts.Phones.TYPE_MOBILE;

            res.add(phone);
        } else {
            // get the matching contacts, dictionary of < id, names >
            ArrayList<Contact> contacts = getMatchingContacts(searchedText);
            if (contacts.size() > 0) {
                for (Contact contact : contacts) {
                    ArrayList<Phone> phones = getPhones(contact.id);
                    for (Phone phone : phones) {
                        phone.contactName = getContactName(contact.name);
                        res.add(phone);
                    }
                }
            }
        }
        return res;
    }

    /**
     * Returns a ArrayList < Phone >
     * with all matching mobile phone for the argument
     */
    public static ArrayList<Phone> getMobilePhones(String searchedText) {
        ArrayList<Phone> res = new ArrayList<Phone>();
        ArrayList<Phone> phones = getPhones(searchedText);

        for (Phone phone : phones) {
            if (phone.type == Contacts.Phones.TYPE_MOBILE) {
                res.add(phone);
            }
        }

        // manage all phones number
        if (res.size() == 0) {
            for (Phone phone : phones) {
                res.add(phone);
            }
        }

        return res;
    }
}
