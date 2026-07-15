package com.grimnej.lmcomment.relay

import com.grimnej.lmcomment.config.Tone
import java.nio.charset.StandardCharsets
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GenerationContractCodecTest {
    @Test
    fun `shared valid request fixture round trips without contract drift`() {
        val fixture = fixture("valid-request.json")

        val decoded = GenerationContractCodec.decodeRequest(fixture)
        val encoded = GenerationContractCodec.encodeRequest(decoded)

        assertEquals(Tone.PROFESSIONAL, decoded.tone)
        assertEquals(2, decoded.optionCount)
        assertEquals(decoded, GenerationContractCodec.decodeRequest(encoded))
        assertEquals(
            setOf("sourceText", "tone", "instruction", "optionCount"),
            JSONObject(encoded).keys().asSequence().toSet(),
        )
    }

    @Test
    fun `shared valid response fixture parses with exact requested count`() {
        val decoded = GenerationContractCodec.decodeResponse(
            fixture("valid-response.json"),
            expectedOptionCount = 2,
        )

        assertEquals("aa7fbfed-5e87-4987-8ca4-9df6f169d3ae", decoded.requestId)
        assertEquals(2, decoded.options.size)
        assertEquals("b9d53cc0-b48e-401f-8792-876142b47836", decoded.options.first().id)
    }

    @Test
    fun `shared error fixture parses only frozen error metadata`() {
        val decoded = GenerationContractCodec.decodeError(fixture("error-response.json"))

        assertEquals(RelayFailureCode.RATE_LIMITED, decoded.code)
        assertEquals("c845f81e-641a-4b90-9f93-2a7f56f541db", decoded.requestId)
    }

    @Test
    fun `unknown request field is rejected`() {
        val json = JSONObject(fixture("valid-request.json"))
            .put("unexpected", true)
            .toString()

        assertFailure(RelayFailureCode.BAD_REQUEST) {
            GenerationContractCodec.decodeRequest(json)
        }
    }

    @Test
    fun `unknown response field is rejected`() {
        val json = JSONObject(fixture("valid-response.json"))
            .put("unexpected", true)
            .toString()

        assertFailure(RelayFailureCode.INVALID_RESPONSE) {
            GenerationContractCodec.decodeResponse(json, expectedOptionCount = 2)
        }
    }

    @Test
    fun `oversized source text is rejected before serialization`() {
        val request = validRequest(
            sourceText = "x".repeat(GenerationContractCodec.MAX_SOURCE_CHARACTERS + 1),
        )

        assertFailure(RelayFailureCode.BAD_REQUEST) {
            GenerationContractCodec.encodeRequest(request)
        }
    }

    @Test
    fun `oversized instruction is rejected before serialization`() {
        val request = validRequest(
            instruction = "x".repeat(GenerationContractCodec.MAX_INSTRUCTION_CHARACTERS + 1),
        )

        assertFailure(RelayFailureCode.BAD_REQUEST) {
            GenerationContractCodec.encodeRequest(request)
        }
    }

    @Test
    fun `invalid option count is rejected before serialization`() {
        assertFailure(RelayFailureCode.BAD_REQUEST) {
            GenerationContractCodec.encodeRequest(validRequest(optionCount = 4))
        }
    }

    @Test
    fun `response with fewer options than requested is rejected`() {
        val json = responseJson("A useful response.")

        assertFailure(RelayFailureCode.INVALID_RESPONSE) {
            GenerationContractCodec.decodeResponse(json, expectedOptionCount = 2)
        }
    }

    @Test
    fun `response with more options than requested is rejected`() {
        val json = responseJson("First response.", "Second response.")

        assertFailure(RelayFailureCode.INVALID_RESPONSE) {
            GenerationContractCodec.decodeResponse(json, expectedOptionCount = 1)
        }
    }

    @Test
    fun `normalized duplicate options are rejected`() {
        val json = responseJson("  Same   RESPONSE ", "same response")

        assertFailure(RelayFailureCode.INVALID_RESPONSE) {
            GenerationContractCodec.decodeResponse(json, expectedOptionCount = 2)
        }
    }

    @Test
    fun `unicode whitespace duplicate options are rejected`() {
        val json = responseJson("Same\u00a0response", "same response")

        assertFailure(RelayFailureCode.INVALID_RESPONSE) {
            GenerationContractCodec.decodeResponse(json, expectedOptionCount = 2)
        }
    }

    @Test
    fun `noncanonical UUID is rejected`() {
        val json = JSONObject(fixture("valid-response.json"))
            .put("requestId", "1-1-1-1-1")
            .toString()

        assertFailure(RelayFailureCode.INVALID_RESPONSE) {
            GenerationContractCodec.decodeResponse(json, expectedOptionCount = 2)
        }
    }

    @Test
    fun `trailing JSON content is rejected`() {
        val json = fixture("valid-request.json") + " true"

        assertFailure(RelayFailureCode.BAD_REQUEST) {
            GenerationContractCodec.decodeRequest(json)
        }
    }

    private fun responseJson(vararg optionTexts: String): String {
        val optionIds = listOf(
            "b9d53cc0-b48e-401f-8792-876142b47836",
            "9ce54289-d046-41f1-b870-d5b44d1dd176",
            "703011e0-f829-44d2-96cc-4a0b986eb742",
        )
        return JSONObject()
            .put("requestId", "aa7fbfed-5e87-4987-8ca4-9df6f169d3ae")
            .put(
                "options",
                optionTexts.mapIndexed { index, text ->
                    JSONObject().put("id", optionIds[index]).put("text", text)
                },
            )
            .toString()
    }

    private fun validRequest(
        sourceText: String = "Synthetic reviewed context.",
        instruction: String = "",
        optionCount: Int = 2,
    ) = GenerationRequest(
        sourceText = sourceText,
        tone = Tone.NATURAL,
        instruction = instruction,
        optionCount = optionCount,
    )

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Missing shared contract fixture: $name"
        }.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }

    private fun assertFailure(code: RelayFailureCode, block: () -> Unit) {
        val error = assertThrows(RelayException::class.java, block)
        assertEquals(code, error.code)
    }
}
