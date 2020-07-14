package in.kashewdevelopers.airdistance;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "places";
    private static final int DB_VERSION = 1;

    private String creatingDb = "CREATE TABLE PLACES (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "NAME TEXT UNIQUE);";

    SQLiteHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(creatingDb);
    }

    public long insert(SQLiteDatabase db, String placeName) {
        ContentValues data = new ContentValues();
        data.put("NAME", placeName);

        return db.insert("PLACES", null, data);
    }

    public Cursor search(SQLiteDatabase db, String placeName) {
        String condition = "NAME like '%" + placeName + "%'";
        return db.query("PLACES", new String[]{"NAME"}, condition,
                null, null, null, "NAME ASC");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
