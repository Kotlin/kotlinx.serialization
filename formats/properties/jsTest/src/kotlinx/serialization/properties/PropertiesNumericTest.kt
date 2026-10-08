/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.properties

import kotlinx.serialization.*
import kotlin.test.*

private const val MAX_SAFE_INTEGER: Double = 9007199254740991.0 // 2^53 - 1

class PropertiesJsNumericTest {

    private fun assertRejected(property: String, value: Any) {
        assertFailsWith<SerializationException>("Expected $value to be rejected for $property") {
            Properties.decodeFromMap(IntegralHolder.serializer(), mapOf(property to value))
        }
    }

    private fun assertAccepted(property: String, value: Any, expected: IntegralHolder) {
        assertEquals(
            expected,
            Properties.decodeFromMap(IntegralHolder.serializer(), mapOf(property to value))
        )
    }

    @Test
    fun testAcceptsIntegralBoundaries() {
        assertAccepted("byte", Byte.MIN_VALUE.toDouble(), IntegralHolder(byte = Byte.MIN_VALUE))
        assertAccepted("byte", Byte.MAX_VALUE.toDouble(), IntegralHolder(byte = Byte.MAX_VALUE))
        assertAccepted("short", Short.MIN_VALUE.toDouble(), IntegralHolder(short = Short.MIN_VALUE))
        assertAccepted("short", Short.MAX_VALUE.toDouble(), IntegralHolder(short = Short.MAX_VALUE))
        assertAccepted("int", Int.MIN_VALUE.toDouble(), IntegralHolder(int = Int.MIN_VALUE))
        assertAccepted("int", Int.MAX_VALUE.toDouble(), IntegralHolder(int = Int.MAX_VALUE))
        assertAccepted("long", -MAX_SAFE_INTEGER, IntegralHolder(long = -MAX_SAFE_INTEGER.toLong()))
        assertAccepted("long", MAX_SAFE_INTEGER, IntegralHolder(long = MAX_SAFE_INTEGER.toLong()))
    }

    @Test
    fun testRejectsFractionalIntegralValues() {
        assertRejected("byte", 1.23)
        assertRejected("short", 1.23)
        assertRejected("int", 1.23)
        assertRejected("long", 1.23)
    }

    @Test
    fun testRejectsNonFiniteIntegralValues() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { value ->
            assertRejected("byte", value)
            assertRejected("short", value)
            assertRejected("int", value)
            assertRejected("long", value)
        }
    }

    @Test
    fun testRejectsOutOfRangeIntegralValues() {
        assertRejected("byte", Byte.MIN_VALUE.toDouble() - 1)
        assertRejected("byte", Byte.MAX_VALUE.toDouble() + 1)
        assertRejected("short", Short.MIN_VALUE.toDouble() - 1)
        assertRejected("short", Short.MAX_VALUE.toDouble() + 1)
        assertRejected("int", Int.MIN_VALUE.toDouble() - 1)
        assertRejected("int", Int.MAX_VALUE.toDouble() + 1)
        assertRejected("long", Long.MIN_VALUE.toDouble())
        assertRejected("long", Long.MAX_VALUE.toDouble())
    }

    @Test
    fun testRejectsUnsafeLongValues() {
        assertRejected("long", -MAX_SAFE_INTEGER - 1)
        assertRejected("long", MAX_SAFE_INTEGER + 1)
    }
}
