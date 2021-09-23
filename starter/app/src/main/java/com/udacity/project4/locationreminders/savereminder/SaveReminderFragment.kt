package com.udacity.project4.locationreminders.savereminder

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceLocation
import com.udacity.project4.locationreminders.geofence.GeofenceLocation.Companion.REQUEST_TURN_DEVICE_LOCATION_ON
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.PermissionManager
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var permissionManager: PermissionManager
    private lateinit var geofenceLocation: GeofenceLocation

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel
        permissionManager = PermissionManager(this)
        geofenceLocation = GeofenceLocation(requireActivity(), _viewModel)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            _viewModel.reminderSelectedLocationStr.value = ""
            // Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            if (permissionManager.foregroundAndBackgroundLocationPermissionApproved()) {
                val reminderLocation =
                    ReminderDataItem(title, description, location, latitude, longitude)
                if (_viewModel.validateEnteredData(reminderLocation)) {
                    // verify the data is correct in order to start the geofence and
                    // save it into the db
                    geofenceLocation.setReminderDataItem(reminderLocation)
                    geofenceLocation.checkDeviceLocationSettingsAndStartGeofence()
                }
            } else request_Location_permissions()


        }

        binding.selectLocation.setOnClickListener {
            if (permissionManager.foregroundAndBackgroundLocationPermissionApproved()) {
                _viewModel.navigationCommand.postValue(
                    NavigationCommand.To(
                        SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment()
                    )
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            if (resultCode == Activity.RESULT_OK) {
                Log.i(TAG, "ActivityOK " + Activity.RESULT_OK)
                geofenceLocation.checkDeviceLocationSettingsAndStartGeofence()
            } else {
                Snackbar.make(
                    binding.root,
                    R.string.location_required_error,
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    geofenceLocation.checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }

        }
    }

    override fun onResume() {
        super.onResume()

        request_Location_permissions()
    }

    fun request_Location_permissions() {
        if (permissionManager.foregroundPermissionApproved() &&
            permissionManager.isApiVerQorLater()
        )
            permissionManager.requestLocationPermissions(true)
        else permissionManager.requestLocationPermissions(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}

private const val TAG = "SaveReminderFragment"
