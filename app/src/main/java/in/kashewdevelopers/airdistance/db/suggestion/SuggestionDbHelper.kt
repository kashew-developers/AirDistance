package `in`.kashewdevelopers.airdistance.db.suggestion

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SuggestionDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "places"
        private const val DB_VERSION = 1

        const val COLUMN_PLACE_NAME = "Name"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db ?: return

        val tableCreationQuery = "CREATE TABLE $DB_NAME(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_PLACE_NAME TEXT UNIQUE);"
        db.execSQL(tableCreationQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}


    fun insert(db: SQLiteDatabase, placeName: String) {
        val data = ContentValues()
        data.put(COLUMN_PLACE_NAME, placeName)
        db.insert(DB_NAME, null, data)
    }

    fun search(db: SQLiteDatabase, placeName: String): Cursor? {
        val condition = "$COLUMN_PLACE_NAME like '%$placeName%'"
        val orderBy = "$COLUMN_PLACE_NAME ASC"
        val limit = "5"

        return db.query(DB_NAME, null, condition, null,
                null, null, orderBy, limit)
    }

}