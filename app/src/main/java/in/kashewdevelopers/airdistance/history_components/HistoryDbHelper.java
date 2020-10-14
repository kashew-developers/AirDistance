package in.kashewdevelopers.airdistance.history_components;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

public class HistoryDbHelper extends SQLiteOpenHelper {

    // table details
    private static final String DB_NAME = "history";
    private static final int DB_VERSION = 1;


    // columns in table
    public static String SRC_NAME = "src_name";
    public static String SRC_LL = "src_lat_lng";
    public static String DST_NAME = "dst_name";
    public static String DST_LL = "dst_lat_lng";
    public static String DISTANCE = "distance";
    public static String HASH = "hash";
    private static String TIME = "timestamp";


    public HistoryDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        String creatingDb = "CREATE TABLE " + DB_NAME + " (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                SRC_NAME + " TEXT," +
                SRC_LL + " TEXT," +
                DST_NAME + " TEXT," +
                DST_LL + " TEXT," +
                DISTANCE + " TEXT," +
                HASH + " TEXT UNIQUE ON CONFLICT REPLACE," +
                TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP);";

        db.execSQL(creatingDb);
    }


    public void insert(SQLiteDatabase db, String srcName, String srcLL, String dstName, String dstLL, String distance) {
        String hash = srcLL + dstLL;
        hash = String.valueOf(hash.hashCode());

        ContentValues data = new ContentValues();
        data.put(SRC_NAME, srcName);
        data.put(SRC_LL, srcLL);
        data.put(DST_NAME, dstName);
        data.put(DST_LL, dstLL);
        data.put(HASH, hash);
        data.put(DISTANCE, distance);

        db.insert(DB_NAME, null, data);
    }


    public Cursor get(SQLiteDatabase db) {
        return db.query(DB_NAME, null, null, null, null,
                null, TIME + " DESC");
    }


    public void deleteAll(SQLiteDatabase db) {
        db.delete(DB_NAME, null, null);
    }


    public void delete(SQLiteDatabase db, String hashCode) {
        db.delete(DB_NAME, HASH + " = ?", new String[]{hashCode});
    }

    public void updateDestinationName(@NonNull SQLiteDatabase db, @NonNull String destinationLatLng, @NonNull String destinationName) {
        ContentValues values = new ContentValues();
        values.put(DST_NAME, destinationName);

        db.update(DB_NAME, values, DST_LL + " = ?", new String[]{destinationLatLng});
    }

    public void updateSourceName(@NonNull SQLiteDatabase db, @NonNull String sourceLatLng, @NonNull String sourceName) {
        ContentValues values = new ContentValues();
        values.put(SRC_NAME, sourceName);

        db.update(DB_NAME, values, SRC_LL + " = ?", new String[]{sourceLatLng});
    }


    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
    }

}
