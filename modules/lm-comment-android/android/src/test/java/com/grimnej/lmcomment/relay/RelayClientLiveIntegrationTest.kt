package com.grimnej.lmcomment.relay

import com.grimnej.lmcomment.config.Tone
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Opt-in canary. Normal CI skips unless both environment variables are present.
 * The test intentionally emits no token, source text, or generated response content.
 */
class RelayClientLiveIntegrationTest {
    @Test
    fun `live relay returns the exact requested option count`() = runBlocking {
        val baseUrl = System.getenv("LM_COMMENT_LIVE_BASE_URL")
        val demoToken = System.getenv("LM_COMMENT_LIVE_DEMO_TOKEN")
        assumeTrue(!baseUrl.isNullOrBlank() && !demoToken.isNullOrBlank())

        val requestedCount = 1
        val response = RelayClient(
            relayBaseUrl = checkNotNull(baseUrl),
            demoToken = checkNotNull(demoToken),
            timeouts = RelayTimeouts(
                connectMillis = 8_000,
                readMillis = 25_000,
                overallMillis = 30_000,
            ),
        ).generate(
            GenerationRequest(
                sourceText = "A synthetic project update says the smaller design is easier to explain.",
                tone = Tone.NATURAL,
                instruction = "Acknowledge the practical benefit.",
                optionCount = requestedCount,
            ),
        )

        assertEquals(requestedCount, response.options.size)
    }
}
