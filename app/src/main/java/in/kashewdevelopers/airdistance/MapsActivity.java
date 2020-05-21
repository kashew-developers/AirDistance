package in.kashewdevelopers.airdistance;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
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
    TextView sourceNotFound, destinationNotFound;
    boolean placeDestinationMarkerOnMap = false, placeSourceMarkerOnMap = false;
    TextView tapOnMapMsg, distanceMsg;

    RelativeLayout.LayoutParams belowControlPanel, belowControlPanelToggle;

    // Markers
    Marker sourceMarker, destinationMarker;
    Polyline distanceLine;

    Geocoder geocoder;
    LocationCallback locationCallback = null;
    InputMethodManager imm;

    int MY_PERMISSIONS_REQUEST_LOCATION = 123;
    float angleCounter = 0;

    Toast gpsToast;

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
        sourceNotFound = findViewById(R.id.sourceNotFound);
        destinationPanel = findViewById(R.id.destinationPanel);
        destinationInputEditText = findViewById(R.id.destinationInput);
        destinationUseLocation = findViewById(R.id.useDestinationLocation);
        destinationChooseOnMap = findViewById(R.id.useDestinationOnMap);
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
        gpsToast = Toast.makeText(this, "Plsease turn on GPS", Toast.LENGTH_LONG);
        gpsToast.setGravity(Gravity.CENTER, 0, 0);


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
                animateAndChangeControlToggle();
                if (controlPanel.getVisibility() == View.GONE) {
                    controlPanel.setVisibility(View.VISIBLE);
                    distanceMsg.setLayoutParams(belowControlPanel);
                } else {
                    controlPanel.setVisibility(View.GONE);
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
                sourceInputEditText.setText("");
            }
        });

        destinationChooseOnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tapOnMapMsg.setVisibility(View.VISIBLE);
                placeDestinationMarkerOnMap = true;
                destinationInputEditText.clearFocus();
                destinationMarker.setDraggable(true);
                destinationInputEditText.setText("");
            }
        });


        sourceUseLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (hasLocationPermission()) {
                    if (isGPSOn()){
                        sourceInputEditText.setText("");
                        getLocationAndSetMarker("Source");
                    } else {
                        gpsToast.show();
                    }
                } else {
                    askLocationPermission();
                }
            }
        });

        destinationUseLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (hasLocationPermission()) {
                    if (isGPSOn()){
                        destinationInputEditText.setText("");
                        getLocationAndSetMarker("Destination");
                    } else {
                        gpsToast.show();
                    }
                } else {
                    askLocationPermission();
                }
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


    public void animateAndChangeControlToggle() {
        final int newIcon = (controlPanel.getVisibility() == View.GONE) ?
                            R.drawable.close_icon :
                            R.drawable.keyboard_icon;

        final float initialAngle = controlToggleButton.getRotation();
        angleCounter = 0;
        final Handler flip = new Handler();
        flip.postDelayed(new Runnable() {
            @Override
            public void run() {
                controlToggleButton.setRotation(initialAngle + angleCounter);
                angleCounter += 15;
                if ( angleCounter >= 90 )
                    controlToggleButton.setImageResource(newIcon);
                if ( angleCounter <= 180 )
                    flip.postDelayed(this, 8);
            }
        }, 10);

    }


    private LatLng stringToLatLng(String place) {
        try {
            List<Address> addressList = geocoder.getFromLocationName(place, 1);
            if (addressList.size() >= 1) {
                Address address = addressList.get(0);
                return new LatLng(address.getLatitude(), address.getLongitude());
            }
        } catch (Exception e) {
            Log.d("KashewDevelopers",
                    "sourceInputEditText onDataChange, afterTextChanged : " +
                            e.getMessage());
        }
        return null;
    }


    private void stringToSourceLatLng(String place) {
        if (place.length() > 2) {
            setSourceMarker(stringToLatLng(place));
        } else {
            setSourceMarker(null);
        }
    }


    private void stringToDestinationLatLng(String place) {
        if (place.length() > 2) {
            setDestinationMarker(stringToLatLng(place));
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
        double a = Math.sin(diffLat / 2) * Math.sin(diffLat / 2) +
                Math.cos(sourceMarker.getPosition().latitude * Math.PI / 180) *
                        Math.cos(destinationMarker.getPosition().latitude * Math.PI / 180) *
                        Math.sin(diffLon / 2) * Math.sin(diffLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = earthRadius * c;

        String unit;
        if (d < 1) {
            unit = "Meter";
            d *= 1000;
        } else {
            unit = "Km";
        }

        String formatted = getString(R.string.distance_msg, d, unit);
        distanceMsg.setText(formatted);

    }


    private boolean hasLocationPermission() {
        return ContextCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED;
    }


    private void askLocationPermission() {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {

            new AlertDialog.Builder(this)
                    .setTitle("Location Access Permission Needed")
                    .setCancelable(false)
                    .setMessage("Please give permission to get your location")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .create()
                    .show();

        } else {
            ActivityCompat
                    .requestPermissions(MapsActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_REQUEST_LOCATION);
        }
    }


    private void getLocationAndSetMarker(final String markerType) {

        final FusedLocationProviderClient mFusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        final LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10);
        locationRequest.setSmallestDisplacement(10);
        locationRequest.setFastestInterval(10);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                LatLng position = null;
                if (locationResult.getLastLocation() != null) {
                    Location currentLocation = locationResult.getLastLocation();
                    position = new LatLng(currentLocation.getLatitude(),
                            currentLocation.getLongitude());

                    mFusedLocationClient.removeLocationUpdates(locationCallback);
                }

                if (markerType.equals("Source")) {
                    setSourceMarker(position);
                } else if(markerType.equals("Destination")){
                    setDestinationMarker(position);
                }

            }
        };

        mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

    }


    public boolean isGPSOn() {
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (manager == null) return false;
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }


    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
