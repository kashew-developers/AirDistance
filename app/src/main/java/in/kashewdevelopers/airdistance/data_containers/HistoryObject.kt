package `in`.kashewdevelopers.airdistance.data_containers

import `in`.kashewdevelopers.airdistance.db.history.HistoryDbHelper
import android.database.Cursor

class HistoryObject(cursor: Cursor) {
    val sourceName: String = cursor.getString(cursor.getColumnIndex(HistoryDbHelper.COLUMN_SRC_NAME))
    val sourceLatLng: String = cursor.getString(cursor.getColumnIndex(HistoryDbHelper.COLUMN_SRC_LAT_LNG))

    val destinationName: String = cursor.getString(cursor.getColumnIndex(HistoryDbHelper.COLUMN_DST_NAME))
    val destinationLatLng: String = cursor.getString(cursor.getColumnIndex(HistoryDbHelper.COLUMN_DST_LAT_LNG))

    val distance: String = cursor.getString(cursor.getColumnIndex(HistoryDbHelper.COLUMN_DISTANCE))
    val hashCode: String = cursor.getString(cursor.getColumnIndex(HistoryDbHelper.COLUMN_HASH))
}