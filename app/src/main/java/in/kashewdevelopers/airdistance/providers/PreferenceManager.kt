package `in`.kashewdevelopers.airdistance.providers

import android.content.Context

class PreferenceManager {
    companion object {
        private const val unitPreferenceName = "distanceUnit"
        private const val distanceUnitName = "distance"
        private const val defaultDistanceUnit = "Km"

        fun getDistanceUnitPreference(context: Context): String {
            val pref = context.getSharedPreferences(unitPreferenceName, Context.MODE_PRIVATE)
            return pref.getString(distanceUnitName, defaultDistanceUnit) ?: defaultDistanceUnit
        }

        fun setDistanceUnitPreference(context: Context, distanceUnit: String) {
            val pref = context.getSharedPreferences(unitPreferenceName, Context.MODE_PRIVATE)
            with(pref.edit()) {
                putString(distanceUnitName, distanceUnit)
                apply()
            }
        }
    }
}