package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

class FakeDataSource(var locationReminder: MutableList<ReminderDTO> = mutableListOf()) :
    ReminderDataSource { // test double to the LocalDataSource
    //   Creating a fake data source to act as a double to the real data source

    var shouldReturnError = false

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) return Result.Error("Test Exception")
        else return Result.Success(ArrayList(locationReminder))
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        locationReminder.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) return Result.Error("Test Exception")

        val reminder = locationReminder.find { it.id == id }
        return if (reminder == null) Result.Error("Reminder does not exit")
        else
            Result.Success(reminder)
    }

    override suspend fun deleteAllReminders() {
        locationReminder.clear()
    }


}