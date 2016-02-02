package com.example.livemap.accelerometertest;

/**
 * Created by nikhil on 11/5/15.
 */
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseWrapper extends SQLiteOpenHelper {

    public static final String TABLE_NAME = "Recording";

    //column names
    private static final String TIME =  "time";
    private static final String STATE =  "state";
    private static final String IS_MOVING =  "Is_Moving";
    private static final String X_ACCELERATION =  "x_acceleration";
    private static final String Y_ACCELERATION =  "y_acceleration";
    private static final String Z_ACCELERATION =  "z_acceleration";
    private static final String JOSTLE_INDEX =  "jostle_index";

    private String LOG = "DatabaseWrapper";




    private static final String DATABASE_NAME = "accelerometer_readings.db";
    private static final int DATABASE_VERSION = 1;

    // creation SQLite statement
    private static final String DATABASE_CREATE = "create table " + TABLE_NAME
            + "(" + TIME + " TEXT primary key not null, "+ STATE +" Text not null, "+IS_MOVING +" INT not null," +
            X_ACCELERATION+ " REAL not null, " +Y_ACCELERATION+ " REAL not null, "+ Z_ACCELERATION+ " REAL not null, "
            + JOSTLE_INDEX+" REAL);";

    // create table Recordings(Time TEXT primary key not null, State Text not null, isMoving INT not null,
    // X-Acceleration INT not null, Y-Acceleration INT not null, Z-Acceleration INT not null
    public DatabaseWrapper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE);
        Log.d(LOG, "Database created using query -" + DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // you should do some logging in here
        // ..

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        Log.d("DatabaseWrapper", "Tables dropped");
        onCreate(db);
    }

    public void printTables(SQLiteDatabase db){
        Cursor test = db.query(TABLE_NAME,null,null,null,null,null,null);
        String[] columns = test.getColumnNames();
        Log.d(LOG, "Getting column names");
        for(String col:columns){
            Log.d(LOG, col);
        }

    }

    public void addDBEntry(DBEntry entry){
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        Log.d(LOG,"inserting new value into database");
        values.put(TIME, entry.getTime());
        values.put(STATE, entry.getState());
        values.put(IS_MOVING,(entry.isMoving())? 1:0);
        values.put(X_ACCELERATION, entry.getX_acceleration());
        values.put(Y_ACCELERATION, entry.getY_acceleration());
        values.put(Z_ACCELERATION, entry.getZ_acceleration());
        values.put(JOSTLE_INDEX,entry.getJostle_index());

        db.insert(TABLE_NAME, null, values);
        db.close();

    }

    public void logRecordings(){
        String myQuery = "SELECT * FROM "+ TABLE_NAME;
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.rawQuery(myQuery,null);
        Log.d(LOG, "Logging entire database table");

        if(cursor.moveToFirst()){
            while(cursor.moveToNext()){
                Log.d("DatabaseWrapper", DatabaseUtils.dumpCurrentRowToString(cursor));
            }
        }

    }

    /**
     * Removes all entries whose timestamp is greater than the entry passed in
     * @param entry
     */
    public void removeLastEntry(DBEntry entry){
        String query = "DELETE FROM "+ TABLE_NAME +" WHERE time > "+ entry.getTime();
        SQLiteDatabase db = this.getWritableDatabase();
        db.rawQuery(query, null);
    }

    public void clearALL(){
        String query = "DELETE FROM "+ TABLE_NAME;
        Log.d(LOG, "Clearing database contents");
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
    }

}