package com.kerosoft.mands.test20171201;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by taka on 2017/12/04.
 */

public class UserOpenHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "myapp.db";

    public static final int DB_VERSION = 11;

    public static final String CREATE_TABLE =
            "create table users(" +
            "_id integer primary key autoincrement," +
            "name text," +
            "score integer," +
            "IDm text" +
                    ")";

    public static final String INIT_TABLE =
            "insert into users(name,score,IDm) values" +
            "('taguchi',42,'aaaa')," +
            "('fkoji',82,'bbbb')," +
            "('dotinstall',62,'ccccZ')";


    public static final String DROP_TABLE =
            "drop table if exists users ";


    public UserOpenHelper(Context c) {
        super(c, DB_NAME,null,DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        //create table
        sqLiteDatabase.execSQL(CREATE_TABLE);

        //init table
        sqLiteDatabase.execSQL(INIT_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        // drop table
        sqLiteDatabase.execSQL(DROP_TABLE);

        // onCreate
        onCreate(sqLiteDatabase);
    }

}
