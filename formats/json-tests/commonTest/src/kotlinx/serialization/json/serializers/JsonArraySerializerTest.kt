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
    fun testJsonArray() = parametrizedTest(default) {
        assertStringFormAndRestored(expected, JsonArrayWrapper(prebuiltJson()), JsonArrayWrapper.serializer())
    }

    @Test
    fun testJsonArrayAsElement() = parametrizedTest(default) {
        assertStringFormAndRestored(expected.replace("array", "element"), JsonElementWrapper(prebuiltJson()), JsonElementWrapper.serializer())
    }

    @Test
    fun testTopLevelJsonObjectAsElement() = parametrizedTest(default) {
        assertStringFormAndRestored(expectedTopLevel, prebuiltJson(), JsonElement.serializer())
    }

    @Test
    fun testJsonArrayToString() {
        val prebuiltJson = prebuiltJson()
        val string = lenient.encodeToString(JsonArray.serializer(), prebuiltJson)
        assertEquals(string, prebuiltJson.toString())
    }

    @Test
    fun testMixedLiterals() = parametrizedTest { jsonTestingMode ->
        val json = """[1, "2", 3, "4"]"""
        val array = default.decodeFromString(JsonArray.serializer(), json, jsonTestingMode)
        array.forEachIndexed { index, element ->
            require(element is JsonPrimitive)
            assertEquals(index % 2 == 1, element.isString)
        }
    }

    @Test
    fun testMissingCommas() = parametrizedTest { jsonTestingMode ->
        val message = "Expected end of the array or comma"
        testFails("[a b c]", message, jsonTestingMode)
        testFails("[ 1 2 3 ]", message, jsonTestingMode)
        testFails("[null 1 null]", message, jsonTestingMode)
        testFails("[1 \n 2]", message, jsonTestingMode)
    }

    @Test
    fun testEmptyArray() = parametrizedTest { jsonTestingMode ->
        assertEquals(JsonArray(emptyList()), lenient.decodeFromString(JsonArray.serializer(), "[]", jsonTestingMode))
        assertEquals(JsonArray(emptyList()), lenient.decodeFromString(JsonArray.serializer(), "[    ]", jsonTestingMode))
        assertEquals(JsonArray(emptyList()), lenient.decodeFromString(JsonArray.serializer(), "[\n\n]", jsonTestingMode))
        assertEquals(JsonArray(emptyList()), lenient.decodeFromString(JsonArray.serializer(), "[     \t]", jsonTestingMode))
    }

    @Test
    fun testWhitespaces() = parametrizedTest { jsonTestingMode ->
        assertEquals(
            JsonArray(listOf(1, 2, 3, 4, 5).map(::JsonPrimitive)),
            lenient.decodeFromString(JsonArray.serializer(), "[1, 2,   3, \n 4, 5]", jsonTestingMode)
        )
    }

    @Test
    fun testExcessiveCommas() = parametrizedTest { jsonTestingMode ->
        val trailing = "Unexpected trailing comma"
        val leading = "Unexpected leading comma"
        testFails("[a,]", trailing, jsonTestingMode)
        testFails("[,1]", leading, jsonTestingMode)
        testFails("[,1,]", leading, jsonTestingMode)
        testFails("[,]", leading, jsonTestingMode)
        testFails("[,,]", leading, jsonTestingMode)
        testFails("[,,1]", leading, jsonTestingMode)
        testFails("[1,,]", trailing, jsonTestingMode)
        testFails("[1,,2]", trailing, jsonTestingMode)
        testFails("[,   ,]", leading, jsonTestingMode)
        testFails("[,\n,]", leading, jsonTestingMode)
    }

    private fun testFails(input: String, errorMessage: String, jsonTestingMode: JsonTestingMode) {
        assertFailsWithSerial("JsonDecodingException", errorMessage) {
            lenient.decodeFromString(
                JsonArray.serializer(),
                input,
                jsonTestingMode
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
