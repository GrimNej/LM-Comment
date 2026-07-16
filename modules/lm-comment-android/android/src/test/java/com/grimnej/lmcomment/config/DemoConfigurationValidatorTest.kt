package com.grimnej.lmcomment.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DemoConfigurationValidatorTest {
    @Test
    fun `accepts HTTPS and normalizes one trailing slash`() {
        val configuration = validConfiguration(
            relayBaseUrl = "HTTPS://Relay.Example.test/api///",
        )

        val validated = DemoConfigurationValidator.validate(configuration, isDebuggable = false)

        assertEquals("https://Relay.Example.test/api/", validated.relayBaseUrl)
        assertEquals("temporary-demo-token", validated.demoToken)
    }

    @Test
    fun `release configuration rejects cleartext URLs`() {
        assertThrows(IllegalArgumentException::class.java) {
            DemoConfigurationValidator.validate(
                validConfiguration(relayBaseUrl = "http://localhost:3000"),
                isDebuggable = false,
            )
        }
    }

    @Test
    fun `release configuration rejects local development hosts even with HTTPS`() {
        listOf("localhost", "127.0.0.1", "[::1]", "10.0.2.2").forEach { host ->
            assertThrows(IllegalArgumentException::class.java) {
                DemoConfigurationValidator.validate(
                    validConfiguration(relayBaseUrl = "https://$host:3000"),
                    isDebuggable = false,
                )
            }
        }
    }

    @Test
    fun `debug cleartext is limited to explicit development hosts`() {
        listOf("localhost", "127.0.0.1", "[::1]", "10.0.2.2").forEach { host ->
            val validated = DemoConfigurationValidator.validate(
                validConfiguration(relayBaseUrl = "http://$host:3000"),
                isDebuggable = true,
            )
            assertEquals("http://$host:3000/", validated.relayBaseUrl)
            assertEquals(true, validated.allowDevelopmentHttp)
        }

        assertThrows(IllegalArgumentException::class.java) {
            DemoConfigurationValidator.validate(
                validConfiguration(relayBaseUrl = "http://192.168.1.10:3000"),
                isDebuggable = true,
            )
        }
    }

    @Test
    fun `rejects URL credentials query fragment and missing host`() {
        listOf(
            "https://user:password@relay.example.test",
            "https://relay.example.test?token=secret",
            "https://relay.example.test#fragment",
            "https:///missing-host",
        ).forEach { relayUrl ->
            assertThrows(relayUrl, IllegalArgumentException::class.java) {
                DemoConfigurationValidator.validate(
                    validConfiguration(relayBaseUrl = relayUrl),
                    isDebuggable = false,
                )
            }
        }
    }

    @Test
    fun `enforces token bounds without exposing token content`() {
        listOf(
            "short",
            "x".repeat(DemoConfigurationValidator.MAX_DEMO_TOKEN_LENGTH + 1),
            "valid-token-with-newline\n",
        ).forEach { token ->
            val error = assertThrows(IllegalArgumentException::class.java) {
                DemoConfigurationValidator.validate(
                    validConfiguration(demoToken = token),
                    isDebuggable = false,
                )
            }
            check(!error.message.orEmpty().contains(token))
        }
    }

    @Test
    fun `enforces frozen tone enum and option range`() {
        Tone.entries.forEach { tone ->
            assertEquals(tone, Tone.fromWireValue(tone.wireValue))
        }
        assertThrows(IllegalArgumentException::class.java) {
            Tone.fromWireValue("casual")
        }
        listOf(0, 4).forEach { count ->
            assertThrows(IllegalArgumentException::class.java) {
                DemoConfigurationValidator.validate(
                    validConfiguration(optionCount = count),
                    isDebuggable = false,
                )
            }
        }
    }

    @Test
    fun `updating writing defaults preserves existing private relay credentials`() {
        val existing = DemoConfigurationValidator.validate(
            validConfiguration(
                relayBaseUrl = "https://override.example.test/api",
                demoToken = "override-demo-token",
            ),
            isDebuggable = false,
        )

        val updated = DemoConfigurationValidator.validate(
            existing.copy(
                defaultTone = Tone.WITTY,
                optionCount = 1,
                demoMode = false,
            ),
            isDebuggable = false,
        )

        assertEquals(existing.relayBaseUrl, updated.relayBaseUrl)
        assertEquals(existing.demoToken, updated.demoToken)
        assertEquals(Tone.WITTY, updated.defaultTone)
        assertEquals(1, updated.optionCount)
        assertEquals(false, updated.demoMode)
    }

    private fun validConfiguration(
        relayBaseUrl: String = "https://relay.example.test",
        demoToken: String = "temporary-demo-token",
        optionCount: Int = 3,
    ) = DemoConfiguration(
        relayBaseUrl = relayBaseUrl,
        demoToken = demoToken,
        defaultTone = Tone.NATURAL,
        optionCount = optionCount,
        demoMode = true,
    )
}
