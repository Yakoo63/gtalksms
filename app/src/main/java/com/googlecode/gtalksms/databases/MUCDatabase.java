package com.googlecode.gtalksms.databases;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

/**
 * Backend Class for the Key Value Database(s)
 * allows manipulation of the database
 * 
 * @author Florian Schmaus fschmaus@gmail.com - on behalf of the GTalkSMS Team
 *
 */
class MUCDatabase extends Database {
    
    public MUCDatabase(Context ctx) {
        super(ctx); 
    }
    
    public static boolean addMUC(String muc, String number, int type) {
        ContentValues numbers = composeValues(muc, number, type);
        long ret = database.insert(DatabaseOpenHelper.MUC_TABLE_NAME, null, numbers);
        return ret != -1;
    }
    
    public static boolean updateMUC(String muc, String number, int type) {
        ContentValues numbers = composeValues(muc, number, type);
        int ret = database.update(DatabaseOpenHelper.MUC_TABLE_NAME, numbers, "muc='" + muc + "'", null);
        return ret == 1;
    }
    
    public static boolean deleteMUC(String muc) {
        int ret = database.delete(DatabaseOpenHelper.MUC_TABLE_NAME, "muc='" + muc + "'", null);
        return ret == 1;
    }
    
    public static String getNumber(String muc) {
        Cursor c = databaseRO.query(DatabaseOpenHelper.MUC_TABLE_NAME, new String[] { "number" }, "muc='" + muc + "'", null, null , null, null);
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
    
    public static boolean containsMUC(String muc) {
        
        Cursor c = databaseRO.query(DatabaseOpenHelper.MUC_TABLE_NAME, new String[] { "number" }, "muc='" + muc + "'", null, null , null, null);
        boolean ret = c.getCount() == 1;
        c.close();
        return ret;
    }
    
    public static String[][] getFullDatabase() {
        Cursor c = databaseRO.query(DatabaseOpenHelper.MUC_TABLE_NAME, new String[] { "muc", "number", "type" }, null, null, null , null, null);
        int rowCount = c.getCount();
        c.moveToFirst();
        String[][] res = new String[rowCount][3];
        for (int i = 0; i < rowCount; i++) {
            res[i][0] = c.getString(0);  // muc field
            res[i][1] = c.getString(1);  // number field           
            res[i][2] = String.valueOf(c.getInt(2));  // type field
            c.moveToNext();
        }
        c.close();
        return res;
    }

    private static ContentValues composeValues(String muc, String number, int type) {
        ContentValues numbers = new ContentValues();
        numbers.put("muc", muc);
        numbers.put("number", number);
        numbers.put("type", type);
        return numbers;
    }
}
