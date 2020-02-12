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
        val string = default.stringify(JsonNullSerializer, JsonNull, useStreaming)
        assertEquals("null", string)
        assertEquals(JsonNull, default.parse(JsonNullSerializer, string, useStreaming))
    }

    @Test
    fun testTopLevelJsonNullAsElement() = parametrizedTest { useStreaming ->
        val string = default.stringify(JsonElementSerializer, JsonNull, useStreaming)
        assertEquals("null", string)
        assertEquals(JsonNull, default.parse(JsonElementSerializer, string, useStreaming))
    }

    @Test
    fun testTopLevelJsonNullAsPrimitive() = parametrizedTest { useStreaming ->
        val string = default.stringify(JsonPrimitiveSerializer, JsonNull, useStreaming)
        assertEquals("null", string)
        assertEquals(JsonNull, default.parse(JsonPrimitiveSerializer, string, useStreaming))
    }

    @Test
    fun testJsonNullToString() {
        val string = default.stringify(JsonPrimitiveSerializer, JsonNull)
        assertEquals(string, JsonNull.toString())
    }
}
