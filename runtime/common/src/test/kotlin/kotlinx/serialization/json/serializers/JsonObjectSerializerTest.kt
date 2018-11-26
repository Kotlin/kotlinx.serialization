/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.serializers

import kotlinx.serialization.json.*
import kotlin.test.*

class JsonObjectSerializerTest : JsonTestBase() {

    private val expected = "{element:{literal:1.0,nullKey:null,nested:{\"another literal\":\"some value\"}}}"
    private val expectedTopLevel = "{literal:1.0,nullKey:null,nested:{\"another literal\":\"some value\"}}"

    @Test
    fun testJsonObject() = parametrizedTest { useStreaming ->
        val wrapper = JsonObjectWrapper(prebuiltJson())
        val string = unquoted.stringify(wrapper, useStreaming)
        assertEquals(expected, string)
        assertEquals(wrapper, unquoted.parse(string, useStreaming))
    }

    @Test
    fun testJsonObjectAsElement() = parametrizedTest { useStreaming ->
        val wrapper = JsonElementWrapper(prebuiltJson())
        val string = unquoted.stringify(wrapper, useStreaming)
        assertEquals(expected, string)
        assertEquals(wrapper, unquoted.parse(string, useStreaming))
    }

    @Test // TODO Top-level nulls are not supported in tagged encoders
    fun testTopLevelJsonObject() { // parametrizedTest { useStreaming ->
        val string = unquoted.stringify(JsonObjectSerializer, prebuiltJson())
        assertEquals(expectedTopLevel, string)
        assertEquals(prebuiltJson(), unquoted.parse(JsonObjectSerializer, string))
    }

    @Test // TODO Top-level nulls are not supported in tagged encoders
    fun testTopLevelJsonObjectAsElement() {
        val string = unquoted.stringify(JsonElementSerializer, prebuiltJson())
        assertEquals(expectedTopLevel, string)
        assertEquals(prebuiltJson(), unquoted.parse(JsonElementSerializer, string))
    }

    private fun prebuiltJson(): JsonObject {
        return json {
            "literal" to 1.0
            content["nullKey"] = JsonNull
            "nested" to json {
                "another literal" to "some value"
            }
        }
    }
}
