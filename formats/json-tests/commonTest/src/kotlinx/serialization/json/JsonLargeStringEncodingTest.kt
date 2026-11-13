/*
 * Copyright 2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.builtins.*
import kotlin.test.*

class JsonLargeStringEncodingTest : JsonTestBase() {
    private val repetitions = 50_000

    @Test
    fun testLargeStringWithEscapes() = parametrizedTest { mode ->
        // Short escapes (\" \\ \n) and a unicode escape (\u0001) between plain letters, so the buffer grows repeatedly
        val value = "a\"b\\c\nd\u0001x".repeat(repetitions)
        val expected = "\"" + "a\\\"b\\\\c\\nd\\u0001x".repeat(repetitions) + "\""

        val encoded = default.encodeToString(String.serializer(), value, mode)
        assertEquals(expected, encoded)
        assertEquals(value, default.decodeFromString(String.serializer(), encoded, mode))
    }

    @Test
    fun testLargeListOfStrings() = parametrizedTest { mode ->
        val value = List(repetitions) { "item\t$it" }
        val expected = List(repetitions) { "\"item\\t$it\"" }.joinToString(",", "[", "]")

        val encoded = default.encodeToString(ListSerializer(String.serializer()), value, mode)
        assertEquals(expected, encoded)
        assertEquals(value, default.decodeFromString(ListSerializer(String.serializer()), encoded, mode))
    }
}
