/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.json.*

internal class JsonParser(private val reader: JsonReader) {

    private fun readObject(): JsonElement {
        reader.requireTokenClass(TC_BEGIN_OBJ) { "Expected start of object" }
        reader.nextToken()
        val result = linkedMapOf<String, JsonElement>()
        while (true) {
            if (reader.tokenClass == TC_COMMA) reader.nextJsonKey()
            if (!reader.canBeginValue) break
            val key = reader.takeString()
            reader.requireTokenClass(TC_COLON) { "Expected ':'" }
            reader.nextToken()
            val element = read()
            result[key] = element
        }
        reader.requireTokenClass(TC_END_OBJ) { "Expected end of object" }
        reader.nextToken()
        return JsonObject(result)
    }

    private fun readValue(isString: Boolean): JsonElement {
        val str = reader.takeString()
        return JsonLiteral(str, isString)
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
        reader.require(!valueExpected, reader.currentPosition) { "Expected end of the array or comma" }
        reader.nextToken()
        return JsonArray(result)
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
