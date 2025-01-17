package com.neuroid.tracker.service

import android.content.Context
import android.location.LocationManager
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.CLOSE_SESSION
import com.neuroid.tracker.events.CREATE_SESSION
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.events.MOBILE_METADATA_ANDROID
import com.neuroid.tracker.events.PAUSE_EVENT_CAPTURE
import com.neuroid.tracker.events.RESUME_EVENT_CAPTURE
import com.neuroid.tracker.models.SessionStartResult
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.NIDSingletonIDs
import com.neuroid.tracker.utils.generateUniqueHexID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

internal class NIDSessionService(
    val logger: NIDLogWrapper,
    val neuroID: NeuroID,
    private val configService: ConfigService,
    private val samplingService: NIDSamplingService,
    private val sharedPreferenceDefaults: NIDSharedPrefsDefaults,
    private val identifierService: NIDIdentifierService,
    private val validationService: NIDValidationService,
) {
    internal fun createSession() {
        neuroID.timestamp = System.currentTimeMillis()

        neuroID.application?.let {
            neuroID.sessionID = sharedPreferenceDefaults.getNewSessionID()
            neuroID.clientID = sharedPreferenceDefaults.getClientID()

            captureSessionOrMetaDataEvent(
                type = CREATE_SESSION,
            )

            createMobileMetadata()
        }
    }

    // Init listeners, begin config retrieval call, start session events
    internal fun setupSession(
        // Leaving siteID for now for when we re-visit sampling update from config (ENG-8324)
        siteID: String?,
        customFunctionality: () -> Unit,
        completion: () -> Unit,
    ) {
        configService.retrieveOrRefreshCache()

        samplingService.updateIsSampledStatus(siteID)
        logger.i(msg = "NID isSessionFlowSampled ${samplingService.isSessionFlowSampled()} for $siteID")

        neuroID.setupListeners()

        customFunctionality()

        NeuroID._isSDKStarted = true

        CoroutineScope(neuroID.dispatcher).launch {
            createSession()
        }

        NIDSingletonIDs.retrieveOrCreateLocalSalt()

        neuroID.dataStore.saveAndClearAllQueuedEvents()
        neuroID.checkThenCaptureAdvancedDevice()

        completion()
    }

    // internal start() with siteID
    fun start(
        siteID: String?,
        completion: (Boolean) -> Unit = {},
    ) {
        neuroID.captureEvent(
            type = LOG,
            level = "info",
            m = "start attempt - siteID:$siteID",
        )

        if (NeuroID.isSDKStarted || !validationService.verifyClientKeyExists(neuroID.clientKey)) {
            completion(false)
            return
        }

        setupSession(
            siteID,
            {
                // custom functionality
                neuroID.application?.let {
                    neuroID.nidJobServiceManager?.startJob(
                        it,
                        neuroID.clientKey,
                    )
                }
            },
        ) {
            completion(true)
        }
    }

    // internal startSession() with siteID
    fun startSession(
        siteID: String?,
        sessionID: String? = null,
        completion: (SessionStartResult) -> Unit = { },
    ) {
        neuroID.captureEvent(
            type = LOG,
            level = "info",
            m = "startSession attempt - siteID:$siteID - userID:${if (sessionID != null) {
                validationService.scrubIdentifier(sessionID)
            } else {
                "null"
            }}",
        )

        if (!validationService.verifyClientKeyExists(neuroID.clientKey)) {
            completion(SessionStartResult(false, ""))
            return
        }

        if (neuroID.userID != "" || NeuroID.isSDKStarted) {
            stopSession()
        }

        var finalSessionID = sessionID ?: generateUniqueHexID(true)
        if (!identifierService.setSessionID(finalSessionID, sessionID != null)) {
            completion(SessionStartResult(false, ""))
            return
        }

        setupSession(
            siteID,
            {
                resumeCollection()
            },
        ) {
            // we need to set finalSessionID with the set random user id
            // if a sessionID was not passed in
            finalSessionID = neuroID.getUserID()

            completion(SessionStartResult(true, finalSessionID))
        }
    }

    @Synchronized
    internal fun pauseCollection(flushEvents: Boolean) {
        neuroID.captureEvent(type = PAUSE_EVENT_CAPTURE, ct = "SDK_EVENT")
        NeuroID._isSDKStarted = false
        if (neuroID.pauseCollectionJob == null ||
            neuroID.pauseCollectionJob?.isCancelled == true ||
            neuroID.pauseCollectionJob?.isCompleted == true
        ) {
            neuroID.pauseCollectionJob =
                CoroutineScope(neuroID.dispatcher).launch {
                    neuroID.nidJobServiceManager?.sendEvents(flushEvents)
                    neuroID.nidJobServiceManager?.stopJob()
                }
        }

        neuroID.locationService.shutdownLocationCoroutine(
            neuroID.getApplicationContext()?.getSystemService(Context.LOCATION_SERVICE) as LocationManager,
        )
    }

    @Synchronized
    fun resumeCollection() {
        neuroID.captureEvent(queuedEvent = true, type = RESUME_EVENT_CAPTURE, ct = "SDK_EVENT")
        // Don't allow resume to be called if SDK has not been started
        if (neuroID.userID.isEmpty() && !NeuroID.isSDKStarted) {
            return
        }
        if (neuroID.pauseCollectionJob?.isCompleted == true ||
            neuroID.pauseCollectionJob?.isCancelled == true ||
            neuroID.pauseCollectionJob == null
        ) {
            resumeCollectionCompletion()
        } else {
            neuroID.pauseCollectionJob?.invokeOnCompletion { resumeCollectionCompletion() }
        }

        if (configService.configCache.geoLocation) {
            neuroID.locationService.setupLocationCoroutine(
                neuroID.getApplicationContext()?.getSystemService(Context.LOCATION_SERVICE) as LocationManager,
            )
        }
    }

    internal fun resumeCollectionCompletion() {
        NeuroID._isSDKStarted = true
        neuroID.application?.let {
            if (neuroID.nidJobServiceManager?.isSetup == false) {
                neuroID.nidJobServiceManager?.startJob(it, neuroID.clientKey)
            } else {
                neuroID.nidJobServiceManager?.restart()
            }
        }
    }

    fun stopSession(): Boolean {
        neuroID.captureEvent(
            type = LOG,
            level = "info",
            m = "stopSession attempt",
        )

        neuroID.captureEvent(type = CLOSE_SESSION, ct = "SDK_EVENT")

        pauseCollection(true)
        clearSessionVariables()

        neuroID.nidCallActivityListener.unregisterCallActivityListener(
            neuroID.getApplicationContext(),
        )

        return true
    }

    fun stop(): Boolean {
        neuroID.captureEvent(
            type = LOG,
            level = "info",
            m = "stop attempt",
        )

        pauseCollection(true)

        neuroID.linkedSiteID = ""

        neuroID.nidCallActivityListener.unregisterCallActivityListener(
            neuroID.getApplicationContext(),
        )

        return true
    }

    // This might be able to be deprecated/removed shortly. Not used anywhere
    fun closeSession() {
        if (!neuroID.isStopped()) {
            neuroID.captureEvent(type = CLOSE_SESSION, ct = "SDK_EVENT")
            stop()
        }
    }

    fun clearSessionVariables() {
        neuroID.userID = ""
        neuroID.registeredUserID = ""
        neuroID.linkedSiteID = ""

        neuroID.captureEvent(
            type = LOG,
            level = "info",
            m = "session variables cleared",
        )
    }

    fun startAppFlow(
        siteID: String,
        sessionID: String? = null,
        completion: (SessionStartResult) -> Unit = {},
    ) {
        neuroID.captureEvent(
            type = LOG,
            level = "info",
            m = "startAppFlow attempt - siteID:$siteID - userID:${if (sessionID != null) {
                validationService.scrubIdentifier(sessionID)
            } else {
                "null"
            }}",
        )

        if (!validationService.verifyClientKeyExists(neuroID.clientKey) || !validationService.validateSiteID(siteID)) {
            // reset linked site id (in case of failure)
            neuroID.linkedSiteID = ""
            neuroID.captureEvent(
                type = LOG,
                m = "Failed to set invalid Linked Site $siteID",
                level = "ERROR",
            )

            completion(SessionStartResult(false, ""))
            return
        }

        // Clear or Send events based on sample rate
        clearSendOldFlowEvents {
            // The following events have to happen for either
            //  an existing session that begins a new flow OR
            //  a new session with a new flow
            // 1. Determine if flow should be sampled
            // 2. CREATE_SESSION and MOBILE_METADATA events captured
            // 3. Capture ADV (based on global config and lib installed)

            neuroID.addLinkedSiteID(siteID)

            // If SDK is already started, update sampleStatus and continue
            if (NeuroID.isSDKStarted) {
                // capture CREATE_SESSION and METADATA events for new flow
                neuroID.captureEvent(
                    type = LOG,
                    level = "info",
                    m = "startAppFlow - SDK already started",
                )

                createSession()

                neuroID.checkThenCaptureAdvancedDevice()

                completion(
                    SessionStartResult(
                        true,
                        neuroID.getUserID(),
                    ),
                )
            } else {
                // If the SDK is not started we have to start it first
                //  (which will get the config using passed siteID)

                // if userID passed then startSession should be used
                if (!sessionID.isNullOrEmpty()) {
                    startSession(
                        siteID,
                        sessionID,
                    ) {
                        completion(it)
                    }
                } else {
                    start(siteID) {
                        completion(
                            SessionStartResult(
                                it,
                                neuroID.getUserID(),
                            ),
                        )
                    }
                }
            }
        }
    }

    internal fun createMobileMetadata() {
        captureSessionOrMetaDataEvent(
            type = MOBILE_METADATA_ANDROID,
            attrs = listOf(mapOf("isRN" to neuroID.isRN)),
        )

        // capture application metadata on every capture mobile metadata to ensure we see it
        //  otherwise it could be dropped on the first packet
        neuroID.captureApplicationMetaData()
    }

    internal fun captureSessionOrMetaDataEvent(
        type: String,
        attrs: List<Map<String, Any>>? = null,
    ) {
        neuroID.application?.let {
            neuroID.metaData?.getLastKnownLocation(
                it,
                configService.configCache.geoLocation,
                neuroID.locationService,
            )

            neuroID.captureEvent(
                type = type,
                f = neuroID.clientKey,
                sid = neuroID.sessionID,
                lsid = "null",
                cid = neuroID.clientID,
                did = sharedPreferenceDefaults.getDeviceID(),
                iid = sharedPreferenceDefaults.getIntermediateID(),
                loc = sharedPreferenceDefaults.getLocale(),
                ua = sharedPreferenceDefaults.getUserAgent(),
                tzo = sharedPreferenceDefaults.getTimeZone(),
                lng = sharedPreferenceDefaults.getLanguage(),
                ce = true,
                je = true,
                ol = true,
                p = sharedPreferenceDefaults.getPlatform(),
                jsl = listOf(),
                dnt = false,
                url = "",
                ns = "nid",
                jsv = NeuroID.getInstance()?.getSDKVersion(),
                sw = sharedPreferenceDefaults.getDisplayWidth().toFloat(),
                sh = sharedPreferenceDefaults.getDisplayHeight().toFloat(),
                metadata = neuroID.metaData,
                attrs = attrs,
            )
        }
    }

    fun clearSendOldFlowEvents(completion: () -> Unit = {}) =
        runBlocking {
            if (samplingService.isSessionFlowSampled()) {
                val deferred =
                    CoroutineScope(neuroID.dispatcher).async {
                        neuroID.nidJobServiceManager?.sendEvents(true)
                    }

                deferred.await()

                completion()
            } else {
                neuroID.dataStore.clearEvents()

                completion()
            }
        }
}
