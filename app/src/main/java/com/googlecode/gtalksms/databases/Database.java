package com.googlecode.gtalksms.databases;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

abstract class Database {
    static SQLiteDatabase database;
    static SQLiteDatabase databaseRO;

    Database(Context ctx) {
        if (database == null) {
            DatabaseOpenHelper helper = new DatabaseOpenHelper(ctx);
            database = helper.getWritableDatabase();
            databaseRO = helper.getReadableDatabase();
        }
    }
}
