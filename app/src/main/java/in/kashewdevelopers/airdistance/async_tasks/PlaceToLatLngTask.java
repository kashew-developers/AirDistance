package in.kashewdevelopers.airdistance.async_tasks;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.text.Editable;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.List;

import in.kashewdevelopers.airdistance.LocationObject;


@SuppressWarnings("deprecation")
public class PlaceToLatLngTask extends AsyncTask<Editable, Void, LocationObject> {

    public interface OnTaskCompleteListener {
        void onTaskCompleteListener(LocationObject locationObject, String locationType, int taskId);
    }

    OnTaskCompleteListener onTaskCompleteListener;

    private WeakReference<Context> context;
    private String locationType;
    private int taskId;

    public PlaceToLatLngTask(@NonNull Context context, @NonNull String locationType, int taskId) {
        this.context = new WeakReference<>(context);
        this.locationType = locationType;
        this.taskId = taskId;
    }

    @Override
    protected LocationObject doInBackground(Editable... placeList) {
        if (placeList.length <= 0) {
            return null;
        }

        String place = String.valueOf(placeList[0]);

        Geocoder geocoder = new Geocoder(context.get());
        List<Address> addressList;

        try {
            addressList = geocoder.getFromLocationName(place, 10);
        } catch (Exception e) {
            addressList = null;
        }

        if (addressList != null && addressList.size() >= 1) {
            Address address = addressList.get(0);

            if (address.getMaxAddressLineIndex() < 0)
                return null;

            return new LocationObject(address.getAddressLine(0),
                    address.getLatitude(), address.getLongitude());
        }

        return null;
    }

    @Override
    protected void onPostExecute(LocationObject locationObject) {
        super.onPostExecute(locationObject);
        if (onTaskCompleteListener != null) {
            onTaskCompleteListener.onTaskCompleteListener(locationObject, locationType, taskId);
        }
    }

    public PlaceToLatLngTask setOnTaskCompleteListener(@NonNull OnTaskCompleteListener onTaskCompleteListener) {
        this.onTaskCompleteListener = onTaskCompleteListener;
        return this;
    }
}
