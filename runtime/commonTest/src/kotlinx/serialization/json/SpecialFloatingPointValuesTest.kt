/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.test.*
import kotlin.test.*

class SpecialFloatingPointValuesTest : JsonTestBase() {

    @Serializable
    data class Box(val d: Double, val f: Float) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as Box
            if (d != other.d && !(d.isNaN() && other.d.isNaN())) return false
            if (f != other.f && !(f.isNaN() && other.f.isNaN())) return false
            return true
        }

        override fun hashCode(): Int {
            var result = d.hashCode()
            result = 31 * result + f.hashCode()
            return result
        }
    }

    val json = Json { serializeSpecialFloatingPointValues = true }

    @Test
    fun testNans() = parametrizedTest {
        test(Box(Double.NaN, Float.NaN), """{"d":NaN,"f":NaN}""", it)
        noJs { // Number formatting
            test(Box(0.0, Float.NaN), """{"d":0.0,"f":NaN}""", it)
            test(Box(Double.NaN, 0.0f), """{"d":NaN,"f":0.0}""", it)
        }
    }

    @Test
    fun testInfinities() = parametrizedTest {
        test(Box(Double.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY), """{"d":-Infinity,"f":Infinity}""", it)
        test(Box(Double.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY), """{"d":Infinity,"f":-Infinity}""", it)
    }

    private fun test(box: Box, expected: String, useStreaming: Boolean) {
        assertFailsWith<JsonException> { strict.stringify(Box.serializer(), box, useStreaming) }
        assertEquals(expected, json.stringify(Box.serializer(), box, useStreaming))
        assertEquals(box, json.parse(Box.serializer(), expected, useStreaming))
        assertEquals(box, strict.parse(Box.serializer(), expected, useStreaming))

    }

    private inline fun noJs(test: () -> Unit) {
        if (!isJs()) test()
    }
}
