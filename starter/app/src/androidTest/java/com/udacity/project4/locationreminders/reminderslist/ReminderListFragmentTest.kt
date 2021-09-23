package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.FakeAndroidTestRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.inject
import org.mockito.Mockito
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
/**
 * Integration test for [ReminderListFragment]
 */
class ReminderListFragmentTest : AutoCloseKoinTest() {
// AutoCloseKoinTest stops Koin after every test

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val repository: FakeAndroidTestRepository by inject()

    val koinTestModules = module {
        single { FakeAndroidTestRepository() }
        viewModel {
            RemindersListViewModel(get(), get() as FakeAndroidTestRepository)
        }
    }

    @Before
    fun initRepository() {
        stopKoin()
        startKoin {
            androidContext(getApplicationContext())
            modules(koinTestModules)
        }
    }

    @After
    fun cleanUp() = runBlockingTest {
        repository.deleteAllReminders()
    }

    //    test the navigation of the fragments.
    @Test
    fun clickAddReminderButton_navigateToSaveFragment() {
        // GIVEN - On the home screen
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = Mockito.mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // WHEN - Click on the "+" button
        onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())

        // THEN - Verify that we navigate to the add screen
        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder()
        ) // got error here
//        Wanted but not invoked:
//        navController.navigate(
//            ActionOnlyNavDirections(actionId=2131231213)
//        );
        // Actually, there were zero interactions with this mock.

        // the answer here
        // https://knowledge.udacity.com/questions/560547 // didn't help I tried that way and nothing
    }

    // test the displayed data on the UI.
    @Test
    fun addReminderLocationData_checkItOnUI() = runBlockingTest {
        repository.saveReminder(
            ReminderDTO(
                "Title1", "Description1", "Location", 1.0, 1.0
            )
        )
        repository.saveReminder(
            ReminderDTO(
                "Title2", "Description2", "Location", 1.0, 1.0
            )
        )
        // GIVEN - On the home screen with the above reminders
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // The check the reminders are shown on screen
        onView(withText("Title1")).check(matches(isDisplayed()))
        onView(withText("Title2")).check(matches(isDisplayed()))
    }

    //    add testing for the error messages.
    @Test
    fun showErrorMessage() = runBlockingTest {
        // GIVEN - On the home screen, when there is no location to reminder
        repository.deleteAllReminders()
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // THEN check that the snackBar is shown with the message
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText("There is no reminder")))
        // Also check that the icon noDataTextView shows up on the screen
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

}