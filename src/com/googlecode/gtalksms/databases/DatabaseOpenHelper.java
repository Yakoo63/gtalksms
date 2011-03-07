package com.googlecode.gtalksms.databases;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseOpenHelper extends SQLiteOpenHelper {
    
    /* general database version gtalksms uses */
    private static final int DATABASE_VERSION = 1;
    
    /* information for the alias database */
    public static final String ALIAS_TABLE_NAME = "alias";
    private static final String ALIAS_TABLE_CREATE = 
        "CREATE TABLE " + ALIAS_TABLE_NAME + " (" +
            "aliasName TEXT NOT NULL, " +
            "number TEXT NOT NULL, " +
            "PRIMARY KEY(aliasName)" +
         ")";
    

//    public AliasOpenHelper(Context context, String name, CursorFactory factory, int version) {
//        super(context, name, factory, version);
//    }
    
    DatabaseOpenHelper(Context context) {
        super(context, ALIAS_TABLE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(ALIAS_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // we could drop the table here 
        // - or just sit around and do nothing (preferred :-P )
    }

}
