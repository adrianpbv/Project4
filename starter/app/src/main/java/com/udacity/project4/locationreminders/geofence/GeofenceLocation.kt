package com.udacity.project4.locationreminders.geofence

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.util.Log
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.udacity.project4.R
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel

/**
 * Class to create and start a location reminder by adding geofence
 */
class GeofenceLocation(
    private val activity: Activity,
    private val viewModel: SaveReminderViewModel
) {

    private var reminderDataItem: ReminderDataItem? = null
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(activity.applicationContext, GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        // Use FLAG_UPDATE_CURRENT so that you get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        PendingIntent.getBroadcast(
            activity.applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun setReminderDataItem(reminderDataItem: ReminderDataItem) {
        this.reminderDataItem = reminderDataItem
    }

    /**
     *  Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn on location services within the app.
     */
    fun checkDeviceLocationSettingsAndStartGeofence(): Boolean {
        // check that the device's location is on if not show a dialog to activate it.

        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        // Using LocationServices to get the Settings Client and create
        // a val called locationSettingsResponseTask to check the location settings.
        val settingsClient = LocationServices.getSettingsClient(activity)
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        // finding out if the location settings are not satisfied
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(
                        activity,
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.e(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            }
        }

        // If the locationSettingsResponseTask does complete, check that it is successful,
        // if so you will want to add the geofence.
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                Log.i(TAG, "Successful " + locationSettingsResponseTask.result.toString())
                addGeofence()
            }
        }
        return false
    }

    @SuppressLint("MissingPermission")
    fun addGeofence() {

        if (reminderDataItem == null) {
            viewModel.showSnackBarInt.value = R.string.error_adding_geofence
            return
        }

        val geofence = Geofence.Builder()
            // Set the request ID, string to identify the geofence.
            .setRequestId(reminderDataItem?.id)
            // Set the circular region of this geofence.
            .setCircularRegion(
                reminderDataItem?.latitude!!,
                reminderDataItem?.longitude!!,
                GEOFENCE_RADIUS_IN_METERS
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            // Set the transition types of interest. Alerts are only generated for these
            // transition. We track entry and exit transitions in this app.
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        // Build the geofence request
        val geofencingRequest = GeofencingRequest.Builder()
            // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
            // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
            // is already inside that geofence.
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)

            // Add the geofences to be monitored by geofencing service.
            .addGeofence(geofence)
            .build()

        val geofencingClient = LocationServices.getGeofencingClient(activity)

        // Add the new geofence request with the new geofence
        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
            addOnSuccessListener {
                // Geofence added, then save it into the dataBase
                viewModel.saveReminder(reminderDataItem!!)
            }
            addOnFailureListener {
                // there was an issue in adding the geofences.
                viewModel.showErrorMessage.value = activity.applicationContext.getString(
                    R.string.geofences_not_added
                )
                if ((it.message != null)) {
                    Log.e(TAG, it.message.toString())
                }
            }
        }
    }

    companion object {
        private const val TAG = "GeofenceLocation"
        const val GEOFENCE_RADIUS_IN_METERS = 200f
        const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
        const val ACTION_GEOFENCE_EVENT =
            "GeofenceTransitionsJobIntentService.locationReminder.action.ACTION_GEOFENCE_EVENT"
    }
}