/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlin.test.*

class JsonArrayBuilderTest : JsonTestBase() {
    @Test
    fun testExistingOverloads() {
        val element: JsonElement = JsonPrimitive("element")
        val json = buildJsonArray {
            add(1)
            add(Long.MAX_VALUE)
            add(1.5)
            add(true)
            add("1.2300")
            add(element)
            add(null)
        }
        assertJsonFormAndRestored(
            JsonArray.serializer(), json, "[1,9223372036854775807,1.5,true,\"1.2300\",\"element\",null]"
        )
    }

    @Test
    fun testSpecialFloatingPointValidationUnchanged() {
        val json = buildJsonArray { add(Double.NaN) }
        assertFailsWith<SerializationException> { default.encodeToString(JsonArray.serializer(), json) }
        assertEquals("[NaN]", lenient.encodeToString(JsonArray.serializer(), json))
    }
}
