/*
 * Copyright 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.serialization.json

import kotlinx.serialization.json.internal.*

// TODO revisit
class JsonTreeParser internal constructor(private val parser: JsonReader) {

    companion object {
        fun parse(input: String): JsonObject = JsonTreeParser(input).readFully() as JsonObject
    }

    constructor(input: String) : this(JsonReader(input))

    private fun readObject(): JsonElement {
        parser.requireTokenClass(TC_BEGIN_OBJ) { "Expected start of object" }
        parser.nextToken()
        val result: MutableMap<String, JsonElement> = linkedMapOf()
        while (true) {
            if (parser.tokenClass == TC_COMMA) parser.nextToken()
            if (!parser.canBeginValue) break
            val key = parser.takeString()
            parser.requireTokenClass(TC_COLON) { "Expected ':'" }
            parser.nextToken()
            val elem = read()
            result[key] = elem
        }
        parser.requireTokenClass(TC_END_OBJ) { "Expected end of object" }
        parser.nextToken()
        return JsonObject(result)
    }

    private fun readValue(isString: Boolean): JsonElement {
        val str = parser.takeString()
        return JsonLiteral(str, isString)
    }

    private fun readArray(): JsonElement {
        parser.requireTokenClass(TC_BEGIN_LIST) { "Expected start of array" }
        parser.nextToken()
        val result: MutableList<JsonElement> = arrayListOf()
        while (true) {
            if (parser.tokenClass == TC_COMMA) parser.nextToken()
            if (!parser.canBeginValue) break
            val elem = read()
            result.add(elem)
        }
        parser.requireTokenClass(TC_END_LIST) { "Expected end of array" }
        parser.nextToken()
        return JsonArray(result)
    }

    fun read(): JsonElement {
        if (!parser.canBeginValue) fail(parser.currentPosition, "Can't begin reading value from here")
        val tc = parser.tokenClass
        return when (tc) {
            TC_NULL -> JsonNull.also { parser.nextToken() }
            TC_STRING -> readValue(isString = true)
            TC_OTHER -> readValue(isString = false)
            TC_BEGIN_OBJ -> readObject()
            TC_BEGIN_LIST -> readArray()
            else -> fail(parser.currentPosition, "Can't begin reading element")
        }
    }

    fun readFully(): JsonElement {
        val r = read()
        parser.requireTokenClass(TC_EOF) { "Input wasn't consumed fully" }
        return r
    }
}
