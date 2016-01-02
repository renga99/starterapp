package com.raina.android.starterapp.persist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class UploadService {

    private static final String TAG = "DBService";

    private MobDBHandler db;

    public UploadService(Context context) {
        db = new MobDBHandler(context);
    }


    public void save(String name,String phone,String date, String option, String num, String photo,
                                      String lat, String lon,boolean synced) {
        SQLiteDatabase sql1 = db.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("name",name );
        contentValues.put("phone", phone);
        contentValues.put("date", date);
        contentValues.put("option", option);
        contentValues.put("num", num);
        contentValues.put("photo", photo);
        contentValues.put("lat", lat);
        contentValues.put("lon", lon);
        contentValues.put("synced", synced);
        sql1.insert("info", null, contentValues);
        Log.d(TAG, "data inserted");
    }
}
