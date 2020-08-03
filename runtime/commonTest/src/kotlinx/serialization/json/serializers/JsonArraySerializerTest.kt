/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.serializers

import kotlinx.serialization.json.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.test.*

import kotlin.test.*

class JsonArraySerializerTest : JsonTestBase() {

    private val expected = "{\"array\":[1,null,[\"nested literal\"],[],{\"key\":\"value\"}]}"
    private val expectedTopLevel = "[1,null,[\"nested literal\"],[],{\"key\":\"value\"}]"

    @Test
    fun testJsonArray() = parametrizedTest(default) {
        assertStringFormAndRestored(expected, JsonArrayWrapper(prebuiltJson()), JsonArrayWrapper.serializer())
    }

    @Test
    fun testJsonArrayAsElement() = parametrizedTest(default) {
        assertStringFormAndRestored(expected.replace("array", "element"), JsonElementWrapper(prebuiltJson()), JsonElementWrapper.serializer())
    }

    @Test
    fun testTopLevelJsonObjectAsElement() = parametrizedTest(default) {
        assertStringFormAndRestored(expectedTopLevel, prebuiltJson(), JsonElementSerializer)
    }

    @Test
    fun testJsonArrayToString() {
        val prebuiltJson = prebuiltJson()
        val string = lenient.encodeToString(JsonArraySerializer, prebuiltJson)
        assertEquals(string, prebuiltJson.toString())
    }

    @Test
    fun testMixedLiterals() = parametrizedTest { useStreaming ->
        val json = """[1, "2", 3, "4"]"""
        val array = default.decodeFromString(JsonArraySerializer, json, useStreaming)
        array.forEachIndexed { index, element ->
            require(element is JsonLiteral)
            assertEquals(index % 2 == 1, element.isString)
        }
    }

    @Test
    fun testMissingCommas() = parametrizedTest { useStreaming ->
        val message = "Expected end of the array or comma"
        testFails("[a b c]", message, useStreaming)
        testFails("[ 1 2 3 ]", message, useStreaming)
        testFails("[null 1 null]", message, useStreaming)
        testFails("[1 \n 2]", message, useStreaming)
    }

    @Test
    fun testEmptyArray() = parametrizedTest { useStreaming ->
        assertEquals(JsonArray(emptyList()), lenient.decodeFromString(JsonArraySerializer, "[]", useStreaming))
        assertEquals(JsonArray(emptyList()), lenient.decodeFromString(JsonArraySerializer, "[    ]", useStreaming))
        assertEquals(JsonArray(emptyList()), lenient.decodeFromString(JsonArraySerializer, "[\n\n]", useStreaming))
        assertEquals(JsonArray(emptyList()), lenient.decodeFromString(JsonArraySerializer, "[     \t]", useStreaming))
    }

    @Test
    fun testWhitespaces() = parametrizedTest { useStreaming ->
        assertEquals(
            JsonArray(listOf(1, 2, 3, 4, 5).map(::JsonPrimitive)),
            lenient.decodeFromString(JsonArraySerializer, "[1, 2,   3, \n 4, 5]", useStreaming)
        )
    }

    @Test
    fun testExcessiveCommas() = parametrizedTest { useStreaming ->
        val trailing = "Unexpected trailing comma"
        val leading = "Unexpected leading comma"
        testFails("[a,]", trailing, useStreaming)
        testFails("[,1]", leading, useStreaming)
        testFails("[,1,]", leading, useStreaming)
        testFails("[,]", leading, useStreaming)
        testFails("[,,]", leading, useStreaming)
        testFails("[,,1]", leading, useStreaming)
        testFails("[1,,]", trailing, useStreaming)
        testFails("[1,,2]", trailing, useStreaming)
        testFails("[,   ,]", leading, useStreaming)
        testFails("[,\n,]", leading, useStreaming)
    }

    private fun testFails(input: String, errorMessage: String, useStreaming: Boolean) {
        assertFailsWithMessage<JsonDecodingException>(errorMessage) {
            lenient.decodeFromString(
                JsonArraySerializer,
                input,
                useStreaming
            )
        }
    }

    private fun prebuiltJson(): JsonArray {
        return buildJsonArray {
            add(1)
            add(JsonNull)
            addJsonArray {
                add("nested literal")
            }
            addJsonArray {}
            addJsonObject {
                put("key", "value")
            }
        }
    }
}
