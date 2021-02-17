/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.json.*

internal class JsonParser(
    configuration: JsonConf,
    private val reader: JsonReader
) {
    private val isLenient = configuration.isLenient

    private fun readObject(): JsonElement {
        var lastToken = reader.consumeNextToken(TC_BEGIN_OBJ)
        if (reader.peekNextToken() == TC_COMMA) reader.fail("Unexpected leading comma")
        val result = linkedMapOf<String, JsonElement>()
        while (reader.canConsumeValue()) {
            // Read key and value
            val key = if (isLenient) reader.consumeStringLenient() else reader.consumeString()
            reader.consumeNextToken(TC_COLON)
            val element = read()
            result[key] = element
            // Verify the next token
            lastToken = reader.consumeNextToken()
            if (lastToken != TC_COMMA && lastToken != TC_END_OBJ) {
                reader.fail("Expected end of the object or comma")
            }
        }
        // Check for the correct ending
        if (lastToken == TC_BEGIN_OBJ) { // Case of empty object
            reader.consumeNextToken(TC_END_OBJ)
        } else if (lastToken == TC_COMMA) { // Trailing comma
            reader.fail("Unexpected trailing comma")
        }
        return JsonObject(result)
    }

    private fun readArray(): JsonElement {
        var lastToken = reader.consumeNextToken(TC_BEGIN_LIST)
        // Prohibit leading comma
        if (reader.peekNextToken() == TC_COMMA) reader.fail("Unexpected leading comma")
        val result = arrayListOf<JsonElement>()
        while (reader.canConsumeValue()) {
            val element = read()
            result.add(element)
            lastToken = reader.consumeNextToken()
            if (lastToken != TC_COMMA) {
                reader.require(lastToken == TC_END_LIST) { "Expected end of the array or comma" }
            }
        }
        // Check for the correct ending
        if (lastToken == TC_BEGIN_LIST) { // Case of empty object
            reader.consumeNextToken(TC_END_LIST)
        } else if (lastToken == TC_COMMA) { // Trailing comma
            reader.fail("Unexpected trailing comma")
        }
        return JsonArray(result)
    }

    private fun readValue(isString: Boolean): JsonPrimitive {
        val string = if (isLenient || !isString) {
            reader.consumeStringLenient()
        } else {
            reader.consumeString()
        }
        if (string == NULL) return JsonNull
        return JsonLiteral(string, isString)
    }

    fun read(): JsonElement {
        return when (reader.peekNextToken()) {
            TC_STRING -> readValue(isString = true)
            TC_OTHER -> readValue(isString = false)
            TC_BEGIN_OBJ -> readObject()
            TC_BEGIN_LIST -> readArray()
            else -> reader.fail("Can't begin reading element, unexpected token")
        }
    }
}
