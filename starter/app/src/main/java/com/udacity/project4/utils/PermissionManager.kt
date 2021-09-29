package com.udacity.project4.utils

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
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
            if (isApiVerQorLater()) {
                checkSinglePermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                true
            }
        // Return true if the permissions are granted and false if not.
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    fun checkSinglePermission(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            fragment.requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}
