package com.googlecode.gtalksms.tools;

import java.util.ArrayList;

import android.content.Context;

import com.googlecode.gtalksms.data.contacts.ContactsManager;
import com.googlecode.gtalksms.data.phone.Phone;
import com.googlecode.gtalksms.databases.AliasDatabase;

/**
 * Middle-end Helper, if we ever want to integrate the Alias function also in
 * the App itself: This is the way to go.
 * At one place in the code, the constructor needs to be called, to make sure
 * that the Database is setup correctly.
 * 
 * @author Florian Schmaus fschmaus@gmail.com - on behalf of the GTalkSMS Team
 *
 */
public class AliasHelper {
    private Context ctx;
    
    /**
     * This constructor ensures that the database is setup correctly
     * @param ctx
     */
    public AliasHelper(Context ctx) {
        new AliasDatabase(ctx);
        this.ctx = ctx;
    }

    public void addAliasByNumber(String aliasName, String number) {
        String contactName = ContactsManager.getContactNameOrNull(ctx, number);
        addOrUpdate(aliasName, number, contactName);      
    }
    
    /**
     * Tries to add or update a alias
     * Returns an ArrayList of Phones with matching names
     * The List has the size 1 if the name was distinct enough top
     * 
     * @param aliasName
     * @param name
     * @return
     */
    public ArrayList<Phone> addAliasByName(String aliasName, String name) {
        ArrayList<Phone> res;
        res = ContactsManager.getMobilePhones(ctx, name);
            if(res.size() == 1) {
                Phone p = res.get(0);
                addOrUpdate(aliasName, p.cleanNumber, p.contactName);
            }
        return res;
    }
    
    /**
     * Deletes an Alias
     * 
     * @param aliasName
     * @return true if successful, otherwise false
     */
    public boolean deleteAlias(String aliasName) {
        if(AliasDatabase.containsAlias(aliasName)) {
            return AliasDatabase.deleteAlias(aliasName);
        } else {
            return false;
        }
    }
    
    /**
     * Converts an given alias to a unique phone number
     * 
     * @param aliasName
     * @return the phone number, or the given alias if there is no number
     */
    public String convertAliasToNumber(String aliasName) {
        if(AliasDatabase.containsAlias(aliasName)) {
            String[] res = AliasDatabase.getAlias(aliasName); 
            return res[1];
        } 
        return aliasName;
    }
    
    public String[] getAliasOrNull(String aliasName) {
        if(AliasDatabase.containsAlias(aliasName)) {
            return AliasDatabase.getAlias(aliasName);
        } 
        return null;
    }
    
    /**
     * Returns the all known aliases
     * or null if there are none
     * 
     * @return
     */
    public String[][] getAllAliases() {
        String[][] res = AliasDatabase.getFullDatabase();
        if (res.length == 0)
            res = null;
        return res;
    }
    
    private void addOrUpdate(String aliasName, String number, String contactName) {
       if(AliasDatabase.containsAlias(aliasName)) {
           AliasDatabase.updateAlias(aliasName, number, contactName);
       } else {
           AliasDatabase.addAlias(aliasName, number, contactName);
       }
    }
}
