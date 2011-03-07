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
public class AliasDatabase extends Database {  
    public AliasDatabase(Context ctx) {
        super(ctx);
    }

    public static boolean setAlias(String alias, String number) {
        ContentValues values = composeValues(alias, number);
        long ret = database.insert(DatabaseOpenHelper.ALIAS_TABLE_NAME, null, values);
        return ret != -1;
    }
    
    public static boolean updateAlias(String alias, String number) {
        ContentValues values = composeValues(alias, number);
        int ret = database.update(DatabaseOpenHelper.ALIAS_TABLE_NAME, values, "aliasName =" + alias, null);
        return ret == 1;
    }
    
    public static boolean deleteAlias(String alias) {
        int ret = database.delete(DatabaseOpenHelper.ALIAS_TABLE_NAME, "aliasName =" + alias, null);
        return ret == 1;
    }
    
    public static String getNumber(String alias) {
        Cursor c = databaseRO.query(DatabaseOpenHelper.ALIAS_TABLE_NAME, new String[] { "number" }, alias, null, null , null, null);
        if(c.getCount() == 1) {
            return c.getString(0);
        } else { 
            return null;
        }
    }
    
    public static boolean containsAlias(String alias) {
        Cursor c = databaseRO.query(DatabaseOpenHelper.ALIAS_TABLE_NAME, new String[] { "number" }, alias, null, null , null, null);
        return c.getCount() == 1;
    }
    
    public static String[][] getFullDatabase() {
        Cursor c = databaseRO.query(DatabaseOpenHelper.ALIAS_TABLE_NAME, new String[] { "aliasName", "number" }, null, null, null , null, null);
        int rowCount = c.getCount();
        String[][] res = new String[rowCount][2];
        for (int i = 0; i < rowCount; i++) {
            res[i][0] = c.getString(0);
            res[i][1] = c.getString(1);
        }
        return res;
    }
    
    private static ContentValues composeValues(String alias, String number) {
        ContentValues values = new ContentValues();
        values.put("aliasName", alias);
        values.put("number", number);
        return values;
    }
}
