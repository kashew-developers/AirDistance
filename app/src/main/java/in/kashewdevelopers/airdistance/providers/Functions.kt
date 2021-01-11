package `in`.kashewdevelopers.airdistance.providers

import android.content.Context
import android.view.Gravity
import android.widget.Toast
import com.google.android.gms.maps.model.LatLng
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Functions {
    companion object {
        fun getDistanceInKm(srcCoordinate: LatLng, dstCoordinate: LatLng): Double {
            // havershine formula
            val earthRadius = 6378.137

            val diffLat = (srcCoordinate.latitude * Math.PI / 180) -
                    (dstCoordinate.latitude * Math.PI / 180)
            val diffLng = (srcCoordinate.longitude * Math.PI / 180) -
                    (dstCoordinate.longitude * Math.PI / 180)

            val a = (sin(diffLat / 2) * sin(diffLat / 2)) +
                    (cos(srcCoordinate.latitude * Math.PI / 180) *
                            cos(dstCoordinate.latitude * Math.PI / 180) *
                            sin(diffLng / 2) * sin(diffLng / 2))

            val c = 2 * atan2(sqrt(a), sqrt(1 - a))

            return c * earthRadius
        }

        fun showToast(context: Context, stringRes: Int, centerToast: Boolean) {
            val toast = Toast.makeText(context, stringRes, Toast.LENGTH_LONG)
            if (centerToast) {
                toast.setGravity(Gravity.CENTER, 0, 0)
            }
            toast.show()
        }

    }
}