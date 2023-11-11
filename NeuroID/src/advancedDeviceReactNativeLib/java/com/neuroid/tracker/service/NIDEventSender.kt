package com.neuroid.tracker.service

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * quiet retry on slow networks will occur auto-magically by OKHttp and is explained here:
 * https://medium.com/inloopx/okhttp-is-quietly-retrying-requests-is-your-api-ready-19489ef35ace
 * and here:
 * https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/-builder/retry-on-connection-failure/
 * we retry requests that connect and come back with bad response codes.
 */
class NIDEventSender(private var apiService: NIDApiService) {
    fun sendTrackerData(events: String, key: String, nidResponseCallback: NIDResponseCallBack) {
        val requestBody = events.toRequestBody("application/JSON".toMediaTypeOrNull())
        val call = apiService.sendEvents(requestBody, key)
        try {
            var retryCount = 0
            while (retryCount < RETRY_COUNT) {
                // retain the existing call and always execute on clones of it so we can retry when
                // there is a failure!
                val retryCall = call.clone()
                val response = retryCall.execute()
                // only allow 200 codes to succeed, everything else is failure
                if (response.code() == HTTP_SUCCESS) {
                    nidResponseCallback.onSuccess(response.code())
                    response.body()?.close()
                    break
                } else {
                    // response code is not 200, retry these up to RETRY_COUNT times
                    retryCount ++
                    Thread.sleep(FAIL_DELAY)
                    nidResponseCallback.onFailure(response.code(), response.message(),
                        retryCount < RETRY_COUNT)
                    response.body()?.close()

                }
            }
        } catch (e: Exception) {
            var errorMessage = "no error message available"
            e.message?.let {
                errorMessage = it
            }
            nidResponseCallback.onFailure(-1, errorMessage, false)
        }
    }

    companion object {
        // if you change the retry count, please update the test!
        const val RETRY_COUNT = 3
        const val HTTP_SUCCESS = 200
        const val FAIL_DELAY = 2000L
    }

}