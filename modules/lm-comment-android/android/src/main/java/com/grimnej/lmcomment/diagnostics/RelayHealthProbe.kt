package com.grimnej.lmcomment.diagnostics

import com.grimnej.lmcomment.config.DemoConfiguration
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

enum class RelayHealthStatus(val wireValue: String) {
    NOT_CONFIGURED("not-configured"),
    HEALTHY("healthy"),
    DEGRADED("degraded"),
    UNAVAILABLE("unavailable"),
}

/** A bounded, unauthenticated health check that never sends or returns user content. */
internal class RelayHealthProbe private constructor(
    private val ioDispatcher: CoroutineDispatcher,
    private val connectionFactory: (URL) -> HttpURLConnection,
) {
    constructor() : this(
        ioDispatcher = Dispatchers.IO,
        connectionFactory = { url -> url.openConnection() as HttpURLConnection },
    )

    internal constructor(
        ioDispatcher: CoroutineDispatcher,
        connectionFactory: (URL) -> HttpURLConnection,
        @Suppress("UNUSED_PARAMETER") testOnly: Unit = Unit,
    ) : this(ioDispatcher, connectionFactory)

    suspend fun check(configuration: DemoConfiguration?): RelayHealthStatus {
        if (configuration == null) return RelayHealthStatus.NOT_CONFIGURED
        return withTimeoutOrNull(OVERALL_TIMEOUT_MILLIS) {
            execute(configuration.relayBaseUrl)
        } ?: RelayHealthStatus.UNAVAILABLE
    }

    @OptIn(InternalCoroutinesApi::class)
    private suspend fun execute(baseUrl: String): RelayHealthStatus = withContext(ioDispatcher) {
        val connectionReference = AtomicReference<HttpURLConnection?>()
        val job = currentCoroutineContext().job
        val cancellationHandle = job.invokeOnCompletion(
            onCancelling = true,
            invokeImmediately = true,
        ) { cause ->
            if (cause != null) connectionReference.get()?.disconnect()
        }
        try {
            job.ensureActive()
            val connection = connectionFactory(healthUrl(baseUrl))
            connectionReference.set(connection)
            job.ensureActive()
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.doInput = true
            connection.doOutput = false
            connection.useCaches = false
            connection.allowUserInteraction = false
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", JSON_MEDIA_TYPE)

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext RelayHealthStatus.UNAVAILABLE
            }
            val mediaType = connection.contentType.orEmpty().substringBefore(';').trim()
            if (!mediaType.equals(JSON_MEDIA_TYPE, ignoreCase = true)) {
                return@withContext RelayHealthStatus.DEGRADED
            }
            val body = connection.inputStream.use(::readBounded)
            val payload = JSONObject(body.toString(Charsets.UTF_8))
            if (
                payload.optString("status") == "ok" &&
                payload.optBoolean("generationEnabled", false) &&
                payload.optBoolean("modelConfigured", false)
            ) {
                RelayHealthStatus.HEALTHY
            } else {
                RelayHealthStatus.DEGRADED
            }
        } catch (_: Throwable) {
            currentCoroutineContext().ensureActive()
            RelayHealthStatus.UNAVAILABLE
        } finally {
            cancellationHandle.dispose()
            connectionReference.getAndSet(null)?.disconnect()
        }
    }

    private fun healthUrl(rawBaseUrl: String): URL {
        val base = URI(rawBaseUrl)
        return URI(base.toString().trimEnd('/') + HEALTH_PATH).toURL()
    }

    private fun readBounded(input: java.io.InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            total += read
            require(total <= MAX_RESPONSE_BYTES) { "Health response is too large." }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    companion object {
        private const val HEALTH_PATH = "/healthz"
        private const val JSON_MEDIA_TYPE = "application/json"
        private const val TIMEOUT_MILLIS = 2_500
        private const val OVERALL_TIMEOUT_MILLIS = 4_000L
        private const val MAX_RESPONSE_BYTES = 4 * 1024
    }
}
