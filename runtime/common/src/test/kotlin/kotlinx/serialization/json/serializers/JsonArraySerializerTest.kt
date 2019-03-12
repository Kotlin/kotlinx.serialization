/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.serializers

import kotlinx.serialization.json.*
import kotlinx.serialization.json.internal.*
import kotlin.test.*

class JsonArraySerializerTest : JsonTestBase() {

    private val expected = "{\"array\":[1,null,[\"nested literal\"],[],{\"key\":\"value\"}]}"
    private val expectedTopLevel = "[1,null,[\"nested literal\"],[],{\"key\":\"value\"}]"

    @Test
    fun testJsonArray() = parametrizedTest { useStreaming ->
        val wrapper = JsonArrayWrapper(prebuiltJson())
        val string = strict.stringify(wrapper, useStreaming)
        assertEquals(expected, string)
        val actual: JsonArrayWrapper = strict.parse(string, useStreaming)
        assertEquals(wrapper, actual)
    }

    @Test
    fun testJsonArrayAsElement() = parametrizedTest { useStreaming ->
        val wrapper = JsonElementWrapper(prebuiltJson())
        val string = strict.stringify(wrapper, useStreaming)
        assertEquals(expected.replace("array", "element"), string)
        assertEquals(wrapper, strict.parse(string, useStreaming))
    }

    @Test // TODO Top-level nulls are not supported in tagged encoders
    fun testTopLevelJsonObject() { // parametrizedTest { useStreaming ->
        val string = strict.stringify(JsonArraySerializer, prebuiltJson())
        assertEquals(expectedTopLevel, string)
        assertEquals(prebuiltJson(), strict.parse(JsonArraySerializer, string))
    }

    @Test // TODO Top-level nulls are not supported in tagged encoders
    fun testTopLevelJsonObjectAsElement() {
        val string = strict.stringify(JsonArraySerializer, prebuiltJson())
        assertEquals(expectedTopLevel, string)
        assertEquals(prebuiltJson(), strict.parse(JsonArraySerializer, string))
    }

    @Test
    fun testJsonArrayToString() {
        val prebuiltJson = prebuiltJson()
        val string = nonStrict.stringify(JsonArraySerializer, prebuiltJson)
        assertEquals(string, prebuiltJson.toString())
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
