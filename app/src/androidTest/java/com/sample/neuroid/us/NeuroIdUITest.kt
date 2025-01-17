package com.sample.neuroid.us

import android.Manifest
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getTestingDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.sample.neuroid.us.activities.MainActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.*
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import kotlin.time.Duration


data class ResponseData(
    val siteId: String,
    val userId: String,
    val clientId: String,
    val identityId: String,
    val registeredUserId: String,
    val pageTag: String,
    val pageId: String,
    val tabId: String,
    val responseId: String,
    val url: String,
    val jsVersion: String,
    val sdkVersion: String,
    val environment: String,
    val jsonEvents: List<NIDEventModel>
)

/**
 * Neuro ID: 26 UI Test
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
@LargeTest
@ExperimentalCoroutinesApi
class NeuroIdUITest: MockServerTest() {

    // take care of the phone and location permissions dialogs.
    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.READ_PHONE_STATE)

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> =
        ActivityScenarioRule(MainActivity::class.java)

    /*
    Helper Test Functions
     */

    fun forceSendEvents(){
        // stop to force send all events in queue
        NeuroID.getInstance()?.stop()
        delay(500)
    }

    /*
    Actual Tests
     */

    /**
     * Validate CREATE_SESSION on start method
     */
    @Test
    fun test01ValidateCreateSession() = runTest(timeout = Duration.parse("120s")) {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.start()
        delay(500)

        forceSendEvents()
        assertRequestBodyContains("CREATE_SESSION")
    }

    /**
     * Validate REGISTER_TARGET on MainActivity class
     */
    @Test
    fun test02ValidateRegisterTargets() = runTest(timeout = Duration.parse("120s"))  {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        delay(1000) //Wait a half second for create the MainActivity View

        forceSendEvents()
        assertRequestBodyContains("REGISTER_TARGET")
    }

    /**
     * Validate SET_USER_ID after sdk is started
     */
    @Test
    fun test03ValidateSetUserId() = runTest(timeout = Duration.parse("120s")) {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.identify("UUID1234")
        delay(500)

        forceSendEvents()
        assertRequestBodyContains("SET_USER_ID")
    }

    /**
     * Validate SET_REGISTERED_USER_ID after sdk is started
     */
    @Test
    fun test03aValidateSetRegisteredUserId() = runTest(timeout = Duration.parse("120s")) {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.setRegisteredUserID("UUID1234")
        delay(500)

        forceSendEvents()
        assertRequestBodyContains("SET_REGISTERED_USER_ID")
    }

    /**
     * Validate WINDOW_LOAD on MainActivity class
     */
    @Test
    fun test04ValidateLifecycleStart() = runTest(timeout = Duration.parse("120s")) {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        delay(500)

        forceSendEvents()
        assertRequestBodyContains("WINDOW_LOAD")
    }

    /**
     * Validate WINDOW_FOCUS on MainActivity class
     */
    @Test
    fun test05ValidateLifecycleResume() = runTest(timeout = Duration.parse("120s")) {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())

        forceSendEvents()
        assertRequestBodyContains("WINDOW_FOCUS")
    }

    /**
     * Validate WINDOW_BLUR on MainActivity class
     */
    @Test
    fun test06ValidateLifecyclePause() = runTest(timeout = Duration.parse("120s")) {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500)
        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        Espresso.pressBack()
        delay(500)

        forceSendEvents()
        assertRequestBodyContains("WINDOW_BLUR")
    }

    /**
     * Validate WINDOW_UNLOAD on MainActivity class
     */
    @Test
    fun test07ValidateLifecycleStop() = runTest(timeout = Duration.parse("120s")) {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500)
        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500)
        Espresso.pressBack()
        delay(1000)

        forceSendEvents()
        assertRequestBodyContains("WINDOW_UNLOAD")
    }

    /**
     * Validate TOUCH_START when the user click on screen
     */
    @Test
    fun test08ValidateTouchStart() {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(200) // When you go to the next test, the activity is destroyed and recreated
        onView(withId(R.id.button_show_activity_fragments))
            .perform(click())
        delay(200)
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        onView(withId(R.id.editText_normal_field))
            .perform(click())
        delay(1000)

        forceSendEvents()
        assertRequestBodyContains("TOUCH_START")
    }

    /**
     * Validate TOUCH_END when the user up finger on screen
     */
    @Test
    fun test09ValidateTouchEnd() = runTest(timeout = Duration.parse("120s")) {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500) // When you go to the next test, the activity is destroyed and recreated
        onView(withId(R.id.button_show_activity_fragments))
            .perform(click())
        delay(500)
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        onView(withId(R.id.editText_normal_field))
            .perform(click())

        forceSendEvents()
        assertRequestBodyContains("TOUCH_END")
    }

    /**
     * Validate WINDOW_FOCUS when the user swipes on screen
     */
    @Test
    fun test11ValidateSwipeScreen() = runTest(timeout = Duration.parse("120s")) {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        // TODO
        // Implement swipe test
    }

    /**
     * Validate WINDOW_RESIZE when the user click on editText
     */
    @Test
    fun test12ValidateWindowsResize() = runTest(timeout = Duration.parse("120s")) {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500) // When you go to the next test, the activity is destroyed and recreated
        onView(withId(R.id.button_show_activity_fragments))
            .perform(click())
        delay(500)
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        onView(withId(R.id.editText_normal_field))
            .perform(click())

        forceSendEvents()
        assertRequestBodyContains("WINDOW_RESIZE")
    }



    /**
     * Validate on TOUCH_START that the input is registered
     */
    @Test
    fun test13ValidateTouchStartAddsRegisterEvent() = runTest(timeout = Duration.parse("120s")) {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500) // When you go to the next test, the activity is destroyed and recreated
        onView(withId(R.id.button_show_activity_fragments))
            .perform(click())
        onView(withId(R.id.editText_normal_field))
            .perform(click())

        forceSendEvents()
        assertRequestBodyContains("REGISTER_TARGET")
    }

    /**
     * Validate SET_USER_ID when sdk is not started
     */
    @Test
    fun test14ValidateSetUserIdPreStart() = runTest(timeout = Duration.parse("120s")) {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        NeuroID.getInstance()?.stop()
        delay(500)
        NeuroID.getInstance()?.identify("UUID123")
        delay(500)
        NeuroID.getInstance()?.start()
        delay(500)

        forceSendEvents()
        assertRequestBodyContains("SET_USER_ID")
    }

    /**
    * Validate SET_REGISTERED_USER_ID when sdk is not started
    */
    @Test
    fun test15ValidateSetRegisteredUserIdPreStart() = runTest(timeout = Duration.parse("120s")) {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.stop()
        delay(500)
        NeuroID.getInstance()?.setRegisteredUserID("UUID1231212")
        delay(500)
        NeuroID.getInstance()?.start()
        delay(500)

        forceSendEvents()
        assertRequestBodyContains("SET_REGISTERED_USER_ID")
    }
}






