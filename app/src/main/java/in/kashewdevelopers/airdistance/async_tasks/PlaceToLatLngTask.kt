package `in`.kashewdevelopers.airdistance.async_tasks

import `in`.kashewdevelopers.airdistance.data_containers.LocationObject
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.AsyncTask
import android.text.Editable
import java.lang.ref.WeakReference

class PlaceToLatLngTask(context: Context, private val locationType: String, private val taskId: Int) : AsyncTask<Editable, Void, LocationObject>() {

    private val weakReference = WeakReference(context)

    override fun doInBackground(vararg params: Editable?): LocationObject? {
        if (params.isNullOrEmpty())
            return null

        val place: String = params[0].toString()

        val geocoder = Geocoder(weakReference.get())
        val addressList: MutableList<Address>

        try {
            addressList = geocoder.getFromLocationName(place, 10)
        } catch (e: Exception) {
            return null
        }

        if (addressList.isNullOrEmpty()) return null

        val address: Address = addressList[0]
        if (address.maxAddressLineIndex < 0) return null

        return LocationObject(address.getAddressLine(0), address.latitude, address.longitude)
    }

    override fun onPostExecute(result: LocationObject?) {
        super.onPostExecute(result)
        onTaskCompleteListener?.onTaskCompleteListener(result, locationType, taskId)
    }


    interface OnTaskCompleteListener {
        fun onTaskCompleteListener(locationObject: LocationObject?, locationType: String, taskId: Int)
    }

    private var onTaskCompleteListener: OnTaskCompleteListener? = null

    fun setOnTaskCompleteListener(onTaskCompleteListener: OnTaskCompleteListener?): PlaceToLatLngTask {
        this.onTaskCompleteListener = onTaskCompleteListener
        return this
    }

}
