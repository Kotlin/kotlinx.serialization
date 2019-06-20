/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.json.*

internal class JsonParser(private val reader: JsonReader) {

    private fun readObject(): JsonElement {
        reader.requireTokenClass(TC_BEGIN_OBJ) { "Expected start of object" }
        reader.nextToken()
        val result: MutableMap<String, JsonElement> = linkedMapOf()
        while (true) {
            if (reader.tokenClass == TC_COMMA) reader.nextJsonKey()
            if (!reader.canBeginValue) break
            val key = reader.takeString()
            reader.requireTokenClass(TC_COLON) { "Expected ':'" }
            reader.nextToken()
            val elem = read()
            result[key] = elem
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
        reader.requireTokenClass(TC_BEGIN_LIST) { "Expected start of array" }
        reader.nextToken()
        val result: MutableList<JsonElement> = arrayListOf()
        while (true) {
            if (reader.tokenClass == TC_COMMA) reader.nextToken()
            if (!reader.canBeginValue) break
            val elem = read()
            result.add(elem)
        }
        reader.requireTokenClass(TC_END_LIST) { "Expected end of array" }
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
