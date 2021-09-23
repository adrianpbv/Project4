package com.udacity.project4.locationreminders.savereminder

import android.content.Context
import android.provider.Settings.System.getString
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.MainCoroutineRule
import com.udacity.project4.utils.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    //Testing the SaveReminderViewModel and its live data objects
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the coroutines Test dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Use a fake repository to be injected into the view model.
    private lateinit var fakeData: FakeDataSource

    private lateinit var saveReminderViewModel: SaveReminderViewModel

    @Before
    fun setupRemindersListViewModel() {
        // Initialise the repository with no tasks.
        fakeData = FakeDataSource()

        //FirebaseApp.initializeApp(getApplicationContext())

        saveReminderViewModel = SaveReminderViewModel(getApplicationContext(), fakeData)
    }


    @After
    fun cleanUp(){
        mainCoroutineRule.runBlockingTest {
            // Clean the fake repository after each test
            fakeData.deleteAllReminders()
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun saveReminderData_showLoading(){
        val reminder = ReminderDataItem(
            "Title1", "Description1", "location1",1.0, 1.0)

        // Pause the coroutine to see what happens while it's running
        mainCoroutineRule.pauseDispatcher()

        // Save the reminderLocation into the fake repository
        saveReminderViewModel.saveReminder(reminder)


        // The loading indicator is shown
        assertThat( saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // When the dispatcher resumes
        mainCoroutineRule.resumeDispatcher()

        //showLoading is false, therefore it's hidden
        assertThat( saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun saveReminderData_addReminder_savedIntoFakeDataSource(){
        val reminder = ReminderDataItem(
            "Title1", "Description1", "location1",1.0, 1.0)

        mainCoroutineRule.runBlockingTest {
            saveReminderViewModel.saveReminder(reminder)

            // As the coroutine code with the TestCoroutineScope runs immediately (in a
            // deterministic order) get the saved reminder from the FakeDataSource
            val savedReminder = fakeData.getReminder(reminder.id)
            // then assert that are the same reminder
            assertThat((savedReminder as Result.Success).data.id, `is`(reminder.id))

            assertThat(saveReminderViewModel.showToast.getOrAwaitValue(), `is`(
                (getApplicationContext() as Context).getString(R.string.reminder_saved)
            ))
        }
    }

    @Test
    fun validateEnteredData_emptyList_returnFalse(){
        saveReminderViewModel.validateEnteredData(
            ReminderDataItem("","","",0.0,0.0)
        )
        assertThat(saveReminderViewModel.showSnackBarInt.value, `is`(R.string.err_enter_title))

        saveReminderViewModel.validateEnteredData(
            ReminderDataItem("Title","","",0.0,0.0)
        )
        assertThat(saveReminderViewModel.showSnackBarInt.value, `is`(R.string.err_select_location))
    }

    @Test
    fun validateEnteredData_addReminder_returnTrue(){
        val validate = saveReminderViewModel.validateEnteredData(
            ReminderDataItem("Title","Description","Location",
                0.0,0.0)
        )
        assertThat(validate, `is`(true))
    }

}