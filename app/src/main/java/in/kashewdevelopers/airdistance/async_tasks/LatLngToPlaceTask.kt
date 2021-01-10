package `in`.kashewdevelopers.airdistance.async_tasks

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.AsyncTask
import com.google.android.gms.maps.model.LatLng
import java.lang.ref.WeakReference

class LatLngToPlaceTask(context: Context, private val placeType: String) : AsyncTask<LatLng, Void, String>() {

    private val weakReference = WeakReference(context)
    private lateinit var coordinates: LatLng

    override fun doInBackground(vararg params: LatLng?): String {
        if (params.isNullOrEmpty())
            return ""

        coordinates = params[0] ?: return ""

        val geocoder = Geocoder(weakReference.get())
        val addressList: MutableList<Address>

        try {
            addressList = geocoder.getFromLocation(coordinates.latitude, coordinates.longitude, 1)
        } catch (e: Exception) {
            return ""
        }

        if (addressList.isNullOrEmpty()) return ""

        val address = addressList[0]
        if (address.maxAddressLineIndex < 0) return ""

        return address.getAddressLine(0)
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        if (result.isNullOrBlank()) return
        onTaskCompleteListener?.onTaskCompleteListener(coordinates, result, placeType)
    }

    interface OnTaskCompleteListener {
        fun onTaskCompleteListener(coordinates: LatLng, placeName: String, placeType: String)
    }

    private var onTaskCompleteListener: OnTaskCompleteListener? = null

    fun setTaskListener(onTaskCompleteListener: OnTaskCompleteListener?): LatLngToPlaceTask {
        this.onTaskCompleteListener = onTaskCompleteListener
        return this
    }

}
