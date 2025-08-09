package com.googlecode.gtalksms.data.contacts;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.googlecode.gtalksms.data.phone.Phone;
import com.googlecode.gtalksms.databases.AliasHelper;

/**
 * This class provides the logic of resolving searchPatterns to phone numbers.
 * E.g. the user wants to send a text message to "Annemarie" it tries find
 * the best number for this.
 * The classes main method is resolveContact()
 *
 */
public class ContactsResolver {
    
    public static final int TYPE_ALL = 1;
    public static final int TYPE_CELL = 2;
    
    private static AliasHelper sAliasHelper; 
    private static Context sContext;
    private static ContactsResolver sContactsResolver;
    
    private ContactsResolver(Context ctx) {
        sContext = ctx;
        sAliasHelper = AliasHelper.getAliasHelper(ctx);
    }
    
    public static ContactsResolver getInstance(Context ctx) {
        if (sContactsResolver == null) {
            sContactsResolver = new ContactsResolver(ctx);
        }
        return sContactsResolver;
    }
    
    /**
     * Tries to find the best contact for a given searchPattern/contactInformation
     * You have to specify a searchType: Use TYPE_ALL to get all matching phone
     * numbers, or TYPE_CELL to get only cell phone numbers.
     * Handles resolving aliasName via aliasHelper transparently.
     * Will return an object of ResolvedContact if there are results.
     * 
     * YOU HAVE TO CHECK THAT THE RESULT IS DISTINCT by calling
     * resolvedContact.isDistinct(), if there are no matching contacts, null
     * is returned.
     * If the ResolvedContact is not distinct, an array with possible candidates
     * can be retrieved via ResolvedContact.getCandidates() and presented the 
     * user.
     *  
     * @param contactInformation
     * @param searchType
     * @return The resolvedContact or null if there is none.
     */
    public ResolvedContact resolveContact(String contactInformation, int searchType) {
        String resolvedName;
        String number = sAliasHelper.convertAliasToNumber(contactInformation);
        
        // Best that can happen, we were able to resolve a distinct number via
        // an alias
        if (Phone.isCellPhoneNumber(number)) {
            resolvedName = ContactsManager.getContactName(sContext, number);
            return new ResolvedContact(resolvedName, number);
        }              
        
        return resolveContactRec(contactInformation, searchType);
    }
    
    private ResolvedContact resolveContactRec(String contactInformation, int searchType) {
        ArrayList<Phone> phones;
        switch (searchType) {
        case TYPE_ALL:
            phones = ContactsManager.getPhones(sContext, contactInformation);
            break;
        case TYPE_CELL:
            phones = ContactsManager.getMobilePhones(sContext, contactInformation);
            break;
        default:
            throw new IllegalStateException();
        }
        
        if (phones.size() > 1) {
            // Start searching for a default number
            for (Phone phone : phones) {
                if (phone.isDefaultNumber()) {
                    return new ResolvedContact(phone.getContactName(), phone.getCleanNumber());
                }
            }
            // There are more then 1 phones for this contact
            // and none of them is marked as default
            List<ResolvedContact> contacts = new ArrayList<ResolvedContact>();
            for (Phone phone : phones) {
                contacts.add(new ResolvedContact(phone.getContactName(), phone.getCleanNumber()));
            }
            // Add the contacts list to the resolvedContact and let the calling
            // function decide what to do with the information
            return new ResolvedContact(contacts);
        } else if (phones.size() == 1) {
            Phone phone = phones.get(0);
            return new ResolvedContact(phone.getContactName(), phone.getCleanNumber());
        // We have not found a matching contact with TYPE_CELL
        // In this case we fall back to return any matching contacts numbers
        // this could cause some SMS to be send to a regular phone instead of a
        // cell.
        } else if (searchType == TYPE_CELL) {
            return resolveContactRec(contactInformation, TYPE_ALL);
        }
        
        // We found no matching phone
        return null;
    }
}