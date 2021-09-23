package com.udacity.project4.utils

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.fragment.app.Fragment
import com.udacity.project4.R

/**
 * Class to deal with all the permissions required by the app
 */
class PermissionManager(private var fragment: Fragment) {

    private val androidApiVer = android.os.Build.VERSION.SDK_INT

    fun isApiVerQorLater() = androidApiVer >= android.os.Build.VERSION_CODES.Q

    /**
     * Verify just for the foreground location permission
     */
    fun foregroundPermissionApproved() =
        checkSinglePermission(Manifest.permission.ACCESS_FINE_LOCATION)

    /**
     * Ask for both location permissions as when the background is required the foreground
     * permission is implicit to it
     */
    @SuppressLint("InlinedApi")
    fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved =
            checkSinglePermission(Manifest.permission.ACCESS_FINE_LOCATION)

        val backgroundPermissionApproved =
            if (androidApiVer >= android.os.Build.VERSION_CODES.Q) {
                checkSinglePermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                true
            }
        // Return true if the permissions are granted and false if not.
        return foregroundLocationApproved && backgroundPermissionApproved
    }


    fun requestLocationPermissions(requestBackgroundLocation: Boolean) {
        // when the user wants to know the location or mark a POI in the map, only ask for
        // foreground location permission
        // when the user sets a reminder by using a geofence in order to save it, the user will
        // need to give access to the background location service before adding it
        // Once the app have been started to remind locations and the user somehow deny the
        // app's location permissions the app will ask for both permission one by one if the Api is
        // the 30 one and plus. For prior version just ask for the foreground permission

        when {
            (androidApiVer < android.os.Build.VERSION_CODES.Q) -> {
                checkLocationPermissionAPI28()
            }
            (androidApiVer == android.os.Build.VERSION_CODES.Q) -> {
                checkLocationPermissionAPI29(requestBackgroundLocation)
            }
            else -> {
                checkBackgroundLocationPermissionAPI30(
                    requestBackgroundLocation
                )
            }
        }
    }


    fun checkSinglePermission(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            fragment.requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Location permission is only requested once, to use both foreground and background apps
    @TargetApi(28)
    fun checkLocationPermissionAPI28() {
        if (!checkSinglePermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            requireLocationPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    // request fore & background location permissions at the same time
    @TargetApi(29)
    fun checkLocationPermissionAPI29(requestBackgroundLocation: Boolean) {
        if (checkSinglePermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
            checkSinglePermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        ) return
        if (!requestBackgroundLocation) {
            requireLocationPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            )
        } else {
            requireLocationPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ),
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            )
        }

// it's enough to ask for the foreground permission (either ACCESS_COARSE_LOCATION or
// ACCESS_FINE_LOCATION), the Android OS will automatically add the background permission
// (ACCESS_BACKGROUND_LOCATION) into the request
    }

    // Ask for the fore & background location permissions separately when each one is needed
    @TargetApi(30) // Android 11
    fun checkBackgroundLocationPermissionAPI30(
        requestBackgroundPermission: Boolean
    ) {
        if (!requestBackgroundPermission &&
            !checkSinglePermission(Manifest.permission.ACCESS_FINE_LOCATION)
        ) {
            Log.i("PermissionManager", "HEREEE")
            requestPermissions(
                fragment.requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            )
            return
        } else
            if (checkSinglePermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                return

        AlertDialog.Builder(fragment.requireContext())
            .setTitle(R.string.background_location_permission_title)
            .setMessage(R.string.background_location_permission_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                // this request will take user to Application's Setting page
                requireLocationPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ),
                    REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
                )
            }
            .setNegativeButton(R.string.no) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun requireLocationPermissions(permList: Array<String>, requestCode: Int) {
        requestPermissions(fragment.requireActivity(), permList, requestCode)
    }

    companion object {
        const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
        const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
    }
}
