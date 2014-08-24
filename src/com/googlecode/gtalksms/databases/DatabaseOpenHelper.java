package com.googlecode.gtalksms.databases;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.googlecode.gtalksms.tools.Log;

class DatabaseOpenHelper extends SQLiteOpenHelper {
    
    /* general database version gtalksms uses */
    private static final int DATABASE_VERSION = 6;
    
    /* information for the alias database */
    public static final String ALIAS_TABLE_NAME = "alias";
    private static final String ALIAS_TABLE_CREATE =
        "CREATE TABLE " + ALIAS_TABLE_NAME + " (" +
            "aliasName TEXT NOT NULL, " +
            "number TEXT NOT NULL, " +
            "contactName TEXT, " +
            "PRIMARY KEY(aliasName)" +
         ")";
        
    /* information for the key value string table */
    public static final String KV_TABLE_NAME = "key_value";
    private static final String KV_TABLE_CREATE =
        "CREATE TABLE " + KV_TABLE_NAME + " (" +
            "key TEXT NOT NULL, " +
            "value TEXT NOT NULL, " +
            "PRIMARY KEY(key)" +
         ")";
    
    /* information for the muc table */
    public static final String MUC_TABLE_NAME = "muc";
    private static final String MUC_TABLE_CREATE =
        "CREATE TABLE " + MUC_TABLE_NAME + " (" +
            "muc TEXT NOT NULL, " +
            "number TEXT NOT NULL, " +
            "type INTEGER NOT NULL, " +
            "PRIMARY KEY(muc)" +
         ")";

    public static final String SMS_TABLE_NAME = "sms";
    private static final String SMS_TABLE_CREATE =
        "Create TABLE " + SMS_TABLE_NAME + " (" +
            "smsID INTEGER NOT NULL, " +
            "phoneNumber TEXT NOT NULL, " +
            "name TEXT NOT NULL, " +
            "shortenedMessage TEXT NOT NULL, " +
            "answerTo TEXT NOT NULL, " +
            "dIntents TEXT NOT NULL, " +
            "sIntents TEXT NOT NULL, " +
            "numParts INTEGER NOT NULL, " +
            "resSIntent INTEGER NOT NULL, " +
            "resDIntent INTEGER NOT NULL, " +
            "date INTEGER NOT NULL, " +
            "PRIMARY KEY(smsID)" +
        ")";
    
    DatabaseOpenHelper(Context context) {
        // I made a small mistake here, the database is now called "alias"
        super(context, ALIAS_TABLE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.w("Creating DB...");

        db.execSQL(ALIAS_TABLE_CREATE);
        db.execSQL(KV_TABLE_CREATE);
        db.execSQL(MUC_TABLE_CREATE);
        db.execSQL(SMS_TABLE_CREATE);
    }

    @Override
    // NOTICE: when adding a new table, don't forget to add the create command also into the onCreate() method
    // TODO: manage a better "update"
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // add table that came with version 2 of our database
        if (oldVersion < 1) {
            Log.w("Upgrading Alias database from " + oldVersion + " to " + newVersion);
            db.execSQL(ALIAS_TABLE_CREATE);
        }
        if (oldVersion < 2) {
            Log.w("Upgrading Key Values database from " + oldVersion + " to " + newVersion);
            db.execSQL(KV_TABLE_CREATE);
        }
        if (oldVersion < 5) {
            Log.w("Upgrading SMS database from " + oldVersion + " to " + newVersion);
            dropTable(db, SMS_TABLE_NAME);
            db.execSQL(SMS_TABLE_CREATE);
        }
        if (oldVersion < 6) {
            Log.w("Upgrading SMS database from " + oldVersion + " to " + newVersion);
            dropTable(db, MUC_TABLE_NAME);
            db.execSQL(MUC_TABLE_CREATE);
        }
    }

    @Override
    // Should not happen except in dev, all data will be lost
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("DB Version downgraded from " + oldVersion + " to " + newVersion + ", dropping all tables, all data will be lost");
        dropTable(db, KV_TABLE_NAME);
        dropTable(db, ALIAS_TABLE_NAME);
        dropTable(db, MUC_TABLE_NAME);
        dropTable(db, SMS_TABLE_NAME);

        onCreate(db);
    }

    /**
     * Drop the table using the specified db connection
     * @param db
     * @param name
     */
    private void dropTable(SQLiteDatabase db, String name) {
        try {
            db.execSQL("DROP TABLE " + name);
        } catch (Exception e) {
            Log.e("Failed to drop table " + name, e);
        }
    }
}