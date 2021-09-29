package com.udacity.project4

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import com.udacity.project4.utils.ToastManager
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest // //END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    // An Idling Resource that waits for Data Binding to have no pending bindings.
    private val dataBindingIdlingResource = DataBindingIdlingResource()

//    @get:Rule
//    var activityScenarioRule = ActivityScenarioRule(RemindersActivity::class.java)
//    @Rule
//    val instantTaskExecutorRule = InstantTaskExecutorRule()

//    @Rule
//    val activityRule = ActivityTestRule(RemindersActivity::class.java)


    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin() // stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    /**
     * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
     * are not scheduled in the main Looper (for example when executed on a different thread).
     */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
        IdlingRegistry.getInstance().register(ToastManager.idlingResource)
    }

    /**
     * Unregister your idling resource so it can be garbage collected and does not leak any memory.
     */
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
        IdlingRegistry.getInstance().register(ToastManager.idlingResource)
    }

    @After
    fun reset() {
        runBlocking {
            repository.deleteAllReminders()
        }
    }

//    Add End to End testing to the app

    private fun getActivity(activityScenario: ActivityScenario<RemindersActivity>): Activity? {
        var activity: Activity? = null
        activityScenario.onActivity {
            activity = it
        }
        return activity
    }

    private fun addReminder() {
        runBlocking {
            repository.saveReminder(
                ReminderDTO(
                    "Title1", "Description1", "Location", 1.0, 1.0
                )
            )
        }
    }

    @Test
    fun addLocationReminder_showSavedToast() {
        // start with a reminder
        addReminder()

        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // check that reminder is displayed
        onView(withText("Title1")).check(matches(isDisplayed()))

        // Click on the "+" button to add a new reminder
        onView(withId(R.id.addReminderFAB)).perform(click())
        // Navigate to the map to choose a location
        onView(withId(R.id.selectLocation)).perform(click())

        // In this case the device's current location will be selected,
        // then navigates to save the reminder
        onView(withId(R.id.pickLocation)).perform(click())

        // finish writing the details of the location reminder
        onView(withId(R.id.reminderTitle)).perform(
            typeText("Title2")
        )
        onView(withId(R.id.reminderDescription)).perform(
            typeText("Description2"),
            closeSoftKeyboard()
        )

        // Save the reminder into the database
        onView(withId(R.id.saveReminder)).perform(click()) // save reminder

        // Check that the toast is displayed
        // Next approach to test Toast is not working with api 30
        // https://github.com/android/android-test/issues/803#issue-744609125
        // Either with this way as given at https://stackoverflow.com/a/38379219/
//        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q) {
//            onView(withText(R.string.reminder_saved)).inRoot(
//                withDecorView(not(`is`(getActivity(activityScenario)!!.window.decorView)))
//            ).check(matches(isDisplayed()))
//        }

        // check that the reminder was successfully added
        onView(withText("Title2")).check(matches(isDisplayed()))

        //  Make sure the activity is closed.
        activityScenario.close()
    }

    @Test
    fun showSnackBarError_WhenAnEmptyTitle() {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Click on the "+" button to add a new reminder
        onView(withId(R.id.addReminderFAB)).perform(click())

        //writing some text in the description field
        onView(withId(R.id.reminderDescription)).perform(
            typeText("Description2"),
            closeSoftKeyboard()
        )

        // Try to save the reminder into the database
        onView(withId(R.id.saveReminder)).perform(click()) // save reminder

        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText("Please enter title")))

        //  Make sure the activity is closed.
        activityScenario.close()
    }

    @Test
    fun navigation_pressingBackButton_backToReminderList() {
        // Start at the reminders list screen.
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // The reminderList is empty
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))

        // Navigate to add a reminder
        onView(withId(R.id.addReminderFAB)).perform(click())

        // Navigate to add a location
        onView(withId(R.id.selectLocation)).perform(click())

        // Confirm that if we click Back once, we end up back at the save reminder fragment.
        pressBack()
        onView(withText(R.string.reminder_location)).check(matches(isDisplayed()))

        // Confirm that if we click Back a second time, we end up back at the home screen.
        pressBack()
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))

        // When using ActivityScenario.launch(), always call close()
        activityScenario.close()
    }

}
