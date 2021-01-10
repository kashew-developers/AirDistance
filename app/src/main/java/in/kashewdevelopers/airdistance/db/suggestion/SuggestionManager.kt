package `in`.kashewdevelopers.airdistance.db.suggestion

import `in`.kashewdevelopers.airdistance.R
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.widget.CursorAdapter
import android.widget.SimpleCursorAdapter

class SuggestionManager(val context: Context) {

    private lateinit var dbHelper: SuggestionDbHelper
    private var db: SQLiteDatabase? = null
    private var adapter: SimpleCursorAdapter? = null
    private var cursor: Cursor? = null

    fun initializeElements() {
        dbHelper = SuggestionDbHelper(context)
        db = dbHelper.readableDatabase

        val tempDb = db
        tempDb ?: return

        cursor = dbHelper.search(tempDb, "")
        adapter = SimpleCursorAdapter(context, R.layout.suggestion_list_layout,
                cursor, arrayOf(SuggestionDbHelper.COLUMN_PLACE_NAME), intArrayOf(R.id.text),
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER)

        adapter?.setCursorToStringConverter {
            it.getString(it.getColumnIndex(SuggestionDbHelper.COLUMN_PLACE_NAME))
        }

        adapter?.setFilterQueryProvider { searchQuery ->
            db?.let { cursor = dbHelper.search(it, searchQuery.toString()) }
            cursor
        }
    }

    fun insertPlace(placeName: String) {
        db?.let { dbHelper.insert(it, placeName) }
    }

    fun getAdapter(): SimpleCursorAdapter? {
        return adapter
    }

}