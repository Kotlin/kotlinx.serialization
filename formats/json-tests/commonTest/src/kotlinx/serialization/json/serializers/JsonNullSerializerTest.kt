/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.serializers

import kotlinx.serialization.json.*
import kotlinx.serialization.test.assertFailsWithSerialMessage
import kotlinx.serialization.test.assertStringFormAndRestored
import kotlin.test.*

class JsonNullSerializerTest : JsonTestBase() {

    @Test
    fun testJsonNull() = parametrizedTest(default) {
        assertStringFormAndRestored("{\"element\":null}", JsonNullWrapper(JsonNull), JsonNullWrapper.serializer())
    }

    @Test
    fun testJsonNullFailure() = parametrizedTest(default) {
        assertFailsWithSerialMessage("JsonDecodingException", "'null' literal") { default.decodeFromString(JsonNullWrapper.serializer(), "{\"element\":\"foo\"}", JsonTestingMode.STREAMING) }
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
        val string = default.encodeToString(JsonNull.serializer(), JsonNull, jsonTestingMode)
        assertEquals("null", string)
        assertEquals(JsonNull, default.decodeFromString(JsonNull.serializer(), string, jsonTestingMode))
    }

    @Test
    fun testTopLevelJsonNullAsElement() = parametrizedTest { jsonTestingMode ->
        val string = default.encodeToString(JsonElement.serializer(), JsonNull, jsonTestingMode)
        assertEquals("null", string)
        assertEquals(JsonNull, default.decodeFromString(JsonElement.serializer(), string, jsonTestingMode))
    }

    @Test
    fun testTopLevelJsonNullAsPrimitive() = parametrizedTest { jsonTestingMode ->
        val string = default.encodeToString(JsonPrimitive.serializer(), JsonNull, jsonTestingMode)
        assertEquals("null", string)
        assertEquals(JsonNull, default.decodeFromString(JsonPrimitive.serializer(), string, jsonTestingMode))
    }

    @Test
    fun testJsonNullToString() {
        val string = default.encodeToString(JsonPrimitive.serializer(), JsonNull)
        assertEquals(string, JsonNull.toString())
    }
}
