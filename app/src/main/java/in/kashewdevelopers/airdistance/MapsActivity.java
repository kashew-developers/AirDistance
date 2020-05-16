package in.kashewdevelopers.airdistance;

import androidx.fragment.app.FragmentActivity;

import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private FloatingActionButton controlToggleButton;
    private RelativeLayout controlPanel;
    EditText sourceInputEditText, destinationInputEditText;

    // Markers
    Marker sourceMarker, destinationMarker;
    Polyline distanceLine;

    Geocoder geocoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null)
            mapFragment.getMapAsync(this);


        // initialize widgets
        controlToggleButton = findViewById(R.id.controlToggleButton);
        controlPanel = findViewById(R.id.controlsPanel);
        sourceInputEditText = findViewById(R.id.sourceInput);
        destinationInputEditText = findViewById(R.id.destinationInput);


        // other variables
        geocoder = new Geocoder(getApplicationContext());


        controlToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (controlPanel.getVisibility() == View.GONE) {
                    controlPanel.setVisibility(View.VISIBLE);
                    controlToggleButton.setImageResource(R.drawable.close_icon);
                } else {
                    controlPanel.setVisibility(View.GONE);
                    controlToggleButton.setImageResource(R.drawable.keyboard_icon);
                }
            }
        });


        sourceInputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(final Editable editable) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stringToLatLng(editable.toString(), "source");
                    }
                }, 50);
            }
        });

        destinationInputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(final Editable editable) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stringToLatLng(editable.toString(), "destination");
                    }
                }, 50);
            }
        });

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        sourceMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)));
        sourceMarker.setVisible(false);
        destinationMarker = mMap.addMarker(new MarkerOptions().position(sourceMarker.getPosition()));
        destinationMarker.setVisible(false);
        distanceLine = mMap.addPolyline(new PolylineOptions()
                .add(sourceMarker.getPosition(), sourceMarker.getPosition())
                .width(5)
                .color(Color.RED));
        distanceLine.setVisible(false);

    }


    private void stringToLatLng(String place, String inputLocation) {
        Log.d("KashewDevelopers", "In stringToLatLng, place : " + place + " inputType : " + inputLocation);
        if (place.length() > 3) {
            try {
                List<Address> addressList = geocoder.getFromLocationName(place, 1);
                Log.d("KashewDevelopers", "In stringToLatLng, post getFromLocationName, listSize : " + addressList.size());
                if (addressList.size() < 1) {
                    if (inputLocation.equals("source"))
                        setSourceMarker(null);
                    else
                        setDestinationMarker(null);
                } else {
                    Address address = addressList.get(0);

                    if (inputLocation.equals("source")) {
                        setSourceMarker(new LatLng(address.getLatitude(), address.getLongitude()));
                    } else {
                        setDestinationMarker(new LatLng(address.getLatitude(),
                                address.getLongitude()));
                    }
                }
            } catch (Exception e) {
                Log.d("KashewDevelopers",
                        "sourceInputEditText onDataChange, afterTextChanged : " +
                                e.getMessage());
                if (inputLocation.equals("source"))
                    setSourceMarker(null);
                else
                    setDestinationMarker(null);
            }
        } else {
            if (inputLocation.equals("source"))
                setSourceMarker(null);
            else
                setDestinationMarker(null);
        }
    }


    private void setSourceMarker(LatLng val) {

        if (val != null) {
            sourceMarker.setPosition(val);
            sourceMarker.setVisible(true);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(val));
            if (destinationMarker.isVisible()) {
                getDistance();
            }
        } else {
            sourceMarker.setVisible(false);
            distanceLine.setVisible(false);
        }

    }


    private void setDestinationMarker(LatLng val) {

        if (val != null) {
            destinationMarker.setPosition(val);
            destinationMarker.setVisible(true);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(val));
            if (sourceMarker.isVisible()) {
                getDistance();
            }
        } else {
            destinationMarker.setVisible(false);
            distanceLine.setVisible(false);
        }

    }


    private void getDistance() {

        distanceLine.remove();

        distanceLine = mMap.addPolyline(new PolylineOptions()
                .add(sourceMarker.getPosition(), destinationMarker.getPosition())
                .width(5)
                .color(Color.RED));

    }

}
