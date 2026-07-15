package com.grimnej.lmcomment.relay

import com.grimnej.lmcomment.config.Tone
import java.util.Locale
import java.util.UUID
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

object GenerationContractCodec {
    const val MAX_SOURCE_CHARACTERS = 8_000
    const val MAX_INSTRUCTION_CHARACTERS = 500
    const val MAX_OPTION_CHARACTERS = 700
    const val MIN_OPTION_COUNT = 1
    const val MAX_OPTION_COUNT = 3

    private val requestKeys = setOf("sourceText", "tone", "instruction", "optionCount")
    private val responseKeys = setOf("requestId", "options")
    private val optionKeys = setOf("id", "text")
    private val errorRootKeys = setOf("error")
    private val errorKeys = setOf("code", "message", "requestId")

    fun encodeRequest(request: GenerationRequest): String {
        validateRequest(request)
        return JSONObject()
            .put("sourceText", request.sourceText)
            .put("tone", request.tone.wireValue)
            .put("instruction", request.instruction)
            .put("optionCount", request.optionCount)
            .toString()
    }

    /** Used by the shared golden-fixture tests; production sends typed requests. */
    fun decodeRequest(json: String): GenerationRequest = contractCall(RelayFailureCode.BAD_REQUEST) {
        val root = parseObject(json)
        requireExactKeys(root, requestKeys)
        val request = GenerationRequest(
            sourceText = requireString(root, "sourceText"),
            tone = Tone.fromWireValue(requireString(root, "tone")),
            instruction = requireString(root, "instruction"),
            optionCount = requireInteger(root, "optionCount"),
        )
        validateRequest(request)
        request
    }

    fun decodeResponse(json: String, expectedOptionCount: Int): GenerationResponse =
        contractCall(RelayFailureCode.INVALID_RESPONSE) {
            require(expectedOptionCount in MIN_OPTION_COUNT..MAX_OPTION_COUNT)
            val root = parseObject(json)
            requireExactKeys(root, responseKeys)

            val requestId = requireCanonicalUuid(root, "requestId")
            val optionsValue = root.get("options")
            require(optionsValue is JSONArray)
            require(optionsValue.length() == expectedOptionCount)

            val options = ArrayList<GenerationOption>(optionsValue.length())
            val normalizedTexts = HashSet<String>(optionsValue.length())
            val optionIds = HashSet<String>(optionsValue.length())
            for (index in 0 until optionsValue.length()) {
                val value = optionsValue.get(index)
                require(value is JSONObject)
                requireExactKeys(value, optionKeys)
                val id = requireCanonicalUuid(value, "id")
                val text = requireString(value, "text")
                require(text.isNotBlank())
                require(text.length <= MAX_OPTION_CHARACTERS)
                require(optionIds.add(id.lowercase(Locale.ROOT)))
                require(normalizedTexts.add(normalizeForComparison(text)))
                options += GenerationOption(id = id, text = text)
            }

            GenerationResponse(requestId = requestId, options = options)
        }

    internal fun decodeError(json: String): RelayErrorEnvelope =
        contractCall(RelayFailureCode.INVALID_RESPONSE) {
            val root = parseObject(json)
            requireExactKeys(root, errorRootKeys)
            val errorValue = root.get("error")
            require(errorValue is JSONObject)
            requireExactKeys(errorValue, errorKeys)

            val code = RelayFailureCode.fromRemoteWireValue(requireString(errorValue, "code"))
            require(code != null)
            val message = requireString(errorValue, "message")
            require(message.isNotBlank() && message.length <= MAX_ERROR_MESSAGE_CHARACTERS)
            RelayErrorEnvelope(
                code = code,
                requestId = requireCanonicalUuid(errorValue, "requestId"),
            )
        }

    private fun validateRequest(request: GenerationRequest) {
        if (request.sourceText.isEmpty() || request.sourceText.length > MAX_SOURCE_CHARACTERS) {
            throw RelayException(RelayFailureCode.BAD_REQUEST)
        }
        if (request.instruction.length > MAX_INSTRUCTION_CHARACTERS) {
            throw RelayException(RelayFailureCode.BAD_REQUEST)
        }
        if (request.optionCount !in MIN_OPTION_COUNT..MAX_OPTION_COUNT) {
            throw RelayException(RelayFailureCode.BAD_REQUEST)
        }
    }

    private inline fun <T> contractCall(
        failureCode: RelayFailureCode,
        block: () -> T,
    ): T = try {
        block()
    } catch (error: RelayException) {
        throw error
    } catch (_: JSONException) {
        throw RelayException(failureCode)
    } catch (_: IllegalArgumentException) {
        throw RelayException(failureCode)
    } catch (_: ClassCastException) {
        throw RelayException(failureCode)
    }

    private fun parseObject(json: String): JSONObject {
        val tokener = JSONTokener(json)
        val value = tokener.nextValue()
        require(value is JSONObject)
        require(tokener.nextClean() == '\u0000')
        return value
    }

    private fun requireExactKeys(value: JSONObject, expected: Set<String>) {
        val actual = mutableSetOf<String>()
        val keys = value.keys()
        while (keys.hasNext()) actual += keys.next()
        require(actual == expected)
    }

    private fun requireString(value: JSONObject, key: String): String {
        val candidate = value.get(key)
        require(candidate is String)
        return candidate
    }

    private fun requireInteger(value: JSONObject, key: String): Int {
        val candidate = value.get(key)
        require(candidate is Number)
        val asDouble = candidate.toDouble()
        val asLong = candidate.toLong()
        require(asDouble.isFinite() && asDouble == asLong.toDouble())
        require(asLong in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong())
        return asLong.toInt()
    }

    private fun requireCanonicalUuid(value: JSONObject, key: String): String {
        val candidate = requireString(value, key)
        val parsed = UUID.fromString(candidate)
        require(parsed.toString().equals(candidate, ignoreCase = true))
        return candidate
    }

    private fun normalizeForComparison(value: String): String =
        value
            .trim { it.isWhitespace() || Character.isSpaceChar(it) }
            .replace(WHITESPACE_RUN, " ")
            .lowercase(Locale.ROOT)

    private val WHITESPACE_RUN = Regex("[\\s\\p{Z}]+")
    private const val MAX_ERROR_MESSAGE_CHARACTERS = 1_000
}
