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
sealed class JsonElement {

    open val primitive: JsonPrimitive
        get() = error("JsonLiteral")

    open val jsonObject: JsonObject
        get() = error("JsonObject")

    open val jsonArray: JsonArray
        get() = error("JsonArray")

    open val jsonNull: JsonNull
        get() = error("JsonPrimitive")

    val isNull: Boolean
        get() = this === JsonNull

    private fun error(element: String): Nothing =
        throw UnsupportedOperationException("${this::class} is not a $element")
}

/**
 * Represents either quoted string, unquoted primitive or null
 */
sealed class JsonPrimitive : JsonElement() {
    abstract val content: String

    final override val primitive: JsonPrimitive = this

    val asInt: Int get() = content.toInt()
    val asIntOrNull: Int? get() = content.toIntOrNull()

    val asLong: Long get() = content.toLong()
    val asLongOrNull: Long? get() = content.toLongOrNull()

    val asDouble: Double get() = content.toDouble()
    val asDoubleOrNull: Double? get() = content.toDoubleOrNull()

    val asFloat: Float get() = content.toFloat()
    val asFloatOrNull: Float? get() = content.toFloatOrNull()

    val asBoolean: Boolean
        get() = asBooleanOrNull ?: throw IllegalStateException("$content does not represent a Boolean")

    val asBooleanOrNull: Boolean?
        get() = when {
            content.equals("true", ignoreCase = true) -> true
            content.equals("false", ignoreCase = true) -> false
            else -> null
        }

    override fun toString() = content
}

/**
 * Represents unquoted JSON primitives (numbers or booleans)
 */
data class JsonLiteral internal constructor(
    private val body: Any,
    private val isString: Boolean
) : JsonPrimitive() {
    override val content = body.toString()

    constructor(number: Number) : this(number, false)
    constructor(boolean: Boolean) : this(boolean, false)
    constructor(string: String) : this(string, true)

    override fun toString() =
        if (isString) buildString { printQuoted(content) }
        else content
}

object JsonNull : JsonPrimitive() {
    override val jsonNull: JsonNull = this
    override val content: String = "null"
}

@PublishedApi
internal fun unexpectedJson(key: String, expected: String): Nothing =
    throw IllegalStateException("Element $key is not a $expected")

data class JsonObject(val content: Map<String, JsonElement>) : JsonElement(), Map<String, JsonElement> by content {

    override val jsonObject: JsonObject = this

    override fun get(key: String): JsonElement = content[key] ?: throw NoSuchElementException("Element $key is missing")

    fun getAsValue(key: String): JsonPrimitive = content.getValue(key) as? JsonPrimitive
            ?: unexpectedJson(key, "JsonPrimitive")
    fun getAsObject(key: String): JsonObject = content.getValue(key) as? JsonObject
            ?: unexpectedJson(key, "JsonObject")
    fun getAsArray(key: String): JsonArray = content.getValue(key) as? JsonArray
            ?: unexpectedJson(key, "JsonArray")

    fun lookupValue(key: String): JsonPrimitive? = content[key] as? JsonPrimitive
    fun lookupObject(key: String): JsonObject? = content[key] as? JsonObject
    fun lookupArray(key: String): JsonArray? = content[key] as? JsonArray

    inline fun <reified J : JsonElement> getAs(key: String): J = content.getValue(key) as? J
            ?: unexpectedJson(key, J::class.toString())

    inline fun <reified J : JsonElement> lookup(key: String): J? = content[key] as? J

    override fun toString(): String {
        return content.entries.joinToString(
            prefix = "{",
            postfix = "}",
            transform = {(k, v) -> """"$k": $v"""}
        )
    }
}

data class JsonArray(val content: List<JsonElement>) : JsonElement(), List<JsonElement> by content {

    override val jsonArray: JsonArray = this

    fun getAsValue(index: Int) = content[index] as? JsonPrimitive
            ?: unexpectedJson("at $index", "JsonPrimitive")
    fun getAsObject(index: Int) = content[index] as? JsonObject
            ?: unexpectedJson("at $index", "JsonObject")
    fun getAsArray(index: Int) = content[index] as? JsonArray
            ?: unexpectedJson("at $index", "JsonArray")

    fun lookupValue(index: Int) = content.getOrNull(index) as? JsonPrimitive
    fun lookupObject(index: Int) = content.getOrNull(index) as? JsonObject
    fun lookupArray(index: Int) = content.getOrNull(index) as? JsonArray

    inline fun <reified J : JsonElement> getAs(index: Int): J = content[index] as? J
            ?: unexpectedJson("at $index", J::class.toString())

    inline fun <reified J : JsonElement> lookup(index: Int): J? = content.getOrNull(index) as? J


    override fun toString() = content.joinToString(prefix = "[", postfix = "]")
}
