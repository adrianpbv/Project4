package com.udacity.project4.locationreminders.savereminder

import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.FakeAndroidTestRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.mockito.Mockito


@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@MediumTest //UI Testing
class SaveReminderFragmentTest : AutoCloseKoinTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    val koinTestModules = module {
        single { FakeAndroidTestRepository() }
        single {
            SaveReminderViewModel(get(), FakeAndroidTestRepository() as ReminderDataSource)
        }
    }


    @Before
    fun initRepository() {
        stopKoin()
        startKoin {
            androidContext(getApplicationContext())
            loadKoinModules(koinTestModules)
        }
    }

    @Test
    fun clickSelectLocationButton_navigateToSelectLocationFragment() {
        val scenario = launchFragmentInContainer<SaveReminderFragment>(Bundle(), R.style.AppTheme)
        val navController = Mockito.mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        onView(withId(R.id.selectLocation)).perform(ViewActions.click())

        // THEN - Verify that we navigate to the map screen
        Mockito.verify(navController).navigate(
            SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment()
        )
    }

    @Test
    fun saveReminderLocation_emptyTitle_ShowToastError() {
        launchFragmentInContainer<SaveReminderFragment>(Bundle(), R.style.AppTheme)
        onView(withId(R.id.reminderDescription)).perform(
            ViewActions.typeText("Description1"),
            ViewActions.closeSoftKeyboard()
        )

        onView(withId(R.id.saveReminder)).perform(ViewActions.click()) // save reminder

        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_enter_title)))
    }

    @Test
    fun saveReminderLocation_emptyLocation_ShowToastError() {
        launchFragmentInContainer<SaveReminderFragment>(Bundle(), R.style.AppTheme)
        onView(withId(R.id.reminderTitle))
            .perform(ViewActions.typeText("Title1"), ViewActions.closeSoftKeyboard())
        onView(withId(R.id.reminderDescription)).perform(
            ViewActions.typeText("Description1"),
            ViewActions.closeSoftKeyboard()
        )

        onView(withId(R.id.saveReminder)).perform(ViewActions.click()) // save reminder

        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_select_location)))

    }
}