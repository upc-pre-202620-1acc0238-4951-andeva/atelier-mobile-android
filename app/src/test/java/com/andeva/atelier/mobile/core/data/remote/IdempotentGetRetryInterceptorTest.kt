package com.andeva.atelier.mobile.core.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.io.IOException

class IdempotentGetRetryInterceptorTest {

    private val server = MockWebServer()
    private val client = OkHttpClient.Builder()
        .addInterceptor(IdempotentGetRetryInterceptor())
        .retryOnConnectionFailure(false)
        .build()

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `reintenta una sola vez un GET que falla por IOException`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        val request = Request.Builder().url(server.url("/api/v1/work-orders")).get().build()
        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            assertEquals("ok", response.body?.string())
        }
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `no reintenta un POST cuando falla la red`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        val request = Request.Builder()
            .url(server.url("/api/v1/billing/vouchers"))
            .post("{}".toRequestBody())
            .build()

        assertThrows(IOException::class.java) {
            client.newCall(request).execute()
        }
        assertEquals(1, server.requestCount)
    }
}
