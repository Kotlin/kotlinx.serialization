/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.serializers

import kotlinx.serialization.json.*
import kotlinx.serialization.test.*
import kotlin.test.*

class JsonArraySerializerTest : JsonTestBase() {

    private val expected = "{\"array\":[1,null,[\"nested literal\"],[],{\"key\":\"value\"}]}"
    private val expectedTopLevel = "[1,null,[\"nested literal\"],[],{\"key\":\"value\"}]"

    @Test
    fun testJsonArray() = parametrizedTest(strict) {
        assertStringFormAndRestored(expected, JsonArrayWrapper(prebuiltJson()), JsonArrayWrapper.serializer())
    }

    @Test
    fun testJsonArrayAsElement() = parametrizedTest(strict) {
        assertStringFormAndRestored(expected.replace("array", "element"), JsonElementWrapper(prebuiltJson()), JsonElementWrapper.serializer())
    }

    @Test
    fun testTopLevelJsonObjectAsElement() = parametrizedTest(strict) {
        assertStringFormAndRestored(expectedTopLevel, prebuiltJson(), JsonElementSerializer)
    }

    @Test
    fun testJsonArrayToString() {
        val prebuiltJson = prebuiltJson()
        val string = nonStrict.stringify(JsonArraySerializer, prebuiltJson)
        assertEquals(string, prebuiltJson.toString())
    }

    @Test
    fun testMissingCommas() = parametrizedTest { useStreaming ->
        assertFailsWith<JsonParsingException> { nonStrict.parse(JsonArraySerializer, "[a b c]", useStreaming) }
        assertFailsWith<JsonParsingException> { nonStrict.parse(JsonArraySerializer, "[ 1 2 3 ]", useStreaming) }
        assertFailsWith<JsonParsingException> { nonStrict.parse(JsonArraySerializer, "[null 1 null]", useStreaming) }
        assertFailsWith<JsonParsingException> { nonStrict.parse(JsonArraySerializer, "[1 \n 2]", useStreaming) }
    }

    @Test
    fun testEmptyArray() = parametrizedTest { useStreaming ->
        assertEquals(JsonArray(emptyList()), nonStrict.parse(JsonArraySerializer, "[]", useStreaming))
        assertEquals(JsonArray(emptyList()), nonStrict.parse(JsonArraySerializer, "[    ]", useStreaming))
        assertEquals(JsonArray(emptyList()), nonStrict.parse(JsonArraySerializer, "[\n\n]", useStreaming))
        assertEquals(JsonArray(emptyList()), nonStrict.parse(JsonArraySerializer, "[     \t]", useStreaming))
    }

    @Test
    fun testWhitespaces() = parametrizedTest { useStreaming ->
        assertEquals(
            JsonArray(listOf(1, 2, 3, 4, 5).map(::JsonLiteral)),
            nonStrict.parse(JsonArraySerializer, "[1, 2,   3, \n 4, 5]", useStreaming)
        )
    }

    @Test
    fun testExcessiveCommas() = parametrizedTest { useStreaming ->
        assertFailsWith<JsonParsingException> { nonStrict.parse(JsonArraySerializer, "[a,]", useStreaming) }
        assertFailsWith<JsonParsingException> { nonStrict.parse(JsonArraySerializer, "[,1]", useStreaming) }
        assertFailsWith<JsonParsingException> { nonStrict.parse(JsonArraySerializer, "[,1,]", useStreaming) }
        assertFailsWith<JsonParsingException> { nonStrict.parse(JsonArraySerializer, "[,]", useStreaming) }
        assertFailsWith<JsonParsingException> { nonStrict.parse(JsonArraySerializer, "[,,]", useStreaming) }
        assertFailsWith<JsonParsingException> { nonStrict.parse(JsonArraySerializer, "[,,1]", useStreaming) }
        assertFailsWith<JsonParsingException> { nonStrict.parse(JsonArraySerializer, "[1,,]", useStreaming) }
        assertFailsWith<JsonParsingException> { nonStrict.parse(JsonArraySerializer, "[1,,2]", useStreaming) }
        assertFailsWith<JsonParsingException> { nonStrict.parse(JsonArraySerializer, "[,   ,]", useStreaming) }
        assertFailsWith<JsonParsingException> { nonStrict.parse(JsonArraySerializer, "[,\n,]", useStreaming) }
    }

    private fun prebuiltJson(): JsonArray {
        return jsonArray {
            +JsonLiteral(1)
            +JsonNull
            +jsonArray {
                +JsonLiteral("nested literal")
            }
            +jsonArray { }
            +json {
                "key" to "value"
            }
        }
    }
}
