/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.serializers

import kotlinx.serialization.json.*
import kotlinx.serialization.json.internal.*
import kotlin.test.*

class JsonNullSerializerTest : JsonTestBase() {

    @Test
    fun testJsonNull() = parametrizedTest { useStreaming ->
        val wrapper = JsonNullWrapper(JsonNull)
        val string = strict.stringify(wrapper, useStreaming)
        assertEquals("{\"element\":null}", string)
        assertEquals(JsonNullWrapper(JsonNull), strict.parse(string, useStreaming))
    }

    @Test
    fun testJsonNullAsElement() = parametrizedTest { useStreaming ->
        val wrapper = JsonElementWrapper(JsonNull)
        val string = strict.stringify(wrapper, useStreaming)
        assertEquals("{\"element\":null}", string)
        assertEquals(JsonElementWrapper(JsonNull), strict.parse(string, useStreaming))
    }

    @Test
    fun testJsonNullAsPrimitive() = parametrizedTest { useStreaming ->
        val wrapper = JsonPrimitiveWrapper(JsonNull)
        val string = strict.stringify(wrapper, useStreaming)
        assertEquals("{\"primitive\":null}", string)
        assertEquals(JsonPrimitiveWrapper(JsonNull), strict.parse(string, useStreaming))
    }

    @Test // TODO Top-level nulls are not supported in tagged encoders
    fun testTopLevelJsonNull() { // parametrizedTest { useStreaming ->
        val string = strict.stringify(JsonNullSerializer, JsonNull)
        assertEquals("null", string)
        assertEquals(JsonNull, strict.parse(JsonNullSerializer, string))
    }

    @Test // TODO Top-level nulls are not supported in tagged encoders
    fun testTopLevelJsonNullAsElement() { // parametrizedTest { useStreaming ->
        val string = strict.stringify(JsonElementSerializer, JsonNull)
        assertEquals("null", string)
        assertEquals(JsonNull, strict.parse(JsonElementSerializer, string))
    }

    @Test // TODO Top-level nulls are not supported in tagged encoders
    fun testTopLevelJsonNullAsPrimitive() { // parametrizedTest { useStreaming ->
        val string = strict.stringify(JsonPrimitiveSerializer, JsonNull)
        assertEquals("null", string)
        assertEquals(JsonNull, strict.parse(JsonPrimitiveSerializer, string))
    }

    @Test
    fun testJsonNullToString() {
        val string = strict.stringify(JsonPrimitiveSerializer, JsonNull)

        assertEquals(string, JsonNull.toString())
    }
}