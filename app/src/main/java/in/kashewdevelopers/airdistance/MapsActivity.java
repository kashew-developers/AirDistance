package in.kashewdevelopers.airdistance;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
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
    private FloatingActionButton controlToggleButton, distanceUnitButton;
    private LinearLayout controlPanel;
    ProgressBar sourceProgressBar, destinationProgressBar;
    ConstraintLayout sourcePanel, destinationPanel;
    AutoCompleteTextView sourceInputEditText, destinationInputEditText;
    ImageView clearSource, clearDestination;
    TextView sourceUseLocation, destinationUseLocation;
    TextView sourceChooseOnMap, destinationChooseOnMap;
    TextView sourceNotFound, destinationNotFound;
    TextView tapOnMapMsg, distanceMsg;
    TextWatcher sourceTextWatcher, destinationTextWatcher;


    // Navigation Drawer UI
    DrawerLayout drawerLayout;
    ConstraintLayout navigationDrawer;
    ImageView clearHistoryButton;
    TextView nothingToShowTv;
    ListView historyList;
    ActionBarDrawerToggle drawerToggle;


    // Map Elements
    private GoogleMap mMap;
    Marker sourceMarker, destinationMarker;
    Polyline distanceLine;
    boolean placeDestinationMarkerOnMap = false, placeSourceMarkerOnMap = false;
    Geocoder geocoder;
    LocationCallback locationCallback = null;


    int MY_PERMISSIONS_REQUEST_LOCATION = 123;

    InputMethodManager imm;
    Toast gpsToast, noInternetToast;


    // suggestion db
    private SuggestionDbHelper suggestionDbHelper;
    private SQLiteDatabase suggestionDb;
    SimpleCursorAdapter suggestionAdapter;
    Cursor suggestionCursor;


    // history db
    private HistoryDbHelper historyDbHelper;
    private SQLiteDatabase historyDb;
    HistoryAdapter historyAdapter;
    Cursor historyCursor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // make activity full screen - no status bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
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

        controlToggleButton.performClick();


        // set listeners
        setFocusChangeListeners();
        setTextChangeListeners();
        setChooseOnMapListeners();
        setMyLocationListeners();
        setHistoryClickListener();
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
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            moveTaskToBack(true);
        }
    }


    // initialize
    @SuppressLint("ShowToast")
    public void initialize() {
        initializeWidgets();

        setWidgetsVisibility();

        geocoder = new Geocoder(getApplicationContext());
        imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);

        gpsToast = Toast.makeText(this, "Please turn on GPS", Toast.LENGTH_LONG);
        gpsToast.setGravity(Gravity.CENTER, 0, 0);

        noInternetToast = Toast.makeText(this, "No Internet Connection", Toast.LENGTH_SHORT);
        noInternetToast.setGravity(Gravity.CENTER, 0, 0);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);

        initializeSuggestionDbElements();
        initializeHistoryDbElements();
    }

    public void initializeSuggestionDbElements() {
        suggestionDbHelper = new SuggestionDbHelper(this);
        try {
            suggestionDb = suggestionDbHelper.getReadableDatabase();
            suggestionCursor = suggestionDbHelper.search(suggestionDb, "");

            suggestionAdapter = new SimpleCursorAdapter(this, R.layout.suggestion_list_layout,
                    suggestionCursor, new String[]{"NAME"}, new int[]{R.id.text});

            suggestionAdapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
                @Override
                public CharSequence convertToString(Cursor cursor) {
                    return cursor.getString(cursor.getColumnIndex("NAME"));
                }
            });

            suggestionAdapter.setFilterQueryProvider(new FilterQueryProvider() {
                @Override
                public Cursor runQuery(CharSequence charSequence) {
                    if (suggestionDb != null) {
                        suggestionCursor = suggestionDbHelper.search(suggestionDb, charSequence.toString());
                    }
                    return suggestionCursor;
                }
            });

            sourceInputEditText.setAdapter(suggestionAdapter);
            sourceInputEditText.setThreshold(1);

            destinationInputEditText.setAdapter(suggestionAdapter);
            destinationInputEditText.setThreshold(1);
        } catch (Exception e) {
            suggestionDb = null;
            Log.d("KashewDevelopers", "Error : " + e.getMessage());
        }
    }

    public void initializeHistoryDbElements() {
        historyDbHelper = new HistoryDbHelper(this);
        try {
            historyDb = historyDbHelper.getReadableDatabase();
            historyCursor = historyDbHelper.get(historyDb);

            historyAdapter = new HistoryAdapter(this, historyCursor, 0);

            historyList.setAdapter(historyAdapter);

            if (historyCursor.getCount() == 0) {
                nothingToShowTv.setVisibility(View.VISIBLE);
                clearHistoryButton.setVisibility(View.GONE);
            } else {
                nothingToShowTv.setVisibility(View.GONE);
                clearHistoryButton.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            historyDb = null;
            Log.d("KashewDevelopers", "Error : " + e.getMessage());
        }
    }

    public void initializeWidgets() {
        // control widgets
        controlToggleButton = findViewById(R.id.controlToggleButton);
        distanceUnitButton = findViewById(R.id.distanceUnitButton);
        controlPanel = findViewById(R.id.controlsPanel);

        // source widgets
        sourceProgressBar = findViewById(R.id.sourceProgressBar);
        sourcePanel = findViewById(R.id.sourcePanel);
        sourceInputEditText = findViewById(R.id.sourceInput);
        clearSource = findViewById(R.id.sourceCloseIcon);
        sourceUseLocation = findViewById(R.id.useSourceLocation);
        sourceChooseOnMap = findViewById(R.id.useSourceOnMap);
        sourceNotFound = findViewById(R.id.sourceNotFound);

        // destination widgets
        destinationProgressBar = findViewById(R.id.destinationProgressBar);
        destinationPanel = findViewById(R.id.destinationPanel);
        destinationInputEditText = findViewById(R.id.destinationInput);
        clearDestination = findViewById(R.id.destinationCloseIcon);
        destinationUseLocation = findViewById(R.id.useDestinationLocation);
        destinationChooseOnMap = findViewById(R.id.useDestinationOnMap);
        destinationNotFound = findViewById(R.id.destinationNotFound);

        // info widgets
        tapOnMapMsg = findViewById(R.id.tapOnMapMsg);
        distanceMsg = findViewById(R.id.distanceMsg);

        // navigation drawer
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationDrawer = findViewById(R.id.slider);
        clearHistoryButton = findViewById(R.id.clear_history);
        nothingToShowTv = findViewById(R.id.nothing_to_show);
        historyList = findViewById(R.id.history_list);
    }

    public void setWidgetsVisibility() {
        distanceUnitButton.setVisibility(View.GONE);
        controlPanel.setVisibility(View.GONE);

        clearSource.setVisibility(View.GONE);
        sourceUseLocation.setVisibility(View.GONE);
        sourceChooseOnMap.setVisibility(View.GONE);
        sourceProgressBar.setVisibility(View.GONE);
        sourceNotFound.setVisibility(View.GONE);

        clearDestination.setVisibility(View.GONE);
        destinationUseLocation.setVisibility(View.GONE);
        destinationChooseOnMap.setVisibility(View.GONE);
        destinationProgressBar.setVisibility(View.GONE);
        destinationNotFound.setVisibility(View.GONE);

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
        sourceTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(final Editable editable) {
                noInternetToast.cancel();
                if (noNetworkConnection()) {
                    noInternetToast.show();
                    return;
                }

                sourceProgressBar.setVisibility(View.VISIBLE);
                clearSource.setVisibility(View.GONE);
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
        };
        sourceInputEditText.addTextChangedListener(sourceTextWatcher);

        destinationTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(final Editable editable) {
                noInternetToast.cancel();
                if (noNetworkConnection()) {
                    noInternetToast.show();
                    return;
                }

                destinationProgressBar.setVisibility(View.VISIBLE);
                clearDestination.setVisibility(View.GONE);
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
        };
        destinationInputEditText.addTextChangedListener(destinationTextWatcher);
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

    public void setHistoryClickListener() {
        // show history on map
        historyAdapter.setOnHistoryClickListener(new HistoryAdapter.OnHistoryClickListener() {
            @Override
            public void onHistoryClickListener(View view) {
                String srcName = ((TextView) (view.findViewById(R.id.src))).getText().toString();
                String srcLL = ((TextView) (view.findViewById(R.id.srcLL))).getText().toString();
                if (srcName.equals(srcLL)) {
                    srcName = "Source";
                }

                String dstName = ((TextView) (view.findViewById(R.id.dst))).getText().toString();
                String dstLL = ((TextView) (view.findViewById(R.id.dstLL))).getText().toString();
                if (dstName.equals(dstLL)) {
                    dstName = "Destination";
                }

                setHistoryMarkers(srcName, srcLL, dstName, dstLL);
            }
        });

        // delete individual history
        historyAdapter.setOnDeleteClickListener(new HistoryAdapter.OnDeleteClickListener() {
            @Override
            public void onDeleteClickListener(final String hashCode) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                builder.setTitle(R.string.delete);
                builder.setMessage(R.string.are_you_sure);
                builder.setNegativeButton(R.string.cancel, null);
                builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        historyDbHelper.delete(historyDb, hashCode);

                        historyCursor = historyDbHelper.get(historyDb);
                        historyAdapter.swapCursor(historyCursor);
                        historyAdapter.notifyDataSetChanged();
                        if (historyCursor.getCount() == 0) {
                            nothingToShowTv.setVisibility(View.VISIBLE);
                            clearHistoryButton.setVisibility(View.GONE);
                        } else {
                            nothingToShowTv.setVisibility(View.GONE);
                            clearHistoryButton.setVisibility(View.VISIBLE);
                        }
                    }
                });
                builder.show();
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
            Log.d("KashewDevelopers", "Error : " + e.getMessage());
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
                if (sourceInputEditText.getText().length() > 0) {
                    clearSource.setVisibility(View.VISIBLE);
                } else {
                    clearSource.setVisibility(View.GONE);
                }

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
                    distanceUnitButton.setVisibility(View.GONE);
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
                if (destinationInputEditText.getText().length() > 0) {
                    clearDestination.setVisibility(View.VISIBLE);
                } else {
                    clearDestination.setVisibility(View.GONE);
                }

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
                    distanceUnitButton.setVisibility(View.GONE);
                    if (destinationInputEditText.getText().length() > 0)
                        destinationNotFound.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void setHistoryMarkers(String srcName, String srcLL, String dstName, String dstLL) {
        sourceInputEditText.removeTextChangedListener(sourceTextWatcher);
        sourceInputEditText.setText(srcName.equals("Source") ? "" : srcName);
        sourceInputEditText.addTextChangedListener(sourceTextWatcher);
        sourceMarker.setVisible(false);

        String[] srcLaLn = srcLL.split(",");
        double sourceLat = Double.parseDouble(srcLaLn[0]);
        double sourceLng = Double.parseDouble(srcLaLn[1]);
        LocationObject source = new LocationObject(srcName, sourceLat, sourceLng);
        setSourceMarker(source);


        destinationInputEditText.removeTextChangedListener(destinationTextWatcher);
        destinationInputEditText.setText(dstName.equals("Destination") ? "" : dstName);
        destinationInputEditText.addTextChangedListener(destinationTextWatcher);
        destinationMarker.setVisible(false);

        String[] dstLaLn = dstLL.split(",");
        double destinationLat = Double.parseDouble(dstLaLn[0]);
        double destinationLng = Double.parseDouble(dstLaLn[1]);
        LocationObject destination = new LocationObject(dstName, destinationLat, destinationLng);
        setDestinationMarker(destination);

        drawerLayout.closeDrawer(GravityCompat.START);
    }


    // network, gps & permission
    public boolean noNetworkConnection() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null)
            return true;

        return cm.getActiveNetworkInfo() == null || !cm.getActiveNetworkInfo().isConnected();
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

        handleDistanceUnit(earthRadius * c);

        insertHistory();
    }

    public void handleDistanceUnit(double distanceInKm) {
        String unit = getUnitPreference();
        double distance;

        if (unit.equals("Km")) {
            distanceUnitButton.setImageResource(R.drawable.km_icon);
            if (distanceInKm < 1) {
                distance = distanceInKm * 1000;
                unit = (distance > 1) ? "Meters" : "Meter";
            } else {
                distance = distanceInKm;
            }
        } else {
            distanceUnitButton.setImageResource(R.drawable.mile_icon);
            distance = distanceInKm * 0.62;
            if (distance < 1) {
                distance = distance * 1760;
                unit = (distance > 1) ? "Yards" : "Yard";
            }
        }

        String formatted = getString(R.string.distance_msg, distance, unit);
        distanceMsg.setText(formatted);
        distanceMsg.setVisibility(View.VISIBLE);
        distanceUnitButton.setVisibility(View.VISIBLE);
    }

    public String getUnitPreference() {
        SharedPreferences prefs = getSharedPreferences("distanceUnit", MODE_PRIVATE);
        return prefs.getString("unit", "Km");
    }

    public void setUnitPreference(String unit) {
        SharedPreferences.Editor editor =
                getSharedPreferences("distanceUnit", MODE_PRIVATE).edit();
        editor.putString("unit", unit);
        editor.apply();
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
        if (suggestionDbHelper != null && suggestionDb != null) {
            suggestionDbHelper.insert(suggestionDb, place);
        }
    }

    public void insertHistory() {
        if (historyDbHelper == null || historyDb == null)
            return;

        String srcName = sourceMarker.getTitle();
        String srcLL = sourceMarker.getPosition().latitude + "," +
                sourceMarker.getPosition().longitude;
        if (srcName.length() == 0 || srcName.equals("Source")) {
            srcName = srcLL;
        }

        String dstName = destinationMarker.getTitle();
        String dstLL = destinationMarker.getPosition().latitude + "," +
                destinationMarker.getPosition().longitude;
        if (dstName.length() == 0 || dstName.equals("Destination")) {
            dstName = dstLL;
        }

        String distance = distanceMsg.getText().toString();

        historyDbHelper.insert(historyDb, srcName, srcLL, dstName, dstLL, distance);
        historyCursor = historyDbHelper.get(historyDb);
        historyAdapter.swapCursor(historyCursor);
        historyAdapter.notifyDataSetChanged();

        if (historyCursor.getCount() == 0) {
            nothingToShowTv.setVisibility(View.VISIBLE);
            clearHistoryButton.setVisibility(View.GONE);
        } else {
            nothingToShowTv.setVisibility(View.GONE);
            clearHistoryButton.setVisibility(View.VISIBLE);
        }
    }


    // widget clicks
    public void clearSourceClicked(View v) {
        sourceInputEditText.setText("");
    }

    public void clearDestinationClicked(View v) {
        destinationInputEditText.setText("");
    }

    public void distanceUnitButtonClicked(View v) {
        final String[] userTypes = {"Kilometers", "Miles"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getText(R.string.distance_in));
        builder.setItems(userTypes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (userTypes[i].equals("Kilometers")) {
                    setUnitPreference("Km");
                } else if (userTypes[i].equals("Miles")) {
                    setUnitPreference("Mi");
                }
                getDistance();
            }
        });
        builder.show();
    }

    public void openHistoryClicked(View v) {
        drawerLayout.openDrawer(GravityCompat.START);
    }

    public void clearHistoryButtonClicked(View v) {
        if (historyDbHelper == null || historyDb == null) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.clear_history);
        builder.setMessage(R.string.are_you_sure);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                historyDbHelper.deleteAll(historyDb);

                historyCursor = historyDbHelper.get(historyDb);
                historyAdapter.swapCursor(historyCursor);
                historyAdapter.notifyDataSetChanged();
                nothingToShowTv.setVisibility(View.VISIBLE);
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

}