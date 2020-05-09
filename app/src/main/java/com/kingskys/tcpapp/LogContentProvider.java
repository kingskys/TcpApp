package com.kingskys.tcpapp;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class LogContentProvider extends ContentProvider {
    private Context m_context = null;
    private SQLiteDatabase m_db = null;
    private LogDBHelper m_dbHelper = null;

    // 表名
    public static final String i_table_name = "infos";
    // 数据库名
    private static final String i_db_name = "loginfo.db";

    public static final String i_authorities = "com.kingskys.tcpapp.log";

    public static final int info_code = 1;

    private static final UriMatcher m_matcher;

    static {
        m_matcher = new UriMatcher(UriMatcher.NO_MATCH);
        m_matcher.addURI(i_authorities, i_table_name, info_code);
    }

    public LogContentProvider() {
    }

    @Override
    public boolean onCreate() {
        m_context = getContext();

        m_dbHelper = new LogDBHelper(m_context, i_db_name, i_table_name);
        m_db = m_dbHelper.getWritableDatabase();

        return true;
    }

    private String getTableName(Uri uri) {
        String tablename = null;
        switch (m_matcher.match(uri)) {
            case info_code:
                tablename = i_table_name;
                break;
        }

        return tablename;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        String table = getTableName(uri);
        int ret = m_db.delete(table, selection, selectionArgs);
        m_context.getContentResolver().notifyChange(uri, null);
        return ret;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String table = getTableName(uri);
        m_db.insert(table, null, values);
        m_context.getContentResolver().notifyChange(uri, null);

        return uri;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        String table = getTableName(uri);
        return m_db.query(table, projection, selection, selectionArgs, null, null, sortOrder, null);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        String table = getTableName(uri);
        int ret = m_db.update(table, values, selection, selectionArgs);
        m_context.getContentResolver().notifyChange(uri, null);
        return ret;
    }
}
