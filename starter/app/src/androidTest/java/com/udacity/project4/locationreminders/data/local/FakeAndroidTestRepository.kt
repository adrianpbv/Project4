package com.udacity.project4.locationreminders.data.local

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.Result.Error
import com.udacity.project4.locationreminders.data.dto.Result.Success
import java.util.*

class FakeAndroidTestRepository : ReminderDataSource {

    var reminderLocationData: LinkedHashMap<String, ReminderDTO> = LinkedHashMap()

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return if (reminderLocationData.isEmpty())
            Error("There is no reminder")
        else
            Success(reminderLocationData.values.toList())
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminderLocationData[reminder.id] = reminder
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        reminderLocationData[id]?.let {
            return Success(it)
        }
        return Error("Could not find reminder")
    }

    override suspend fun deleteAllReminders() {
        reminderLocationData.clear()
    }
}