/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.json.*

internal class JsonTreeReader(
    configuration: JsonConf,
    private val lexer: JsonLexer
) {
    private val isLenient = configuration.isLenient

    private fun readObject(): JsonElement {
        var lastToken = lexer.consumeNextToken(TC_BEGIN_OBJ)
        if (lexer.peekNextToken() == TC_COMMA) lexer.fail("Unexpected leading comma")
        val result = linkedMapOf<String, JsonElement>()
        while (lexer.canConsumeValue()) {
            // Read key and value
            val key = if (isLenient) lexer.consumeStringLenient() else lexer.consumeString()
            lexer.consumeNextToken(TC_COLON)
            val element = read()
            result[key] = element
            // Verify the next token
            lastToken = lexer.consumeNextToken()
            if (lastToken != TC_COMMA && lastToken != TC_END_OBJ) {
                lexer.fail("Expected end of the object or comma")
            }
        }
        // Check for the correct ending
        if (lastToken == TC_BEGIN_OBJ) { // Case of empty object
            lexer.consumeNextToken(TC_END_OBJ)
        } else if (lastToken == TC_COMMA) { // Trailing comma
            lexer.fail("Unexpected trailing comma")
        }
        return JsonObject(result)
    }

    private fun readArray(): JsonElement {
        var lastToken = lexer.consumeNextToken()
        // Prohibit leading comma
        if (lexer.peekNextToken() == TC_COMMA) lexer.fail("Unexpected leading comma")
        val result = arrayListOf<JsonElement>()
        while (lexer.canConsumeValue()) {
            val element = read()
            result.add(element)
            lastToken = lexer.consumeNextToken()
            if (lastToken != TC_COMMA) {
                lexer.require(lastToken == TC_END_LIST) { "Expected end of the array or comma" }
            }
        }
        // Check for the correct ending
        if (lastToken == TC_BEGIN_LIST) { // Case of empty object
            lexer.consumeNextToken(TC_END_LIST)
        } else if (lastToken == TC_COMMA) { // Trailing comma
            lexer.fail("Unexpected trailing comma")
        }
        return JsonArray(result)
    }

    private fun readValue(isString: Boolean): JsonPrimitive {
        val string = if (isLenient || !isString) {
            lexer.consumeStringLenient()
        } else {
            lexer.consumeString()
        }
        if (string == NULL) return JsonNull
        return JsonLiteral(string, isString)
    }

    fun read(): JsonElement {
        return when (lexer.peekNextToken()) {
            TC_STRING -> readValue(isString = true)
            TC_OTHER -> readValue(isString = false)
            TC_BEGIN_OBJ -> readObject()
            TC_BEGIN_LIST -> readArray()
            else -> lexer.fail("Can't begin reading element, unexpected token")
        }
    }
}
