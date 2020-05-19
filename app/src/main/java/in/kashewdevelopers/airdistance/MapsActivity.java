package in.kashewdevelopers.airdistance;

import androidx.fragment.app.FragmentActivity;

import android.app.Activity;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    // Widgets
    private FloatingActionButton controlToggleButton;
    private LinearLayout controlPanel;
    RelativeLayout sourcePanel, destinationPanel;
    EditText sourceInputEditText, destinationInputEditText;
    TextView sourceUseLocation, destinationUseLocation;
    TextView sourceChooseOnMap, destinationChooseOnMap;
    ProgressBar sourceProgressBar, destinationProgressBar;
    TextView sourceNotFound, destinationNotFound;
    boolean placeDestinationMarkerOnMap = false, placeSourceMarkerOnMap = false;
    TextView tapOnMapMsg, distanceMsg;

    RelativeLayout.LayoutParams belowControlPanel, belowControlPanelToggle;

    // Markers
    Marker sourceMarker, destinationMarker;
    Polyline distanceLine;

    Geocoder geocoder;
    InputMethodManager imm;

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
        sourcePanel = findViewById(R.id.sourcePanel);
        sourceInputEditText = findViewById(R.id.sourceInput);
        sourceUseLocation = findViewById(R.id.useSourceLocation);
        sourceChooseOnMap = findViewById(R.id.useSourceOnMap);
        sourceProgressBar = findViewById(R.id.sourceProgressBar);
        sourceNotFound = findViewById(R.id.sourceNotFound);
        destinationPanel = findViewById(R.id.destinationPanel);
        destinationInputEditText = findViewById(R.id.destinationInput);
        destinationUseLocation = findViewById(R.id.useDestinationLocation);
        destinationChooseOnMap = findViewById(R.id.useDestinationOnMap);
        destinationProgressBar = findViewById(R.id.destinationProgressBar);
        destinationNotFound = findViewById(R.id.destinationNotFound);
        tapOnMapMsg = findViewById(R.id.tapOnMapMsg);
        distanceMsg = findViewById(R.id.distanceMsg);

        controlPanel.setVisibility(View.GONE);
        sourceUseLocation.setVisibility(View.GONE);
        sourceChooseOnMap.setVisibility(View.GONE);
        destinationUseLocation.setVisibility(View.GONE);
        destinationChooseOnMap.setVisibility(View.GONE);
        distanceMsg.setVisibility(View.GONE);

        belowControlPanel = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        belowControlPanel.setMargins(0, 10, 0, 0);
        belowControlPanel.addRule(RelativeLayout.BELOW, controlPanel.getId());
        belowControlPanelToggle = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        belowControlPanelToggle.setMargins(0, 20, 0, 0);
        belowControlPanelToggle.addRule(RelativeLayout.BELOW, controlToggleButton.getId());


        // other variables
        geocoder = new Geocoder(getApplicationContext());
        imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);


        sourceInputEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                int visibility = b ? View.VISIBLE : View.GONE;
                sourceUseLocation.setVisibility(visibility);
                sourceChooseOnMap.setVisibility(visibility);

                if (!b) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                } else {
                    placeDestinationMarkerOnMap = placeSourceMarkerOnMap = false;
                    sourceMarker.setDraggable(false);
                    destinationMarker.setDraggable(false);
                    tapOnMapMsg.setVisibility(View.GONE);
                }
            }
        });

        destinationInputEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                int visibility = b ? View.VISIBLE : View.GONE;
                destinationUseLocation.setVisibility(visibility);
                destinationChooseOnMap.setVisibility(visibility);

                if (!b) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                } else {
                    placeDestinationMarkerOnMap = placeSourceMarkerOnMap = false;
                    sourceMarker.setDraggable(false);
                    destinationMarker.setDraggable(false);
                    tapOnMapMsg.setVisibility(View.GONE);
                }
            }
        });


        controlToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sourceInputEditText.clearFocus();
                destinationInputEditText.clearFocus();
                if (controlPanel.getVisibility() == View.GONE) {
                    controlPanel.setVisibility(View.VISIBLE);
                    controlToggleButton.setImageResource(R.drawable.close_icon);
                    distanceMsg.setLayoutParams(belowControlPanel);
                } else {
                    controlPanel.setVisibility(View.GONE);
                    controlToggleButton.setImageResource(R.drawable.keyboard_icon);
                    placeSourceMarkerOnMap = placeDestinationMarkerOnMap = false;
                    sourceMarker.setDraggable(false);
                    destinationMarker.setDraggable(false);
                    tapOnMapMsg.setVisibility(View.GONE);
                    distanceMsg.setLayoutParams(belowControlPanelToggle);
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
                        stringToSourceLatLng(editable.toString());
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
                        stringToDestinationLatLng(editable.toString());
                    }
                }, 50);
            }
        });

        sourceChooseOnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tapOnMapMsg.setVisibility(View.VISIBLE);
                placeSourceMarkerOnMap = true;
                sourceInputEditText.clearFocus();
                sourceMarker.setDraggable(true);
            }
        });

        destinationChooseOnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tapOnMapMsg.setVisibility(View.VISIBLE);
                placeDestinationMarkerOnMap = true;
                destinationInputEditText.clearFocus();
                destinationMarker.setDraggable(true);
            }
        });

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Initialize source marker, destination marker & distance line
        sourceMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Source"));
        sourceMarker.setVisible(false);
        destinationMarker = mMap.addMarker(new MarkerOptions().position(sourceMarker.getPosition()).title("Destination"));
        destinationMarker.setVisible(false);
        distanceLine = mMap.addPolyline(new PolylineOptions()
                .add(sourceMarker.getPosition(), sourceMarker.getPosition())
                .width(5)
                .color(Color.RED));
        distanceLine.setVisible(false);


        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (placeDestinationMarkerOnMap) {
                    setDestinationMarker(latLng);
                } else if (placeSourceMarkerOnMap) {
                    setSourceMarker(latLng);
                } else if (controlPanel.getVisibility() == View.VISIBLE) {
                    controlToggleButton.performClick();
                }
            }
        });


        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {

            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                if (placeDestinationMarkerOnMap) {
                    setDestinationMarker(marker.getPosition());
                } else if (placeSourceMarkerOnMap) {
                    setSourceMarker(marker.getPosition());
                }
            }
        });

    }


    private LatLng stringToLatLng(String place) {
        try {
            List<Address> addressList = geocoder.getFromLocationName(place, 1);
            Log.d("KashewDevelopers", "In stringToLatLng, listSize : " + addressList.size());
            if (addressList.size() < 1) {
                return null;
            } else {
                Address address = addressList.get(0);
                return new LatLng(address.getLatitude(), address.getLongitude());
            }
        } catch (Exception e) {
            Log.d("KashewDevelopers",
                    "sourceInputEditText onDataChange, afterTextChanged : " +
                            e.getMessage());
            return null;
        }
    }


    private void stringToSourceLatLng(String place) {
        if (place.length() > 2) {
            sourceProgressBar.setVisibility(View.VISIBLE);
            setSourceMarker(stringToLatLng(place));
            sourceProgressBar.setVisibility(View.GONE);
        } else {
            setSourceMarker(null);
        }
    }


    private void stringToDestinationLatLng(String place) {
        if (place.length() > 2) {
            destinationProgressBar.setVisibility(View.VISIBLE);
            setDestinationMarker(stringToLatLng(place));
            destinationProgressBar.setVisibility(View.GONE);
        } else {
            setDestinationMarker(null);
        }
    }


    private void setSourceMarker(LatLng val) {

        distanceMsg.setVisibility(View.GONE);
        if (val != null) {
            sourceMarker.setPosition(val);
            sourceMarker.setVisible(true);
            sourceNotFound.setVisibility(View.GONE);

            if (!placeSourceMarkerOnMap) {
                mMap.moveCamera(CameraUpdateFactory.newLatLng(val));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
            }

            if (destinationMarker.isVisible()) {
                getDistance();
            }
        } else {
            sourceMarker.setVisible(false);
            distanceLine.setVisible(false);
            sourceNotFound.setVisibility(View.VISIBLE);
        }

    }


    private void setDestinationMarker(LatLng val) {

        distanceMsg.setVisibility(View.GONE);
        if (val != null) {
            destinationMarker.setPosition(val);
            destinationMarker.setVisible(true);
            destinationNotFound.setVisibility(View.GONE);

            if (!placeDestinationMarkerOnMap) {
                mMap.moveCamera(CameraUpdateFactory.newLatLng(val));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
            }

            if (sourceMarker.isVisible()) {
                getDistance();
            }
        } else {
            destinationMarker.setVisible(false);
            distanceLine.setVisible(false);
            destinationNotFound.setVisibility(View.VISIBLE);
        }

    }


    private void getDistance() {

        distanceLine.remove();

        distanceLine = mMap.addPolyline(new PolylineOptions()
                .add(sourceMarker.getPosition(), destinationMarker.getPosition())
                .width(5)
                .color(Color.RED));

        distanceMsg.setVisibility(View.VISIBLE);

        double earthRadius = 6378.137; // Radius of earth in KM
        double diffLat = sourceMarker.getPosition().latitude * Math.PI / 180
                - destinationMarker.getPosition().latitude * Math.PI / 180;
        double diffLon = sourceMarker.getPosition().longitude * Math.PI / 180
                - destinationMarker.getPosition().longitude * Math.PI / 180;
        double a = Math.sin(diffLat/2) * Math.sin(diffLat/2) +
                Math.cos(sourceMarker.getPosition().latitude * Math.PI / 180) *
                        Math.cos(destinationMarker.getPosition().latitude * Math.PI / 180) *
                        Math.sin(diffLon/2) * Math.sin(diffLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = earthRadius * c;

        String unit;
        if ( d < 1 ) {
            unit = "Meter";
            d *= 1000;
        } else {
            unit = "Km";
        }

        String formatted = getString(R.string.distance_msg, d, unit);
        distanceMsg.setText(formatted);

    }


    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
