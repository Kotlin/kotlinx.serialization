/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.serializers

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlinx.serialization.test.*
import kotlin.test.*

class JsonObjectSerializerTest : JsonTestBase() {

    private val expected = "{element:{literal:1,nullKey:null,nested:{\"another literal\":\"some value\"}}}"
    private val expectedTopLevel = "{literal:1,nullKey:null,nested:{\"another literal\":\"some value\"}}"

    @Test
    fun testJsonObject() = parametrizedTest(unquoted) {
        assertStringFormAndRestored(expected, JsonObjectWrapper(prebuiltJson()), JsonObjectWrapper.serializer())
    }

    @Test
    fun testJsonObjectAsElement() = parametrizedTest(unquoted) {
        assertStringFormAndRestored(expected, JsonElementWrapper(prebuiltJson()), JsonElementWrapper.serializer())
    }

    @Test
    fun testTopLevelJsonObject() = parametrizedTest (unquoted) {
        assertStringFormAndRestored(expectedTopLevel, prebuiltJson(), JsonObjectSerializer)
    }

    @Test
    fun testTopLevelJsonObjectAsElement() = parametrizedTest (unquoted) {
        assertStringFormAndRestored(expectedTopLevel, prebuiltJson(), JsonElementSerializer)
    }

    @Test
    fun testJsonObjectToString() {
        val prebuiltJson = prebuiltJson()
        val string = nonStrict.stringify(JsonElementSerializer, prebuiltJson)
        assertEquals(string, prebuiltJson.toString())
    }

    @Test
    fun testDocumentationSample() {
        val string = Json.stringify(JsonElementSerializer, json { "key" to 1.0 })
        val literal = Json.parse(JsonElementSerializer, string)
        assertEquals(JsonObject(mapOf("key" to JsonLiteral(1.0))), literal)
    }

    @Test
    fun testMissingCommas() = parametrizedTest { useStreaming ->
        assertFailsWith<JsonDecodingException> { nonStrict.parse(JsonObjectSerializer, "{ \"1\": \"2\" \"3\":\"4\"}", useStreaming) }
    }

    @Test
    fun testEmptyObject() = parametrizedTest { useStreaming ->
        assertEquals(JsonObject(emptyMap()), nonStrict.parse(JsonObjectSerializer, "{}", useStreaming))
        assertEquals(JsonObject(emptyMap()), nonStrict.parse(JsonObjectSerializer, "{}", useStreaming))
        assertEquals(JsonObject(emptyMap()), nonStrict.parse(JsonObjectSerializer, "{\n\n}", useStreaming))
        assertEquals(JsonObject(emptyMap()), nonStrict.parse(JsonObjectSerializer, "{     \t}", useStreaming))
    }

    @Test
    fun testInvalidObject() = parametrizedTest { useStreaming ->
        assertFailsWith<JsonDecodingException> { strict.parse(JsonObjectSerializer, "{\"a\":\"b\"]", false) }
        assertFailsWith<JsonDecodingException> { strict.parse(JsonObjectSerializer, "{", useStreaming) }
        assertFailsWith<IllegalStateException> { strict.parse(JsonObjectSerializer, "{}}", useStreaming) }
        assertFailsWith<JsonDecodingException> { strict.parse(JsonObjectSerializer, "{]", useStreaming) }
    }

    @Test
    fun testWhitespaces() = parametrizedTest { useStreaming ->
        assertEquals(
            JsonObject(mapOf("1" to JsonPrimitive(2), "3" to JsonPrimitive(4), "5" to JsonPrimitive(6))),
            nonStrict.parse(JsonObjectSerializer, "{1: 2,   3: \n 4, 5:6}", useStreaming)
        )
    }

    @Test
    fun testExcessiveCommas() = parametrizedTest { useStreaming ->
        assertFailsWith<JsonDecodingException> { nonStrict.parse(JsonObjectSerializer, "{\"a\":\"b\",}", useStreaming) }
        assertFailsWith<JsonDecodingException> { nonStrict.parse(JsonObjectSerializer, "{\"a\",}", useStreaming) }
        assertFailsWith<JsonDecodingException> { nonStrict.parse(JsonObjectSerializer, "{,\"1\":\"2\"}", useStreaming) }
        assertFailsWith<JsonDecodingException> { nonStrict.parse(JsonObjectSerializer, "{,\"1\":\"2\",}", useStreaming) }
        assertFailsWith<JsonDecodingException> { nonStrict.parse(JsonObjectSerializer, "{,}", useStreaming) }
        assertFailsWith<JsonDecodingException> { nonStrict.parse(JsonObjectSerializer, "{,,}", useStreaming) }
        assertFailsWith<JsonDecodingException> { nonStrict.parse(JsonObjectSerializer, "{,,\"1\":\"2\"}", useStreaming) }
        assertFailsWith<JsonDecodingException> { nonStrict.parse(JsonObjectSerializer, "{\"1\":\"2\",,}", useStreaming) }
        assertFailsWith<JsonDecodingException> { nonStrict.parse(JsonObjectSerializer, "{\"1\":\"2\",,\"2\":\"2\"}", useStreaming) }
        assertFailsWith<JsonDecodingException> { nonStrict.parse(JsonObjectSerializer, "{,   ,}", useStreaming) }
        assertFailsWith<JsonDecodingException> { nonStrict.parse(JsonObjectSerializer, "{,\n,}", useStreaming) }
    }

    @Serializable
    data class Holder(val a: String)

    @Test
    fun testExcessiveCommasInObject() = parametrizedTest { useStreaming ->
        assertFailsWith<JsonDecodingException> { nonStrict.parse(Holder.serializer(), "{\"a\":\"b\",}", useStreaming) }
        assertFailsWith<JsonDecodingException> { nonStrict.parse(Holder.serializer(), "{\"a\",}", useStreaming) }
        assertFailsWith<JsonDecodingException> { nonStrict.parse(Holder.serializer(), "{,\"a\":\"b\"}", useStreaming) }
        assertFailsWith<JsonDecodingException> { nonStrict.parse(Holder.serializer(), "{,\"a\":\"b\",}", useStreaming) }
        assertFailsWith<JsonDecodingException> { nonStrict.parse(Holder.serializer(), "{,}", useStreaming) }
        assertFailsWith<JsonDecodingException> { nonStrict.parse(Holder.serializer(), "{,,}", useStreaming) }
        assertFailsWith<JsonDecodingException> { nonStrict.parse(Holder.serializer(), "{,,\"a\":\"b\"}", useStreaming) }
        assertFailsWith<JsonDecodingException> { nonStrict.parse(Holder.serializer(), "{\"a\":\"b\",,}", useStreaming) }
        assertFailsWith<JsonDecodingException> { nonStrict.parse(Holder.serializer(), "{,   ,}", useStreaming) }
        assertFailsWith<JsonDecodingException> { nonStrict.parse(Holder.serializer(), "{,\n,}", useStreaming) }
    }

    private fun prebuiltJson(): JsonObject {
        return json {
            "literal" to 1
            content["nullKey"] = JsonNull
            "nested" to json {
                "another literal" to "some value"
            }
        }
    }
}
