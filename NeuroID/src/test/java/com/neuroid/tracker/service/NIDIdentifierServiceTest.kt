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
import com.neuroid.tracker.getMockedLogger
import com.neuroid.tracker.getMockedNeuroID
import com.neuroid.tracker.getMockedValidationService
import com.neuroid.tracker.models.SessionIDOriginResult
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.verifyCaptureEvent
import io.mockk.every
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class NIDIdentifierServiceTest {
    private lateinit var logger: NIDLogWrapper
    private lateinit var neuroID: NeuroID
    private lateinit var identifierService: NIDIdentifierService
    private lateinit var validationService: NIDValidationService

    private val goodUID = "good_UID"
    private val badUID = "bad UID @#!"

    @Before
    fun setup() {
        logger = getMockedLogger()
        neuroID = getMockedNeuroID()
        validationService = getMockedValidationService()

        identifierService =
            NIDIdentifierService(
                logger = logger,
                neuroID,
                validationService,
            )

        NeuroID._isSDKStarted = false
    }

    @After
    fun teardown() {
        neuroID.registeredUserID = ""
        neuroID.userID = ""
        unmockkAll()
    }

//    getOriginResult
    @Test
    fun test_getOriginResult_CUSTOMER_SET_OK() {
        val sessionID = "gasdgasdgdsgds"
        val result = identifierService.getOriginResult(sessionID, true, true, "generic")
        Assert.assertEquals(result.origin, ORIGIN_CUSTOMER_SET)
        Assert.assertEquals(result.originCode, ORIGIN_CODE_CUSTOMER)
        Assert.assertEquals(result.idValue, sessionID)
    }

    @Test
    fun test_getOriginResult_CUSTOMER_SET_FAIL() {
        val badSessionID = "gasdgas dgdsgds"
        val result = identifierService.getOriginResult(badSessionID, false, true, "generic")
        Assert.assertEquals(result.origin, ORIGIN_CUSTOMER_SET)
        Assert.assertEquals(result.originCode, ORIGIN_CODE_FAIL)
    }

    @Test
    fun test_getOriginResult_CUSTOMER_SET_EMPTY_SESSION_ID() {
        val emptySessionID = ""
        val result = identifierService.getOriginResult(emptySessionID, true, false, "generic")
        Assert.assertEquals(result.origin, ORIGIN_NID_SET)
        Assert.assertEquals(result.originCode, ORIGIN_CODE_NID)
    }

    @Test
    fun test_getOriginResult_NID_SET_EMPTY_SESSION_ID() {
        val emptySessionID = ""
        val result = identifierService.getOriginResult(emptySessionID, false, false, "generic")
        Assert.assertEquals(result.origin, ORIGIN_NID_SET)
        Assert.assertEquals(result.originCode, ORIGIN_CODE_FAIL)
    }

    //    sendOriginEvent
    @Test
    fun test_sendOriginEvent() {
        NeuroID._isSDKStarted = false
        val originResult =
            SessionIDOriginResult(
                "testOrigin",
                "testCode",
                "sessionID",
                "testType",
            )

        identifierService.sendOriginEvent(originResult)

        verifyCaptureEvent(
            neuroID,
            SET_VARIABLE,
            queuedEvent = true,
            key = "sessionIdCode",
            v = originResult.originCode,
        )

        verifyCaptureEvent(
            neuroID,
            SET_VARIABLE,
            queuedEvent = true,
            key = "sessionIdSource",
            v = originResult.origin,
        )

        verifyCaptureEvent(
            neuroID,
            SET_VARIABLE,
            queuedEvent = true,
            key = "sessionId",
            v = "'${originResult.idValue}'",
        )
    }

    //    setGenericUserID
    @Test
    fun test_setGenericUserID_invalid() {
        every { validationService.validateUserID(any()) } returns false

        val validID =
            identifierService.setGenericUserID(
                SET_USER_ID,
                badUID,
                false,
            )

        Assert.assertFalse(validID)

        verifyCaptureEvent(
            neuroID,
            SET_VARIABLE,
            4,
        )
    }

    @Test
    fun test_setGenericUserID_valid() {
        every { validationService.validateUserID(any()) } returns true

        val validID =
            identifierService.setGenericUserID(
                SET_USER_ID,
                goodUID,
                false,
            )

        assert(validID)

        verifyCaptureEvent(
            neuroID,
            SET_VARIABLE,
            4,
        )

        verifyCaptureEvent(
            neuroID,
            SET_USER_ID,
            1,
            uid = goodUID,
        )
    }

    //    getUserID
    @Test
    fun test_getUserID() {
        every { neuroID.userID } returns goodUID

        assert(identifierService.getUserID() == goodUID)
    }

    //    setUserID
    @Test
    fun test_setUserId_not_empty() {
        every { validationService.validateUserID(any()) } returns true

        val result = identifierService.setSessionID(goodUID, false)

        Assert.assertTrue(result)

        verify(exactly = 1) {
            neuroID.userID = goodUID
        }

        verifyCaptureEvent(
            neuroID,
            SET_USER_ID,
            1,
            queuedEvent = true,
            uid = goodUID,
        )
    }

    //    getRegisteredUserID
    @Test
    fun test_getRegisteredUserID() {
        every { neuroID.registeredUserID } returns goodUID

        assert(identifierService.getRegisteredUserID() == goodUID)
    }

    //    setRegisteredUserID
    @Test
    fun test_setRegisteredUserId_success() {
        every { validationService.validateUserID(any()) } returns true

        val result = identifierService.setRegisteredUserID(goodUID)

        Assert.assertTrue(result)
        verify(exactly = 1) {
            neuroID.registeredUserID = goodUID
        }

        verifyCaptureEvent(
            neuroID,
            SET_REGISTERED_USER_ID,
            1,
            queuedEvent = true,
            uid = goodUID,
        )
    }

    @Test
    fun test_setRegisteredUserId_failure_existingValue() {
        every { validationService.validateUserID(any()) } returns true
        every { neuroID.registeredUserID } returns goodUID

        val result = identifierService.setRegisteredUserID("newValue")
        Assert.assertTrue(result)

        verify(exactly = 1) {
            neuroID.registeredUserID = any()
        }
        verifyCaptureEvent(
            neuroID,
            LOG,
            1,
            level = "warn",
        )
    }
}
