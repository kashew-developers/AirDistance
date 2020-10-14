package in.kashewdevelopers.airdistance.async_tasks;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

import java.lang.ref.WeakReference;
import java.util.List;

@SuppressWarnings("deprecation")
public class LatLngToPlaceTask extends AsyncTask<LatLng, Void, String> {

    private WeakReference<Context> weakReference;
    private LatLng coordinates;
    private String placeType;

    public LatLngToPlaceTask(@NonNull Context context, @NonNull String placeType) {
        weakReference = new WeakReference<>(context);
        this.placeType = placeType;
    }

    @Override
    protected String doInBackground(LatLng... latLngs) {
        if (latLngs == null || latLngs.length <= 0)
            return null;

        coordinates = latLngs[0];
        Geocoder geocoder = new Geocoder(weakReference.get());
        List<Address> addressList;

        try {
            addressList = geocoder.getFromLocation(coordinates.latitude, coordinates.longitude, 1);
        } catch (Exception e) {
            return null;
        }

        if (addressList == null || addressList.size() <= 0)
            return null;

        Address address = addressList.get(0);
        if (address.getMaxAddressLineIndex() < 0)
            return null;

        return address.getAddressLine(0);
    }

    @Override
    protected void onPostExecute(String placeName) {
        super.onPostExecute(placeName);
        if (onTaskCompleteListener != null && placeName != null) {
            onTaskCompleteListener.onTaskCompleteListener(coordinates, placeName, placeType);
        }
    }

    public interface OnTaskCompleteListener {
        void onTaskCompleteListener(LatLng coordinates, String placeName, String placeType);
    }

    private OnTaskCompleteListener onTaskCompleteListener;

    public LatLngToPlaceTask setTaskListener(OnTaskCompleteListener onTaskCompleteListener) {
        this.onTaskCompleteListener = onTaskCompleteListener;
        return this;
    }

}
