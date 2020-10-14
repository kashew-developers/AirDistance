package in.kashewdevelopers.airdistance;

import androidx.annotation.NonNull;

public class LocationObject {
    String placeName;
    double lat, lng;

    public LocationObject(@NonNull String placeName, double lat, double lng) {
        this.placeName = placeName;
        this.lat = lat;
        this.lng = lng;
    }

}
