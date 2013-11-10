package com.googlecode.gtalksms.databases;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

/**
 * Backend Class for the Alias Feature
 * allows manipulation of the database for aliases
 * 
 * @author Florian Schmaus fschmaus@gmail.com - on behalf of the GTalkSMS Team
 *
 */
class AliasDatabase extends Database {
    
    public AliasDatabase(Context ctx) {
        super(ctx);
    }

    /**
     * 
     * 
     * @param aliasName
     * @param number
     * @param contactName - the human readable name of the contact - may be null
     * @return true on success, otherwise false
     */
    public static boolean addAlias(String aliasName, String number, String contactName) {
        ContentValues values = composeValues(aliasName, number, contactName);
        long ret = database.insert(DatabaseOpenHelper.ALIAS_TABLE_NAME, null, values);
        return ret != -1;
    }
    
    /**
     * 
     * @param aliasName
     * @param number
     * @param contactName
     * @return true on success, otherwise false
     */
    public static boolean updateAlias(String aliasName, String number, String contactName) {
        ContentValues values = composeValues(aliasName, number, contactName);
        int ret = database.update(DatabaseOpenHelper.ALIAS_TABLE_NAME, values, "aliasName='" + aliasName + "'", null);
        return ret == 1;
    }
    
    public static boolean deleteAlias(String aliasName) {
        int ret = database.delete(DatabaseOpenHelper.ALIAS_TABLE_NAME, "aliasName='" + aliasName + "'", null);
        return ret == 1;
    }
    
    public static String getNumber(String aliasName) {
        Cursor c = databaseRO.query(DatabaseOpenHelper.ALIAS_TABLE_NAME, new String[] { "number" }, "aliasName='" + aliasName + "'", null, null , null, null);
        if(c.getCount() == 1) {
            c.moveToFirst();
            String res = c.getString(0);
            c.close();
            return res;
        } else { 
            c.close();
            return null;
        }
    }
    
    public static boolean containsAlias(String aliasName) {
        
        Cursor c = databaseRO.query(DatabaseOpenHelper.ALIAS_TABLE_NAME, new String[] { "number" }, "aliasName='" + aliasName + "'", null, null , null, null);
        boolean ret = false;
        if (c != null) {
            ret = c.getCount() == 1;
            c.close();
        }
        return ret;
    }
    
    public static String[][] getFullDatabase() {
        String[][] res = null;

        Cursor c = databaseRO.query(DatabaseOpenHelper.ALIAS_TABLE_NAME, new String[] { "aliasName", "number", "contactName" }, null, null, null , null, null);
        if (c != null) {
            int rowCount = c.getCount();
            c.moveToFirst();
            res = new String[rowCount][3];
            for (int i = 0; i < rowCount; i++) {
                res[i][0] = c.getString(0);  // aliasName field
                res[i][1] = c.getString(1);  // number field
                res[i][2] = c.getString(2);   // contactName field - may be null
                c.moveToNext();
            }
            c.close();
        }
        return res;
    }
    
    public static String[] getAlias(String aliasName) {
        String[] res = null;
        Cursor c = databaseRO.query(DatabaseOpenHelper.ALIAS_TABLE_NAME, new String[] { "aliasName", "number", "contactName" }, "aliasName='" + aliasName + "'", null, null , null, null);
        if (c != null) {
            c.moveToFirst();
            if(c.getString(2) == null) {
                res = new String[2];
                res[0] = c.getString(0);
                res[1] = c.getString(1);
            } else {
                res = new String[3];
                res[0] = c.getString(0);
                res[1] = c.getString(1);
                res[2] = c.getString(2);
            }
            c.close();
        }
        return res;       
    }
    
    private static ContentValues composeValues(String aliasName, String number, String contactName) {
        ContentValues values = new ContentValues();
        values.put("aliasName", aliasName);
        values.put("number", number);
        if (contactName != null) {
            values.put("contactName", contactName);
        }
        return values;
    }
}
