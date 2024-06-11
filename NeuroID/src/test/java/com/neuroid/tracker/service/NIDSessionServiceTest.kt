package com.neuroid.tracker.service

import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.CLOSE_SESSION
import com.neuroid.tracker.events.CREATE_SESSION
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.events.MOBILE_METADATA_ANDROID
import com.neuroid.tracker.events.SET_LINKED_SITE
import com.neuroid.tracker.getMockSampleService
import com.neuroid.tracker.getMockedCallActivityListener
import com.neuroid.tracker.getMockedConfigService
import com.neuroid.tracker.getMockedDataStore
import com.neuroid.tracker.getMockedJob
import com.neuroid.tracker.getMockedLocationService
import com.neuroid.tracker.getMockedLogger
import com.neuroid.tracker.getMockedNIDJobServiceManager
import com.neuroid.tracker.getMockedNeuroID
import com.neuroid.tracker.getMockedSharedPreferenceDefaults
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.models.SessionStartResult
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.verifyCaptureEvent
import io.mockk.every
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class NIDSessionServiceTest {
    val testSiteID = "form_abcde123"

    // HELPER FUNCTIONS

    internal data class MockedServices(
        val mockedNeuroID: NeuroID,
        val mockedJobServiceManager: NIDJobServiceManager,
        val mockedDataStore: NIDDataStoreManager,
        val mockedLocationService: LocationService,
        val mockedCallListener: NIDCallActivityListener,
        val mockedConfigService: ConfigService,
        val mockedSampleService: NIDSamplingService,
    )

    private fun setNeuroIDInstance() {
        NeuroID.Builder(
            null,
            "key_test_fake1234",
            false,
            NeuroID.DEVELOPMENT,
        ).build()
    }

    private fun buildMockClasses(): MockedServices {
        val mockedDataStore = getMockedDataStore()
        val mockedJobServiceManager = getMockedNIDJobServiceManager()
        val mockedLocationService = getMockedLocationService()
        val mockedCallListener = getMockedCallActivityListener()

        val mockedNeuroID =
            getMockedNeuroID(
                shouldMockApplication = true,
                mockDataStore = mockedDataStore,
                mockJobServiceManager = mockedJobServiceManager,
                mockLocationService = mockedLocationService,
                mockCallActivityListener = mockedCallListener,
            )

        val mockedConfigService = getMockedConfigService()
        val mockedSampleService =
            getMockSampleService(
                0L,
                10.0,
                configService = mockedConfigService,
            )

        return MockedServices(
            mockedNeuroID,
            mockedJobServiceManager,
            mockedDataStore,
            mockedLocationService,
            mockedCallListener,
            mockedConfigService,
            mockedSampleService,
        )
    }

    private fun createSessionServiceInstance(
        mockedNeuroID: NeuroID,
        configService: ConfigService = getMockedConfigService(),
        samplingService: NIDSamplingService =
            getMockSampleService(
                0L,
                10.0,
            ),
    ): NIDSessionService {
        return NIDSessionService(
            getMockedLogger(),
            mockedNeuroID,
            configService,
            samplingService,
            getMockedSharedPreferenceDefaults(),
        )
    }

    // SETUP/TAKEDOWN
    @Before
    fun setUp() {
        // setup instance and logging
        setNeuroIDInstance()

        NeuroID._isSDKStarted = false
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // TESTS
    @Test
    fun test_captureSessionOrMetaDataEvent() {
        val mockedNeuroID = getMockedNeuroID(shouldMockApplication = true)
        every { mockedNeuroID.metaData?.getLastKnownLocation(any(), any(), any()) } returns Unit

        val sessionService =
            createSessionServiceInstance(mockedNeuroID)

        sessionService.captureSessionOrMetaDataEvent(type = "TEST")

        verifyCaptureEvent(
            mockedNeuroID,
            "TEST",
            1,
        )
    }

    @Test
    fun test_createMobileMetadata() {
        val mockedNeuroID = getMockedNeuroID(shouldMockApplication = true)
        every { mockedNeuroID.metaData?.getLastKnownLocation(any(), any(), any()) } returns Unit

        val sessionService =
            createSessionServiceInstance(mockedNeuroID)

        sessionService.createMobileMetadata()

        verifyCaptureEvent(
            mockedNeuroID,
            MOBILE_METADATA_ANDROID,
            1,
        )
    }

    @Test
    fun test_createSession() {
        val mockedNeuroID = getMockedNeuroID(shouldMockApplication = true)
        every { mockedNeuroID.metaData?.getLastKnownLocation(any(), any(), any()) } returns Unit

        val sessionService =
            createSessionServiceInstance(mockedNeuroID)

        sessionService.createSession()

        verifyCaptureEvent(
            mockedNeuroID,
            CREATE_SESSION,
            1,
        )

        verifyCaptureEvent(
            mockedNeuroID,
            MOBILE_METADATA_ANDROID,
            1,
        )
    }

    @Test
    fun test_setupSession() {
        val mockedServices = buildMockClasses()

        val mockedNeuroID = mockedServices.mockedNeuroID

        val mockedDataStore = mockedServices.mockedDataStore
        val mockedLocationService = mockedServices.mockedLocationService
        val mockedCallListener = mockedServices.mockedCallListener

        val mockedConfigService = mockedServices.mockedConfigService
        val mockedSampleService = mockedServices.mockedSampleService

        val sessionService =
            createSessionServiceInstance(
                mockedNeuroID,
                mockedConfigService,
                mockedSampleService,
            )

        var customFuncRan = false
        var completionFuncRan = false

        sessionService.setupSession(
            siteID = testSiteID,
            customFunctionality = {
                customFuncRan = true
            },
        ) {
            completionFuncRan = true
        }

        assert(customFuncRan)
        assert(completionFuncRan)

        verify(exactly = 1) {
            mockedConfigService.retrieveOrRefreshCache(any())

            mockedSampleService.updateIsSampledStatus(testSiteID)

            mockedCallListener.setCallActivityListener(any())

            mockedLocationService.setupLocationCoroutine(any())
        }

        verifyCaptureEvent(
            mockedNeuroID,
            CREATE_SESSION,
            1,
        )

        verifyCaptureEvent(
            mockedNeuroID,
            MOBILE_METADATA_ANDROID,
            1,
        )

        verify(exactly = 1) {
            mockedDataStore.saveAndClearAllQueuedEvents()

            mockedNeuroID.checkThenCaptureAdvancedDevice()
        }
    }

    // START
    @Test
    fun test_start_failure_noClientKey() {
        val mockedNeuroID =
            getMockedNeuroID()

        every {
            mockedNeuroID.verifyClientKeyExists()
        } returns false

        val sessionService =
            createSessionServiceInstance(
                mockedNeuroID,
            )

        var isStarted: Boolean? = null

        sessionService.start(
            siteID = testSiteID,
        ) {
            isStarted = it
        }

        assert(isStarted == false)
    }

    /**
     * NOTE: This test ONLY tests the result and verifies that the NIDJobServiceManager
     *       method was called. It would be redundant to retest everything being tested in
     *       the `setupSession` method.
     */
    @Test
    fun test_start_success() {
        val mockedServices = buildMockClasses()
        val mockedJobServiceManager = mockedServices.mockedJobServiceManager
        val mockedNeuroID = mockedServices.mockedNeuroID

        every {
            mockedNeuroID.verifyClientKeyExists()
        } returns true

        val sessionService =
            createSessionServiceInstance(
                mockedNeuroID,
            )

        var isStarted: Boolean? = null

        sessionService.start(
            siteID = testSiteID,
        ) {
            isStarted = it
        }

        assert(isStarted == true)

        verify(exactly = 1) {
            mockedJobServiceManager.startJob(any(), any())
        }
    }

    // START SESSION
    @Test
    fun test_startSession_failure_noClientKey() {
        val mockedNeuroID =
            getMockedNeuroID()

        every {
            mockedNeuroID.verifyClientKeyExists()
        } returns false

        val sessionService =
            createSessionServiceInstance(
                mockedNeuroID,
            )

        var isStarted: Boolean? = null

        sessionService.startSession(
            siteID = testSiteID,
        ) {
            isStarted = it.started
        }

        assert(isStarted == false)
    }

    /**
     * Test if a bad sessionID is passed in. Should exit early.
     *
     *  NOTE: This function is a collection of multiple other functions within the SessionService class
     *        however this test ONLY tests the completion status. All other functions called inside
     *        have their own test to verify functionality.
     */
    @Test
    fun test_startSession_failure_badSessionID() {
        val mockedServices = buildMockClasses()
        val mockedJobServiceManager = mockedServices.mockedJobServiceManager
        val mockedNeuroID = mockedServices.mockedNeuroID

        every {
            mockedNeuroID.verifyClientKeyExists()
        } returns true

        every { mockedNeuroID.userID } returns "fakeID"
        every { mockedNeuroID.setUserID(any(), true) } returns false

        val sessionService =
            createSessionServiceInstance(
                mockedNeuroID,
            )

        var isStarted: Boolean? = null
        var newID: String? = null

        sessionService.startSession(
            siteID = testSiteID,
            sessionID = "BAD_ID",
        ) {
            isStarted = it.started
            newID = it.sessionID
        }

        assert(isStarted == false)
        assert(newID == "")

        verify(exactly = 0) {
            mockedJobServiceManager.startJob(any(), any())
        }
    }

    /**
     * Test if there is already a userID/isSDKStarted = true. Should close session and then create
     *  a new one with a random userID.
     *
     *  NOTE: This function is a collection of multiple other functions within the SessionService class
     *        however this test ONLY tests the completion status. All other functions called inside
     *        have their own test to verify functionality.
     */
    @Test
    fun test_startSession_existing_UID_success() {
        val mockedServices = buildMockClasses()
        val mockedJobServiceManager = mockedServices.mockedJobServiceManager
        val mockedNeuroID = mockedServices.mockedNeuroID

        every {
            mockedNeuroID.verifyClientKeyExists()
        } returns true

        every { mockedNeuroID.userID } returns "fakeID"
        every { mockedNeuroID.getUserID() } returns "fakeID2"
        every { mockedNeuroID.setUserID(any(), false) } returns true

        val sessionService =
            createSessionServiceInstance(
                mockedNeuroID,
            )

        var isStarted: Boolean? = null
        var newID: String? = null

        sessionService.startSession(
            siteID = testSiteID,
        ) {
            isStarted = it.started
            newID = it.sessionID
        }

        assert(isStarted == true)
        assert(newID == "fakeID2")

        // assert variables were called to clear (stopSession)
        verify(exactly = 1) {
            mockedNeuroID.userID = ""
        }

        // assert resumeCollection job was called
        verify(exactly = 1) {
            mockedJobServiceManager.startJob(any(), any())
        }
    }

    // PAUSE COLLECTION

    /**
     * Test if there is no pause collection job, then create one that runs to send events
     */
    @Test
    fun test_pauseCollection_noJob() {
        val mockedServices = buildMockClasses()
        val mockedJobServiceManager = mockedServices.mockedJobServiceManager
        val mockedLocationService = mockedServices.mockedLocationService

        val mockedNeuroID = mockedServices.mockedNeuroID

        every { mockedNeuroID.pauseCollectionJob } returns null

        val sessionService =
            createSessionServiceInstance(
                mockedNeuroID,
            )

        sessionService.pauseCollection(true)

        verify(exactly = 1) {
            mockedJobServiceManager.sendEvents(true)

            mockedJobServiceManager.stopJob()
        }

        // assert resumeCollection job was called
        verify(exactly = 1) {
            mockedLocationService.shutdownLocationCoroutine(any())
        }
    }

    /**
     * Test if there is no pause collection job, then create one that runs to send events
     */
    @Test
    fun test_pauseCollection_existingJob() {
        val mockedServices = buildMockClasses()
        val mockedJobServiceManager = mockedServices.mockedJobServiceManager
        val mockedLocationService = mockedServices.mockedLocationService

        val mockedNeuroID = mockedServices.mockedNeuroID

        val mockedJob =
            getMockedJob(
                isCompleted = false,
                isCancelled = false,
            )

        every { mockedNeuroID.pauseCollectionJob } returns mockedJob

        val sessionService =
            createSessionServiceInstance(
                mockedNeuroID,
            )

        sessionService.pauseCollection(true)

        verify(exactly = 0) {
            mockedJobServiceManager.sendEvents(true)
            mockedJobServiceManager.stopJob()
        }

        // assert resumeCollection job was called
        verify(exactly = 1) {
            mockedLocationService.shutdownLocationCoroutine(any())
        }
    }

    // RESUME COLLECTION

    /**
     * Test if the sdk is NOT running, then return immediately
     */
    @Test
    fun test_resumeCollection_notStarted() {
        val mockedServices = buildMockClasses()
        val mockedJobServiceManager = mockedServices.mockedJobServiceManager
        val mockedLocationService = mockedServices.mockedLocationService

        val mockedNeuroID = mockedServices.mockedNeuroID

        every { mockedNeuroID.userID } returns ""
        every { mockedNeuroID.pauseCollectionJob } returns null

        val sessionService =
            createSessionServiceInstance(
                mockedNeuroID,
            )

        sessionService.resumeCollection()

        verify(exactly = 0) {
            mockedJobServiceManager.startJob(any(), any())
        }

        // assert resumeCollection job was called
        verify(exactly = 0) {
            mockedLocationService.setupLocationCoroutine(any())
        }
    }

    /**
     * Test if there is no pauseJob, then resume and setup location
     */
    @Test
    fun test_resumeCollection_existingJob() {
        val mockedServices = buildMockClasses()
        val mockedJobServiceManager = mockedServices.mockedJobServiceManager
        val mockedLocationService = mockedServices.mockedLocationService

        val mockedNeuroID = mockedServices.mockedNeuroID

        NeuroID._isSDKStarted = true
        every { mockedNeuroID.userID } returns "ID"

        every { mockedNeuroID.pauseCollectionJob } returns null

        val sessionService =
            createSessionServiceInstance(
                mockedNeuroID,
            )

        sessionService.resumeCollection()

        verify(exactly = 1) {
            mockedJobServiceManager.startJob(any(), any())
        }

        // assert resumeCollection job was called
        verify(exactly = 1) {
            mockedLocationService.setupLocationCoroutine(any())
        }

        NeuroID._isSDKStarted = false
    }

    /**
     * Test if there is no pauseJob, then resume and setup location
     *
     * NOTE: This tests that the NIDJobServiceManager was NOT called because of how mocking
     *       Coroutine jobs works (specifically the `invokeOnCompletion` piece)
     */
    @Test
    fun test_resumeCollection_noExistingJob() {
        val mockedServices = buildMockClasses()
        val mockedJobServiceManager = mockedServices.mockedJobServiceManager
        val mockedLocationService = mockedServices.mockedLocationService

        val mockedNeuroID = mockedServices.mockedNeuroID

        NeuroID._isSDKStarted = true
        every { mockedNeuroID.userID } returns "ID"

        every { mockedNeuroID.pauseCollectionJob } returns
            getMockedJob(
                isCompleted = false,
            )

        val sessionService =
            createSessionServiceInstance(
                mockedNeuroID,
            )

        sessionService.resumeCollection()

        verify(exactly = 0) {
            mockedJobServiceManager.startJob(any(), any())
        }

        // assert resumeCollection job was called
        verify(exactly = 1) {
            mockedLocationService.setupLocationCoroutine(any())
        }

        NeuroID._isSDKStarted = false
    }

    // RESUME COLLECTION COMPLETION

    /**
     * Test if the NIDJobServiceManager has NOT been setup prior
     */
    @Test
    fun test_resumeCollectionCompletion_notSetup() {
        val mockedServices = buildMockClasses()
        val mockedJobServiceManager = mockedServices.mockedJobServiceManager

        val mockedNeuroID = mockedServices.mockedNeuroID

        every { mockedJobServiceManager.isSetup } returns false

        val sessionService =
            createSessionServiceInstance(
                mockedNeuroID,
            )

        sessionService.resumeCollectionCompletion()

        verify(exactly = 1) {
            mockedJobServiceManager.isSetup
            mockedJobServiceManager.startJob(any(), any())
        }

        assert(NeuroID._isSDKStarted)

        NeuroID._isSDKStarted = false
    }

    /**
     * Test if the NIDJobServiceManager HAS been setup prior
     */
    @Test
    fun test_resumeCollectionCompletion_isSetup() {
        val mockedServices = buildMockClasses()
        val mockedJobServiceManager = mockedServices.mockedJobServiceManager

        val mockedNeuroID = mockedServices.mockedNeuroID

        every { mockedJobServiceManager.isSetup } returns true

        val sessionService =
            createSessionServiceInstance(
                mockedNeuroID,
            )

        sessionService.resumeCollectionCompletion()

        verify(exactly = 1) {
            mockedJobServiceManager.isSetup
            mockedJobServiceManager.restart()
        }

        assert(NeuroID._isSDKStarted)

        NeuroID._isSDKStarted = false
    }

    // STOP SESSION

    /**
     * Test stopping a session
     *
     * NOTE: This function calls other functions within the SessionService. These functions are for
     *       the most part mocked and not tested here as they have their own tests
     */
    @Test
    fun test_stopSession() {
        val mockedServices = buildMockClasses()
        val mockedLocationService = mockedServices.mockedLocationService
        val mockedCallActivityListener = mockedServices.mockedCallListener

        val mockedNeuroID = mockedServices.mockedNeuroID

        val sessionService =
            createSessionServiceInstance(
                mockedNeuroID,
            )

        val stopped = sessionService.stopSession()
        assert(stopped)

        verifyCaptureEvent(
            mockedNeuroID,
            CLOSE_SESSION,
            1,
        )

        verify(exactly = 1) {
            mockedNeuroID.pauseCollectionJob = any()
        }

        // make sure clearSessionVars was called
        verify(exactly = 1) {
            mockedNeuroID.userID = ""
        }

        verify(exactly = 1) {
            mockedCallActivityListener.unregisterCallActivityListener(any())

            // happens in the pauseCollection
            mockedLocationService.shutdownLocationCoroutine(any())
        }
    }

    // STOP

    /**
     * Test stopping
     *
     * NOTE: This function calls other functions within the SessionService. These functions are for
     *       the most part mocked and not tested here as they have their own tests
     */
    @Test
    fun test_stop() {
        val mockedServices = buildMockClasses()
        val mockedLocationService = mockedServices.mockedLocationService
        val mockedCallActivityListener = mockedServices.mockedCallListener

        val mockedNeuroID = mockedServices.mockedNeuroID

        val sessionService =
            createSessionServiceInstance(
                mockedNeuroID,
            )

        val stopped = sessionService.stop()
        assert(stopped)

        // TO-DO - Why are we not sending this event in stop?
        verifyCaptureEvent(
            mockedNeuroID,
            CLOSE_SESSION,
            0,
        )

        verify(exactly = 1) {
            mockedNeuroID.pauseCollectionJob = any()
        }

        // make sure clearSessionVars was NOT called
        verify(exactly = 0) {
            mockedNeuroID.userID = ""
        }

        verify(exactly = 1) {
            mockedNeuroID.linkedSiteID = ""

            mockedCallActivityListener.unregisterCallActivityListener(any())

            // happens in the pauseCollection
            mockedLocationService.shutdownLocationCoroutine(any())
        }
    }

    // CLOSE SESSION

    /**
     * Test closing a session and capturing a CLOSE_SESSION event
     *
     * NOTE: This function calls `stop` which has its own test so this test verifies the event was stored
     */
    @Test
    fun test_closeSession() {
        val mockedNeuroID =
            getMockedNeuroID(
                shouldMockApplication = true,
            )

        every { mockedNeuroID.isStopped() } returns false

        val sessionService =
            createSessionServiceInstance(
                mockedNeuroID,
            )

        sessionService.closeSession()

        verifyCaptureEvent(
            mockedNeuroID,
            CLOSE_SESSION,
            1,
        )
    }

    // CLEAR SESSION VARIABLES
    @Test
    fun test_clearSessionVariables() {
        val mockedNeuroID =
            getMockedNeuroID(
                shouldMockApplication = true,
            )

        val sessionService =
            createSessionServiceInstance(
                mockedNeuroID,
            )

        sessionService.clearSessionVariables()

        verify {
            mockedNeuroID.userID = ""
            mockedNeuroID.registeredUserID = ""
            mockedNeuroID.linkedSiteID = ""
        }
    }

    // START APP FLOW

    /**
     * Test start app flow failing because no client key
     */
    @Test
    fun test_startAppFlow_failure_noClientKey() {
        val mockedNeuroID =
            getMockedNeuroID(
                shouldMockApplication = true,
            )

        every { mockedNeuroID.verifyClientKeyExists() } returns false

        val sessionService =
            createSessionServiceInstance(
                mockedNeuroID,
            )

        var completionFuncSuccess: Boolean? = null
        var completionFuncID: String? = null
        sessionService.startAppFlow(
            testSiteID,
        ) {
            completionFuncSuccess = it.started
            completionFuncID = it.sessionID
        }

        assert(completionFuncSuccess == false)
        assert(completionFuncID == "")

        verify {
            mockedNeuroID.linkedSiteID = ""
        }

        verifyCaptureEvent(
            mockedNeuroID,
            eventType = LOG,
        )
    }

    /**
     * Test start app flow failing because invalid siteID
     */
    @Test
    fun test_startAppFlow_failure_invalidSiteID() {
        val mockedNeuroID =
            getMockedNeuroID(
                shouldMockApplication = true,
            )

        every { mockedNeuroID.verifyClientKeyExists() } returns true
        every { mockedNeuroID.validateSiteID(testSiteID) } returns false

        val sessionService =
            createSessionServiceInstance(
                mockedNeuroID,
            )

        var completionFuncSuccess: Boolean? = null
        var completionFuncID: String? = null
        sessionService.startAppFlow(
            testSiteID,
        ) {
            completionFuncSuccess = it.started
            completionFuncID = it.sessionID
        }

        assert(completionFuncSuccess == false)
        assert(completionFuncID == "")

        verify {
            mockedNeuroID.linkedSiteID = ""
        }

        verifyCaptureEvent(
            mockedNeuroID,
            eventType = LOG,
        )
    }

    /**
     * Test start app flow when the sdk is already started
     *
     * NOTE: This method relies on `clearSendOldFlowEvents` which is tested in its own test.
     *       Immediate calling of the callback is assumed for this test
     */
    @Test
    fun test_startAppFlow_alreadyStarted() {
        val mockedServices = buildMockClasses()
        val mockedSampleService = mockedServices.mockedSampleService

        val mockedNeuroID = mockedServices.mockedNeuroID

        every {
            mockedSampleService.isSessionFlowSampled()
        } returns false

        every { mockedNeuroID.verifyClientKeyExists() } returns true
        every { mockedNeuroID.validateSiteID(testSiteID) } returns true
        every { mockedNeuroID.getUserID() } returns "GoodUID"
        NeuroID._isSDKStarted = true

        val sessionService =
            createSessionServiceInstance(
                mockedNeuroID,
                samplingService = mockedSampleService,
            )

        var completionFuncResult: SessionStartResult? = null
        sessionService.startAppFlow(
            testSiteID,
        ) {
            completionFuncResult = it
        }

        assert(completionFuncResult?.started == true)
        assert(completionFuncResult?.sessionID == "GoodUID")

        verify(exactly = 1) {
            mockedSampleService.updateIsSampledStatus(testSiteID)

            mockedNeuroID.checkThenCaptureAdvancedDevice()
            mockedNeuroID.addLinkedSiteID(testSiteID)
        }

        verifyCaptureEvent(
            mockedNeuroID,
            eventType = CREATE_SESSION,
        )
    }

    /**
     * Test start app flow when the sdk is NOT already started and NO sessionID passed
     *
     * NOTE: This method relies on `clearSendOldFlowEvents` & `start` which are tested in their own test.
     *       Immediate calling of the callback is assumed for this test
     */
    @Test
    fun test_startAppFlow_start_noUID() {
        val mockedServices = buildMockClasses()
        val mockedSampleService = mockedServices.mockedSampleService
        val mockedJobServiceManager = mockedServices.mockedJobServiceManager

        val mockedNeuroID = mockedServices.mockedNeuroID

        every {
            mockedSampleService.isSessionFlowSampled()
        } returns false

        every { mockedNeuroID.verifyClientKeyExists() } returns true
        every { mockedNeuroID.validateSiteID(testSiteID) } returns true
        every { mockedNeuroID.getUserID() } returns "GoodUID"
        NeuroID._isSDKStarted = false

        val sessionService =
            createSessionServiceInstance(
                mockedNeuroID,
                samplingService = mockedSampleService,
            )

        var completionFuncResult: SessionStartResult? = null
        sessionService.startAppFlow(
            testSiteID,
        ) {
            completionFuncResult = it
        }

        assert(completionFuncResult?.started == true)
        assert(completionFuncResult?.sessionID == "GoodUID")

        verify(exactly = 1) {
            mockedSampleService.updateIsSampledStatus(testSiteID)

            mockedNeuroID.checkThenCaptureAdvancedDevice()
            mockedNeuroID.addLinkedSiteID(testSiteID)

            mockedJobServiceManager.startJob(any(), any())
        }

        verifyCaptureEvent(
            mockedNeuroID,
            eventType = CREATE_SESSION,
        )
    }

    /**
     * Test start app flow when the sdk is NOT already started AND a sessionID is passed
     *
     * NOTE: This method relies on `clearSendOldFlowEvents` & `startSession` which are tested in their own test.
     *       Immediate calling of the callback is assumed for this test
     */
    @Test
    fun test_startAppFlow_start_withUID() {
        val userID = "myUserID"

        val mockedServices = buildMockClasses()
        val mockedSampleService = mockedServices.mockedSampleService

        val mockedNeuroID = mockedServices.mockedNeuroID

        every {
            mockedSampleService.isSessionFlowSampled()
        } returns false

        every { mockedNeuroID.verifyClientKeyExists() } returns true
        every { mockedNeuroID.validateSiteID(testSiteID) } returns true
        every { mockedNeuroID.getUserID() } returns userID
        every { mockedNeuroID.setUserID(userID, true) } returns true

        NeuroID._isSDKStarted = false

        val sessionService =
            createSessionServiceInstance(
                mockedNeuroID,
                samplingService = mockedSampleService,
            )

        var completionFuncResult: SessionStartResult? = null
        sessionService.startAppFlow(
            testSiteID,
            userID = userID,
        ) {
            completionFuncResult = it
        }

        assert(completionFuncResult?.started == true)
        assert(completionFuncResult?.sessionID == userID)

        verify(exactly = 1) {
            mockedNeuroID.addLinkedSiteID(testSiteID)

            mockedNeuroID.setUserID(userID, any())

            mockedSampleService.updateIsSampledStatus(testSiteID)

            mockedNeuroID.checkThenCaptureAdvancedDevice()
        }

        verifyCaptureEvent(
            mockedNeuroID,
            eventType = CREATE_SESSION,
        )
    }

    // CLEAR SEND OLD FLOW EVENTS

    @Test
    fun test_clearSendOldFlowEvents_sampled() {
        val mockedServices = buildMockClasses()
        val mockedNIDJobServiceManager = mockedServices.mockedJobServiceManager
        val mockedSampleService = mockedServices.mockedSampleService

        val mockedNeuroID = mockedServices.mockedNeuroID

        every {
            mockedSampleService.isSessionFlowSampled()
        } returns true

        val sessionService =
            createSessionServiceInstance(
                mockedNeuroID,
                samplingService = mockedSampleService,
            )

        var completionFuncResult: Boolean? = null
        sessionService.clearSendOldFlowEvents {
            completionFuncResult = true
        }

        assert(completionFuncResult == true)

        verify(exactly = 1) {
            mockedSampleService.isSessionFlowSampled()
            mockedNIDJobServiceManager.sendEvents(true)
        }
    }

    @Test
    fun test_clearSendOldFlowEvents_notSampled() {
        val mockedServices = buildMockClasses()
        val mockedNIDJobServiceManager = mockedServices.mockedJobServiceManager
        val mockedSampleService = mockedServices.mockedSampleService
        val mockedDataStore = mockedServices.mockedDataStore

        val mockedNeuroID = mockedServices.mockedNeuroID

        every {
            mockedSampleService.isSessionFlowSampled()
        } returns false

        val sessionService =
            createSessionServiceInstance(
                mockedNeuroID,
                samplingService = mockedSampleService,
            )

        var completionFuncResult: Boolean? = null
        sessionService.clearSendOldFlowEvents {
            completionFuncResult = true
        }

        assert(completionFuncResult == true)

        verify(exactly = 1) {
            mockedSampleService.isSessionFlowSampled()
            mockedDataStore.clearEvents()
        }

        verify(exactly = 0) {
            mockedNIDJobServiceManager.sendEvents(true)
        }
    }

    private fun validateStartAppFlowTest(
        flowResult: SessionStartResult,
        siteID: String,
        userID: String?,
        startedExpectation: Boolean = true,
        throttleTest: Boolean = false,
    ) {
        Assert.assertEquals(startedExpectation, flowResult.started)
        Assert.assertEquals(userID, flowResult.sessionID)
        Assert.assertEquals(!startedExpectation, NeuroID.getInstance()?.isStopped())

        if (startedExpectation || throttleTest) {
            if (throttleTest) {
                verify {
                    NeuroID.getInternalInstance()?.nidJobServiceManager?.sendEvents(
                        true,
                    )
                }
                NeuroID.getInternalInstance()?.dataStore?.saveEvent(
                    NIDEventModel(
                        type = CLOSE_SESSION,
                        v = siteID,
                    ),
                )
            } else {
                verify {
                    NeuroID.getInternalInstance()?.nidJobServiceManager?.sendEvents(
                        true,
                    )
                }
                verify {
                    NeuroID.getInternalInstance()?.dataStore?.saveEvent(
                        NIDEventModel(
                            type = SET_LINKED_SITE,
                            v = siteID,
                        ),
                    )
                }
            }
        } else {
            verify(exactly = 0) {
                NeuroID.getInternalInstance()?.nidJobServiceManager?.sendEvents(
                    true,
                )
            }
            verify(exactly = 0) {
                NeuroID.getInternalInstance()?.dataStore?.saveEvent(
                    NIDEventModel(
                        type = SET_LINKED_SITE,
                        v = siteID,
                    ),
                )
            }
        }

        assert(
            if (startedExpectation) {
                siteID
            } else {
                ""
            } == "",
//                NeuroID.linkedSiteID,
        ) { "NeuroID.linkedSiteID value mismatch" }
        assert(startedExpectation == NeuroID.isSDKStarted) { "NeuroID.isSDKStarted value mismatch" }
    }
//
//    fun runThrottleTestStartAppFlow(
//        siteID: String,
//        userID: String,
//        isStarted: Boolean,
//        startingLinkedId: String,
//        clockTimeInMS: Long,
//        randomNumber: Double,
//        expectedIsStarted: Boolean,
//        expectedlinkedSiteID: String,
//        expectedSessionID: String,
//        expectedIsSessionFlowSampled: Boolean,
//    ) {
//        runTest {
//            NeuroID._isSDKStarted = isStarted
//            setupStartAppFlowTest(startingLinkedId)
//            setMockedEmptyLogger()
//            mockSampleServiceSupport(clockTimeInMS, randomNumber)
//            NeuroID.getInternalInstance()?.userID = userID
//            NeuroID.getInternalInstance()?.let {
//                it.clientKey = "dummy_key"
//                val flowResult = it.startAppFlow(siteID = siteID, userID = userID)
//                println(
//                    "$flowResult, isSampled: ${NeuroID.getInternalInstance()?.samplingService?.isSessionFlowSampled()}, linkedSiteID: ${NeuroID.linkedSiteID}",
//                )
//                assert(NeuroID.getInternalInstance()?.samplingService?.isSessionFlowSampled() == expectedIsSessionFlowSampled)
//                assert(flowResult.started == expectedIsStarted)
//                assert(NeuroID.linkedSiteID == expectedlinkedSiteID)
//                assert(flowResult.sessionID == expectedSessionID)
//            }
//            unmockkStatic(Calendar::class)
//        }
//    }
//
//    @Test
//    fun testStartAppFlow_no_userID_session_started() =
//        runTest {
//            val siteID = "form_zzzzz123"
//            setupStartAppFlowTest()
//
//            setMockedEmptyLogger()
//
//            NeuroID._isSDKStarted = false
//            NeuroID.getInternalInstance()?.let {
//                it.clientKey = "dummyKey"
//
//                val flowResult = it.startAppFlow(siteID)
//                validateStartAppFlowTest(
//                    flowResult,
//                    siteID,
//                    "",
//                    true,
//                )
//            }
//            unmockkStatic(Calendar::class)
//        }
//
//    @Test
//    fun testStartAppFlow_userID_session_started() =
//        runTest {
//            unmockkAll()
//            val siteID = "form_zzzzz123"
//            val userID = "test_1234"
//
//            setupStartAppFlowTest()
//
//            setMockedEmptyLogger()
//
//            NeuroID._isSDKStarted = false
//
//            NeuroID.getInternalInstance()?.let {
//                it.clientKey = "dummyKey"
//                val flowResult = it.startAppFlow(siteID, userID)
//
//                validateStartAppFlowTest(
//                    flowResult,
//                    siteID,
//                    userID,
//                    true,
//                )
//            }
//            unmockkStatic(Calendar::class)
//        }
//
//    @Test
//    fun testStartAppFlow_userID_session_already_started_no_userID_set() =
//        runTest {
//            val siteID = "form_zzzzz123"
//            NeuroID._isSDKStarted = true
//
//            setupStartAppFlowTest("oldSite")
//
//            setMockedEmptyLogger()
//
//            NeuroID.getInternalInstance()?.let {
//                it.clientKey = "dummyKey"
//                val flowResult = it.startAppFlow(siteID)
//
//                validateStartAppFlowTest(
//                    flowResult,
//                    siteID,
//                    "",
//                    true,
//                )
//            }
//            unmockkStatic(Calendar::class)
//        }
//
//    @Test
//    fun testStartAppFlow_bad_form_id() =
//        runTest {
//            val siteID = "flow_zzzzz123"
//
//            NeuroID._isSDKStarted = false
//
//            setupStartAppFlowTest("")
//
//            setMockedEmptyLogger()
//
//            NeuroID.getInternalInstance()?.let {
//                it.clientKey = "dummyKey"
//                val flowResult = it.startAppFlow(siteID)
//
//                validateStartAppFlowTest(
//                    flowResult,
//                    siteID,
//                    "",
//                    false,
//                )
//            }
//            unmockkStatic(Calendar::class)
//        }
//
//    @Test
//    fun throttleTestHasLinkedSiteID_throttle() {
//        runThrottleTestStartAppFlow(
//            siteID = "form_testa123",
//            userID = "gsdgsda",
//            isStarted = false,
//            startingLinkedId = "",
//            clockTimeInMS = 0,
//            randomNumber = 10.0,
//            expectedIsStarted = true,
//            expectedlinkedSiteID = "form_testa123",
//            expectedSessionID = "gsdgsda",
//            expectedIsSessionFlowSampled = false,
//        )
//    }
//
//    @Test
//    fun throttleTestHasLinkedSiteID_no_throttle() {
//        runThrottleTestStartAppFlow(
//            siteID = "form_testa124",
//            userID = "gsdgsda",
//            isStarted = false,
//            startingLinkedId = "",
//            clockTimeInMS = 0,
//            randomNumber = 10.0,
//            expectedIsStarted = true,
//            expectedlinkedSiteID = "form_testa124",
//            expectedSessionID = "gsdgsda",
//            expectedIsSessionFlowSampled = true,
//        )
//    }
//
//    @Test
//    fun throttleTestHasLinkedSiteID_parent_no_throttle() {
//        runThrottleTestStartAppFlow(
//            siteID = "form_zappa345",
//            userID = "gsdgsda",
//            isStarted = true,
//            startingLinkedId = "form_testa124",
//            clockTimeInMS = 0,
//            randomNumber = 10.0,
//            expectedIsStarted = true,
//            expectedlinkedSiteID = "form_zappa345",
//            expectedSessionID = "gsdgsda",
//            expectedIsSessionFlowSampled = true,
//        )
//    }
//
//    @Test
//    fun throttleTestHasLinkedSiteID_parent_throttle() {
//        runThrottleTestStartAppFlow(
//            siteID = "form_zappa345",
//            userID = "gsdgsda",
//            isStarted = true,
//            startingLinkedId = "form_testa124",
//            clockTimeInMS = 0,
//            randomNumber = 90.0,
//            expectedIsStarted = true,
//            expectedlinkedSiteID = "form_zappa345",
//            expectedSessionID = "gsdgsda",
//            expectedIsSessionFlowSampled = false,
//        )
//    }
//
//    @Test
//    fun throttleTestHasLinkedSiteID_empty_site_id_exit_immediate() {
//        runThrottleTestStartAppFlow(
//            siteID = "",
//            userID = "gsdgsda",
//            isStarted = true,
//            startingLinkedId = "form_testa124",
//            clockTimeInMS = 0,
//            randomNumber = 90.0,
//            expectedIsStarted = true,
//            expectedlinkedSiteID = "",
//            expectedSessionID = "",
//            expectedIsSessionFlowSampled = true,
//        )
//    }
}
