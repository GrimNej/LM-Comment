package com.grimnej.lmcomment.relay

import com.grimnej.lmcomment.config.Tone
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.security.Principal
import java.security.cert.Certificate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RelayClientTest {
    @Test
    fun `successful call sends only contract JSON and demo gate header`() = runBlocking {
        val response = fixture("valid-response.json")
        val connection = FakeHttpsConnection(
            status = 200,
            successBody = response,
        )
        var openedUrl: URL? = null
        val client = testClient(connection) { openedUrl = it }

        val result = client.generate(validRequest())

        assertEquals(2, result.options.size)
        assertEquals("https://relay.example/v1/generate", openedUrl.toString())
        assertEquals("POST", connection.requestMethod)
        assertEquals(5_000, connection.connectTimeout)
        assertEquals(20_000, connection.readTimeout)
        assertFalse(connection.instanceFollowRedirects)
        assertEquals("abcdefghijklmnop", connection.getRequestProperty("X-Demo-Token"))
        assertEquals("application/json", connection.getRequestProperty("Accept"))
        assertEquals(
            "application/json; charset=utf-8",
            connection.getRequestProperty("Content-Type"),
        )
        val sent = GenerationContractCodec.decodeRequest(connection.requestBody())
        assertEquals(validRequest(), sent)
        assertEquals(1, connection.disconnectCalls)
    }

    @Test
    fun `remote error maps by frozen code and ignores untrusted message`() = runBlocking {
        val body = JSONObject(fixture("error-response.json"))
        body.getJSONObject("error").put("message", "Untrusted upstream detail")
        val connection = FakeHttpsConnection(status = 429, errorBody = body.toString())
        val client = testClient(connection)

        val error = relayFailure { client.generate(validRequest()) }

        assertEquals(RelayFailureCode.RATE_LIMITED, error.code)
        assertEquals("c845f81e-641a-4b90-9f93-2a7f56f541db", error.requestId)
        assertEquals(RelayFailureCode.RATE_LIMITED.safeMessage, error.message)
        assertFalse(error.message.orEmpty().contains("Untrusted"))
    }

    @Test
    fun `remote error HTTP mismatch fails closed`() = runBlocking {
        val connection = FakeHttpsConnection(
            status = 503,
            errorBody = fixture("error-response.json"),
        )
        val client = testClient(connection)

        val error = relayFailure { client.generate(validRequest()) }

        assertEquals(RelayFailureCode.INVALID_RESPONSE, error.code)
    }

    @Test
    fun `socket timeout maps to stable network timeout`() = runBlocking {
        val connection = FakeHttpsConnection(
            responseFailure = SocketTimeoutException("sensitive network detail"),
        )
        val client = testClient(connection)

        val error = relayFailure { client.generate(validRequest()) }

        assertEquals(RelayFailureCode.NETWORK_TIMEOUT, error.code)
        assertEquals(RelayFailureCode.NETWORK_TIMEOUT.safeMessage, error.message)
    }

    @Test
    fun `unknown host maps to stable offline failure`() = runBlocking {
        val connection = FakeHttpsConnection(
            responseFailure = UnknownHostException("private-host.example"),
        )
        val client = testClient(connection)

        val error = relayFailure { client.generate(validRequest()) }

        assertEquals(RelayFailureCode.NETWORK_UNAVAILABLE, error.code)
        assertFalse(error.message.orEmpty().contains("private-host"))
    }

    @Test
    fun `response body over bound is rejected without parsing content`() = runBlocking {
        val connection = FakeHttpsConnection(
            status = 200,
            successBytes = ByteArray(32 * 1024 + 1) { 'x'.code.toByte() },
        )
        val client = testClient(connection)

        val error = relayFailure { client.generate(validRequest()) }

        assertEquals(RelayFailureCode.INVALID_RESPONSE, error.code)
    }

    @Test
    fun `non JSON response is rejected`() = runBlocking {
        val connection = FakeHttpsConnection(
            status = 200,
            successBody = fixture("valid-response.json"),
            responseContentType = "text/html",
        )
        val client = testClient(connection)

        val error = relayFailure { client.generate(validRequest()) }

        assertEquals(RelayFailureCode.INVALID_RESPONSE, error.code)
    }

    @Test
    fun `invalid UTF-8 response is rejected`() = runBlocking {
        val connection = FakeHttpsConnection(
            status = 200,
            successBytes = byteArrayOf(0xc3.toByte(), 0x28),
        )
        val client = testClient(connection)

        val error = relayFailure { client.generate(validRequest()) }

        assertEquals(RelayFailureCode.INVALID_RESPONSE, error.code)
    }

    @Test
    fun `overall timeout disconnects blocked connection`() = runBlocking {
        val connection = BlockingHttpsConnection()
        val client = testClient(
            connection = connection,
            timeouts = RelayTimeouts(connectMillis = 100, readMillis = 5_000, overallMillis = 100),
        )

        val error = relayFailure { client.generate(validRequest()) }

        assertEquals(RelayFailureCode.NETWORK_TIMEOUT, error.code)
        assertTrue(connection.responseStarted.await(1, TimeUnit.SECONDS))
        assertTrue(connection.disconnected.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun `caller cancellation disconnects blocked connection and remains cancellation`() = runBlocking {
        val connection = BlockingHttpsConnection()
        val client = testClient(connection)
        val requestJob = launch {
            client.generate(validRequest())
        }
        assertTrue(withContext(Dispatchers.IO) {
            connection.responseStarted.await(2, TimeUnit.SECONDS)
        })

        requestJob.cancelAndJoin()

        assertTrue(requestJob.isCancelled)
        assertTrue(connection.disconnected.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun `HTTP base URL is rejected before a connection is opened`() = runBlocking {
        var opened = false
        val connection = FakeHttpsConnection()
        val client = RelayClient(
            relayBaseUrl = "http://relay.example/",
            demoToken = "abcdefghijklmnop",
            timeouts = RelayTimeouts(),
            ioDispatcher = Dispatchers.IO,
            connectionFactory = {
                opened = true
                connection
            },
        )

        val error = relayFailure { client.generate(validRequest()) }

        assertEquals(RelayFailureCode.INVALID_CONFIGURATION, error.code)
        assertFalse(opened)
    }

    @Test
    fun `validated development HTTP loopback reaches generation endpoint`() = runBlocking {
        val connection = FakeHttpsConnection(
            status = 200,
            successBody = fixture("valid-response.json"),
        )
        var openedUrl: URL? = null
        val client = RelayClient(
            relayBaseUrl = "http://10.0.2.2:8787/",
            demoToken = "abcdefghijklmnop",
            allowDevelopmentHttp = true,
            timeouts = RelayTimeouts(),
            ioDispatcher = Dispatchers.IO,
            connectionFactory = { url ->
                openedUrl = url
                connection
            },
        )

        val result = client.generate(validRequest())

        assertEquals(2, result.options.size)
        assertEquals("http://10.0.2.2:8787/v1/generate", openedUrl.toString())
    }

    @Test
    fun `token containing a control character is rejected before connection`() = runBlocking {
        var opened = false
        val connection = FakeHttpsConnection()
        val client = RelayClient(
            relayBaseUrl = "https://relay.example/",
            demoToken = "abcdefghijkl\nmnop",
            timeouts = RelayTimeouts(),
            ioDispatcher = Dispatchers.IO,
            connectionFactory = {
                opened = true
                connection
            },
        )

        val error = relayFailure { client.generate(validRequest()) }

        assertEquals(RelayFailureCode.INVALID_CONFIGURATION, error.code)
        assertFalse(opened)
    }

    private fun testClient(
        connection: HttpsURLConnection,
        timeouts: RelayTimeouts = RelayTimeouts(),
        onOpen: (URL) -> Unit = {},
    ) = RelayClient(
        relayBaseUrl = "https://relay.example/",
        demoToken = "abcdefghijklmnop",
        timeouts = timeouts,
        ioDispatcher = Dispatchers.IO,
        connectionFactory = { url ->
            onOpen(url)
            connection
        },
    )

    private fun validRequest() = GenerationRequest(
        sourceText = "The prototype is simpler now and the team understands it.",
        tone = Tone.PROFESSIONAL,
        instruction = "Acknowledge the tradeoff.",
        optionCount = 2,
    )

    private suspend fun relayFailure(block: suspend () -> Unit): RelayException = try {
        block()
        fail("Expected RelayException")
        throw AssertionError("unreachable")
    } catch (error: RelayException) {
        error
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name"))
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }

    private open class FakeHttpsConnection(
        private val status: Int = 200,
        successBody: String = "{}",
        errorBody: String = "{}",
        private val successBytes: ByteArray = successBody.toByteArray(Charsets.UTF_8),
        private val errorBytes: ByteArray = errorBody.toByteArray(Charsets.UTF_8),
        private val responseFailure: IOException? = null,
        private val responseContentType: String = "application/json; charset=utf-8",
    ) : HttpsURLConnection(URL("https://relay.example/v1/generate")) {
        private val sentBody = ByteArrayOutputStream()
        var disconnectCalls = 0
            private set

        fun requestBody(): String = sentBody.toString(Charsets.UTF_8.name())

        override fun connect() = Unit

        override fun disconnect() {
            disconnectCalls++
        }

        override fun usingProxy(): Boolean = false

        override fun getOutputStream(): ByteArrayOutputStream = sentBody

        override fun getResponseCode(): Int {
            responseFailure?.let { throw it }
            return status
        }

        override fun getInputStream(): InputStream = ByteArrayInputStream(successBytes)

        override fun getErrorStream(): InputStream = ByteArrayInputStream(errorBytes)

        override fun getHeaderField(name: String?): String? =
            if (name.equals("Content-Type", ignoreCase = true)) responseContentType else null

        override fun getCipherSuite(): String = "TLS_FAKE"

        override fun getLocalCertificates(): Array<Certificate>? = null

        override fun getServerCertificates(): Array<Certificate> = emptyArray()

        override fun getPeerPrincipal(): Principal? = null

        override fun getLocalPrincipal(): Principal? = null
    }

    private class BlockingHttpsConnection : FakeHttpsConnection() {
        val responseStarted = CountDownLatch(1)
        val disconnected = CountDownLatch(1)

        override fun getResponseCode(): Int {
            responseStarted.countDown()
            disconnected.await(5, TimeUnit.SECONDS)
            throw IOException("disconnected")
        }

        override fun disconnect() {
            super.disconnect()
            disconnected.countDown()
        }
    }
}
