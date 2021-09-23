package com.udacity.project4.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@ExperimentalCoroutinesApi
class MainCoroutineRule(val dispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()):
    TestWatcher(), // which extends TestRule interface this make it a JUnit rule
    TestCoroutineScope by TestCoroutineScope(dispatcher) {
    // By using TestCoroutineScope give the ability to control Coroutine timing using TestCoroutineDispatcher dispatcher
    override fun starting(description: Description?) {
        super.starting(description)
        Dispatchers.setMain(dispatcher) // you're swapping the dispatcher
    }

    override fun finished(description: Description?) {
        super.finished(description)
        cleanupTestCoroutines()
        Dispatchers.resetMain() // clean it up
    }
}