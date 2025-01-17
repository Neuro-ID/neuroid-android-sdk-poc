package com.neuroid.tracker

import android.app.Activity
import androidx.annotation.VisibleForTesting
import com.neuroid.tracker.compose.JetpackCompose
import com.neuroid.tracker.models.SessionStartResult

interface NeuroIDPublic {
    /**
     * A variable to access the Jetpack Compose specific tracking functions
     */
    val compose: JetpackCompose

    /**
     * Enable/Disable a Debug Integration Health Report to be generated. (Note: Debug Build Only)
     */
    @Deprecated("setVerifyIntegrationHealth is deprecated")
    fun setVerifyIntegrationHealth(verify:Boolean)

    /**
     * Print Instructions to locate and run Debug Integration Health Report. (Note: Debug Build Only)
     */
    @Deprecated("printIntegrationHealthInstruction is deprecated")
    fun printIntegrationHealthInstruction()

    /**
     * Deprecated, do not use!
     */
    @Deprecated("Replaced with getClientID", ReplaceWith("getClientID()"))
    fun getClientId(): String

    /**
     * Return the currently set client id (use this one).
     */
    fun getClientID(): String

    /**
     * Update the test event sender endpoint to a new collection endpoint. This will generally be
     * set to the mock collection endpoint. The remote config endpoint will be updated to point to
     * the dev remote config endpoint.
     */
    fun setTestURL(newEndpoint: String)

    /**
     * Set a dev testing URL for testing.
     */
    @VisibleForTesting
    fun setTestingNeuroIDDevURL()

    /**
     * Set a user id.
     */
    fun identify(userID: String): Boolean

    /**
     * Deprecated, do not use!
     */
    @Deprecated("Replaced with getUserID", ReplaceWith("getUserID()"))
    fun getUserId(): String

    /**
     * Return the currently set user id.
     */
    fun getUserID(): String

    /**
     * Return the currently set registered user id.
     */
    fun getRegisteredUserID(): String

    /**
     * Set a registered user id.
     */
    fun setRegisteredUserID(registeredUserID: String): Boolean

    /**
     * Set a screen name for the current page in a session. This should be called
     * when displaying a new screen.
     */
    fun setScreenName(screen: String): Boolean

    /**
     * Return the currently set screen name for the current page in a session.
     */
    fun getScreenName(): String

    /**
     * Register page targets. This will send events targets to the server for the current page. This
     * should only be called in ReactNative based apps when rendering each new screen.
     */
    fun registerPageTargets(activity: Activity)

    /**
     * Exclude the event that is specified. Used for testing purposes.
     */
    @VisibleForTesting
    fun excludeViewByTestID(id: String)

    /**
     * Deprecated, do not use!
     */
    @Deprecated("setEnvironment is deprecated and no longer required")
    fun setEnvironment(environment: String)

    /**
     * Deprecated, do not use!
     */
    @Deprecated("setEnvironmentProduction is deprecated and no longer required")
    fun setEnvironmentProduction(prod: Boolean)

    /**
     * Return the current environment set for operation (live/test).
     */
    fun getEnvironment(): String

    /**
     * Deprecated, do not use!
     */
    @Deprecated("setSiteId is deprecated and no longer required")
    fun setSiteId(siteId: String)

    /**
     * Deprecated, do not use
     */
    @Deprecated("Replaced with getSessionID", ReplaceWith("getSessionID()"))
    fun getSessionId(): String

    /**
     * get the currently set session id
     */
    fun getSessionID(): String

    /**
     * Deprecated, do not use!
     */
    @Deprecated("formSubmit is deprecated and no longer required")
    fun formSubmit()

    /**
     * Deprecated, do not use!
     */
    @Deprecated("formSubmitSuccess is deprecated and no longer required")
    fun formSubmitSuccess()

    /**
     * Deprecated, do not use
     */
    @Deprecated("formSubmitFailure is deprecated and no longer required")
    fun formSubmitFailure()

    /**
     * Start the SDK, start a new session and use the userID as the sessionID. Return true if
     * the SDK is started. Returns false if the SDK is stopped. In the case of
     * a false return, check that the client key is set properly in the NeuroID.Builder() call.
     */
    fun start(completion: (Boolean) -> Unit = {})

    /**
     * Stop the SDK and close the current session. Return true to indicate that
     * the SDK is stopped.
     */
    fun stop(): Boolean

    /**
     * Return true if the SDK is stopped and false if SDK is not stopped.
     */
    fun isStopped(): Boolean

    /**
     * Set the ReactNative flag to true.
     */
    fun setIsRN()

    /**
     * Enable/disable logging for the SDK, This is set true on startup. This turns on/off
     * all logging levels (error, debug, verbose, info and warn) that are generated in logcat.
     */
    fun enableLogging(enable: Boolean)

    /**
     * Return the NeuroID SDK version.
     */
    fun getSDKVersion(): String

    /**
     * Stop the SDK and close the current session. Return true to indicate that the SDK is stopped.
     */
    fun stopSession(): Boolean

    /**
     * Restart the current session and continue to collect events that was paused by
     * pauseCollection(). This will not start a new session.
     */
    fun resumeCollection()

    /**
     * Pause the currently running session. This will stop event collection in the current session.
     * The current session will not be closed. The session can be restarted by calling
     * restartSession()
     */
    fun pauseCollection()

    /**
     * Start a new session. This will start the SDK and start a new session using the session ID
     * that is passed in. If a session ID is not passed in, a session ID at random will be used.
     * Return a session result that contains the session ID and a boolean
     * indicating the started state of the SDK.
     */
    fun startSession(
        sessionID: String? = null,
        completion: (SessionStartResult) -> Unit = {},
    )

    /**
     * This should be called when the user attempts to login. Returns true always. Returns false if
     * exception is thrown during the process.
     */
    fun attemptedLogin(attemptedRegisteredUserId: String? = null): Boolean

    /**
     * Start a new app flow session with the specified site id argument for the flow
     * that you wish to start a session for. ALl events after this will be linked to the site
     * id that is specified here until a new flow is specified (startAppFlow() is called
     * with a new site id).
     *
     * If the SDK was not started previously, start will be called
     * here for you with a user ID that is specified in the optional userID argument. If the
     * SDK is already started, the optional user id is not used and the SDK will not be restarted.
     */
    fun startAppFlow(
        siteID: String,
        userID: String? = null,
        completion: (SessionStartResult) -> Unit = {},
    )

    /**
     * This method allows a custom variable and value to be set. e.g. Funnels
     */
    fun setVariable(
        key: String,
        value: String,
    )
}
