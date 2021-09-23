package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {
    //    Testing implementation for RemindersDao.kt

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun insertReminder_GetItById() = runBlockingTest {
        // GIVEN - Insert a location reminder.
        val reminder = ReminderDTO("title", "description", "location", 1.0, 1.0)
        database.reminderDao().saveReminder(reminder) // tests if it is saved correctly

        // WHEN - Get the reminder by id from the database.
        val loaded = database.reminderDao().getReminderById(reminder.id)

        // THEN - The loaded data contains the expected values.
        assertThat<ReminderDTO>(loaded as ReminderDTO, CoreMatchers.notNullValue())
        assertThat(loaded.id, `is`(reminder.id))// it was correctly saved
        assertThat(loaded.title, `is`(reminder.title))
        assertThat(loaded.description, `is`(reminder.description))
    }

    @Test
    fun deleteAllReminders_GetEmptyReminderList() = runBlockingTest {
        // GIVEN a non empty reminder list
        val reminder = ReminderDTO("title", "description", "location", 1.0, 1.0)
        database.reminderDao().saveReminder(reminder)

        val loaded = database.reminderDao().getReminders()
        assertThat(loaded.isNullOrEmpty(), `is`(false)) // Reminder was successfully inserted
        // so the list is not empty

        // WHEN all the reminders are delete from the database
        database.reminderDao().deleteAllReminders()

        // THEN check there is an empty list
        val emptyList = database.reminderDao().getReminders()
        assertThat(emptyList.isNullOrEmpty(), `is`(true))
    }
}