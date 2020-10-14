package in.kashewdevelopers.airdistance.suggestion_components;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SuggestionDbHelper extends SQLiteOpenHelper {

    // table details
    private static final String DB_NAME = "places";
    private static final int DB_VERSION = 1;


    public SuggestionDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        String creatingDb = "CREATE TABLE " + DB_NAME + " (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "NAME TEXT UNIQUE);";
        db.execSQL(creatingDb);
    }


    public void insert(SQLiteDatabase db, String placeName) {
        ContentValues data = new ContentValues();
        data.put("NAME", placeName);

        db.insert(DB_NAME, null, data);
    }


    public Cursor search(SQLiteDatabase db, String placeName) {
        String condition = "NAME like '%" + placeName + "%'";
        String orderBy = "NAME ASC";
        String limit = "5";

        return db.query(DB_NAME, null, condition, null,
                null, null, orderBy, limit);
    }


    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
    }

}
