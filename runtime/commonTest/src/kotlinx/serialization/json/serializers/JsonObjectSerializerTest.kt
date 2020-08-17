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

    private val expected = """{"element":{"literal":1,"nullKey":null,"nested":{"another literal":"some value"}}}"""
    private val expectedTopLevel = """{"literal":1,"nullKey":null,"nested":{"another literal":"some value"}}"""

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
        assertStringFormAndRestored(expectedTopLevel, prebuiltJson(), JsonObjectSerializer)
    }

    @Test
    fun testTopLevelJsonObjectAsElement() = parametrizedTest (default) {
        assertStringFormAndRestored(expectedTopLevel, prebuiltJson(), JsonElementSerializer)
    }

    @Test
    fun testJsonObjectToString() {
        val prebuiltJson = prebuiltJson()
        val string = lenient.encodeToString(JsonElementSerializer, prebuiltJson)
        assertEquals(string, prebuiltJson.toString())
    }

    @Test
    fun testDocumentationSample() {
        val string = Json.encodeToString(JsonElementSerializer, buildJsonObject { put("key", 1.0) })
        val literal = Json.decodeFromString(JsonElementSerializer, string)
        assertEquals(JsonObject(mapOf("key" to JsonPrimitive(1.0))), literal)
    }

    @Test
    fun testMissingCommas() = parametrizedTest { useStreaming ->
        assertFailsWith<JsonDecodingException> { lenient.decodeFromString(JsonObjectSerializer, "{ \"1\": \"2\" \"3\":\"4\"}", useStreaming) }
    }

    @Test
    fun testEmptyObject() = parametrizedTest { useStreaming ->
        assertEquals(JsonObject(emptyMap()), lenient.decodeFromString(JsonObjectSerializer, "{}", useStreaming))
        assertEquals(JsonObject(emptyMap()), lenient.decodeFromString(JsonObjectSerializer, "{}", useStreaming))
        assertEquals(JsonObject(emptyMap()), lenient.decodeFromString(JsonObjectSerializer, "{\n\n}", useStreaming))
        assertEquals(JsonObject(emptyMap()), lenient.decodeFromString(JsonObjectSerializer, "{     \t}", useStreaming))
    }

    @Test
    fun testInvalidObject() = parametrizedTest { useStreaming ->
        assertFailsWith<JsonDecodingException> { default.decodeFromString(JsonObjectSerializer, "{\"a\":\"b\"]", false) }
        assertFailsWith<JsonDecodingException> { default.decodeFromString(JsonObjectSerializer, "{", useStreaming) }
        assertFailsWith<IllegalStateException> { default.decodeFromString(JsonObjectSerializer, "{}}", useStreaming) }
        assertFailsWith<JsonDecodingException> { default.decodeFromString(JsonObjectSerializer, "{]", useStreaming) }
    }

    @Test
    fun testWhitespaces() = parametrizedTest { useStreaming ->
        assertEquals(
            JsonObject(mapOf("1" to JsonPrimitive(2), "3" to JsonPrimitive(4), "5" to JsonPrimitive(6))),
            lenient.decodeFromString(JsonObjectSerializer, "{1: 2,   3: \n 4, 5:6}", useStreaming)
        )
    }

    @Test
    fun testExcessiveCommas() = parametrizedTest { useStreaming ->
        assertFailsWith<JsonDecodingException> { lenient.decodeFromString(JsonObjectSerializer, "{\"a\":\"b\",}", useStreaming) }
        assertFailsWith<JsonDecodingException> { lenient.decodeFromString(JsonObjectSerializer, "{\"a\",}", useStreaming) }
        assertFailsWith<JsonDecodingException> { lenient.decodeFromString(JsonObjectSerializer, "{,\"1\":\"2\"}", useStreaming) }
        assertFailsWith<JsonDecodingException> { lenient.decodeFromString(JsonObjectSerializer, "{,\"1\":\"2\",}", useStreaming) }
        assertFailsWith<JsonDecodingException> { lenient.decodeFromString(JsonObjectSerializer, "{,}", useStreaming) }
        assertFailsWith<JsonDecodingException> { lenient.decodeFromString(JsonObjectSerializer, "{,,}", useStreaming) }
        assertFailsWith<JsonDecodingException> { lenient.decodeFromString(JsonObjectSerializer, "{,,\"1\":\"2\"}", useStreaming) }
        assertFailsWith<JsonDecodingException> { lenient.decodeFromString(JsonObjectSerializer, "{\"1\":\"2\",,}", useStreaming) }
        assertFailsWith<JsonDecodingException> { lenient.decodeFromString(JsonObjectSerializer, "{\"1\":\"2\",,\"2\":\"2\"}", useStreaming) }
        assertFailsWith<JsonDecodingException> { lenient.decodeFromString(JsonObjectSerializer, "{,   ,}", useStreaming) }
        assertFailsWith<JsonDecodingException> { lenient.decodeFromString(JsonObjectSerializer, "{,\n,}", useStreaming) }
    }

    @Serializable
    data class Holder(val a: String)

    @Test
    fun testExcessiveCommasInObject() = parametrizedTest { useStreaming ->
        assertFailsWith<JsonDecodingException> { lenient.decodeFromString(Holder.serializer(), "{\"a\":\"b\",}", useStreaming) }
        assertFailsWith<JsonDecodingException> { lenient.decodeFromString(Holder.serializer(), "{\"a\",}", useStreaming) }
        assertFailsWith<JsonDecodingException> { lenient.decodeFromString(Holder.serializer(), "{,\"a\":\"b\"}", useStreaming) }
        assertFailsWith<JsonDecodingException> { lenient.decodeFromString(Holder.serializer(), "{,\"a\":\"b\",}", useStreaming) }
        assertFailsWith<JsonDecodingException> { lenient.decodeFromString(Holder.serializer(), "{,}", useStreaming) }
        assertFailsWith<JsonDecodingException> { lenient.decodeFromString(Holder.serializer(), "{,,}", useStreaming) }
        assertFailsWith<JsonDecodingException> { lenient.decodeFromString(Holder.serializer(), "{,,\"a\":\"b\"}", useStreaming) }
        assertFailsWith<JsonDecodingException> { lenient.decodeFromString(Holder.serializer(), "{\"a\":\"b\",,}", useStreaming) }
        assertFailsWith<JsonDecodingException> { lenient.decodeFromString(Holder.serializer(), "{,   ,}", useStreaming) }
        assertFailsWith<JsonDecodingException> { lenient.decodeFromString(Holder.serializer(), "{,\n,}", useStreaming) }
    }

    private fun prebuiltJson(): JsonObject {
        return buildJsonObject {
            put("literal", 1)
            put("nullKey", JsonNull)
            putJsonObject("nested") {
                put("another literal", "some value")
            }
        }
    }
}
