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

/**
 * Root node for whole JSON DOM
 */
sealed class JsonElement

sealed class JsonPrimitive : JsonElement() {
    protected abstract val content: String

    val asInt: Int get() = content.toInt()
    val asLong: Long get() = content.toLong()

    val asDouble: Double get() = content.toDouble()
    val asFloat: Float get() = content.toFloat()

    val asBoolean: Boolean get() = content.toBoolean()

    val str: String get() = content
}

/**
 * Represents quoted JSON strings
 */
data class JsonString(override val content: String): JsonPrimitive() {
    private var quotedString: String? = null

    override fun toString(): String = if (quotedString != null) quotedString!! else {
        quotedString = buildString { printQuoted(content) }
        quotedString!!
    }
}

/**
 * Represents unquoted JSON primitives (numbers, booleans and null)
 */
data class JsonLiteral(override val content: String): JsonPrimitive() {
    constructor(number: Number): this(number.toString())
    constructor(boolean: Boolean): this(boolean.toString())

    override fun toString() = content
}

val JsonNull = JsonLiteral("null")

data class JsonObject(val content: Map<String, JsonElement>) : JsonElement(), Map<String, JsonElement> by content {
    fun getAsValue(key: String)= content[key] as? JsonPrimitive
    fun getAsObject(key: String) = content[key] as? JsonObject
    fun getAsArray(key: String) = content[key] as? JsonArray

    override fun toString(): String {
        return content.entries.joinToString(
            prefix = "{",
            postfix = "}",
            transform = {(k, v) -> """"$k": $v"""}
        )
    }
}

data class JsonArray(val content: List<JsonElement>) : JsonElement(), List<JsonElement> by content {
    fun getAsValue(index: Int) = content.getOrNull(index) as? JsonPrimitive
    fun getAsObject(index: Int) = content.getOrNull(index) as? JsonObject
    fun getAsArray(index: Int) = content.getOrNull(index) as? JsonArray

    override fun toString() = content.joinToString(prefix = "[", postfix = "]")
}


class JsonTreeParser internal constructor(private val p: Parser) {
    constructor(input: String) : this(Parser(input))

    private fun readObject(): JsonElement {
        p.requireTc(TC_BEGIN_OBJ) { "Expected start of object" }
        p.nextToken()
        val result: MutableMap<String, JsonElement> = hashMapOf()
        while (true) {
            if (p.tc == TC_COMMA) p.nextToken()
            if (!p.canBeginValue) break
            val key = p.takeStr()
            p.requireTc(TC_COLON) { "Expected ':'" }
            p.nextToken()
            val elem = read()
            result[key] = elem
        }
        p.requireTc(TC_END_OBJ) { "Expected end of object" }
        p.nextToken()
        return JsonObject(result)
    }

    private fun readValue(asLiteral: Boolean = false): JsonElement {
        val str = p.takeStr()
        return if (asLiteral) JsonLiteral(str) else JsonString(str)
    }

    private fun readArray(): JsonElement {
        p.requireTc(TC_BEGIN_LIST) { "Expected start of array" }
        p.nextToken()
        val result: MutableList<JsonElement> = arrayListOf()
        while (true) {
            if (p.tc == TC_COMMA) p.nextToken()
            if (!p.canBeginValue) break
            val elem = read()
            result.add(elem)
        }
        p.requireTc(TC_END_LIST) { "Expected end of array" }
        p.nextToken()
        return JsonArray(result)
    }

    fun read(): JsonElement {
        if (!p.canBeginValue) fail(p.curPos, "Can't begin reading value from here")
        val tc = p.tc
        return when (tc) {
            TC_NULL -> JsonNull.also { p.nextToken() }
            TC_STRING -> readValue(asLiteral = false)
            TC_OTHER -> readValue(asLiteral = true)
            TC_BEGIN_OBJ -> readObject()
            TC_BEGIN_LIST -> readArray()
            else -> fail(p.curPos, "Can't begin reading element")
        }
    }

    fun readFully(): JsonElement {
        val r = read()
        p.requireTc(TC_EOF) { "Input wasn't consumed fully" }
        return r
    }
}
