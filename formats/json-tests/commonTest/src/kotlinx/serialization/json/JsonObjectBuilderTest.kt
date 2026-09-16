/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlin.test.*

class JsonObjectBuilderTest : JsonTestBase() {
    @Test
    fun testExistingOverloads() {
        val json = buildJsonObject {
            put("int", 1)
            put("long", Long.MAX_VALUE)
            put("double", 1.5)
            put("boolean", true)
            put("string", "1.2300")
        }
        parametrizedTest { mode ->
            assertEquals(
                "{\"int\":1,\"long\":9223372036854775807,\"double\":1.5," +
                    "\"boolean\":true,\"string\":\"1.2300\"}",
                default.encodeToString(JsonObject.serializer(), json, mode), "mode:$mode"
            )
        }
    }

    @Test
    fun testSpecialFloatingPointValidationUnchanged() {
        val json = buildJsonObject { put("number", Double.NaN) }
        assertFailsWith<SerializationException> { default.encodeToString(JsonObject.serializer(), json) }
        assertEquals("{\"number\":NaN}", lenient.encodeToString(JsonObject.serializer(), json))
    }
}
