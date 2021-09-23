package com.udacity.project4.locationreminders.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.JobIntentService
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import kotlin.coroutines.CoroutineContext

class GeofenceAfterBootJobIntentService : JobIntentService(), CoroutineScope {

    private lateinit var geofencingClient: GeofencingClient

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onHandleWork(intent: Intent) {

        //geofenceLocation = GeofenceLocation(applicationContext as Activity)
        val remindersLocalRepository: RemindersLocalRepository by inject()

        CoroutineScope(coroutineContext).launch(SupervisorJob()) {
            //get the reminder with the request id
            val result: Result<List<ReminderDTO>> = remindersLocalRepository.getReminders()
            when (result) {
                is Result.Success<*> -> {
                    (result.data as List<ReminderDTO>).map { reminder ->
                        addGeofencesInBackground(
                            ReminderDataItem(
                                reminder.title,
                                reminder.description,
                                reminder.location,
                                reminder.latitude,
                                reminder.longitude,
                                reminder.id
                            )
                        )
                    }
                }
                is Result.Error -> Log.e(TAG, "" + result.message)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.v(TAG, "All work complete")
    }

    private fun addGeofencesInBackground(reminderDataItem: ReminderDataItem) {
        // re-add the Geofences
        val geofence = Geofence.Builder()
            .setRequestId(reminderDataItem.id)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setCircularRegion(
                reminderDataItem.latitude!!, reminderDataItem.longitude!!,
                GeofenceLocation.GEOFENCE_RADIUS_IN_METERS
            )
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnFailureListener {
                    Log.e(
                        TAG,
                        "An error occurred while adding the geofences in background. " + it.message
                    )
                }
                addOnSuccessListener {
                    Log.v(TAG, "Geofences added successfully in background")
                }
            }
        }

    }

    companion object {
        private const val JOB_ID = 572
        private const val TAG = "AddingGeofencesJobServ"

        //  start the JobIntentService to handle the geofencing transition events
        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(
                context,
                GeofenceAfterBootJobIntentService::class.java, JOB_ID,
                intent
            )
        }
    }
}