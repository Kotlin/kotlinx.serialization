/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.serializers

import kotlinx.serialization.json.*
import kotlinx.serialization.test.assertStringFormAndRestored
import kotlin.test.*

class JsonNullSerializerTest : JsonTestBase() {

    @Test
    fun testJsonNull() = parametrizedTest(strict) {
        assertStringFormAndRestored("{\"element\":null}", JsonNullWrapper(JsonNull), JsonNullWrapper.serializer())
    }

    @Test
    fun testJsonNullAsElement() = parametrizedTest(strict) {
        assertStringFormAndRestored("{\"element\":null}", JsonElementWrapper(JsonNull), JsonElementWrapper.serializer())
    }

    @Test
    fun testJsonNullAsPrimitive() = parametrizedTest(strict) {
        assertStringFormAndRestored("{\"primitive\":null}", JsonPrimitiveWrapper(JsonNull), JsonPrimitiveWrapper.serializer())
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
