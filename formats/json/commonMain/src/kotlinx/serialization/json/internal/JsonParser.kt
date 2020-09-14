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
        reader.requireTokenClass(TC_BEGIN_OBJ) { "Expected start of the object" }
        reader.nextToken()
        // Prohibit leading comma
        reader.require(reader.tokenClass != TC_COMMA, reader.currentPosition) { "Unexpected leading comma" }
        val result = linkedMapOf<String, JsonElement>()
        var valueExpected = false
        while (reader.canBeginValue) {
            valueExpected = false
            val key = if (isLenient) reader.takeString() else reader.takeStringQuoted()
            reader.requireTokenClass(TC_COLON) { "Expected ':'" }
            reader.nextToken()
            val element = read()
            result[key] = element
            if (reader.tokenClass != TC_COMMA) {
                // Prohibit whitespaces instead of commas {a:b c:d}
                reader.requireTokenClass(TC_END_OBJ) { "Expected end of the object or comma" }
            } else {
                valueExpected = true
                reader.nextToken()
            }
        }
        reader.require(!valueExpected && reader.tokenClass == TC_END_OBJ, reader.currentPosition) { "Expected end of the object" }
        reader.nextToken()
        return JsonObject(result)
    }

    private fun readArray(): JsonElement {
        reader.requireTokenClass(TC_BEGIN_LIST) { "Expected start of the array" }
        reader.nextToken()
        // Prohibit leading comma
        reader.require(reader.tokenClass != TC_COMMA, reader.currentPosition) { "Unexpected leading comma" }
        val result = arrayListOf<JsonElement>()
        var valueExpected = false
        while (reader.canBeginValue) {
            valueExpected = false
            val element = read()
            result.add(element)
            if (reader.tokenClass != TC_COMMA) {
                // Prohibit whitespaces instead of commas [a b]
                reader.requireTokenClass(TC_END_LIST) { "Expected end of the array or comma" }
            } else {
                valueExpected = true
                reader.nextToken()
            }
        }
        // Prohibit trailing commas
        reader.require(!valueExpected, reader.currentPosition) { "Unexpected trailing comma" }
        reader.nextToken()
        return JsonArray(result)
    }

    private fun readValue(isString: Boolean): JsonElement {
        val str = if (isLenient) {
            reader.takeString()
        } else {
            if (isString) reader.takeStringQuoted() else reader.takeString()
        }
        return JsonLiteral(str, isString)
    }

    fun read(): JsonElement {
        if (!reader.canBeginValue) reader.fail("Can't begin reading value from here")
        return when (reader.tokenClass) {
            TC_NULL -> JsonNull.also { reader.nextToken() }
            TC_STRING -> readValue(isString = true)
            TC_OTHER -> readValue(isString = false)
            TC_BEGIN_OBJ -> readObject()
            TC_BEGIN_LIST -> readArray()
            else -> reader.fail("Can't begin reading element, unexpected token")
        }
    }
}
