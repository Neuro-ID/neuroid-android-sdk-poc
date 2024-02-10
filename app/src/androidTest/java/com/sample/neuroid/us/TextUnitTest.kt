package com.sample.neuroid.us

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.sample.neuroid.us.activities.MainActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
@LargeTest
@ExperimentalCoroutinesApi
class TextUnitTest {

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> =
        ActivityScenarioRule(MainActivity::class.java)

    /**
     * The sending of events to the server is stopped so that they are not eliminated from
     * the SharedPreferences and can be obtained one by one
     */
    @ExperimentalCoroutinesApi
    @Before
    fun stopSendEventsToServer() = runTest {
        NIDJobServiceManager.isSendEventsNowEnabled = false
        NeuroID.getInstance()?.isStopped()?.let {
            if (it) {
                NeuroID.getInstance()?.start()
            }
        }
        delay(500)
    }

    @After
    fun resetDispatchers() = runTest {
        getDataStoreInstance().clearEvents()
        NeuroID.getInstance()?.stop()
        delay(500)
    }

    /**
     * Validate FOCUS when the user click on editText
     */
    @Test
    fun test01ValidateFocusOnEditText() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        getDataStoreInstance().clearEvents()
        Espresso.onView(ViewMatchers.withId(R.id.button_show_activity_fragments))
            .perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.editText_normal_field))
            .perform(ViewActions.click())

        val eventType = "\"type\":\"FOCUS\""
        val events = getDataStoreInstance().getAllEvents()
        NIDSchema().validateEvents(events, eventType, -1)
        NIDSchema().validateSchema(events)
    }

    /**
     * Validate BLUR when the user change the focus
     */
    @Test
    fun test02ValidateBlurOnEditText() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Espresso.onView(ViewMatchers.withId(R.id.button_show_activity_fragments))
            .perform(ViewActions.click())
        getDataStoreInstance().clearEvents()
        Espresso.onView(ViewMatchers.withId(R.id.editText_normal_field))
            .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.editText_password_field))
            .perform(ViewActions.click())

        val eventType = "\"type\":\"BLUR\""
        val events = getDataStoreInstance().getAllEvents()
        NIDSchema().validateSchema(events)
        NIDSchema().validateEvents(events, eventType)
    }

    /**
     * Validate INPUT when the user type on editText
     */
    @Test
    fun test03ValidateInputText() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        getDataStoreInstance().clearEvents()
        Espresso.onView(ViewMatchers.withId(R.id.button_show_activity_fragments))
            .perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.editText_normal_field))
            .perform(ViewActions.click())
        val text = "Some text"
        Espresso.onView(ViewMatchers.withId(R.id.editText_normal_field))
            .perform(ViewActions.typeText(text))

        val eventType = "\"type\":\"INPUT\""
        val events = getDataStoreInstance().getAllEvents()
        NIDSchema().validateSchema(events)
        NIDSchema().validateEvents(events, eventType, text.length)
    }
}