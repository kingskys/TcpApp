package com.kingskys.tcpapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LogDBHelper extends SQLiteOpenHelper {

    // 表名
    private String m_table_name = "";

    //数据库版本号
    private static final int DATABASE_VERSION = 1;


    public LogDBHelper(Context context, String dbName, String tableName) {
        super(context, dbName, null, DATABASE_VERSION);
        m_table_name = tableName;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS " + m_table_name;
        db.execSQL(sql);
        onCreate(db);
    }

    private void createTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + m_table_name + "(_id int(64), name TEXT)");
    }

}
