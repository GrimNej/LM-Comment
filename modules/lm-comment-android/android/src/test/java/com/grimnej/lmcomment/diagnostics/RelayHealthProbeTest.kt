package com.grimnej.lmcomment.diagnostics

import com.grimnej.lmcomment.config.DemoConfiguration
import com.grimnej.lmcomment.config.Tone
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RelayHealthProbeTest {
    @Test
    fun `healthy relay uses content-free unauthenticated GET`() = runTest {
        lateinit var requestedUrl: URL
        val connection = FakeConnection(
            statusCode = 200,
            body = """{"status":"ok","generationEnabled":true,"modelConfigured":true}""",
        )
        val probe = RelayHealthProbe(
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
            connectionFactory = {
                requestedUrl = it
                connection
            },
            testOnly = Unit,
        )

        assertEquals(RelayHealthStatus.HEALTHY, probe.check(configuration()))
        assertEquals("https://relay.example/base/healthz", requestedUrl.toString())
        assertEquals("GET", connection.requestMethod)
        assertFalse(connection.requestProperties.keys.any { it.equals("X-Demo-Token", true) })
    }

    @Test
    fun `disabled generation is reported as degraded`() = runTest {
        val probe = probeWith(
            FakeConnection(
                statusCode = 200,
                body = """{"status":"ok","generationEnabled":false,"modelConfigured":true}""",
            ),
        )
        assertEquals(RelayHealthStatus.DEGRADED, probe.check(configuration()))
    }

    @Test
    fun `missing configuration does not open a connection`() = runTest {
        var opened = false
        val probe = RelayHealthProbe(
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
            connectionFactory = {
                opened = true
                FakeConnection(200, "{}")
            },
            testOnly = Unit,
        )
        assertEquals(RelayHealthStatus.NOT_CONFIGURED, probe.check(null))
        assertFalse(opened)
    }

    @Test
    fun `invalid health body is safely degraded`() = runTest {
        val probe = probeWith(FakeConnection(200, "not-json"))
        assertEquals(RelayHealthStatus.UNAVAILABLE, probe.check(configuration()))
    }

    @Test
    fun `caller cancellation disconnects a blocked health probe`() = runBlocking {
        val connection = BlockingConnection()
        val probe = RelayHealthProbe(
            ioDispatcher = Dispatchers.IO,
            connectionFactory = { connection },
            testOnly = Unit,
        )
        val request = launch { probe.check(configuration()) }
        assertTrue(withContext(Dispatchers.IO) {
            connection.responseStarted.await(2, TimeUnit.SECONDS)
        })

        request.cancelAndJoin()

        assertTrue(connection.disconnected.await(1, TimeUnit.SECONDS))
    }

    private fun probeWith(connection: FakeConnection) = RelayHealthProbe(
        ioDispatcher = UnconfinedTestDispatcher(),
        connectionFactory = { connection },
        testOnly = Unit,
    )

    private fun configuration() = DemoConfiguration(
        relayBaseUrl = "https://relay.example/base/",
        demoToken = "demo-token-not-sent",
        defaultTone = Tone.NATURAL,
        optionCount = 3,
        demoMode = true,
    )

    private class FakeConnection(
        private val statusCode: Int,
        body: String,
    ) : HttpURLConnection(URL("https://relay.example")) {
        private val bytes = body.toByteArray(Charsets.UTF_8)

        override fun getResponseCode(): Int = statusCode
        override fun getContentType(): String = "application/json; charset=utf-8"
        override fun getInputStream(): InputStream = ByteArrayInputStream(bytes)
        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy(): Boolean = false
    }

    private class BlockingConnection : HttpURLConnection(URL("https://relay.example/healthz")) {
        val responseStarted = CountDownLatch(1)
        val disconnected = CountDownLatch(1)

        override fun getResponseCode(): Int {
            responseStarted.countDown()
            disconnected.await()
            throw IOException("disconnected")
        }

        override fun connect() = Unit
        override fun disconnect() {
            disconnected.countDown()
        }
        override fun usingProxy(): Boolean = false
    }
}
