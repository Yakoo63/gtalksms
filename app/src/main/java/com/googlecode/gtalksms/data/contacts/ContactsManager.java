package com.googlecode.gtalksms.data.contacts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;

import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.data.phone.Phone;
import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.tools.StringFmt;
import com.googlecode.gtalksms.tools.Tools;

public class ContactsManager {
    // constants used by getMatchingContacts(Context, String)
    private static final String[] projection = { Contacts._ID, Contacts.DISPLAY_NAME };
    private static final String sortOrder = ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC";
    
    public static String getContactNameOrNull(Context ctx, String phoneNumber) {
        String res = null;
        try {
            ContentResolver resolver = ctx.getContentResolver();
            Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
            Cursor c = resolver.query(uri, new String[]{PhoneLookup.DISPLAY_NAME}, null, null, null);

            if (c != null) {
                if (c.moveToFirst()) {
                    res = Tools.getString(c, CommonDataKinds.Phone.DISPLAY_NAME);
                }
                c.close();
            }
        } catch (Exception ex) {
            /* Ignore */
        }        
        return res;
    }

    /**
     * Tries to get the contact's display name of the specified phone number.
     * If not found, returns the argument. If there is an error or phoneNumber
     * is null, R.string.chat_call_hidden will be returned.
     */
    public static String getContactName (Context ctx, String phoneNumber) {
    	String res;
        if (phoneNumber != null) {
            try {
                res = phoneNumber;
                ContentResolver resolver = ctx.getContentResolver();
                Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
                Cursor c = resolver.query(uri, new String[]{PhoneLookup.DISPLAY_NAME}, null, null, null);
    
                if (c != null) {
                    if (c.moveToFirst()) {
                        res = Tools.getString(c, CommonDataKinds.Phone.DISPLAY_NAME);
                        if (SettingsManager.getSettingsManager(ctx).displayContactNumber) {
                            res += " " + Phone.cleanPhoneNumber(phoneNumber);
                        }
                    }
                    c.close();
                }
            } catch (Exception ex) {
              Log.e("getContactName error: Phone number = " + phoneNumber, ex);
              res = ctx.getString(R.string.chat_call_hidden);
            }
        } else {
            res = ctx.getString(R.string.chat_call_hidden);
        }
        return res;
    }

    /**
     * Tries to get the contact display name of the specified phone number.
     * If not contact could be found null is returned.
     */
    public static String getContactName(Context ctx, long rawId) {
        String res = null;
        
        ContentResolver resolver = ctx.getContentResolver();
        Cursor c = resolver.query(RawContacts.CONTENT_URI,
                new String[]{RawContacts.CONTACT_ID},
                RawContacts._ID + "=?",
                new String[]{String.valueOf(rawId)}, null);
        
        long id = -1;
        if (c != null) {
            if (c.moveToFirst()) {
                id = Tools.getLong(c, RawContacts.CONTACT_ID);
            }
            c.close();
        }
        
        c = resolver.query(Contacts.CONTENT_URI,
                new String[]{Contacts.DISPLAY_NAME},
                RawContacts._ID + "=?",
                new String[]{String.valueOf(id)}, null);
        
        if (c != null) {
            if (c.moveToFirst()) {
                res = Tools.getString(c, Contacts.DISPLAY_NAME);
            }
            c.close();
        }
        
        return res;
    }

    /**
     * Returns all Contacts that match a given search String
     *  
     * @param ctx Application context
     * @param searchedName - the name of the contact or a phone number
     * @return a ArrayList of <Contact> where the names/company match the argument
     */
    public static ArrayList<Contact> getMatchingContacts(Context ctx, String searchedName) {
        ArrayList<Contact> res = getMatchingContacts(ctx, searchedName, true);
        if (res.size() == 0) {
            res = getMatchingContacts(ctx, searchedName, false);
        }
        return res;
    }

    private static ArrayList<Contact> getMatchingContacts(Context ctx, String searchedName, boolean isNameOnly) {
        ArrayList<Contact> res = new ArrayList<Contact>();
        HashMap<String, Contact> contacts = new HashMap<String, Contact>();
        
        if (Phone.isCellPhoneNumber(searchedName)) {
            searchedName = getContactName(ctx, searchedName);
        }

        ContentResolver resolver = ctx.getContentResolver();

        Uri contactUri = searchedName.equals("") ?
                Contacts.CONTENT_URI :
                Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, StringFmt.encodeSQL(searchedName));
        Cursor c = resolver.query(contactUri, projection, null, null, sortOrder);
        if (c != null) {
            for (boolean hasData = c.moveToFirst() ; hasData ; hasData = c.moveToNext()) {
                Long id = Tools.getLong(c, Contacts._ID);
                String contactName = Tools.getString(c, Contacts.DISPLAY_NAME);

                if (null == id || null == contactName || (isNameOnly && !contactName.toLowerCase().contains(searchedName.toLowerCase()))) {
                    continue;
                }

                Contact contact;
                if (contacts.containsKey(contactName)) {
                    contact = contacts.get(contactName);
                } else {
                    contact = new Contact();
                    contact.name = contactName;

                    res.add(contact);
                    contacts.put(contact.name, contact);
                }

                contact.ids.add(id);
                Cursor c1 = resolver.query(RawContacts.CONTENT_URI, new String[]{RawContacts._ID},
                        RawContacts.CONTACT_ID + "=?", new String[]{String.valueOf(id)}, null);
                if (c1 != null) {
                    for (boolean hasData1 = c1.moveToFirst() ; hasData1 ; hasData1 = c1.moveToNext()) {
                        contact.rawIds.add(Tools.getLong(c1, RawContacts._ID));
                    }
                    c1.close();
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
    public static ArrayList<ContactAddress> getPostalAddresses(Context ctx, ArrayList<Long> ids) {
        ArrayList<ContactAddress> res = new ArrayList<ContactAddress>();
        
        if (ids.size() > 0) {
            String where = ContactsContract.Data.CONTACT_ID + " IN (" + TextUtils.join(", ", ids) + ") AND " + ContactsContract.Data.MIMETYPE + " = ?"; 
            String[] whereParams = new String[]{ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE}; 
        
            Cursor c = ctx.getContentResolver().query(ContactsContract.Data.CONTENT_URI, 
                        null, where, whereParams, null); 
            if (c != null) {
                while(c.moveToNext()) {
                    int type = Tools.getLong(c, ContactsContract.CommonDataKinds.StructuredPostal.TYPE).intValue();
                    String label = Tools.getString(c, ContactsContract.CommonDataKinds.StructuredPostal.LABEL);

    //                String poBox        = Tools.getString(c, ContactsContract.CommonDataKinds.StructuredPostal.POBOX);
    //                String street       = Tools.getString(c, ContactsContract.CommonDataKinds.StructuredPostal.STREET);
    //                String city         = Tools.getString(c, ContactsContract.CommonDataKinds.StructuredPostal.CITY);
    //                String state        = Tools.getString(c, ContactsContract.CommonDataKinds.StructuredPostal.REGION);
    //                String postalCode   = Tools.getString(c, ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE);
    //                String country      = Tools.getString(c, ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY);

                    if (type != 0 && (label == null || label.compareTo("") != 0)) {
                        label = ContactsContract.CommonDataKinds.StructuredPostal.getTypeLabel(ctx.getResources(), type, "").toString();
                    }

                    ContactAddress a = new ContactAddress();
                    a.address = Tools.getString(c, ContactsContract.CommonDataKinds.StructuredPostal.DATA);
                    a.label = label;
                    res.add(a);
                }
                c.close();
            }
         }
        
        return res;
    }

    /**
     * Returns a ArrayList of <ContactAddress> containing email addresses which match to contact id
     * You usually want to use ContactsResolver.resolveContact()
     *      
     * @param ctx
     * @param ids
     * @return
     */
    public static ArrayList<ContactAddress> getEmailAddresses(Context ctx, ArrayList<Long> ids) {
        ArrayList<ContactAddress> res = new ArrayList<ContactAddress>();

        if (ids.size() > 0) {
            String where =  ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " IN (" + TextUtils.join(", ", ids) + ")";
            Cursor c = ctx.getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, where, null, null); 
            if (c != null) {
                while(c.moveToNext()) {
                    String label = Tools.getString(c, ContactsContract.CommonDataKinds.Email.LABEL);
                    int type = Tools.getLong(c, ContactsContract.CommonDataKinds.Email.TYPE).intValue();

                    if (type != 0 && (label == null || label.compareTo("") != 0)) {
                        label = ContactsContract.CommonDataKinds.Email.getTypeLabel(ctx.getResources(), type, "").toString();
                    }

                    ContactAddress a = new ContactAddress();
                    a.address = Tools.getString(c, ContactsContract.CommonDataKinds.Email.DATA);
                    a.label = label;
                    res.add(a);
                }
                c.close();
            }
        }
        return res;
    }

    /**
     * Returns and ArrayList<Phone> for specific contact IDs
     * 
     * @param ctx Context
     * @param ids An ArrayList with the contact IDs
     * @return An ArrayList<Phone> with all phones from the IDs
     */
    public static ArrayList<Phone> getPhones(Context ctx, ArrayList<Long> ids) {
        ArrayList<Phone> res = new ArrayList<Phone>();
        HashSet<String> phones = new HashSet<String>();
        
        if (ids.size() > 0) {
            String where =  ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " IN (" + TextUtils.join(", ", ids) + ")";
            Cursor c = ctx.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, where, null, null);
            if (c != null) {
                while (c.moveToNext()) {
                    String number = Tools.getString(c, CommonDataKinds.Phone.NUMBER);
                    String label = Tools.getString(c, CommonDataKinds.Phone.LABEL);
                    int type = Tools.getLong(c, CommonDataKinds.Phone.TYPE).intValue();
                    int super_primary = Tools.getInt(c, ContactsContract.Data.IS_SUPER_PRIMARY);

                    if (label == null || label.compareTo("") != 0) {
                        label = ContactsContract.CommonDataKinds.Phone.getTypeLabel(ctx.getResources(), type, "").toString();
                    }

                    Phone phone = new Phone(number, label, type, super_primary);

                    if (phones.add(phone.getCleanNumber())) {
                        res.add(phone);
                    } else {
                        Log.i("getPhones(ids) Duplicated phone number: " + number);
                    }
                }
                c.close();
            }
        }
             
        return res;
    }    

    /**
     * Search for all phones with owners containing searchedText
     * You usually want to use ContactsResolver.resolveContact()
     * 
     * @param ctx
     * @param searchedText
     * @return ArrayList <Phone>
     */
    public static ArrayList<Phone> getPhones(Context ctx, String searchedText) {
        ArrayList<Phone> res = new ArrayList<Phone>();
        HashSet<String> resPhones = new HashSet<String>();
        
        if (Phone.isCellPhoneNumber(searchedText)) {
            Phone phone = new Phone(getContactName(ctx, searchedText), searchedText);
            res.add(phone);
        } else {
            // get the matching contacts, dictionary of < id, names >
            ArrayList<Contact> contacts = getMatchingContacts(ctx, searchedText);
            if (contacts.size() > 0) {
                for (Contact contact : contacts) {
                    ArrayList<Phone> phones = getPhones(ctx, contact.ids);
                    for (Phone phone : phones) {
                        // TODO set the contact name in the phone constructor
                        phone.setContactName(contact.name);
                        if (resPhones.add(phone.getCleanNumber())) {
                            res.add(phone);
                        } else {
                            Log.i("getPhones(searchedText): Duplicated phone number: " + phone.getContactName() + " " + phone.getCleanNumber());
                        }
                    }
                }
            }
        }
        return res;
    }

    /**
     * Returns a ArrayList<Phone>
     * with all matching mobile phone for the argument
     * You usually want to use ContactsResolver.resolveContact()
     * 
     * @param ctx
     * @param searchedText
     * @return ArrayList<Phone> 
     */
    public static ArrayList<Phone> getMobilePhones(Context ctx, String searchedText) {
        ArrayList<Phone> res = new ArrayList<Phone>();
        ArrayList<Phone> phones = getPhones(ctx, searchedText);

        for (Phone phone : phones) {
            if (phone.getType() == CommonDataKinds.Phone.TYPE_MOBILE) {
                res.add(phone);
            }
        }
        return res;
    }
}
