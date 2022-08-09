/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.serializers

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.test.*
import kotlin.test.*

class JsonObjectSerializerTest : JsonTestBase() {

    private val expected = """{"element":{"literal":1,"nullKey":null,"nested":{"another literal":"some value"},"\\. escaped":"\\. escaped","\n new line":"\n new line"}}"""
    private val expectedTopLevel = """{"literal":1,"nullKey":null,"nested":{"another literal":"some value"},"\\. escaped":"\\. escaped","\n new line":"\n new line"}"""

    @Test
    fun testJsonObject() = parametrizedTest(default) {
        assertStringFormAndRestored(expected, JsonObjectWrapper(prebuiltJson()), JsonObjectWrapper.serializer())
    }

    @Test
    fun testJsonObjectAsElement() = parametrizedTest(default) {
        assertStringFormAndRestored(expected, JsonElementWrapper(prebuiltJson()), JsonElementWrapper.serializer())
    }

    @Test
    fun testTopLevelJsonObject() = parametrizedTest (default) {
        assertStringFormAndRestored(expectedTopLevel, prebuiltJson(), JsonObject.serializer())
    }

    @Test
    fun testTopLevelJsonObjectAsElement() = parametrizedTest (default) {
        assertStringFormAndRestored(expectedTopLevel, prebuiltJson(), JsonElement.serializer())
    }

    @Test
    fun testJsonObjectToString() {
        val prebuiltJson = prebuiltJson()
        val string = lenient.encodeToString(JsonElement.serializer(), prebuiltJson)
        assertEquals(string, prebuiltJson.toString())
    }

    @Test
    fun testDocumentationSample() {
        val string = Json.encodeToString(JsonElement.serializer(), buildJsonObject { put("key", 1.0) })
        val literal = Json.decodeFromString(JsonElement.serializer(), string)
        assertEquals(JsonObject(mapOf("key" to JsonPrimitive(1.0))), literal)
    }

    @Test
    fun testMissingCommas() = parametrizedTest { jsonTestingMode ->
        assertFailsWithSerial("JsonDecodingException") { lenient.decodeFromString(JsonObject.serializer(), "{ \"1\": \"2\" \"3\":\"4\"}", jsonTestingMode) }
    }

    @Test
    fun testEmptyObject() = parametrizedTest { jsonTestingMode ->
        assertEquals(JsonObject(emptyMap()), lenient.decodeFromString(JsonObject.serializer(), "{}", jsonTestingMode))
        assertEquals(JsonObject(emptyMap()), lenient.decodeFromString(JsonObject.serializer(), "{}", jsonTestingMode))
        assertEquals(JsonObject(emptyMap()), lenient.decodeFromString(JsonObject.serializer(), "{\n\n}", jsonTestingMode))
        assertEquals(JsonObject(emptyMap()), lenient.decodeFromString(JsonObject.serializer(), "{     \t}", jsonTestingMode))
    }

    @Test
    fun testInvalidObject() = parametrizedTest { jsonTestingMode ->
        assertFailsWithSerial("JsonDecodingException") { default.decodeFromString(JsonObject.serializer(), "{\"a\":\"b\"]", jsonTestingMode) }
        assertFailsWithSerial("JsonDecodingException") { default.decodeFromString(JsonObject.serializer(), "{", jsonTestingMode) }
        if (jsonTestingMode != JsonTestingMode.JAVA_STREAMS) // Streams support dangling characters
            assertFailsWithSerial("JsonDecodingException") { default.decodeFromString(JsonObject.serializer(), "{}}", jsonTestingMode) }
        assertFailsWithSerial("JsonDecodingException") { default.decodeFromString(JsonObject.serializer(), "{]", jsonTestingMode) }
    }

    @Test
    fun testWhitespaces() = parametrizedTest { jsonTestingMode ->
        assertEquals(
            JsonObject(mapOf("1" to JsonPrimitive(2), "3" to JsonPrimitive(4), "5" to JsonPrimitive(6))),
            lenient.decodeFromString(JsonObject.serializer(), "{1: 2,   3: \n 4, 5:6}", jsonTestingMode)
        )
    }

    @Test
    fun testExcessiveCommas() = parametrizedTest { jsonTestingMode ->
        assertFailsWithSerial("JsonDecodingException") { lenient.decodeFromString(JsonObject.serializer(), "{\"a\":\"b\",}", jsonTestingMode) }
        assertFailsWithSerial("JsonDecodingException") { lenient.decodeFromString(JsonObject.serializer(), "{\"a\",}", jsonTestingMode) }
        assertFailsWithSerial("JsonDecodingException") { lenient.decodeFromString(JsonObject.serializer(), "{,\"1\":\"2\"}", jsonTestingMode) }
        assertFailsWithSerial("JsonDecodingException") { lenient.decodeFromString(JsonObject.serializer(), "{,\"1\":\"2\",}", jsonTestingMode) }
        assertFailsWithSerial("JsonDecodingException") { lenient.decodeFromString(JsonObject.serializer(), "{,}", jsonTestingMode) }
        assertFailsWithSerial("JsonDecodingException") { lenient.decodeFromString(JsonObject.serializer(), "{,,}", jsonTestingMode) }
        assertFailsWithSerial("JsonDecodingException") { lenient.decodeFromString(JsonObject.serializer(), "{,,\"1\":\"2\"}", jsonTestingMode) }
        assertFailsWithSerial("JsonDecodingException") { lenient.decodeFromString(JsonObject.serializer(), "{\"1\":\"2\",,}", jsonTestingMode) }
        assertFailsWithSerial("JsonDecodingException") { lenient.decodeFromString(JsonObject.serializer(), "{\"1\":\"2\",,\"2\":\"2\"}", jsonTestingMode) }
        assertFailsWithSerial("JsonDecodingException") { lenient.decodeFromString(JsonObject.serializer(), "{,   ,}", jsonTestingMode) }
        assertFailsWithSerial("JsonDecodingException") { lenient.decodeFromString(JsonObject.serializer(), "{,\n,}", jsonTestingMode) }
    }

    @Serializable
    data class Holder(val a: String)

    @Test
    fun testExcessiveCommasInObject() = parametrizedTest { jsonTestingMode ->
        assertFailsWithSerial("JsonDecodingException") { lenient.decodeFromString(Holder.serializer(), "{\"a\":\"b\",}", jsonTestingMode) }
        assertFailsWithSerial("JsonDecodingException") { lenient.decodeFromString(Holder.serializer(), "{\"a\",}", jsonTestingMode) }
        assertFailsWithSerial("JsonDecodingException") { lenient.decodeFromString(Holder.serializer(), "{,\"a\":\"b\"}", jsonTestingMode) }
        assertFailsWithSerial("JsonDecodingException") { lenient.decodeFromString(Holder.serializer(), "{,\"a\":\"b\",}", jsonTestingMode) }
        assertFailsWithSerial("JsonDecodingException") { lenient.decodeFromString(Holder.serializer(), "{,}", jsonTestingMode) }
        assertFailsWithSerial("JsonDecodingException") { lenient.decodeFromString(Holder.serializer(), "{,,}", jsonTestingMode) }
        assertFailsWithSerial("JsonDecodingException") { lenient.decodeFromString(Holder.serializer(), "{,,\"a\":\"b\"}", jsonTestingMode) }
        assertFailsWithSerial("JsonDecodingException") { lenient.decodeFromString(Holder.serializer(), "{\"a\":\"b\",,}", jsonTestingMode) }
        assertFailsWithSerial("JsonDecodingException") { lenient.decodeFromString(Holder.serializer(), "{,   ,}", jsonTestingMode) }
        assertFailsWithSerial("JsonDecodingException") { lenient.decodeFromString(Holder.serializer(), "{,\n,}", jsonTestingMode) }
    }

    private fun prebuiltJson(): JsonObject {
        return buildJsonObject {
            put("literal", 1)
            put("nullKey", JsonNull)
            putJsonObject("nested") {
                put("another literal", "some value")
            }
            put("\\. escaped", "\\. escaped")
            put("\n new line", "\n new line")
        }
    }
}
