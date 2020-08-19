package in.kashewdevelopers.airdistance;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SuggestionDbHelper extends SQLiteOpenHelper {

    // table details
    private static final String DB_NAME = "places";
    private static final int DB_VERSION = 1;


    // columns in table
    static String PLACE_NAME = "place_name";
    static String PLACE_LAT = "place_latitude";
    static String PLACE_LNG = "place_longitude";


    SuggestionDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        String creatingDb = "CREATE TABLE " + DB_NAME + " (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                PLACE_NAME + " TEXT UNIQUE ON CONFLICT REPLACE," +
                PLACE_LAT + " DOUBLE," +
                PLACE_LNG + " DOUBLE);";
        db.execSQL(creatingDb);
    }


    public long insert(SQLiteDatabase db, String placeName, double lat, double lng) {
        ContentValues data = new ContentValues();
        data.put(PLACE_NAME, placeName);
        data.put(PLACE_LAT, lat);
        data.put(PLACE_LNG, lng);

        return db.insert(DB_NAME, null, data);
    }


    public Cursor search(SQLiteDatabase db, String placeName) {
        String condition = PLACE_NAME + " like '%" + placeName + "%'";
        String orderBy = PLACE_NAME + " ASC";
        String limit = "5";

        return db.query(DB_NAME, null, condition, null,
                null, null, orderBy, limit);
    }


    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
    }

}
