/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.serializers

import kotlinx.serialization.json.*
import kotlinx.serialization.test.assertStringFormAndRestored
import kotlin.test.*

class JsonNullSerializerTest : JsonTestBase() {

    @Test
    fun testJsonNull() = parametrizedTest(default) {
        assertStringFormAndRestored("{\"element\":null}", JsonNullWrapper(JsonNull), JsonNullWrapper.serializer())
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
    fun testTopLevelJsonNull() = parametrizedTest { useStreaming ->
        val string = default.encodeToString(JsonNullSerializer, JsonNull, useStreaming)
        assertEquals("null", string)
        assertEquals(JsonNull, default.decodeFromString(JsonNullSerializer, string, useStreaming))
    }

    @Test
    fun testTopLevelJsonNullAsElement() = parametrizedTest { useStreaming ->
        val string = default.encodeToString(JsonElementSerializer, JsonNull, useStreaming)
        assertEquals("null", string)
        assertEquals(JsonNull, default.decodeFromString(JsonElementSerializer, string, useStreaming))
    }

    @Test
    fun testTopLevelJsonNullAsPrimitive() = parametrizedTest { useStreaming ->
        val string = default.encodeToString(JsonPrimitiveSerializer, JsonNull, useStreaming)
        assertEquals("null", string)
        assertEquals(JsonNull, default.decodeFromString(JsonPrimitiveSerializer, string, useStreaming))
    }

    @Test
    fun testJsonNullToString() {
        val string = default.encodeToString(JsonPrimitiveSerializer, JsonNull)
        assertEquals(string, JsonNull.toString())
    }
}
