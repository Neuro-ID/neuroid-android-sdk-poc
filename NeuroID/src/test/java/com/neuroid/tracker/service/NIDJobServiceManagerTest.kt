package com.neuroid.tracker.service

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.callbacks.NIDSensorGenListener
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.utils.NIDLogWrapper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NIDJobServiceManagerTest {
    @Test
    fun testStartjob() {
        val mockedSetup = setupNIDJobServiceManagerMocks()
        val mockedApplication = mockedSetup.mockedApplication
        val nidJobServiceManager = mockedSetup.nidJobServiceManager

        assertEquals(nidJobServiceManager.sendCadenceJob, null)
        assertEquals(nidJobServiceManager.gyroCadenceJob, null)
        assertEquals(nidJobServiceManager.isSetup, false)
        assertEquals(nidJobServiceManager.clientKey, "")

        nidJobServiceManager.startJob(
            mockedApplication,
            "clientKey",
        )

        assertEquals(nidJobServiceManager.isSetup, true)
        assertEquals(nidJobServiceManager.clientKey, "clientKey")
        assertNotNull(nidJobServiceManager.sendCadenceJob)
        assertNotNull(nidJobServiceManager.gyroCadenceJob)
    }

    @Test
    fun testStopjob() {
        val mockedSetup = setupNIDJobServiceManagerMocks()
        val mockedApplication = mockedSetup.mockedApplication
        val nidJobServiceManager = mockedSetup.nidJobServiceManager

        nidJobServiceManager.startJob(
            mockedApplication,
            "clientKey",
        )
        assertNotNull(nidJobServiceManager.sendCadenceJob)

        nidJobServiceManager.stopJob()
        assertEquals(nidJobServiceManager.sendCadenceJob, null)
        assertEquals(nidJobServiceManager.gyroCadenceJob, null)
        assertEquals(nidJobServiceManager.isStopped(), true)
    }

    @Test
    fun testRestart() {
        val mockedSetup = setupNIDJobServiceManagerMocks()
        val nidJobServiceManager = mockedSetup.nidJobServiceManager

        assertEquals(nidJobServiceManager.sendCadenceJob, null)
        assertEquals(nidJobServiceManager.gyroCadenceJob, null)
        assertEquals(nidJobServiceManager.isStopped(), true)

        nidJobServiceManager.restart()
        assertNotNull(nidJobServiceManager.sendCadenceJob)
        assertNotNull(nidJobServiceManager.gyroCadenceJob)
        assertEquals(nidJobServiceManager.isStopped(), false)
    }

    @Test
    fun testSendEvents() {
        runTest {
            val mockedSetup = setupNIDJobServiceManagerMocks()
            val mockedApplication = mockedSetup.mockedApplication
            val nidJobServiceManager = mockedSetup.nidJobServiceManager
            nidJobServiceManager.startJob(
                mockedApplication,
                "clientKey",
            )

            // test the thing
            nidJobServiceManager.sendEvents(
                forceSendEvents = true,
            )

            verify(exactly = 1) {
                mockedSetup.mockedEventSender.sendEvents(
                    any(),
                    any(),
                    any(),
                )
            }
        }
    }

    internal data class MockedServices(
        val nidJobServiceManager: NIDJobServiceManager,
        val mockedApplication: Application,
        val mockedLogger: NIDLogWrapper,
        val mockedEventSender: NIDSendingService,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun setupNIDJobServiceManagerMocks(): MockedServices {
        val mockedApplication = getMockedApplication()
        val logger = getMockedLogger()
        val mockedEventSender = getMockEventSender()

        val nidJobServiceManager =
            NIDJobServiceManager(
                getMockedNeuroID(),
                getMockedDatastoreManager(),
                mockedEventSender,
                logger,
                UnconfinedTestDispatcher(),
            )

        return MockedServices(nidJobServiceManager, mockedApplication, logger, mockedEventSender)
    }

    private fun getMockedNeuroID(): NeuroID {
        val nidMock = mockk<NeuroID>()
        every {
            nidMock.captureEvent(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } just runs

        return nidMock
    }

    private fun getMockedDatastoreManager(): NIDDataStoreManager {
        val dataStoreManager = mockk<NIDDataStoreManager>()
        val event = NIDEventModel(type = "TEST_EVENT", ts = 1)
        coEvery { dataStoreManager.getAllEvents() } returns listOf(event)
        return dataStoreManager
    }

    private fun getMockedLogger(): NIDLogWrapper {
        val logger = mockk<NIDLogWrapper>()
        every { logger.d(any(), any()) } just runs
        every { logger.e(any(), any()) } just runs
        return logger
    }

    private fun getMockedApplication(): Application {
        val sensorManager = mockk<SensorManager>()
        every { sensorManager.getSensorList(any()) } returns listOf()
        every { sensorManager.unregisterListener(any<NIDSensorGenListener>()) } just runs
        every { sensorManager.registerListener(any<SensorEventListener>(), any<Sensor>(), any<Int>(), any<Int>()) } returns true

        val application = mockk<Application>()
        every { application.getSystemService(any()) } returns sensorManager

        val sharedPreferences = mockk<SharedPreferences>()
        every { sharedPreferences.getString(any(), any()) } returns "test"
        every { application.getSharedPreferences(any(), any()) } returns sharedPreferences

        val activityManager = mockk<ActivityManager>()
        every { application.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
        every { activityManager.getMemoryInfo(any()) } just runs

        return application
    }

    private fun getMockEventSender(): NIDSendingService {
        val eventSender = mockk<NIDEventSender>()

        every { eventSender.sendEvents(any<String>(), any<List<NIDEventModel>>(), any<NIDResponseCallBack>()) } just runs

        return eventSender
    }
}
