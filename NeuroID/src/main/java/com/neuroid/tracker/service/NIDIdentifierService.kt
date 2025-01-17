package com.neuroid.tracker.service

import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.events.ORIGIN_CODE_CUSTOMER
import com.neuroid.tracker.events.ORIGIN_CODE_FAIL
import com.neuroid.tracker.events.ORIGIN_CODE_NID
import com.neuroid.tracker.events.ORIGIN_CUSTOMER_SET
import com.neuroid.tracker.events.ORIGIN_NID_SET
import com.neuroid.tracker.events.SET_REGISTERED_USER_ID
import com.neuroid.tracker.events.SET_USER_ID
import com.neuroid.tracker.events.SET_VARIABLE
import com.neuroid.tracker.models.SessionIDOriginResult
import com.neuroid.tracker.utils.NIDLogWrapper

internal class NIDIdentifierService(
    val logger: NIDLogWrapper,
    val neuroID: NeuroID,
    val validationService: NIDValidationService,
) {
    internal fun getOriginResult(
        idValue: String,
        validID: Boolean,
        userGenerated: Boolean,
        idType: String,
    ): SessionIDOriginResult {
        var origin =
            if (userGenerated) {
                ORIGIN_CUSTOMER_SET
            } else {
                ORIGIN_NID_SET
            }

        var originCode =
            if (validID) {
                if (userGenerated) {
                    ORIGIN_CODE_CUSTOMER
                } else {
                    ORIGIN_CODE_NID
                }
            } else {
                ORIGIN_CODE_FAIL
            }
        return SessionIDOriginResult(origin, originCode, idValue, idType)
    }

    internal fun sendOriginEvent(originResult: SessionIDOriginResult) {
        // sending these as individual items.
        neuroID.captureEvent(
            queuedEvent = !NeuroID.isSDKStarted,
            type = SET_VARIABLE,
            key = "sessionIdCode",
            v = originResult.originCode,
        )

        neuroID.captureEvent(
            queuedEvent = !NeuroID.isSDKStarted,
            type = SET_VARIABLE,
            key = "sessionIdSource",
            v = originResult.origin,
        )

        neuroID.captureEvent(
            queuedEvent = !NeuroID.isSDKStarted,
            type = SET_VARIABLE,
            key = "sessionId",
            v = "'${originResult.idValue}'",
        )

        neuroID.captureEvent(
            queuedEvent = !NeuroID.isSDKStarted,
            type = SET_VARIABLE,
            key = "idType",
            v = "'${originResult.idType}'",
        )
    }

    fun setGenericUserID(
        type: String,
        genericUserID: String,
        userGenerated: Boolean = true,
    ): Boolean {
        try {
            val validID = validationService.validateUserID(genericUserID)
            val originRes =
                getOriginResult(
                    genericUserID,
                    validID = validID,
                    userGenerated = userGenerated,
                    idType = type,
                )

            sendOriginEvent(originRes)

            if (!validID) {
                return false
            }

            if (NeuroID.isSDKStarted) {
                neuroID.captureEvent(
                    type = type,
                    uid = genericUserID,
                )
            } else {
                neuroID.captureEvent(
                    queuedEvent = true,
                    type = type,
                    uid = genericUserID,
                )
            }

            return true
        } catch (exception: Exception) {
            neuroID.captureEvent(type = LOG, m = "failure processing user id! $type, $genericUserID $exception", level = "ERROR")
            logger.e(msg = "failure processing user id! $type, $genericUserID $exception")
            return false
        }
    }

    fun getUserID() = neuroID.userID

    fun setSessionID(
        sessionID: String,
        userGenerated: Boolean,
    ): Boolean {
        neuroID.captureEvent(
            type = LOG,
            level = "info",
            m = "Set User Id Attempt ${validationService.scrubIdentifier(sessionID)}",
        )

        val validID = setGenericUserID(SET_USER_ID, sessionID, userGenerated)

        if (!validID) {
            return false
        }

        neuroID.userID = sessionID
        return true
    }

    fun getRegisteredUserID() = neuroID.registeredUserID

    fun setRegisteredUserID(registeredUserID: String): Boolean {
        if (neuroID.registeredUserID.isNotEmpty() && registeredUserID != neuroID.registeredUserID) {
            neuroID.captureEvent(
                type = LOG,
                level = "warn",
                m = "Multiple Registered User Id Attempt - existing:${neuroID.registeredUserID} new:${validationService.scrubIdentifier(
                    registeredUserID,
                )}",
            )
            logger.e(msg = "Multiple Registered UserID Attempt: Only 1 Registered UserID can be set per session")
        }

        neuroID.captureEvent(
            type = LOG,
            level = "info",
            m = "Set Registered User Id Attempt ${validationService.scrubIdentifier(registeredUserID)}",
        )

        val validID =
            setGenericUserID(
                SET_REGISTERED_USER_ID,
                registeredUserID,
            )

        if (!validID) {
            return false
        }

        neuroID.registeredUserID = registeredUserID
        return true
    }
}
