package `in`.kashewdevelopers.airdistance.db.history

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class HistoryDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "history"
        private const val DB_VERSION = 1

        const val COLUMN_SRC_NAME = "src_name"
        const val COLUMN_SRC_LAT_LNG = "src_lat_lng"
        const val COLUMN_DST_NAME = "dst_name"
        const val COLUMN_DST_LAT_LNG = "dst_lat_lng"
        const val COLUMN_DISTANCE = "distance"
        const val COLUMN_HASH = "hash"
        const val COLUMN_TIME = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db ?: return

        val query = "CREATE TABLE $DB_NAME(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_SRC_NAME TEXT," +
                "$COLUMN_SRC_LAT_LNG TEXT," +
                "$COLUMN_DST_NAME TEXT," +
                "$COLUMN_DST_LAT_LNG TEXT," +
                "$COLUMN_DISTANCE TEXT," +
                "$COLUMN_HASH TEXT UNIQUE ON CONFLICT REPLACE," +
                "$COLUMN_TIME DATETIME DEFAULT CURRENT_TIMESTAMP);"
        db.execSQL(query)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}

    fun insert(db: SQLiteDatabase, srcName: String, srcLatLng: String, dstName: String, dstLatLng: String, distance: String) {
        val hash = "${(srcLatLng + dstLatLng).hashCode()}"

        val data = ContentValues()
        with(data) {
            put(COLUMN_SRC_NAME, srcName)
            put(COLUMN_SRC_LAT_LNG, srcLatLng)
            put(COLUMN_DST_NAME, dstName)
            put(COLUMN_DST_LAT_LNG, dstLatLng)
            put(COLUMN_HASH, hash)
            put(COLUMN_DISTANCE, distance)
        }

        db.insert(DB_NAME, null, data)
    }

    fun get(db: SQLiteDatabase): Cursor? {
        return db.query(DB_NAME, null, null, null,
                null, null, "$COLUMN_TIME DESC")
    }

    fun deleteAll(db: SQLiteDatabase) {
        db.delete(DB_NAME, null, null)
    }

    fun delete(db: SQLiteDatabase, hashCode: String) {
        db.delete(DB_NAME, "$COLUMN_HASH = ?", arrayOf(hashCode))
    }

    fun updateDestinationName(db: SQLiteDatabase, dstLatLng: String, dstName: String) {
        val data = ContentValues()
        data.put(COLUMN_DST_NAME, dstName)
        db.update(DB_NAME, data, "$COLUMN_DST_LAT_LNG = ?", arrayOf(dstLatLng))
    }

    fun updateSourceName(db: SQLiteDatabase, srcLatLng: String, srcName: String) {
        val data = ContentValues()
        data.put(COLUMN_SRC_NAME, srcName)
        db.update(DB_NAME, data, "$COLUMN_SRC_LAT_LNG = ?", arrayOf(srcLatLng))
    }

}