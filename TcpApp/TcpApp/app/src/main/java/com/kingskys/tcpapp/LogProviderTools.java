package com.kingskys.tcpapp;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class LogProviderTools {
    private static final Uri i_uri = Uri.parse("content://" + LogContentProvider.i_authorities + "/" + LogContentProvider.i_table_name);

    public static Cursor getQueryCursor(Context context) {

        ContentResolver resolver = context.getContentResolver();
        String check = "";

        Cursor cursor = resolver.query(i_uri, new String[]{"_id", "name"}, check, null, null);
        return cursor;

    }

    public static String query(Context context) {

        ContentResolver resolver = context.getContentResolver();
        String check = "";

        Cursor cursor = resolver.query(i_uri, new String[]{"_id", "name"}, check, null, null);
        if (cursor == null) {
            return null;
        }

        StringBuilder msg = new StringBuilder("{");
        while (cursor.moveToNext()) {
            long time = cursor.getLong(0);
            msg.append("\"");
            msg.append(time);
            msg.append("\":\"");
            String data = cursor.getString(1);
            msg.append(data);
            msg.append("\"");
            msg.append(',');
        }

        msg.append("}");

        cursor.close();
        return msg.toString();

    }

    public static void insert(Context context, long time, String value) {
        if (value == null) return;

        try {
            ContentResolver resolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put("_id", time);
            values.put("name", value);

            resolver.insert(i_uri, values);
        }
        catch (Throwable e) {

        }
    }

    public static int delete(Context context, long from, long to, int limit) {
        try {
            ContentResolver resolver = context.getContentResolver();
            String check = "_id >= " + from + " and _id <= " + to;
            if (limit > 0) {
                check = "_id in (select _id from " + LogContentProvider.i_table_name + " where " + check + " limit " + limit + ")";
            }

            return resolver.delete(i_uri, check, null);
        }
        catch (Throwable e) {
            return -1;
        }
    }

    public static int deleteAll(Context context) {
        try {
            ContentResolver resolver = context.getContentResolver();
            String check = "";

            return resolver.delete(i_uri, check, null);
        }
        catch (Throwable e) {
            return -1;
        }
    }
}
