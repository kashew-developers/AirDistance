package in.kashewdevelopers.airdistance;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HistoryDbHelper extends SQLiteOpenHelper {

    // table details
    private static final String DB_NAME = "history";
    private static final int DB_VERSION = 1;


    // columns in table
    static String SRC_NAME = "src_name";
    static String SRC_LL = "src_lat_lng";
    static String DST_NAME = "dst_name";
    static String DST_LL = "dst_lat_lng";
    static String DISTANCE = "distance";
    static String HASH = "hash";
    private static String TIME = "timestamp";


    HistoryDbHelper(Context context) {
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


    void insert(SQLiteDatabase db, String srcName, String srcLL, String dstName, String dstLL, String distance) {
        String hash = srcName + srcLL + dstName + dstLL;
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


    Cursor get(SQLiteDatabase db) {
        return db.query(DB_NAME, null, null, null, null,
                null, TIME + " DESC");
    }


    void deleteAll(SQLiteDatabase db) {
        db.delete(DB_NAME, null, null);
    }


    void delete(SQLiteDatabase db, String hashCode) {
        db.delete(DB_NAME, HASH + " = ?", new String[]{hashCode});
    }


    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
    }

}
