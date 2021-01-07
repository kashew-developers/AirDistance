package in.kashewdevelopers.airdistance;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
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
import android.location.Location;
import android.location.LocationManager;
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
import android.widget.CursorAdapter;
import android.widget.FilterQueryProvider;
import android.widget.SimpleCursorAdapter;
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

import java.util.Random;

import in.kashewdevelopers.airdistance.async_tasks.LatLngToPlaceTask;
import in.kashewdevelopers.airdistance.async_tasks.PlaceToLatLngTask;
import in.kashewdevelopers.airdistance.databinding.ActivityMapsBinding;
import in.kashewdevelopers.airdistance.databinding.HistoryListItemBinding;
import in.kashewdevelopers.airdistance.history_components.HistoryAdapter;
import in.kashewdevelopers.airdistance.history_components.HistoryDbHelper;
import in.kashewdevelopers.airdistance.suggestion_components.SuggestionDbHelper;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private ActivityMapsBinding binding;

    // Widgets helpers
    TextWatcher sourceTextWatcher, destinationTextWatcher;
    PlaceToLatLngTask.OnTaskCompleteListener placeToLatLngTaskListener;
    LatLngToPlaceTask.OnTaskCompleteListener latLngToPlaceTaskListener;
    int lastSourceTaskId = 0, lastDestinationTaskId = 0;

    // Map Elements
    private GoogleMap mMap;
    Marker sourceMarker, destinationMarker;
    Polyline distanceLine;
    boolean chooseDestinationOnMap = false, chooseSourceOnMap = false;
    LocationCallback locationCallback = null;
    int selectedMapType = 0;

    // constants
    int MY_PERMISSIONS_REQUEST_LOCATION = 123;
    long fadeInDuration = 200, fadeOutDuration = 200;
    int avgCruisingSpeedInKmPerHour = 835, avgCruisingSpeedInKmPerMinute = 14;

    InputMethodManager imm;
    Toast gpsToast;

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
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null)
            mapFragment.getMapAsync(this);

        initialize();

        // set listeners
        setFocusChangeListeners();
        setPlaceToLatLngTaskListener();
        setLatLngToPlaceTaskListener();
        setTextChangeListeners();
        setHistoryClickListener();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Initialize source marker, destination marker & distance line
        sourceMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(0, 0))
                .title(getString(R.string.source)));
        sourceMarker.setVisible(false);

        destinationMarker = mMap.addMarker(new MarkerOptions()
                .position(sourceMarker.getPosition())
                .title(getString(R.string.destination)));
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
                if (chooseDestinationOnMap) {
                    setDestinationMarker(new LocationObject(getString(R.string.destination),
                            latLng.latitude, latLng.longitude));
                    initiatePlaceNameRetrieval(latLng, getString(R.string.destination));
                } else if (chooseSourceOnMap) {
                    setSourceMarker(new LocationObject(getString(R.string.source),
                            latLng.latitude, latLng.longitude));
                    initiatePlaceNameRetrieval(latLng, getString(R.string.source));
                } else if (binding.controlPanel.getVisibility() == View.VISIBLE) {
                    binding.controlToggleButton.performClick();
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
                if (chooseDestinationOnMap) {
                    setDestinationMarker(new LocationObject(getString(R.string.destination),
                            marker.getPosition().latitude, marker.getPosition().longitude));
                    initiatePlaceNameRetrieval(marker.getPosition(), getString(R.string.destination));
                } else if (chooseSourceOnMap) {
                    setSourceMarker(new LocationObject(getString(R.string.source),
                            marker.getPosition().latitude, marker.getPosition().longitude));
                    initiatePlaceNameRetrieval(marker.getPosition(), getString(R.string.source));
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
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            moveTaskToBack(true);
        }
    }


    //initialize
    @SuppressLint("ShowToast")
    public void initialize() {
        setWidgetsInitialVisibility();

        imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);

        gpsToast = Toast.makeText(this, "Please turn on GPS", Toast.LENGTH_LONG);
        gpsToast.setGravity(Gravity.CENTER, 0, 0);

        binding.drawerLayout.addDrawerListener(new ActionBarDrawerToggle(this, binding.drawerLayout,
                R.string.drawer_open, R.string.drawer_close));

        initializeSuggestionDbElements();
        initializeHistoryDbElements();
    }

    public void initializeSuggestionDbElements() {
        suggestionDbHelper = new SuggestionDbHelper(this);
        try {
            suggestionDb = suggestionDbHelper.getReadableDatabase();
            suggestionCursor = suggestionDbHelper.search(suggestionDb, "");

            suggestionAdapter = new SimpleCursorAdapter(this, R.layout.suggestion_list_layout,
                    suggestionCursor, new String[]{"NAME"}, new int[]{R.id.text},
                    CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

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

            binding.sourceInput.setAdapter(suggestionAdapter);
            binding.sourceInput.setThreshold(1);

            binding.destinationInput.setAdapter(suggestionAdapter);
            binding.destinationInput.setThreshold(1);
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
            binding.historyList.setAdapter(historyAdapter);

            if (historyCursor.getCount() == 0) {
                binding.nothingToShow.setVisibility(View.VISIBLE);
                binding.clearHistory.setVisibility(View.GONE);
            } else {
                binding.nothingToShow.setVisibility(View.GONE);
                binding.clearHistory.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            historyDb = null;
            Log.d("KashewDevelopers", "Error : " + e.getMessage());
        }
    }

    public void setWidgetsInitialVisibility() {
        binding.distanceUnitButton.setVisibility(View.GONE);

        binding.sourceProgressBar.setVisibility(View.GONE);
        binding.sourceCloseIcon.setVisibility(View.GONE);
        binding.sourceNotFound.setVisibility(View.GONE);
        binding.useSourceLocation.setVisibility(View.GONE);
        binding.useSourceOnMap.setVisibility(View.GONE);

        binding.destinationProgressBar.setVisibility(View.GONE);
        binding.destinationCloseIcon.setVisibility(View.GONE);
        binding.destinationNotFound.setVisibility(View.GONE);
        binding.useDestinationLocation.setVisibility(View.GONE);
        binding.useDestinationOnMap.setVisibility(View.GONE);

        fadeOutView(binding.distanceMsg);
        fadeOutView(binding.averageTimeMsg);
        binding.tapOnMapMsg.setVisibility(View.GONE);
    }


    // UI changes
    public void fadeInView(View v) {
        v.setVisibility(View.VISIBLE);
        v.animate().alpha(1.0f).setDuration(fadeInDuration);
    }

    public void fadeOutView(final View v) {
        v.animate()
                .alpha(0.0f)
                .setDuration(fadeOutDuration)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        v.setVisibility(View.GONE);
                    }
                });
    }

    public void animateAndChangeControlToggle() {
        final int newIcon = (binding.controlPanel.getVisibility() == View.GONE) ?
                R.drawable.close_icon :
                R.drawable.keyboard_icon;

        final float angle = (binding.controlPanel.getVisibility() == View.GONE) ? 180f : 0f;

        binding.controlToggleButton.animate().setDuration(200)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        binding.controlToggleButton.setImageResource(newIcon);
                    }
                })
                .rotation(angle);
    }


    // listeners
    public void setFocusChangeListeners() {
        binding.sourceInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    binding.useSourceOnMap.setVisibility(View.VISIBLE);
                    binding.useSourceLocation.setVisibility(View.VISIBLE);
                    if (chooseSourceOnMap || chooseDestinationOnMap)
                        disableMarkerPlacement();
                } else {
                    binding.useSourceOnMap.setVisibility(View.GONE);
                    binding.useSourceLocation.setVisibility(View.GONE);
                    if (imm != null)
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        });

        binding.destinationInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    binding.useDestinationOnMap.setVisibility(View.VISIBLE);
                    binding.useDestinationLocation.setVisibility(View.VISIBLE);
                    if (chooseSourceOnMap || chooseDestinationOnMap)
                        disableMarkerPlacement();
                } else {
                    binding.useDestinationOnMap.setVisibility(View.GONE);
                    binding.useDestinationLocation.setVisibility(View.GONE);
                    if (imm != null)
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        });
    }

    public void setPlaceToLatLngTaskListener() {
        placeToLatLngTaskListener = new PlaceToLatLngTask.OnTaskCompleteListener() {
            @Override
            public void onTaskCompleteListener(LocationObject locationObject, String locationType, int taskId) {
                if (locationObject != null) {
                    writePlaceToDB(locationObject.placeName);
                }

                if (locationType.equals(getString(R.string.source))) {
                    if (lastSourceTaskId == taskId) {
                        binding.sourceProgressBar.setVisibility(View.GONE);
                        binding.sourceCloseIcon.setVisibility(View.VISIBLE);
                        setSourceMarker(locationObject);
                        if (locationObject == null) {
                            binding.sourceNotFound.setVisibility(View.VISIBLE);
                        } else {
                            binding.sourceNotFound.setVisibility(View.GONE);
                        }
                    } else if (locationObject != null) {
                        setSourceMarker(locationObject);
                    }
                } else {
                    if (lastDestinationTaskId == taskId) {
                        binding.destinationProgressBar.setVisibility(View.GONE);
                        binding.destinationCloseIcon.setVisibility(View.VISIBLE);
                        setDestinationMarker(locationObject);
                        if (locationObject == null) {
                            binding.destinationNotFound.setVisibility(View.VISIBLE);
                        } else {
                            binding.destinationNotFound.setVisibility(View.GONE);
                        }
                    } else if (locationObject != null) {
                        setDestinationMarker(locationObject);
                    }
                }
            }
        };
    }

    public void setLatLngToPlaceTaskListener() {
        latLngToPlaceTaskListener = new LatLngToPlaceTask.OnTaskCompleteListener() {
            @Override
            public void onTaskCompleteListener(LatLng coordinates, String placeName, String placeType) {
                if (historyDbHelper == null || historyDb == null)
                    return;

                String coordinateString = coordinates.latitude + "," + coordinates.longitude;

                if (placeType.equals(getString(R.string.source))) {
                    if (sourceMarker.isVisible() && sourceMarker.getPosition().equals(coordinates)) {
                        sourceMarker.setTitle(placeName);
                        sourceMarker.showInfoWindow();

                        binding.sourceInput.removeTextChangedListener(sourceTextWatcher);
                        binding.sourceInput.setText(placeName);
                        binding.sourceCloseIcon.setVisibility(View.VISIBLE);
                        binding.sourceInput.addTextChangedListener(sourceTextWatcher);
                    }
                } else if (placeType.equals(getString(R.string.destination))) {
                    if (destinationMarker.isVisible() && destinationMarker.getPosition().equals(coordinates)) {
                        destinationMarker.setTitle(placeName);
                        destinationMarker.showInfoWindow();

                        binding.destinationInput.removeTextChangedListener(destinationTextWatcher);
                        binding.destinationInput.setText(placeName);
                        binding.destinationCloseIcon.setVisibility(View.VISIBLE);
                        binding.destinationInput.addTextChangedListener(destinationTextWatcher);
                    }
                }

                historyDbHelper.updateSourceName(historyDb, coordinateString, placeName);
                historyDbHelper.updateDestinationName(historyDb, coordinateString, placeName);
                refreshHistoryList();

                writePlaceToDB(placeName);
            }
        };
    }

    public void setTextChangeListeners() {
        sourceTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @SuppressWarnings("deprecation")
            @Override
            public void afterTextChanged(final Editable editable) {
                // map not yet initialised - onMapReady not executed
                if (sourceMarker == null) {
                    binding.sourceCloseIcon.setVisibility(View.VISIBLE);
                    binding.sourceNotFound.setVisibility(View.VISIBLE);
                    return;
                }

                if (editable.length() == 0) {
                    binding.sourceProgressBar.setVisibility(View.GONE);
                    binding.sourceCloseIcon.setVisibility(View.GONE);
                    binding.sourceNotFound.setVisibility(View.GONE);
                    setSourceMarker(null);
                    return;
                }

                binding.sourceProgressBar.setVisibility(View.VISIBLE);
                binding.sourceCloseIcon.setVisibility(View.GONE);
                binding.sourceNotFound.setVisibility(View.GONE);

                new PlaceToLatLngTask(MapsActivity.this,
                        getString(R.string.source), ++lastSourceTaskId)
                        .setOnTaskCompleteListener(placeToLatLngTaskListener)
                        .execute(editable);
            }
        };
        binding.sourceInput.addTextChangedListener(sourceTextWatcher);

        destinationTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            @SuppressWarnings("deprecation")
            public void afterTextChanged(final Editable editable) {
                // map not yet initialised - onMapReady not executed
                if (destinationMarker == null) {
                    binding.destinationCloseIcon.setVisibility(View.VISIBLE);
                    binding.destinationNotFound.setVisibility(View.VISIBLE);
                    return;
                }

                if (editable.length() == 0) {
                    binding.destinationProgressBar.setVisibility(View.GONE);
                    binding.destinationCloseIcon.setVisibility(View.GONE);
                    binding.destinationNotFound.setVisibility(View.GONE);
                    setDestinationMarker(null);
                    return;
                }

                binding.destinationProgressBar.setVisibility(View.VISIBLE);
                binding.destinationCloseIcon.setVisibility(View.GONE);
                binding.destinationNotFound.setVisibility(View.GONE);

                new PlaceToLatLngTask(MapsActivity.this,
                        getString(R.string.destination), ++lastDestinationTaskId)
                        .setOnTaskCompleteListener(placeToLatLngTaskListener)
                        .execute(editable);
            }
        };
        binding.destinationInput.addTextChangedListener(destinationTextWatcher);
    }

    public void setHistoryClickListener() {
        // show history on map
        historyAdapter.setOnHistoryClickListener(new HistoryAdapter.OnHistoryClickListener() {
            @Override
            public void onHistoryClickListener(HistoryListItemBinding listItemBinding) {
                String sourceName = listItemBinding.sourceName.getText().toString();
                String sourceLatLng = listItemBinding.sourceLatLng.getText().toString();
                if (sourceName.equals(sourceLatLng)) {
                    sourceName = getString(R.string.source);
                }

                String destinationName = listItemBinding.destinationName.getText().toString();
                String destinationLatLng = listItemBinding.destinationLatLng.getText().toString();
                if (destinationName.equals(destinationLatLng)) {
                    destinationName = getString(R.string.destination);
                }

                setHistoryMarkers(sourceName, sourceLatLng, destinationName, destinationLatLng);
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
                        refreshHistoryList();
                    }
                });
                builder.show();
            }
        });
    }


    // set marker
    private void setSourceMarker(final LocationObject locationObject) {
        if (locationObject != null) {
            LatLng latLng = new LatLng(locationObject.lat, locationObject.lng);
            sourceMarker.setPosition(latLng);
            sourceMarker.setVisible(true);
            sourceMarker.setTitle(locationObject.placeName);
            sourceMarker.showInfoWindow();

            if (!chooseSourceOnMap) {
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
            }

            if (destinationMarker.isVisible()) {
                showDistance();
            }
        } else {
            sourceMarker.setVisible(false);
            distanceLine.setVisible(false);
            fadeOutView(binding.distanceMsg);
            fadeOutView(binding.averageTimeMsg);
            binding.distanceUnitButton.setVisibility(View.GONE);
        }
    }

    private void setDestinationMarker(final LocationObject locationObject) {
        if (locationObject != null) {
            LatLng latLng = new LatLng(locationObject.lat, locationObject.lng);
            destinationMarker.setPosition(latLng);
            destinationMarker.setVisible(true);
            destinationMarker.setTitle(locationObject.placeName);
            destinationMarker.showInfoWindow();

            if (!chooseDestinationOnMap) {
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
            }

            if (sourceMarker.isVisible()) {
                showDistance();
            }
        } else {
            destinationMarker.setVisible(false);
            distanceLine.setVisible(false);
            fadeOutView(binding.distanceMsg);
            fadeOutView(binding.averageTimeMsg);
            binding.distanceUnitButton.setVisibility(View.GONE);
        }
    }

    private void setHistoryMarkers(String sourceName, String sourceLatLng, String destinationName, String destinationLatLng) {
        sourceMarker.setVisible(false);
        destinationMarker.setVisible(false);

        if (chooseDestinationOnMap || chooseSourceOnMap)
            disableMarkerPlacement();

        binding.sourceInput.removeTextChangedListener(sourceTextWatcher);
        binding.sourceInput.setText(sourceName.equals(getString(R.string.source)) ? "" : sourceName);
        binding.sourceCloseIcon.setVisibility(sourceName.equals(getString(R.string.source)) ? View.GONE : View.VISIBLE);
        binding.sourceInput.addTextChangedListener(sourceTextWatcher);

        String[] sourceLatLngArr = sourceLatLng.split(",");
        double sourceLat = Double.parseDouble(sourceLatLngArr[0]);
        double sourceLng = Double.parseDouble(sourceLatLngArr[1]);
        LocationObject source = new LocationObject(sourceName, sourceLat, sourceLng);
        setSourceMarker(source);


        binding.destinationInput.removeTextChangedListener(destinationTextWatcher);
        binding.destinationInput.setText(destinationName.equals(getString(R.string.destination)) ? "" : destinationName);
        binding.destinationCloseIcon.setVisibility(destinationName.equals(getString(R.string.destination)) ? View.GONE : View.VISIBLE);
        binding.destinationInput.addTextChangedListener(destinationTextWatcher);

        String[] destinationLatLngArr = destinationLatLng.split(",");
        double destinationLat = Double.parseDouble(destinationLatLngArr[0]);
        double destinationLng = Double.parseDouble(destinationLatLngArr[1]);
        LocationObject destination = new LocationObject(destinationName, destinationLat, destinationLng);
        setDestinationMarker(destination);

        binding.drawerLayout.closeDrawer(GravityCompat.START);

        mMap.animateCamera(CameraUpdateFactory.zoomTo(0));

        if (binding.controlPanel.getVisibility() == View.VISIBLE)
            binding.controlToggleButton.performClick();
    }


    // gps & permission
    private boolean hasLocationPermission() {
        return ContextCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED;
    }

    private void askLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {

            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.location_permission_title))
                    .setCancelable(true)
                    .setMessage(getString(R.string.location_permission_message))
                    .setPositiveButton(getString(R.string.settings), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton(getString(R.string.not_now), null)
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

                if (markerType.equals(getString(R.string.source))) {
                    binding.sourceProgressBar.setVisibility(View.GONE);
                    if (currentLocation == null)
                        setSourceMarker(null);
                    else {
                        setSourceMarker(new LocationObject(getString(R.string.source),
                                currentLocation.getLatitude(), currentLocation.getLongitude()));
                        LatLng coordinates = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                        initiatePlaceNameRetrieval(coordinates, getString(R.string.source));
                    }
                } else if (markerType.equals(getString(R.string.destination))) {
                    binding.destinationProgressBar.setVisibility(View.GONE);
                    if (currentLocation == null)
                        setDestinationMarker(null);
                    else {
                        setDestinationMarker(new LocationObject(getString(R.string.destination),
                                currentLocation.getLatitude(), currentLocation.getLongitude()));
                        LatLng coordinates = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                        initiatePlaceNameRetrieval(coordinates, getString(R.string.destination));
                    }
                }
            }
        };

        try {
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        } catch (SecurityException e) {
            Log.d("KashewDevelopers", "getLocationAndSetMarker: " + e.getMessage());
        }
    }


    // functionality
    public void disableMarkerPlacement() {
        chooseDestinationOnMap = chooseSourceOnMap = false;
        sourceMarker.setDraggable(false);
        destinationMarker.setDraggable(false);
        fadeOutView(binding.tapOnMapMsg);
    }

    private double getDistanceInKm() {
        // haversine formula
        double earthRadius = 6378.137; // Radius of earth at equator in KM
        double diffLat = sourceMarker.getPosition().latitude * Math.PI / 180
                - destinationMarker.getPosition().latitude * Math.PI / 180;
        double diffLon = sourceMarker.getPosition().longitude * Math.PI / 180
                - destinationMarker.getPosition().longitude * Math.PI / 180;
        double a = Math.sin(diffLat / 2) * Math.sin(diffLat / 2) +
                Math.cos(sourceMarker.getPosition().latitude * Math.PI / 180) *
                        Math.cos(destinationMarker.getPosition().latitude * Math.PI / 180) *
                        Math.sin(diffLon / 2) * Math.sin(diffLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return c * earthRadius;
    }

    private void showDistance() {
        distanceLine.remove();

        distanceLine = mMap.addPolyline(new PolylineOptions()
                .add(sourceMarker.getPosition(), destinationMarker.getPosition())
                .width(5)
                .color(Color.RED));

        double distanceInKm = getDistanceInKm();
        handleDistanceUnit(distanceInKm);
        insertHistory();

        showTime(distanceInKm);
    }

    public void handleDistanceUnit(double distanceInKm) {
        String unit = getUnitPreference();
        double distance;

        if (unit.equals("Km")) {
            binding.distanceUnitButton.setImageResource(R.drawable.km_icon);
            if (distanceInKm < 1) {
                distance = distanceInKm * 1000;
                unit = (distance > 1) ? "Meters" : "Meter";
            } else {
                distance = distanceInKm;
            }
        } else {
            binding.distanceUnitButton.setImageResource(R.drawable.mile_icon);
            distance = distanceInKm * 0.62;
            if (distance < 1) {
                distance = distance * 1760;
                unit = (distance > 1) ? "Yards" : "Yard";
            }
        }

        String formatted = getString(R.string.distance_msg, distance, unit);
        binding.distanceMsg.setText(formatted);
        fadeInView(binding.distanceMsg);
        binding.distanceUnitButton.setVisibility(View.VISIBLE);
    }

    @SuppressWarnings("deprecation")
    public void initiatePlaceNameRetrieval(@NonNull LatLng coordinates, @NonNull String placeType) {
        new LatLngToPlaceTask(MapsActivity.this, placeType)
                .setTaskListener(latLngToPlaceTaskListener)
                .execute(coordinates);
    }

    public void showTime(double distanceInKm) {
        int hours, minutes;

        try {
            hours = (int) (distanceInKm / avgCruisingSpeedInKmPerHour);
            distanceInKm %= avgCruisingSpeedInKmPerHour;
            minutes = (int) (distanceInKm / avgCruisingSpeedInKmPerMinute);
        } catch (Exception e) {
            fadeOutView(binding.averageTimeMsg);
            return;
        }

        StringBuilder time = new StringBuilder();

        if (hours > 0) {
            time.append(getResources().getQuantityString(R.plurals.hours, hours, hours));
        }
        if (minutes > 0) {
            if (time.length() > 0) time.append(", ");
            time.append(getResources().getQuantityString(R.plurals.minutes, minutes, minutes));
        }

        if (time.length() > 0) {
            binding.averageTimeMsg.setText(getString(R.string.average_time, time));
            fadeInView(binding.averageTimeMsg);
        } else {
            fadeOutView(binding.averageTimeMsg);
        }
    }


    // shared preference
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


    // db operations
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
        if (srcName.length() == 0 || srcName.equals(getString(R.string.source))) {
            srcName = srcLL;
        }

        String dstName = destinationMarker.getTitle();
        String dstLL = destinationMarker.getPosition().latitude + "," +
                destinationMarker.getPosition().longitude;
        if (dstName.length() == 0 || dstName.equals(getString(R.string.destination))) {
            dstName = dstLL;
        }

        String distance = binding.distanceMsg.getText().toString();

        historyDbHelper.insert(historyDb, srcName, srcLL, dstName, dstLL, distance);
        refreshHistoryList();
    }

    public void refreshHistoryList() {
        historyCursor = historyDbHelper.get(historyDb);
        historyAdapter.swapCursor(historyCursor);
        historyAdapter.notifyDataSetChanged();

        if (historyCursor.getCount() == 0) {
            binding.nothingToShow.setVisibility(View.VISIBLE);
            binding.clearHistory.setVisibility(View.GONE);
        } else {
            binding.nothingToShow.setVisibility(View.GONE);
            binding.clearHistory.setVisibility(View.VISIBLE);
        }
    }


    // widget clicks
    public void clearSourceClicked(View v) {
        binding.sourceInput.setText("");
    }

    public void clearDestinationClicked(View v) {
        binding.destinationInput.setText("");
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
                showDistance();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    public void layerButtonClicked(View v) {
        final String[] mapTypes = {"Normal", "Satellite", "Hybrid", "Terrain"};
        final int[] mapTypesCode = {GoogleMap.MAP_TYPE_NORMAL, GoogleMap.MAP_TYPE_SATELLITE,
                GoogleMap.MAP_TYPE_HYBRID, GoogleMap.MAP_TYPE_TERRAIN};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getText(R.string.map_type));
        builder.setSingleChoiceItems(mapTypes, selectedMapType, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mMap != null) {
                    mMap.setMapType(mapTypesCode[which]);
                }
                selectedMapType = which;
                dialog.cancel();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    public void openHistoryClicked(View v) {
        binding.drawerLayout.openDrawer(GravityCompat.START);
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
                refreshHistoryList();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    public void controlToggleClicked(View v) {
        binding.sourceInput.clearFocus();
        binding.destinationInput.clearFocus();

        animateAndChangeControlToggle();

        if (binding.controlPanel.getVisibility() == View.GONE) {
            fadeInView(binding.controlPanel);
        } else {
            fadeOutView(binding.controlPanel);
            if (chooseSourceOnMap || chooseDestinationOnMap)
                disableMarkerPlacement();
        }
    }

    public void chooseSourceOnMapClicked(View v) {
        binding.sourceInput.clearFocus();
        binding.sourceInput.setText("");

        fadeInView(binding.tapOnMapMsg);
        chooseSourceOnMap = true;
        sourceMarker.setDraggable(true);
    }

    public void chooseDestinationOnMapClicked(View v) {
        binding.destinationInput.clearFocus();
        binding.destinationInput.setText("");

        fadeInView(binding.tapOnMapMsg);
        chooseDestinationOnMap = true;
        destinationMarker.setDraggable(true);
    }

    public void useSourceLocationClicked(View v) {
        if (hasLocationPermission()) {
            if (isGPSOn()) {
                binding.sourceInput.clearFocus();
                binding.sourceInput.setText("");
                binding.sourceProgressBar.setVisibility(View.VISIBLE);
                getLocationAndSetMarker(getString(R.string.source));
            } else {
                gpsToast.show();
            }
        } else {
            askLocationPermission();
        }
    }

    public void useDestinationLocationClicked(View v) {
        if (hasLocationPermission()) {
            if (isGPSOn()) {
                binding.destinationInput.clearFocus();
                binding.destinationInput.setText("");
                binding.destinationProgressBar.setVisibility(View.VISIBLE);
                getLocationAndSetMarker(getString(R.string.destination));
            } else {
                gpsToast.show();
            }
        } else {
            askLocationPermission();
        }
    }

}
