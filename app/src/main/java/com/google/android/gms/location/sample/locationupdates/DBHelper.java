package com.google.android.gms.location.sample.locationupdates;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper  extends SQLiteOpenHelper{

    private static final String TAG = MainActivity.class.getSimpleName();
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "gpsdata.db";
    public static final String TABLE_GPS = "gps";

    public static final String KEY_ID = "id";
    public static final String KEY_DATE_TIME = "datetime";
    public static final String KEY_LATI = "latitude";
    public static final String KEY_LONGI = "longitude";
    public static final String KEY_BAT = "batLevel";
    public static final String KEY_WIFISSD ="wifissd";
// ------------------------------------------------------
    public static final String TABLE_GPS2 = "phone";
    //public static final String KEY_SIMSERNUMB = "SimSerialNumber";
    public static final String KEY_HARDWARE = "HARDWARE";
    public static final String KEY_BUILDID = "BuildID";
    public static final String KEY_RELASEID = "RelaseID";
    //public static final String KEY_GETDEVICEID ="GetDeviceId";
    public static final String KEY_TELAGAID ="TelegaID";
    public static final String KEY_ANDROIDID ="GetANDID";
    public static final String KEY_REGSERVER ="REGSERVER";
    public static final String KEY_POINTSERVER ="POINTSERVER";
    public static final String KEY_GETSERVER ="GETSERVER";
    public static final String KEY_COMAND ="COMAND";




    public DBHelper(Context context) {

        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE_GPS + "(" + KEY_ID
                + " integer primary key," + KEY_DATE_TIME + " text," + KEY_LATI + " real,"
                +  KEY_LONGI + " real," +  KEY_BAT + " integer," +  KEY_WIFISSD + " text" + ")");

        Log.e(TAG, "Таблица gps создана !!!");

        db.execSQL("create table " + TABLE_GPS2 + "("
                + KEY_ID + " integer primary key,"
                + KEY_DATE_TIME + " text,"
                + KEY_HARDWARE + " text,"
                + KEY_BUILDID + " text,"
                + KEY_RELASEID + " text,"
                + KEY_TELAGAID + " text,"
                + KEY_REGSERVER + " text,"
                + KEY_POINTSERVER + " text,"
                + KEY_GETSERVER + " text,"
                + KEY_COMAND + " text,"
                + KEY_ANDROIDID + " text" + ")");
        Log.e(TAG, "Таблица phone создана !!!");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists " + TABLE_GPS);

        onCreate(db);

    }
}