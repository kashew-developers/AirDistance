package in.kashewdevelopers.airdistance;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
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

    // Widgets
    private FloatingActionButton controlToggleButton;
    private LinearLayout controlPanel;
    ProgressBar sourceProgressBar, destinationProgressBar;
    RelativeLayout sourcePanel, destinationPanel;
    AutoCompleteTextView sourceInputEditText, destinationInputEditText;
    TextView sourceUseLocation, destinationUseLocation;
    TextView sourceChooseOnMap, destinationChooseOnMap;
    TextView sourceNotFound, destinationNotFound;
    TextView tapOnMapMsg, distanceMsg;

    // Map Elements
    private GoogleMap mMap;
    Marker sourceMarker, destinationMarker;
    Polyline distanceLine;
    boolean placeDestinationMarkerOnMap = false, placeSourceMarkerOnMap = false;

    Geocoder geocoder;
    LocationCallback locationCallback = null;
    InputMethodManager imm;

    int MY_PERMISSIONS_REQUEST_LOCATION = 123;

    Toast gpsToast;

    // database
    private SQLiteHelper dbHelper;
    private SQLiteDatabase db;
    SimpleCursorAdapter adapter;
    Cursor cursor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null)
            mapFragment.getMapAsync(this);


        initialize();

        controlToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sourceInputEditText.clearFocus();
                destinationInputEditText.clearFocus();

                animateAndChangeControlToggle();

                if (controlPanel.getVisibility() == View.GONE) {
                    controlPanel.setVisibility(View.VISIBLE);
                } else {
                    controlPanel.setVisibility(View.GONE);
                    disableMarkerPlacement();
                }
            }
        });


        // set listeners
        setFocusChangeListeners();
        setTextChangeListeners();
        setChooseOnMapListeners();
        setMyLocationListeners();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        // Initialize source marker, destination marker & distance line
        sourceMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(0, 0))
                .title("Source"));
        sourceMarker.setVisible(false);

        destinationMarker = mMap.addMarker(new MarkerOptions()
                .position(sourceMarker.getPosition())
                .title("Destination"));
        destinationMarker.setVisible(false);

        distanceLine = mMap.addPolyline(new PolylineOptions()
                .add(sourceMarker.getPosition(), sourceMarker.getPosition())
                .width(5)
                .color(Color.RED));
        distanceLine.setVisible(false);


        // click to place marker
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (placeDestinationMarkerOnMap) {
                    setDestinationMarker(new LocationObject("Destination",
                            latLng.latitude, latLng.longitude));
                } else if (placeSourceMarkerOnMap) {
                    setSourceMarker(new LocationObject("Source",
                            latLng.latitude, latLng.longitude));
                } else if (controlPanel.getVisibility() == View.VISIBLE) {
                    controlToggleButton.performClick();
                }
            }
        });


        // drag marker
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
                    setDestinationMarker(new LocationObject("Destination",
                            marker.getPosition().latitude, marker.getPosition().longitude));
                } else if (placeSourceMarkerOnMap) {
                    setSourceMarker(new LocationObject("Source",
                            marker.getPosition().latitude, marker.getPosition().longitude));
                }
            }
        });


        // on marker click, show infoWindow only, do not center map
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (!marker.isInfoWindowShown()) {
                    marker.showInfoWindow();
                }
                return true;
            }
        });
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }


    // initialize
    public void initialize() {
        initializeWidgets();

        setWidgetsVisibility();

        geocoder = new Geocoder(getApplicationContext());
        imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);

        gpsToast = Toast.makeText(this, "Please turn on GPS", Toast.LENGTH_LONG);
        gpsToast.setGravity(Gravity.CENTER, 0, 0);

        initializeDbElements();
    }

    public void initializeDbElements() {
        dbHelper = new SQLiteHelper(this);
        try {
            db = dbHelper.getReadableDatabase();

            cursor = dbHelper.search(db, "");

            adapter = new SimpleCursorAdapter(this, R.layout.suggestion_list_layout,
                    cursor, new String[]{"NAME"}, new int[]{R.id.text});

            adapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
                @Override
                public CharSequence convertToString(Cursor cursor) {
                    return cursor.getString(1);
                }
            });

            adapter.setFilterQueryProvider(new FilterQueryProvider() {
                @Override
                public Cursor runQuery(CharSequence charSequence) {
                    if (db != null) {
                        cursor = dbHelper.search(db, charSequence.toString());
                    }
                    return cursor;
                }
            });

            sourceInputEditText.setAdapter(adapter);
            sourceInputEditText.setThreshold(1);

            destinationInputEditText.setAdapter(adapter);
            destinationInputEditText.setThreshold(1);
        } catch (Exception e) {
            db = null;
            Log.d("KashewDevelopers", "Error : " + e.getMessage());
        }


    }

    public void initializeWidgets() {
        // control widgets
        controlToggleButton = findViewById(R.id.controlToggleButton);
        controlPanel = findViewById(R.id.controlsPanel);

        // source widgets
        sourceProgressBar = findViewById(R.id.sourceProgressBar);
        sourcePanel = findViewById(R.id.sourcePanel);
        sourceInputEditText = findViewById(R.id.sourceInput);
        sourceInputEditText.setThreshold(1);
        sourceUseLocation = findViewById(R.id.useSourceLocation);
        sourceChooseOnMap = findViewById(R.id.useSourceOnMap);
        sourceNotFound = findViewById(R.id.sourceNotFound);

        // destination widgets
        destinationProgressBar = findViewById(R.id.destinationProgressBar);
        destinationPanel = findViewById(R.id.destinationPanel);
        destinationInputEditText = findViewById(R.id.destinationInput);
        destinationInputEditText.setThreshold(1);
        destinationUseLocation = findViewById(R.id.useDestinationLocation);
        destinationChooseOnMap = findViewById(R.id.useDestinationOnMap);
        destinationNotFound = findViewById(R.id.destinationNotFound);

        // info widgets
        tapOnMapMsg = findViewById(R.id.tapOnMapMsg);
        distanceMsg = findViewById(R.id.distanceMsg);
    }

    public void setWidgetsVisibility() {
        controlPanel.setVisibility(View.GONE);

        sourceUseLocation.setVisibility(View.GONE);
        sourceChooseOnMap.setVisibility(View.GONE);
        sourceProgressBar.setVisibility(View.GONE);

        destinationUseLocation.setVisibility(View.GONE);
        destinationChooseOnMap.setVisibility(View.GONE);
        destinationProgressBar.setVisibility(View.GONE);

        distanceMsg.setVisibility(View.GONE);
        tapOnMapMsg.setVisibility(View.GONE);
    }


    // listeners
    public void setFocusChangeListeners() {
        sourceInputEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                int visibility = hasFocus ? View.VISIBLE : View.GONE;
                sourceUseLocation.setVisibility(visibility);
                sourceChooseOnMap.setVisibility(visibility);

                if (!hasFocus) {
                    try {
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    } catch (Exception e) {
                        Log.d("KashewDevelopers", "Exception e : " + e.getMessage());
                    }
                } else {
                    disableMarkerPlacement();
                }
            }
        });

        destinationInputEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                int visibility = hasFocus ? View.VISIBLE : View.GONE;
                destinationUseLocation.setVisibility(visibility);
                destinationChooseOnMap.setVisibility(visibility);

                if (!hasFocus) {
                    try {
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    } catch (Exception e) {
                        Log.d("KashewDevelopers", "Exception e : " + e.getMessage());
                    }
                } else {
                    disableMarkerPlacement();
                }
            }
        });
    }

    public void setTextChangeListeners() {
        sourceInputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(final Editable editable) {
                if (!isNetworkConnected()) {
                    return;
                }

                sourceProgressBar.setVisibility(View.VISIBLE);
                sourceNotFound.setVisibility(View.GONE);

                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            stringToSourceLatLng(editable.toString());
                        } catch (Exception e) {
                            Log.d("KashewDevelopers",
                                    "sourceInputEditText thread error : " + e.getMessage());
                        }
                    }
                };
                thread.start();
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
                if (!isNetworkConnected()) {
                    return;
                }

                destinationProgressBar.setVisibility(View.VISIBLE);
                destinationNotFound.setVisibility(View.GONE);

                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            stringToDestinationLatLng(editable.toString());
                        } catch (Exception e) {
                            Log.d("KashewDevelopers",
                                    "destinationInputEditText thread error : " + e.getMessage());
                        }
                    }
                };

                thread.start();
            }
        });
    }

    public void setChooseOnMapListeners() {
        sourceChooseOnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sourceInputEditText.clearFocus();
                sourceInputEditText.setText("");
                sourceNotFound.setVisibility(View.GONE);

                tapOnMapMsg.setVisibility(View.VISIBLE);
                placeSourceMarkerOnMap = true;
                sourceMarker.setDraggable(true);
            }
        });

        destinationChooseOnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                destinationInputEditText.clearFocus();
                destinationInputEditText.setText("");
                destinationNotFound.setVisibility(View.GONE);

                tapOnMapMsg.setVisibility(View.VISIBLE);
                placeDestinationMarkerOnMap = true;
                destinationMarker.setDraggable(true);
            }
        });
    }

    public void setMyLocationListeners() {
        sourceUseLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (hasLocationPermission()) {
                    if (isGPSOn()) {
                        sourceInputEditText.clearFocus();
                        sourceInputEditText.setText("");
                        sourceNotFound.setVisibility(View.GONE);
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
                    if (isGPSOn()) {
                        destinationInputEditText.clearFocus();
                        destinationInputEditText.setText("");
                        destinationNotFound.setVisibility(View.GONE);
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


    // place to latlng
    private LocationObject stringToLatLng(String place) {
        try {
            List<Address> addressList = geocoder.getFromLocationName(place, 10);
            if (addressList.size() >= 1) {
                Address address = addressList.get(0);

                writePlaceToDB(address.getAddressLine(0));

                return new LocationObject(address.getAddressLine(0),
                        address.getLatitude(), address.getLongitude());
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


    // set marker
    private void setSourceMarker(final LocationObject locationObject) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                sourceProgressBar.setVisibility(View.GONE);
                distanceMsg.setVisibility(View.GONE);
                if (locationObject != null) {
                    LatLng latLng = new LatLng(locationObject.lat, locationObject.lng);
                    sourceMarker.setPosition(latLng);
                    sourceMarker.setVisible(true);
                    sourceMarker.setTitle(locationObject.placeName);
                    sourceMarker.showInfoWindow();
                    sourceNotFound.setVisibility(View.GONE);

                    if (!placeSourceMarkerOnMap) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
                    }

                    if (destinationMarker.isVisible()) {
                        getDistance();
                    }
                } else {
                    sourceMarker.setVisible(false);
                    distanceLine.setVisible(false);
                    if (sourceInputEditText.getText().length() > 0)
                        sourceNotFound.setVisibility(View.VISIBLE);
                }
            }
        });

    }

    private void setDestinationMarker(final LocationObject locationObject) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                destinationProgressBar.setVisibility(View.GONE);
                distanceMsg.setVisibility(View.GONE);
                if (locationObject != null) {
                    LatLng latLng = new LatLng(locationObject.lat, locationObject.lng);
                    destinationMarker.setPosition(latLng);
                    destinationMarker.setVisible(true);
                    destinationMarker.setTitle(locationObject.placeName);
                    destinationMarker.showInfoWindow();
                    destinationNotFound.setVisibility(View.GONE);

                    if (!placeDestinationMarkerOnMap) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
                    }

                    if (sourceMarker.isVisible()) {
                        getDistance();
                    }
                } else {
                    destinationMarker.setVisible(false);
                    distanceLine.setVisible(false);
                    if (destinationInputEditText.getText().length() > 0)
                        destinationNotFound.setVisibility(View.VISIBLE);
                }
            }
        });

    }


    // network, gps & permission
    public boolean isNetworkConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null)
            return true;

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    private boolean hasLocationPermission() {
        return ContextCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED;
    }

    private void askLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {

            new AlertDialog.Builder(this)
                    .setTitle("Location Permission Needed")
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
                    .requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_REQUEST_LOCATION);
        }
    }

    public boolean isGPSOn() {
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (manager == null) return false;
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
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
                Location currentLocation = null;
                if (locationResult.getLastLocation() != null) {
                    currentLocation = locationResult.getLastLocation();

                    mFusedLocationClient.removeLocationUpdates(locationCallback);
                }

                if (markerType.equals("Source")) {
                    if (currentLocation == null)
                        setSourceMarker(null);
                    else
                        setSourceMarker(new LocationObject("Source",
                                currentLocation.getLatitude(),
                                currentLocation.getLongitude()));
                } else if (markerType.equals("Destination")) {
                    if (currentLocation == null)
                        setDestinationMarker(null);
                    else
                        setDestinationMarker(new LocationObject("Destination",
                                currentLocation.getLatitude(),
                                currentLocation.getLongitude()));
                }

            }
        };

        mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

    }


    // functionality
    public void disableMarkerPlacement() {
        try {
            placeDestinationMarkerOnMap = placeSourceMarkerOnMap = false;
            sourceMarker.setDraggable(false);
            destinationMarker.setDraggable(false);
            tapOnMapMsg.setVisibility(View.GONE);
        } catch (Exception e) {
            Log.d("KashewDevelopers", "Exception e : " + e.getMessage());
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

    public void animateAndChangeControlToggle() {
        final int newIcon = (controlPanel.getVisibility() == View.GONE) ?
                R.drawable.close_icon :
                R.drawable.keyboard_icon;

        final float angle = (controlPanel.getVisibility() == View.GONE) ? 180f : 0f;

        controlToggleButton.animate().setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        controlToggleButton.setImageResource(newIcon);
                    }
                })
                .rotation(angle);
    }

    public void writePlaceToDB(String place) {
        if (dbHelper != null && db != null) {
            dbHelper.insert(db, place);
        }
    }

}