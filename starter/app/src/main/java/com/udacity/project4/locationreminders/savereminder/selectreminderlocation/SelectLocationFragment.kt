package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.PermissionManager
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var gmap: GoogleMap
    private lateinit var permissionManager: PermissionManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mCurrentLocation: Location
    private lateinit var lastMarker: Marker
    private var poi: PointOfInterest? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        permissionManager = PermissionManager(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.googleMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.pickLocation.setOnClickListener {
            onLocationSelected()
        }

        return binding.root
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    mCurrentLocation = location
                }
            }
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        //setup the map
        if (googleMap != null)
            gmap = googleMap

        //show the user's location
        if (enableUserLocation()) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            getLastLocation()
        }

        val latitude = 28.1910
        val longitude = -82.7570

        val zoomLevel = 16f

        val homeLatLng =
            if (_viewModel.latitude.value != null && _viewModel.longitude.value != null) {
                LatLng(_viewModel.latitude.value!!, _viewModel.longitude.value!!)
            } else if (::mCurrentLocation.isInitialized) {
                LatLng(mCurrentLocation.latitude, mCurrentLocation.longitude)
            } else {
                LatLng(latitude, longitude)
            }
        gmap.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng, zoomLevel))
        lastMarker = gmap.addMarker(
            MarkerOptions()
                .position(homeLatLng)
        )

        // Set up for long click on the map
        setMapLongClick(gmap)
        setPoiClick(gmap)

        //setMapStyle(gmap)
    }

    private fun onLocationSelected() {
        // if the user has not marked a point, do nothing
        if (!::lastMarker.isInitialized) {
            _viewModel.showSnackBarInt.value = R.string.location_not_selected
            return
        }
        // When the user confirms on the selected location
        val locationToRemind = lastMarker.position

        Log.i(
            TAG, "Selected Location Lat = " + locationToRemind.latitude
                    + " Long = " + locationToRemind.longitude
        )

        if (poi != null &&
            poi!!.latLng.latitude == locationToRemind.latitude &&
            poi!!.latLng.longitude == locationToRemind.longitude
        ) {
            _viewModel.selectedPOI.value = poi
            _viewModel.reminderSelectedLocationStr.value = poi?.name.toString()
        } else {
            _viewModel.reminderSelectedLocationStr.value = getString(R.string.dropped_pin)
        }


        _viewModel.latitude.value = locationToRemind.latitude
        _viewModel.longitude.value = locationToRemind.longitude
        _viewModel.navigationCommand.value = NavigationCommand.Back
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // Change the map type based on the user's selection.
        R.id.normal_map -> {
            gmap.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            gmap.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            gmap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            gmap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            if (::lastMarker.isInitialized) {
                lastMarker.remove()
            }

            // A Snippet is Additional text that's displayed below the title.
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )

            // In this case the snippet displays the latitude and longitude of a marker.
            lastMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng) // to add a marker on the screen
                    .title(getString(R.string.dropped_pin)) // Additional info,
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            poi = null // if the user set it's own point of interest, reset the default one
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi -> // place a marker at the POI location
            if (::lastMarker.isInitialized) {
                lastMarker.remove()
            }

            this.poi = poi

            lastMarker = map.addMarker( // Save the result to a variable called poiMarker.
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name) // Set the title to the name of the POI.
            )
            lastMarker.showInfoWindow() //  to immediately show the info window.
        }

    }

    @SuppressLint("MissingPermission")
    private fun enableUserLocation(): Boolean {
        return if (permissionManager.foregroundPermissionApproved()) {
            gmap.isMyLocationEnabled = true
            true
        } else {
            permissionManager.requestLocationPermissions(false)
            false
        }
    }

}

const val TAG = "SelectLocationFragment"
