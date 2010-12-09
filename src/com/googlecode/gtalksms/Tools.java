package com.googlecode.gtalksms;

import android.database.Cursor;

public class Tools {
    public static Long getLong(Cursor c, String col) {
        return c.getLong(c.getColumnIndex(col));
    }

    public static String getString(Cursor c, String col) {
        return c.getString(c.getColumnIndex(col));
    }
}
