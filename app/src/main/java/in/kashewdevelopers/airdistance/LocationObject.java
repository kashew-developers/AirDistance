package in.kashewdevelopers.airdistance;

import androidx.annotation.NonNull;

class LocationObject {
    String placeName;
    double lat, lng;

    LocationObject(@NonNull String placeName, double lat, double lng) {
        this.placeName = placeName;
        this.lat = lat;
        this.lng = lng;
    }

}
