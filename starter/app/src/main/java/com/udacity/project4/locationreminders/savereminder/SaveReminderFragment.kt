package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceLocation
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
    private var resultLauncherForegroundLocation: ActivityResultLauncher<String>
    private var resultLauncherBackgroundLocation: ActivityResultLauncher<String>
    private var shouldNavigate: Boolean = false
    private lateinit var reminderLocation: ReminderDataItem

    init {
        resultLauncherForegroundLocation = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (shouldNavigate) {
                navigatetoSelectLocationFragment()
                if (!isGranted)
                    _viewModel.showToast.value = getString(
                        R.string.permission_denied_explanation
                    )
            } else {
                // the foreground permission is asked before the background location permission
                if (!isGranted) showSnackBarAppSettings()
                else
                    requestLocationPermissions()
            }
        }

        resultLauncherBackgroundLocation = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                saveDataAndStartGeofence()
            } else {
                showSnackBarAppSettings()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel
        permissionManager = PermissionManager(this)
        geofenceLocation = GeofenceLocation(this, _viewModel)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this

        binding.selectLocation.setOnClickListener {
            if (permissionManager.foregroundPermissionApproved()) {
                _viewModel.reminderSelectedLocationStr.value = ""
                navigatetoSelectLocationFragment()
            } else {
                shouldNavigate = true
                resultLauncherForegroundLocation.launch(
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )
            }

        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            reminderLocation =
                ReminderDataItem(title, description, location, latitude, longitude)

            //Check the data is correct in order to start the geofence
            if (_viewModel.validateEnteredData(reminderLocation))
                if (permissionManager.foregroundAndBackgroundLocationPermissionApproved()) {
                    saveDataAndStartGeofence()
                } else
                    requestLocationPermissions()
        }
    }

    /**
     * Save the reminder location data in the dataBase and start the geofence
     */
    private fun saveDataAndStartGeofence() {
        geofenceLocation.setReminderDataItem(reminderLocation)
        geofenceLocation.checkDeviceLocationSettingsAndStartGeofence()
    }

    /**
     * Navigate to the map fragment to select a location
     */
    private fun navigatetoSelectLocationFragment() {
        _viewModel.navigationCommand.value =
            NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
    }

    /**
     * Suggest to the user to grant the location permission to improve the experience with the app
     */
    private fun showSnackBarAppSettings() {
        Snackbar.make(
            binding.coordinatorLyt,
            R.string.background_permission_denied_explanation,
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(R.string.settings) {
                // Displays App settings screen.
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }.show()
    }

    @SuppressLint("InlinedApi")
    private fun requestLocationPermissions() {
        /**
         * it's recommended that the app performs incremental requests for location
         * permissions, asking for foreground location access and then background
         * location access, so the users have more control and transparency because they
         * can better understand which features in your app need background location access.
         */
        if (permissionManager.foregroundPermissionApproved()) {
            if (permissionManager.isApiVerQorLater()) {
                val permission = android.Manifest.permission.ACCESS_BACKGROUND_LOCATION

                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.background_location_permission_title)
                    .setMessage(R.string.background_location_permission_message)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        // this request will take user to Application's Setting page
                        Log.v("PermissionManager", "***** AlertDialog *****")
                        resultLauncherBackgroundLocation.launch(permission)
                    }
                    .setNegativeButton(R.string.no) { dialog, _ ->
                        dialog.dismiss()
                        showSnackBarAppSettings()
                    }
                    .create()
                    .show()
            }
        } else {
            shouldNavigate = false
            resultLauncherForegroundLocation.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
