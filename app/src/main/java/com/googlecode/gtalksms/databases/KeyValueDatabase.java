package com.googlecode.gtalksms.databases;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

/**
 * Backend Class for the MUC Database(s)
 * allows manipulation of the database
 * 
 * @author Florian Schmaus fschmaus@gmail.com - on behalf of the GTalkSMS Team
 *
 */
class KeyValueDatabase extends Database {
    
    public KeyValueDatabase(Context ctx) {
        super(ctx);
    }
    
    public static boolean addKey(String key, String value) {
        ContentValues values = composeValues(key, value);
        long ret = database.insert(DatabaseOpenHelper.KV_TABLE_NAME, null, values);
        return ret != -1;
    }
    
    public static boolean updateKey(String key, String value) {
        ContentValues values = composeValues(key, value);
        int ret = database.update(DatabaseOpenHelper.KV_TABLE_NAME, values, "key='" + key + "'", null);
        return ret == 1;
    }
    
    public static boolean deleteKey(String key) {
        int ret = database.delete(DatabaseOpenHelper.KV_TABLE_NAME, "key='" + key + "'", null);
        return ret == 1;
    }
    
    public static String getValue(String key) {
        Cursor c = databaseRO.query(DatabaseOpenHelper.KV_TABLE_NAME, new String[] { "value" }, "key='" + key + "'", null, null , null, null);
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
    
    public static boolean containsKey(String key) {
        
        Cursor c = databaseRO.query(DatabaseOpenHelper.KV_TABLE_NAME, new String[] { "value" }, "key='" + key + "'", null, null , null, null);
        boolean ret = c.getCount() == 1;
        c.close();
        return ret;
    }
    
    public static String[][] getFullDatabase() {
        Cursor c = databaseRO.query(DatabaseOpenHelper.KV_TABLE_NAME, new String[] { "key", "value"}, null, null, null , null, null);
        int rowCount = c.getCount();
        c.moveToFirst();
        String[][] res = new String[rowCount][2];
        for (int i = 0; i < rowCount; i++) {
            res[i][0] = c.getString(0);  // key field
            res[i][1] = c.getString(1);  // value field           
            c.moveToNext();
        }
        c.close();
        return res;
    }

    private static ContentValues composeValues(String key, String value) {
        ContentValues values = new ContentValues();
        values.put("key", key);
        values.put("value", value);
        return values;
    }
}
