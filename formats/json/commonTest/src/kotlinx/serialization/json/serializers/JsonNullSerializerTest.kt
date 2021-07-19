/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.serializers

import kotlinx.serialization.json.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.test.assertStringFormAndRestored
import kotlin.test.*

class JsonNullSerializerTest : JsonTestBase() {

    @Test
    fun testJsonNull() = parametrizedTest(default) {
        assertStringFormAndRestored("{\"element\":null}", JsonNullWrapper(JsonNull), JsonNullWrapper.serializer())
    }

    @Test
    fun testJsonNullFailure() = parametrizedTest(default) {
        val t = assertFailsWith<JsonException> { default.decodeFromString(JsonNullWrapper.serializer(), "{\"element\":\"foo\"}", JsonTestingMode.STREAMING) }
        assertTrue { t.message!!.contains("'null' literal") }
    }

    @Test
    fun testJsonNullAsElement() = parametrizedTest(default) {
        assertStringFormAndRestored("{\"element\":null}", JsonElementWrapper(JsonNull), JsonElementWrapper.serializer())
    }

    @Test
    fun testJsonNullAsPrimitive() = parametrizedTest(default) {
        assertStringFormAndRestored("{\"primitive\":null}", JsonPrimitiveWrapper(JsonNull), JsonPrimitiveWrapper.serializer())
    }

    @Test
    fun testTopLevelJsonNull() = parametrizedTest { jsonTestingMode ->
        val string = default.encodeToString(JsonNullSerializer, JsonNull, jsonTestingMode)
        assertEquals("null", string)
        assertEquals(JsonNull, default.decodeFromString(JsonNullSerializer, string, jsonTestingMode))
    }

    @Test
    fun testTopLevelJsonNullAsElement() = parametrizedTest { jsonTestingMode ->
        val string = default.encodeToString(JsonElementSerializer, JsonNull, jsonTestingMode)
        assertEquals("null", string)
        assertEquals(JsonNull, default.decodeFromString(JsonElementSerializer, string, jsonTestingMode))
    }

    @Test
    fun testTopLevelJsonNullAsPrimitive() = parametrizedTest { jsonTestingMode ->
        val string = default.encodeToString(JsonPrimitiveSerializer, JsonNull, jsonTestingMode)
        assertEquals("null", string)
        assertEquals(JsonNull, default.decodeFromString(JsonPrimitiveSerializer, string, jsonTestingMode))
    }

    @Test
    fun testJsonNullToString() {
        val string = default.encodeToString(JsonPrimitiveSerializer, JsonNull)
        assertEquals(string, JsonNull.toString())
    }
}
