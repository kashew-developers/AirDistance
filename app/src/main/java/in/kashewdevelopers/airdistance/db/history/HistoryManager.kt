package `in`.kashewdevelopers.airdistance.db.history

import `in`.kashewdevelopers.airdistance.adapter.HistoryAdapter
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

class HistoryManager(val context: Context) {

    private lateinit var dbHelper: HistoryDbHelper
    private var db: SQLiteDatabase? = null
    private var cursor: Cursor? = null
    private var adapter: HistoryAdapter? = null

    fun initializeElements() {
        dbHelper = HistoryDbHelper(context)

        db = dbHelper.readableDatabase
        val tempDb = db ?: return

        cursor?.close()
        cursor = dbHelper.get(tempDb)
        cursor?.let { cursor ->
            adapter = HistoryAdapter(context, cursor, 0)
        }
    }

    fun getAdapter(): HistoryAdapter? {
        return adapter
    }

    fun updateAdapter(): HistoryAdapter? {
        val tempDb = db ?: return adapter

        cursor?.close()
        cursor = dbHelper.get(tempDb)
        val tempCursor = cursor ?: return adapter

        adapter?.let {
            it.swapCursor(tempCursor)
            it.notifyDataSetChanged()
        } ?: run { adapter = HistoryAdapter(context, tempCursor, 0) }

        return adapter
    }

    fun getCount(): Int {
        return cursor?.count ?: 0
    }


    fun updateSourceName(srcLatLng: String, srcName: String) {
        db?.let { dbHelper.updateSourceName(it, srcLatLng, srcName) }
    }

    fun updateDestinationName(dstLatLng: String, dstName: String) {
        db?.let { dbHelper.updateDestinationName(it, dstLatLng, dstName) }
    }

    fun delete(hashCode: String) {
        db?.let { dbHelper.delete(it, hashCode) }
    }

    fun deleteAll() {
        db?.let { dbHelper.deleteAll(it) }
    }

    fun insert(srcName: String, scrLatLng: String, dstName: String, dstLatLng: String, distance: String) {
        db?.let { dbHelper.insert(it, srcName, scrLatLng, dstName, dstLatLng, distance) }
    }

}