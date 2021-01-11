package `in`.kashewdevelopers.airdistance.providers

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat

class SystemFeatures {
    companion object {
        fun hasPermission(context: Context, permission: String): Boolean {
            return ContextCompat
                    .checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED
        }

        fun gpsIsOn(context: Context): Boolean {
            val locationManager = context
                    .getSystemService(Context.LOCATION_SERVICE) as LocationManager?
            locationManager ?: return false
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }

        fun closeKeyboard(context: Context, view: View) {
            val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm ?: return
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}