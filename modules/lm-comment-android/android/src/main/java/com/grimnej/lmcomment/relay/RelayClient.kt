package com.grimnej.lmcomment.relay

import com.grimnej.lmcomment.config.DemoConfigurationValidator
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class RelayClient private constructor(
    private val relayBaseUrl: String,
    private val demoToken: String,
    private val allowDevelopmentHttp: Boolean,
    private val timeouts: RelayTimeouts,
    private val ioDispatcher: CoroutineDispatcher,
    private val connectionFactory: (URL) -> HttpURLConnection,
) {
    constructor(
        relayBaseUrl: String,
        demoToken: String,
        allowDevelopmentHttp: Boolean = false,
        timeouts: RelayTimeouts = RelayTimeouts(),
    ) : this(
        relayBaseUrl = relayBaseUrl,
        demoToken = demoToken,
        allowDevelopmentHttp = allowDevelopmentHttp,
        timeouts = timeouts,
        ioDispatcher = Dispatchers.IO,
        connectionFactory = { url -> url.openConnection() as HttpURLConnection },
    )

    internal constructor(
        relayBaseUrl: String,
        demoToken: String,
        allowDevelopmentHttp: Boolean = false,
        timeouts: RelayTimeouts,
        ioDispatcher: CoroutineDispatcher,
        connectionFactory: (URL) -> HttpURLConnection,
        @Suppress("UNUSED_PARAMETER") testOnly: Unit = Unit,
    ) : this(
        relayBaseUrl,
        demoToken,
        allowDevelopmentHttp,
        timeouts,
        ioDispatcher,
        connectionFactory,
    )

    suspend fun generate(request: GenerationRequest): GenerationResponse {
        val result = withTimeoutOrNull(timeouts.overallMillis) {
            execute(request)
        }
        return result ?: throw RelayException(RelayFailureCode.NETWORK_TIMEOUT)
    }

    @OptIn(InternalCoroutinesApi::class)
    private suspend fun execute(request: GenerationRequest): GenerationResponse =
        withContext(ioDispatcher) {
            val endpoint = endpointUrl(relayBaseUrl)
            val token = validatedToken(demoToken)
            val body = GenerationContractCodec.encodeRequest(request)
                .toByteArray(StandardCharsets.UTF_8)
            if (body.size > MAX_REQUEST_BYTES) {
                throw RelayException(RelayFailureCode.BAD_REQUEST)
            }

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
                val connection = connectionFactory(endpoint)
                connectionReference.set(connection)
                job.ensureActive()
                configure(connection, token, body.size)
                connection.outputStream.use { output -> output.write(body) }

                val status = connection.responseCode
                if (status in 200..299 && status != HttpURLConnection.HTTP_OK) {
                    throw RelayException(RelayFailureCode.INVALID_RESPONSE)
                }
                val responseBytes = readResponseBody(connection, status)
                val responseJson = decodeUtf8(responseBytes)
                if (status == HttpURLConnection.HTTP_OK) {
                    GenerationContractCodec.decodeResponse(responseJson, request.optionCount)
                } else {
                    val remoteError = GenerationContractCodec.decodeError(responseJson)
                    if (status != remoteError.code.expectedHttpStatus) {
                        throw RelayException(RelayFailureCode.INVALID_RESPONSE)
                    }
                    throw RelayException(remoteError.code, remoteError.requestId)
                }
            } catch (error: RelayException) {
                throw error
            } catch (_: SocketTimeoutException) {
                currentCoroutineContext().ensureActive()
                throw RelayException(RelayFailureCode.NETWORK_TIMEOUT)
            } catch (_: UnknownHostException) {
                currentCoroutineContext().ensureActive()
                throw RelayException(RelayFailureCode.NETWORK_UNAVAILABLE)
            } catch (_: ConnectException) {
                currentCoroutineContext().ensureActive()
                throw RelayException(RelayFailureCode.NETWORK_UNAVAILABLE)
            } catch (_: NoRouteToHostException) {
                currentCoroutineContext().ensureActive()
                throw RelayException(RelayFailureCode.NETWORK_UNAVAILABLE)
            } catch (_: IOException) {
                currentCoroutineContext().ensureActive()
                throw RelayException(RelayFailureCode.NETWORK_UNAVAILABLE)
            } catch (_: SecurityException) {
                throw RelayException(RelayFailureCode.INVALID_CONFIGURATION)
            } catch (_: ClassCastException) {
                throw RelayException(RelayFailureCode.INVALID_CONFIGURATION)
            } finally {
                cancellationHandle.dispose()
                connectionReference.getAndSet(null)?.disconnect()
            }
        }

    private fun configure(connection: HttpURLConnection, token: String, bodySize: Int) {
        connection.requestMethod = "POST"
        connection.connectTimeout = timeouts.connectMillis
        connection.readTimeout = timeouts.readMillis
        connection.doInput = true
        connection.doOutput = true
        connection.useCaches = false
        connection.allowUserInteraction = false
        connection.instanceFollowRedirects = false
        connection.setFixedLengthStreamingMode(bodySize)
        connection.setRequestProperty("Accept", JSON_MEDIA_TYPE)
        connection.setRequestProperty("Content-Type", "$JSON_MEDIA_TYPE; charset=utf-8")
        connection.setRequestProperty(DEMO_TOKEN_HEADER, token)
    }

    private fun readResponseBody(connection: HttpURLConnection, status: Int): ByteArray {
        val contentType = connection.contentType.orEmpty()
        if (!contentType.substringBefore(';').trim().equals(JSON_MEDIA_TYPE, ignoreCase = true)) {
            throw RelayException(RelayFailureCode.INVALID_RESPONSE)
        }
        val stream = if (status in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: ByteArrayInputStream(ByteArray(0))
        }
        return stream.use(::readBounded)
    }

    private fun readBounded(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(READ_BUFFER_BYTES)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            total += read
            if (total > MAX_RESPONSE_BYTES) {
                throw RelayException(RelayFailureCode.INVALID_RESPONSE)
            }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun decodeUtf8(bytes: ByteArray): String = try {
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (_: Exception) {
        throw RelayException(RelayFailureCode.INVALID_RESPONSE)
    }

    private fun endpointUrl(rawBaseUrl: String): URL = try {
        val base = URI(rawBaseUrl.trim())
        val scheme = base.scheme.orEmpty().lowercase()
        val host = base.host.orEmpty().lowercase()
        val allowedScheme = scheme == "https" ||
            (
                allowDevelopmentHttp &&
                    scheme == "http" &&
                    host in DemoConfigurationValidator.developmentHosts
            )
        if (
            !allowedScheme ||
            base.host.isNullOrBlank() ||
            base.rawUserInfo != null ||
            base.rawQuery != null ||
            base.rawFragment != null
        ) {
            throw RelayException(RelayFailureCode.INVALID_CONFIGURATION)
        }
        URI(base.toString().trimEnd('/') + GENERATE_PATH).toURL()
    } catch (error: RelayException) {
        throw error
    } catch (_: Exception) {
        throw RelayException(RelayFailureCode.INVALID_CONFIGURATION)
    }

    private fun validatedToken(rawToken: String): String {
        if (rawToken.any(Char::isISOControl)) {
            throw RelayException(RelayFailureCode.INVALID_CONFIGURATION)
        }
        val normalized = rawToken.trim()
        if (normalized.length !in MIN_TOKEN_CHARACTERS..MAX_TOKEN_CHARACTERS) {
            throw RelayException(RelayFailureCode.INVALID_CONFIGURATION)
        }
        return normalized
    }

    companion object {
        private const val GENERATE_PATH = "/v1/generate"
        private const val DEMO_TOKEN_HEADER = "X-Demo-Token"
        private const val JSON_MEDIA_TYPE = "application/json"
        private const val MAX_REQUEST_BYTES = 32 * 1024
        private const val MAX_RESPONSE_BYTES = 32 * 1024
        private const val READ_BUFFER_BYTES = 4 * 1024
        private const val MIN_TOKEN_CHARACTERS = 12
        private const val MAX_TOKEN_CHARACTERS = 512
    }
}
