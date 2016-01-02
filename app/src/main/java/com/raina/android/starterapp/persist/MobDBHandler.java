package com.raina.android.starterapp.persist;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

public class MobDBHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "StarterApp.db";
    public static final String TABLE_INFO = "info";


    public MobDBHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE IF NOT EXISTS info (name TEXT , phone TEXT,"+
                "date TEXT, option TEXT, num TEXT,"+
                "photo TEXT, lat TEXT, lon TEXT, synced NUMERIC NOT NULL )");


        Log.d("DBhandler", "DB created");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INFO);
        onCreate(db);

    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }


    public void initializeDB(){
        this.getReadableDatabase();
    }


}
