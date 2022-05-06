package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.error
import com.udacity.project4.locationreminders.data.dto.succeeded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

//    TODO: Add testing implementation to the RemindersLocalRepository.kt

    private lateinit var localDataSource: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {

        // setMain() set TestCoroutineDispatcher as a delegate inside the class provided by Dispatchers.Main .
        Dispatchers.setMain(TestCoroutineDispatcher()) // switch the Dispatcher to use Test Dispatcher

        // Using an in-memory database for testing, because it doesn't survive killing the process.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        localDataSource =
            RemindersLocalRepository(
                database.reminderDao(),
                Dispatchers.Main
            )
    }

    @After
    fun cleanUp() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun saveReminder_retrievesReminderById() {
        //Dispatchers.setMain(TestCoroutineDispatcher()) // switch the Dispatcher to use Test Dispatcher

        runBlockingTest {
            // GIVEN - A new location reminder saved in the database.
            val reminder = ReminderDTO("title", "description", "location", 1.0, 1.0)
            localDataSource.saveReminder(reminder)

            // WHEN  - Reminder retrieved by ID.
            val savedReminder =
                localDataSource.getReminder(reminder.id) // return a Success(reminder) object

            // THEN - Same reminder is returned.
            assertThat(savedReminder.succeeded, `is`(true))
            savedReminder as Result.Success // casts the result as a Success
            assertThat(savedReminder.data.title, `is`("title"))
            assertThat(savedReminder.data.description, `is`("description"))
        }
        //Dispatchers.resetMain() // clean it up
    }

    @Test
    fun retrievesReminderById_getErrorIdNotFund() {
        runBlockingTest {
            // GIVEN - A new location reminder saved in the database.
            addReminder()

            // WHEN  - Reminder retrieved by ID.
            val savedReminder =
                localDataSource.getReminder("whateverID") // return an Error("message") object

            // THEN - Same reminder is returned.
            assertThat(savedReminder.error, `is`(true))
        }
    }

    @Test
    fun deleteAllReminders_retrievesEmptyList() {
        runBlockingTest {
            addReminder()

            val reminderList = localDataSource.getReminders() as Result.Success

            assertThat(reminderList.succeeded, `is`(true))
            assertThat(reminderList.data.isNullOrEmpty(), `is`(false))

            localDataSource.deleteAllReminders()
            val emptyList = localDataSource.getReminders() as Result.Success
            assertThat(emptyList.data.isNullOrEmpty(), `is`(true))
        }
    }

    private fun addReminder() = runBlocking {
        localDataSource.saveReminder(
            ReminderDTO("title", "description", "location", 1.0, 1.0)
        )
    }
}
