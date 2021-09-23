package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.utils.MainCoroutineRule
import com.udacity.project4.utils.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    // Executes each task synchronously using Architecture Components in the same thread.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Subject under test
    private lateinit var reminderListViewModel: RemindersListViewModel

    // Use a fake repository to be injected into the view model.
    private lateinit var fakeData: FakeDataSource

    @Before
    fun setupRemindersListViewModel() {
        val reminder = ReminderDTO(
            "Title1", "Description1", "location1", 1.0, 1.0
        )

        // Initialise the repository with no tasks.
        fakeData = FakeDataSource()

        FirebaseApp.initializeApp(getApplicationContext())

        reminderListViewModel = RemindersListViewModel(getApplicationContext(), fakeData)

        // Using the same Scope for tests
        mainCoroutineRule.runBlockingTest {
            fakeData.saveReminder(reminder)
        }
    }

    @After
    fun cleanUp() {
        mainCoroutineRule.runBlockingTest {
            // Clean the the fake repository after each test
            fakeData.deleteAllReminders()
        }
    }

    @Test
    fun loadingReminderList_emptyReminderList_returnTrue() {
        mainCoroutineRule.runBlockingTest {
            // Given the fake data source empty
            fakeData.deleteAllReminders() // make sure there is no reminder at the fakeDateSource

            // when the reminder are loading from the fake data source
            reminderListViewModel.loadReminders() // Running a coroutine from viewModelScope
            // require a change of Dispatchers.

            // then test the reminder list is empty
            assertThat(
                reminderListViewModel.remindersList.getOrAwaitValue().isNullOrEmpty(),
                `is`(true)
            )
            // the live data is set up to true as there is no reminder in the list
            assertThat(reminderListViewModel.showNoData.getOrAwaitValue(), `is`(true))
        }
    }

    @Test
    fun loadingReminderList_addReminders_ListLoaded() {
        // Given the locations to remind at the beginning

        mainCoroutineRule.runBlockingTest {
            //loading the fake locations
            reminderListViewModel.loadReminders()

            // then check there is already data in the reminderList
            assertThat(reminderListViewModel.showNoData.getOrAwaitValue(), `is`(false))
        }
    }

    @ExperimentalCoroutinesApi // As we're using runBlockingTest{}
    @Test
    fun loadReminderLocations_showLoading() {
        // Pause the coroutine to see what happens while it's running
        mainCoroutineRule.pauseDispatcher()

        // When the reminder location data is loading
        reminderListViewModel.loadReminders()

        // The loading indicator is shown
        assertThat(reminderListViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // When the dispatcher resumed
        mainCoroutineRule.resumeDispatcher()

        //showLoading is false there it's hidden
        assertThat(reminderListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun loadLocationsWhenRemindersUnavailable_snackBarErrorToDisplay() {
        fakeData.setReturnError(true)

        mainCoroutineRule.runBlockingTest {
            reminderListViewModel.loadReminders()

            assertThat(
                reminderListViewModel.showSnackBar.getOrAwaitValue(),
                `is`("Test Exception")
            )
        }
    }

}