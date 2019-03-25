/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.serializers

import kotlinx.serialization.json.*
import kotlinx.serialization.test.assertStringFormAndRestored
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
