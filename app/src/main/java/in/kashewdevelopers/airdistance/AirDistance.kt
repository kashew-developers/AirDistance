package `in`.kashewdevelopers.airdistance

import `in`.kashewdevelopers.airdistance.adapter.HistoryAdapter
import `in`.kashewdevelopers.airdistance.async_tasks.LatLngToPlaceTask
import `in`.kashewdevelopers.airdistance.async_tasks.PlaceToLatLngTask
import `in`.kashewdevelopers.airdistance.data_containers.LocationObject
import `in`.kashewdevelopers.airdistance.databinding.ActivityAirDistanceBinding
import `in`.kashewdevelopers.airdistance.databinding.HistoryListItemBinding
import `in`.kashewdevelopers.airdistance.db.history.HistoryManager
import `in`.kashewdevelopers.airdistance.db.suggestion.SuggestionManager
import `in`.kashewdevelopers.airdistance.providers.*
import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import java.lang.Exception

class AirDistance : FragmentActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityAirDistanceBinding

    // ------------------------------ text watchers --------------------
    private lateinit var sourceTextWatcher: TextWatcher
    private lateinit var destinationTextWatcher: TextWatcher

    // ------------------------------ map elements --------------------
    private var mMap: GoogleMap? = null
    private var sourceMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var distanceLine: Polyline? = null
    private var chooseDestinationOnMap = false
    private var chooseSourceOnMap = false
    private var selectedMapType: Int = 0
    private var tapOnMapMsg: Snackbar? = null

    // ------------------------------ db managers --------------------
    private lateinit var suggestionManager: SuggestionManager
    private lateinit var historyManager: HistoryManager

    // ------------------------------ async task listeners --------------------
    private lateinit var placeToLatLngTaskListener: PlaceToLatLngTask.OnTaskCompleteListener
    private lateinit var latLngToPlaceTaskListener: LatLngToPlaceTask.OnTaskCompleteListener

    private var lastSourceTaskId: Int = 0
    private var lastDestinationTaskId: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAirDistanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // get support fragment & get notified when the map is ready to use
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment ?: return
        mapFragment.getMapAsync(this)

        initialize()

        // set listeners
        setFocusChangeListeners()
        setPlaceToLatLngTaskListener()
        setLatLngToPlaceTaskListener()
        setTextChangeListeners()
        setHistoryClickListeners()
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        mMap = googleMap

        // initialize map elements
        initializeMapElements()

        // click to place marker
        mMap?.setOnMapClickListener { markerPlacedOrMoved(it) }

        // drag marker
        mMap?.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(p0: Marker?) {}
            override fun onMarkerDrag(p0: Marker?) {}

            override fun onMarkerDragEnd(marker: Marker?) {
                val coordinates = marker?.position
                coordinates ?: return
                markerPlacedOrMoved(coordinates)
            }
        })

        // on marker click, show info window, but don't center map
        mMap?.setOnMarkerClickListener {
            if (!it.isInfoWindowShown) {
                it.showInfoWindow()
            }
            true
        }
    }

    override fun onBackPressed() {
        // on back press close the history slider if open, else exit
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            moveTaskToBack(true)
            super.onBackPressed()
        }
    }


    // ------------------------------ initializations --------------------
    private fun initialize() {
        setWidgetsInitialVisibility()

        tapOnMapMsg = Snackbar.make(binding.root, R.string.tap_on_map, Snackbar.LENGTH_INDEFINITE)
                .setAnchorView(binding.airDistance.layerButton)
                .setAction(R.string.ok) { tapOnMapMsg?.dismiss() }
        tapOnMapMsg?.view?.findViewById<Button>(R.id.snackbar_action)?.background = null

        val drawerListener = ActionBarDrawerToggle(this, binding.drawerLayout,
                R.string.drawer_open, R.string.drawer_close)
        binding.drawerLayout.addDrawerListener(drawerListener)


        suggestionManager = SuggestionManager(this)
        suggestionManager.initializeElements()

        binding.airDistance.sourceInput.setAdapter(suggestionManager.getAdapter())
        binding.airDistance.sourceInput.threshold = 1

        binding.airDistance.destinationInput.setAdapter(suggestionManager.getAdapter())
        binding.airDistance.destinationInput.threshold = 1


        historyManager = HistoryManager(this)
        historyManager.initializeElements()

        binding.history.historyList.adapter = historyManager.getAdapter()
        setHistoryListVisibility()
    }

    private fun initializeMapElements() {
        sourceMarker = mMap?.addMarker(MarkerOptions()
                .position(LatLng(0.0, 0.0))
                .title(getString(R.string.source)))
        sourceMarker?.isVisible = false

        destinationMarker = mMap?.addMarker(MarkerOptions()
                .position(LatLng(0.0, 0.0))
                .title(getString(R.string.destination)))
        destinationMarker?.isVisible = false

        distanceLine = mMap?.addPolyline(PolylineOptions()
                .add(sourceMarker?.position, sourceMarker?.position)
                .width(5f)
                .color(Color.RED))
        distanceLine?.isVisible = false
    }


    // ------------------------------ UI changes --------------------
    /*
     * We can set the widgets's visibility to GONE in xml itself,
     * but doing that can make the UI less intuitive, so we keep all the
     * widgets VISIBLE & set there visibility to GONE in code
     */
    private fun setWidgetsInitialVisibility() {
        with(binding.airDistance) {
            distanceUnitButton.visibility = View.GONE

            sourceProgressBar.visibility = View.GONE
            sourceCloseIcon.visibility = View.GONE
            sourceNotFound.visibility = View.GONE
            useSourceLocation.visibility = View.GONE
            useSourceOnMap.visibility = View.GONE

            destinationProgressBar.visibility = View.GONE
            destinationCloseIcon.visibility = View.GONE
            destinationNotFound.visibility = View.GONE
            useDestinationLocation.visibility = View.GONE
            useDestinationOnMap.visibility = View.GONE

            Animators.fadeOutView(distanceMsg)
        }
    }

    private fun setHistoryListVisibility() {
        if (historyManager.getCount() < 1) {
            binding.history.nothingToShow.visibility = View.VISIBLE
            binding.history.clearHistory.visibility = View.GONE
        } else {
            binding.history.nothingToShow.visibility = View.GONE
            binding.history.clearHistory.visibility = View.VISIBLE
        }
    }

    private fun displayDistance(distanceInKm: Double) {
        var unit: String = PreferenceManager.getDistanceUnitPreference(this)
        var distance: Double

        if (unit == Constants.DistanceUnit_Kilometers) {
            binding.airDistance.distanceUnitButton.setImageResource(R.drawable.km_icon)
            if (distanceInKm < 1) {
                distance = distanceInKm * 1000
                unit = if (distance > 1) "Meters" else "Meter"
            } else {
                distance = distanceInKm
            }
        } else {
            binding.airDistance.distanceUnitButton.setImageResource(R.drawable.mile_icon)
            distance = distanceInKm * 0.62
            if (distance < 1) {
                distance *= 1760
                unit = if (distance > 1) "Yards" else "Yard"
            }
        }

        val distanceMsg = getString(R.string.distance_msg, distance, unit)
        binding.airDistance.distanceMsg.text = distanceMsg
        Animators.fadeInView(binding.airDistance.distanceMsg)
        binding.airDistance.distanceUnitButton.visibility = View.VISIBLE
    }


    // ------------------------------ listeners --------------------
    private fun setFocusChangeListeners() {
        /*
         * when sourceInput:
         * - get focus: show other source options & disable previous marker placement
         * - loses focus: hide other source options & close keyboard
         */
        binding.airDistance.sourceInput.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                binding.airDistance.useSourceOnMap.visibility = View.VISIBLE
                binding.airDistance.useSourceLocation.visibility = View.VISIBLE
                if (chooseSourceOnMap || chooseDestinationOnMap) disableMarkerPlacement()
            } else {
                binding.airDistance.useSourceOnMap.visibility = View.GONE
                binding.airDistance.useSourceLocation.visibility = View.GONE
                SystemFeatures.closeKeyboard(this@AirDistance, view)
            }
        }

        /*
         * when destinationInput:
         * - get focus: show other destination options & disable previous marker placement
         * - loses focus: hide other destination options & close keyboard
         */
        binding.airDistance.destinationInput.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                binding.airDistance.useDestinationOnMap.visibility = View.VISIBLE
                binding.airDistance.useDestinationLocation.visibility = View.VISIBLE
                if (chooseSourceOnMap || chooseDestinationOnMap) disableMarkerPlacement()
            } else {
                binding.airDistance.useDestinationOnMap.visibility = View.GONE
                binding.airDistance.useDestinationLocation.visibility = View.GONE
                SystemFeatures.closeKeyboard(this@AirDistance, view)
            }
        }
    }

    private fun setTextChangeListeners() {
        sourceTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(searchText: Editable?) {
                searchText ?: return

                if (searchText.isEmpty() || searchText.isBlank()) {
                    with(binding.airDistance) {
                        sourceProgressBar.visibility = View.GONE
                        sourceCloseIcon.visibility = View.GONE
                        sourceNotFound.visibility = View.GONE
                    }
                    setSourceMarker(null)
                    return
                }

                with(binding.airDistance) {
                    sourceProgressBar.visibility = View.VISIBLE
                    sourceCloseIcon.visibility = View.GONE
                    sourceNotFound.visibility = View.GONE
                }

                PlaceToLatLngTask(this@AirDistance, getString(R.string.source), ++lastSourceTaskId)
                        .setOnTaskCompleteListener(placeToLatLngTaskListener)
                        .execute(searchText)
            }
        }
        binding.airDistance.sourceInput.addTextChangedListener(sourceTextWatcher)

        destinationTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(searchText: Editable?) {
                searchText ?: return

                if (searchText.isEmpty() || searchText.isBlank()) {
                    with(binding.airDistance) {
                        destinationProgressBar.visibility = View.GONE
                        destinationCloseIcon.visibility = View.GONE
                        destinationNotFound.visibility = View.GONE
                    }
                    setDestinationMarker(null)
                    return
                }

                with(binding.airDistance) {
                    destinationProgressBar.visibility = View.VISIBLE
                    destinationCloseIcon.visibility = View.GONE
                    destinationNotFound.visibility = View.GONE
                }

                PlaceToLatLngTask(this@AirDistance, getString(R.string.destination), ++lastDestinationTaskId)
                        .setOnTaskCompleteListener(placeToLatLngTaskListener)
                        .execute(searchText)
            }
        }
        binding.airDistance.destinationInput.addTextChangedListener(destinationTextWatcher)
    }

    private fun setHistoryClickListeners() {
        val adapter = historyManager.getAdapter() ?: return

        // show clicked history on map
        adapter.setOnHistoryClickListener(object : HistoryAdapter.OnHistoryClickListener {
            override fun onHistoryClickListener(binding: HistoryListItemBinding) {
                val srcLatLng: String = binding.sourceLatLng.text.toString()
                val srcName: String = if (binding.sourceName.text.toString() == srcLatLng) getString(R.string.source)
                else binding.sourceName.text.toString()

                val dstLatLng: String = binding.destinationLatLng.text.toString()
                val dstName: String = if (binding.destinationName.text.toString() == dstLatLng) getString(R.string.destination)
                else binding.destinationName.text.toString()

                setHistoryMarkers(srcName, srcLatLng, dstName, dstLatLng)
            }
        })

        // delete individual history element
        adapter.setOnDeleteClickListener(object : HistoryAdapter.OnDeleteClickListener {
            override fun onDeleteClickListener(hashCode: String) {
                with(AlertDialog.Builder(this@AirDistance)) {
                    setTitle(R.string.delete)
                    setMessage(R.string.are_you_sure)
                    setNegativeButton(R.string.cancel, null)
                    setPositiveButton(R.string.delete) { _, _ ->
                        historyManager.delete(hashCode)
                        refreshHistoryList()
                    }
                    show()
                }
            }
        })
    }

    private fun setPlaceToLatLngTaskListener() {
        placeToLatLngTaskListener = object : PlaceToLatLngTask.OnTaskCompleteListener {
            override fun onTaskCompleteListener(locationObject: LocationObject?, locationType: String, taskId: Int) {
                if (locationObject != null) {
                    suggestionManager.insertPlace(locationObject.placeName)
                }

                if (locationType == getString(R.string.source)) {
                    if (lastSourceTaskId == taskId) {
                        binding.airDistance.sourceProgressBar.visibility = View.GONE
                        binding.airDistance.sourceCloseIcon.visibility = View.VISIBLE
                        setSourceMarker(locationObject)

                        binding.airDistance.sourceNotFound.visibility =
                                if (locationObject == null) View.VISIBLE
                                else View.GONE
                    } else if (locationObject != null) {
                        setSourceMarker(locationObject)
                    }
                } else {
                    if (lastDestinationTaskId == taskId) {
                        binding.airDistance.destinationProgressBar.visibility = View.GONE
                        binding.airDistance.destinationCloseIcon.visibility = View.VISIBLE
                        setDestinationMarker(locationObject)

                        binding.airDistance.destinationNotFound.visibility =
                                if (locationObject == null) View.VISIBLE
                                else View.GONE
                    } else if (locationObject != null) {
                        setDestinationMarker(locationObject)
                    }
                }
            }
        }
    }

    private fun setLatLngToPlaceTaskListener() {
        latLngToPlaceTaskListener = object : LatLngToPlaceTask.OnTaskCompleteListener {
            override fun onTaskCompleteListener(coordinates: LatLng, placeName: String, placeType: String) {
                val coordinateString = "${coordinates.latitude},${coordinates.longitude}"

                if (placeType == getString(R.string.source)) {
                    sourceMarker?.let {
                        if (it.isVisible && it.position == coordinates) {
                            it.title = placeName
                            it.showInfoWindow()

                            updateSourceInputText(placeName, false)
                        }
                    }
                } else if (placeType == getString(R.string.destination)) {
                    destinationMarker?.let {
                        if (it.isVisible && it.position == coordinates) {
                            it.title = placeName
                            it.showInfoWindow()

                            updateDestinationInputText(placeName, false)
                        }
                    }
                }

                historyManager.updateSourceName(coordinateString, placeName)
                historyManager.updateDestinationName(coordinateString, placeName)
                refreshHistoryList()

                suggestionManager.insertPlace(placeName)
            }
        }
    }


    // ------------------------------ functionality --------------------
    /*
 * This function is called whenever we tap on map,
 * or when we drag an existing marker.
 *
 * These 2 scenarios are possible only when we choose to manually select a
 * location on map (without searching or using GPS).
 *
 * So whenever we choose this option, the
 * chooseDestinationOnMap or chooseSourceOnMap variable is set to true
 * depending on the marker we choose to place / move.
 *
 * The third scenario occurs when we tap on the map, but the
 * chooseDestinationOnMap & chooseSourceOnMap is false.
 * In this case, instead of placing a marker, we close the control panel
 * if it is open
 */
    private fun markerPlacedOrMoved(coordinates: LatLng) {
        when {
            chooseDestinationOnMap -> {
                setDestinationMarker(LocationObject(getString(R.string.destination),
                        coordinates.latitude, coordinates.longitude))
                initiatePlaceNameRetrieval(coordinates, getString(R.string.destination))
            }
            chooseSourceOnMap -> {
                setSourceMarker(LocationObject(getString(R.string.source),
                        coordinates.latitude, coordinates.longitude))
                initiatePlaceNameRetrieval(coordinates, getString(R.string.source))
            }
            binding.airDistance.controlPanel.visibility == View.VISIBLE -> {
                binding.airDistance.controlToggleButton.performClick()
            }
        }
    }

    private fun disableMarkerPlacement() {
        chooseDestinationOnMap = false
        destinationMarker?.isDraggable = false

        chooseSourceOnMap = false
        sourceMarker?.isDraggable = false

        tapOnMapMsg?.dismiss()
    }

    private fun refreshHistoryList() {
        historyManager.updateAdapter()
        setHistoryListVisibility()
    }

    private fun addHistory() {
        val tempSrcMarker = sourceMarker ?: return
        val tempDstMarker = destinationMarker ?: return

        // if sourceMarker title is null or "Source", use the source coordinates as source name
        val srcLatLng = "${tempSrcMarker.position.latitude},${tempSrcMarker.position.longitude}"
        val srcName: String = when (val tempSrc: String? = tempSrcMarker.title) {
            null -> srcLatLng
            getString(R.string.source) -> srcLatLng
            else -> tempSrc
        }

        // if destinationMarker title is null or "Destination", use the destination coordinates as destination name
        val dstLatLng = "${tempDstMarker.position.latitude},${tempDstMarker.position.longitude}"
        val dstName: String = when (val tempDst: String? = tempDstMarker.title) {
            null -> dstLatLng
            getString(R.string.destination) -> dstLatLng
            else -> tempDst
        }

        val distance: String = binding.airDistance.distanceMsg.text.toString()

        historyManager.insert(srcName, srcLatLng, dstName, dstLatLng, distance)
        refreshHistoryList()
    }

    /*
     * @param useTextWatcher sourceTextWatcher is attached to sourceInput, so whenever
     *                       sourceInput changes, the sourceTextWatcher is called,
     *                       by setting useTextWatcher to false, the text watcher
     *                       is removed before changing the text & then re-applied
     */
    private fun updateSourceInputText(sourceName: String, useTextWatcher: Boolean) {
        if (!useTextWatcher)
            binding.airDistance.sourceInput.removeTextChangedListener(sourceTextWatcher)

        binding.airDistance.sourceInput
                .setText(if (sourceName == getString(R.string.source)) ""
                else sourceName)

        binding.airDistance.sourceCloseIcon.visibility =
                if (binding.airDistance.sourceInput.text.isEmpty()) View.GONE
                else View.VISIBLE

        if (!useTextWatcher)
            binding.airDistance.sourceInput.addTextChangedListener(sourceTextWatcher)
    }

    /*
     * @param useTextWatcher destinationTextWatcher is attached to destinationInput, so whenever
     *                       destinationInput changes, the destinationTextWatcher is called,
     *                       by setting useTextWatcher to false, the text watcher
     *                       is removed before changing the text & then re-applied
     */
    private fun updateDestinationInputText(destinationName: String, useTextWatcher: Boolean) {
        if (!useTextWatcher)
            binding.airDistance.destinationInput.removeTextChangedListener(destinationTextWatcher)

        binding.airDistance.destinationInput
                .setText(if (destinationName == getString(R.string.destination)) ""
                else destinationName)

        binding.airDistance.destinationCloseIcon.visibility =
                if (binding.airDistance.destinationInput.text.isEmpty()) View.GONE
                else View.VISIBLE

        if (!useTextWatcher)
            binding.airDistance.destinationInput.addTextChangedListener(destinationTextWatcher)
    }

    private fun stringToLatLng(text: String): LatLng? {
        try {
            val splitText = text.split(",")
            if (splitText.isEmpty() || splitText.lastIndex > 1) {
                return null
            }

            val latitude: Double = splitText[0].toDouble()
            val longitude: Double = splitText[1].toDouble()

            return LatLng(latitude, longitude)
        } catch (e: Exception) {
            return null
        }
    }

    private fun initiatePlaceNameRetrieval(coordinates: LatLng, placeType: String) {
        LatLngToPlaceTask(this, placeType)
                .setTaskListener(latLngToPlaceTaskListener)
                .execute(coordinates)
    }


    // ------------------------------ handle markers and distance --------------------
    private fun setMarker(marker: Marker?, coordinates: LatLng, title: String, centerMap: Boolean) {
        marker ?: return

        marker.isVisible = true
        marker.position = coordinates
        marker.title = title
        marker.showInfoWindow()

        if (centerMap)
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates, 11f))
    }

    private fun setSourceMarker(locationObject: LocationObject?) {
        locationObject?.let {
            val coordinates = LatLng(it.latitude, it.longitude)

            // when chooseSourceOnMap is
            // - false: user searched for a place, so center the map to this new position
            // - true: use manually chose a place on map, so don't center the map
            setMarker(sourceMarker, coordinates, it.placeName, !chooseSourceOnMap)

            showDistance()
        } ?: run { removeSourceMarker() }
    }

    private fun setDestinationMarker(locationObject: LocationObject?) {
        locationObject?.let {
            val coordinates = LatLng(it.latitude, it.longitude)

            // when chooseDestinationOnMap is
            // - false: user searched for a place, so center the map to this new position
            // - true: use manually chose a place on map, so don't center the map
            setMarker(destinationMarker, coordinates, it.placeName, !chooseDestinationOnMap)

            showDistance()
        } ?: run { removeDestinationMarker() }
    }

    private fun setHistoryMarkers(srcName: String, srcLatLng: String, dstName: String, dstLatLng: String) {
        val sourceCoordinate: LatLng = stringToLatLng(srcLatLng) ?: return
        val destinationCoordinate: LatLng = stringToLatLng(dstLatLng) ?: return

        // if destinationMarker is visible, & we set a new sourceMarker,
        // a new distance is shown between the new sourceMarker and the old destinationMarker
        // to avoid this, we remove both the marker before setting new marker
        sourceMarker?.isVisible = false
        destinationMarker?.isVisible = false

        disableMarkerPlacement()

        updateSourceInputText(srcName, false)
        updateDestinationInputText(dstName, false)

        setSourceMarker(LocationObject(srcName, sourceCoordinate.latitude, sourceCoordinate.longitude))
        setDestinationMarker(LocationObject(dstName, destinationCoordinate.latitude, destinationCoordinate.longitude))

        // history is selected from the history drawer, so the drawer will be open, close it
        binding.drawerLayout.closeDrawer(GravityCompat.START)

        mMap?.animateCamera(CameraUpdateFactory.zoomTo(0f))

        if (binding.airDistance.controlPanel.visibility == View.VISIBLE) {
            binding.airDistance.controlToggleButton.performClick()
        }
    }

    private fun removeDistanceLine() {
        distanceLine?.isVisible = false

        Animators.fadeOutView(binding.airDistance.distanceMsg)
        binding.airDistance.distanceUnitButton.visibility = View.GONE
    }

    private fun removeSourceMarker() {
        sourceMarker?.isVisible = false
        removeDistanceLine()
    }

    private fun removeDestinationMarker() {
        destinationMarker?.isVisible = false
        removeDistanceLine()
    }

    private fun showDistance() {
        val srcMarker = sourceMarker ?: return
        val dstMarker = destinationMarker ?: return

        if (!srcMarker.isVisible) return
        if (!dstMarker.isVisible) return

        distanceLine?.remove()

        distanceLine = mMap?.addPolyline(PolylineOptions()
                .add(srcMarker.position, dstMarker.position)
                .width(5f)
                .color(Color.RED))

        val distanceInKm = Functions.getDistanceInKm(srcMarker.position, dstMarker.position)
        displayDistance(distanceInKm)
        addHistory()
    }


    // ------------------------------ widget clicks --------------------
    fun clearSourceClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        updateSourceInputText("", true)
    }

    fun clearDestinationClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        updateDestinationInputText("", true)
    }

    /*
     * change the type of map: Normal, Satellite, Hybrid, Terrain
     */
    fun layerButtonClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        val mapTypes = arrayOf("Normal", "Satellite", "Hybrid", "Terrain")
        val mapTypeCodes = arrayOf(GoogleMap.MAP_TYPE_NORMAL, GoogleMap.MAP_TYPE_SATELLITE,
                GoogleMap.MAP_TYPE_HYBRID, GoogleMap.MAP_TYPE_TERRAIN)

        val mapTypeDialog = AlertDialog.Builder(this)
        with(mapTypeDialog) {
            setTitle(R.string.map_type)

            setSingleChoiceItems(mapTypes, selectedMapType) { dialog, which ->
                mMap?.mapType = mapTypeCodes[which]
                selectedMapType = which
                dialog.cancel()
            }

            setNegativeButton(R.string.cancel, null)
            show()
        }
    }

    fun distanceUnitButtonClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        val distanceTypes = arrayOf(Constants.DistanceType_Kilometers, Constants.DistanceType_Miles)
        val distanceUnits = arrayOf(Constants.DistanceUnit_Kilometers, Constants.DistanceUnit_Miles)

        with(AlertDialog.Builder(this)) {
            setTitle(R.string.distance_in)
            setNegativeButton(R.string.cancel, null)
            setItems(distanceTypes) { _, which ->
                PreferenceManager
                        .setDistanceUnitPreference(this@AirDistance, distanceUnits[which])
                showDistance()
            }
            show()
        }
    }

    fun openHistoryClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        binding.drawerLayout.openDrawer(GravityCompat.START)
    }

    /*
     * When closing the control panel, clear focus from
     * source and destination input fields, disable marker placement,
     * and animate the toggle button
     */
    fun controlToggleClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        binding.airDistance.sourceInput.clearFocus()
        binding.airDistance.destinationInput.clearFocus()

        Animators.rotateControlToggle(binding.airDistance.controlPanel,
                binding.airDistance.controlToggleButton)

        if (binding.airDistance.controlPanel.visibility == View.GONE) {
            Animators.fadeInView(binding.airDistance.controlPanel)
        } else {
            Animators.fadeOutView(binding.airDistance.controlPanel)
            if (chooseSourceOnMap || chooseDestinationOnMap) {
                disableMarkerPlacement()
            }
        }
    }

    /*
     * here we set chooseSourceOnMap to true, due to this,
     * we can tap anywhere on map to move the source marker over there
     * & also make the sourceMarker draggable, so we can drag the already
     * existing marker
     */
    fun chooseSourceOnMapClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        binding.airDistance.sourceInput.clearFocus()
        binding.airDistance.sourceInput.setText("")

        chooseSourceOnMap = true
        sourceMarker?.isDraggable = true

        tapOnMapMsg?.show()
    }

    /*
     * here we set chooseDestinationOnMap to true, due to this,
     * we can tap anywhere on map to move the destination marker over there
     * & also make the destinationMarker draggable, so we can drag the already
     * existing marker
     */
    fun chooseDestinationOnMapClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        binding.airDistance.destinationInput.clearFocus()
        binding.airDistance.destinationInput.setText("")

        chooseDestinationOnMap = true
        destinationMarker?.isDraggable = true

        tapOnMapMsg?.show()
    }

    fun clearHistoryButtonClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        with(AlertDialog.Builder(this)) {
            setTitle(R.string.clear_history)
            setMessage(R.string.are_you_sure)
            setNegativeButton(R.string.cancel, null)
            setPositiveButton(R.string.delete) { _, _ ->
                historyManager.deleteAll()
                refreshHistoryList()
            }
            show()
        }
    }

    fun useSourceLocationClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        if (SystemFeatures.hasPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            if (SystemFeatures.gpsIsOn(this)) {
                updateSourceInputText("", true)
                binding.airDistance.sourceProgressBar.visibility = View.VISIBLE
                getLocationAndSetMarker(getString(R.string.source))
            } else {
                Functions.showToast(this, R.string.turn_on_gps, true)
            }
        } else {
            askLocationPermission()
        }
    }

    fun useDestinationLocationClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        if (SystemFeatures.hasPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            if (SystemFeatures.gpsIsOn(this)) {
                updateDestinationInputText("", true)
                binding.airDistance.destinationProgressBar.visibility = View.VISIBLE
                getLocationAndSetMarker(getString(R.string.destination))
            } else {
                Functions.showToast(this, R.string.turn_on_gps, true)
            }
        } else {
            askLocationPermission()
        }
    }

    private fun goToSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }


    // ------------------------------ location & permission --------------------
    private fun askLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {

            with(AlertDialog.Builder(this)) {
                setTitle(R.string.location_permission_title)
                setCancelable(true)
                setMessage(R.string.location_permission_message)
                setNegativeButton(R.string.not_now, null)
                setPositiveButton(R.string.settings) { _, _ -> goToSettings() }
                show()
            }

        } else {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    Constants.PermissionFineLocation)
        }
    }

    private fun getLocationAndSetMarker(markerType: String) {
        val mFusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val locationRequest = LocationRequest()
        with(locationRequest) {
            interval = 10
            smallestDisplacement = 10f
            fastestInterval = 10
            setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        }

        val locationCallBack: LocationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult?) {
                result ?: return

                val currentLocation: Location? = result.lastLocation
                result.lastLocation?.let { mFusedLocationClient.removeLocationUpdates(this) }

                if (markerType == getString(R.string.source)) {
                    binding.airDistance.sourceProgressBar.visibility = View.GONE
                    currentLocation?.let {
                        setSourceMarker(LocationObject(markerType, it.latitude, it.longitude))
                        initiatePlaceNameRetrieval(LatLng(it.latitude, it.longitude), markerType)
                    } ?: run { setSourceMarker(null) }
                } else if (markerType == getString(R.string.destination)) {
                    binding.airDistance.destinationProgressBar.visibility = View.GONE
                    currentLocation?.let {
                        setDestinationMarker(LocationObject(markerType, it.latitude, it.longitude))
                        initiatePlaceNameRetrieval(LatLng(it.latitude, it.longitude), markerType)
                    } ?: run { setSourceMarker(null) }
                }
            }
        }

        try {
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallBack, Looper.myLooper())
        } catch (e: SecurityException) {
            return
        }
    }

}