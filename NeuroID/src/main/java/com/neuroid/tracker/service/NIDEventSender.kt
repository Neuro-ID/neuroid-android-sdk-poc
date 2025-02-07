package com.neuroid.tracker.service

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.ANDROID_URI
import com.neuroid.tracker.events.OUT_OF_MEMORY
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.models.NIDResponseCallBack
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.generateUniqueHexID
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

interface NIDSendingService {
    fun sendEvents(
        key: String,
        events: List<NIDEventModel>,
        responseCallback: NIDResponseCallBack<Any>,
    )

// Request Prep Functions
    fun getRequestPayloadJSON(events: List<NIDEventModel>): String
}

/**
 * quiet retry on slow networks will occur auto-magically by OKHttp and is explained here:
 * https://medium.com/inloopx/okhttp-is-quietly-retrying-requests-is-your-api-ready-19489ef35ace
 * and here:
 * https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/-builder/retry-on-connection-failure/
 * we retry requests that connect and come back with bad response codes.
 */
class NIDEventSender(
    private var httpService: HttpService,
    private val context: Context,
) : NIDSendingService, RetrySender() {
    // a static payload to send if OOM occurs
    private var oomPayload = ""

    init {
        initializeStaticPayload()
    }

    /**
     * Create a payload that can be used in the event of an OOM error without additional allocations.
     */
    private fun initializeStaticPayload() {
        if (oomPayload.isEmpty()) {
            oomPayload =
                getRequestPayloadJSON(
                    listOf(
                        NIDEventModel(
                            type = OUT_OF_MEMORY,
                            ts = System.currentTimeMillis(),
                        ),
                    ),
                )
        }
    }

    override fun sendEvents(
        key: String,
        events: List<NIDEventModel>,
        responseCallback: NIDResponseCallBack<Any>,
    ) {
        var data = ""
        try {
            if (events.isEmpty()) {
                // nothing to send
                return
            }

            data = getRequestPayloadJSON(events)

            NIDLog.d("Payload", msg = "payload size: ${data.length} bytes")
        } catch (exception: OutOfMemoryError) {
            // make a best effort attempt to continue and send an out of memory event
            data = oomPayload
        }

        val requestBody = data.toRequestBody("application/JSON".toMediaTypeOrNull())

        httpService.sendEvents(
            requestBody,
            key,
            responseCallback,
        )
    }

    override fun getRequestPayloadJSON(events: List<NIDEventModel>): String {
        val sharedDefaults = NIDSharedPrefsDefaults(context)

        val userID: String? =
            if (NeuroID.getInstance()?.getUserID() != null) {
                NeuroID.getInstance()?.getUserID()
            } else {
                null
            }
        val registeredUserID: String? =
            if (NeuroID.getInstance()?.getRegisteredUserID() != null) {
                NeuroID.getInstance()?.getRegisteredUserID()
            } else {
                null
            }

        val linkedSiteID: String? = NeuroID.getInternalInstance()?.linkedSiteID
        val firstInstallTime: Long? = NeuroID.getInternalInstance()?.firstInstallTime
        val lastUpdateTime: Long? = NeuroID.getInternalInstance()?.lastUpdateTime
        val tabID =
            if (NeuroID.getInternalInstance()?.tabID != null) {
                NeuroID.getInternalInstance()?.tabID
            } else {
                NeuroID.rndmId
            }
        val packetNumber: Int = NeuroID.getInternalInstance()?.packetNumber ?: 0

        val jsonBody =
            mapOf(
                "siteId" to NeuroID.siteID,
                "userId" to userID,
                "clientId" to sharedDefaults.getClientID(),
                "identityId" to userID,
                "registeredUserId" to registeredUserID,
                "pageTag" to NeuroID.screenActivityName,
                "pageId" to NeuroID.rndmId,
                "tabId" to tabID,
                "responseId" to generateUniqueHexID(),
                "url" to "$ANDROID_URI${NeuroID.screenActivityName}",
                "jsVersion" to "5.0.0",
                "sdkVersion" to NeuroID.getInstance()?.getSDKVersion(),
                "environment" to NeuroID.environment,
                "jsonEvents" to events,
                "linkedSiteId" to linkedSiteID,
                "packetNumber" to packetNumber,
                "firstInstallTime" to firstInstallTime,
                "lastUpdateTime" to lastUpdateTime
            )

        NIDLog.d(
            "Payload:",
            msg =
                """
                Payload Summary
                ClientID: ${jsonBody["clientId"]}
                UserID: ${jsonBody["userId"]}
                RegisteredUserID: ${jsonBody["registeredUserId"]}
                LinkedSiteID: ${jsonBody["linkedSiteId"]}
                TabID: ${jsonBody["tabId"]}
                Packet Number: ${jsonBody["packetNumber"]}
                SDK Version: ${jsonBody["sdkVersion"]}
                Screen Name: ${NeuroID.screenName}
                Event Count: ${events.size}
                First Install Time: ${jsonBody["firstInstallTime"]}
                Last Update Time: ${jsonBody["lastUpdateTime"]}
                """.trimIndent(),
        )

        // using this JSON library (already included) does not escape /
        NIDLog.i(msg = "NID logging events (${events.count()}) as linkedSiteID: $linkedSiteID")
        val gson: Gson = GsonBuilder().create()
        return gson.toJson(jsonBody)
    }
}

fun getSendingService(
    httpService: HttpService,
    context: Context,
): NIDSendingService =
    NIDEventSender(
        httpService,
        context,
    )
