/*
 * Copyright 2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlin.test.*

@OptIn(CoreFriendModuleApi::class)
class ArrayCapacityTest {
    private val maxCapacity = Int.MAX_VALUE - 8

    @Test
    fun testGrowsFromEmptyArray() {
        assertEquals(1, newArrayCapacity(0, 1))
        assertEquals(10, newArrayCapacity(0, 10))
    }

    @Test
    fun testGrowsFromSingleElementArray() {
        assertEquals(2, newArrayCapacity(1, 2))
    }

    @Test
    fun testGrowsByOneAndAHalf() {
        assertEquals(12, newArrayCapacity(8, 9))
        assertEquals(48, newArrayCapacity(32, 33))
        assertEquals(1_536, newArrayCapacity(1_024, 1_025))
    }

    @Test
    fun testNeverGrowsBelowRequiredCapacity() {
        assertEquals(100, newArrayCapacity(8, 100))
        assertEquals(maxCapacity, newArrayCapacity(8, maxCapacity))
    }

    @Test
    fun testZeroRequiredCapacity() {
        assertEquals(0, newArrayCapacity(0, 0))
    }

    @Test
    fun testCapsAtMaximumCapacity() {
        assertEquals(1_610_612_736, newArrayCapacity(1_073_741_824, 1_073_741_825))
        assertEquals(maxCapacity, newArrayCapacity(1_500_000_000, 1_500_000_001))
        assertEquals(maxCapacity, newArrayCapacity(maxCapacity - 1, maxCapacity))
    }

    @Test
    fun testRequiredCapacityAboveMaximumThrows() {
        assertFailsWith<SerializationException> { newArrayCapacity(maxCapacity, maxCapacity + 1) }
        assertFailsWith<SerializationException> { newArrayCapacity(maxCapacity, Int.MAX_VALUE) }
    }

    @Test
    fun testOverflowedRequiredCapacityThrows() {
        val overflowedPosition = maxCapacity + 100 // e.g. 'position + count' at the call site
        assertTrue(overflowedPosition < 0)
        assertFailsWith<SerializationException> { newArrayCapacity(maxCapacity, overflowedPosition) }
        assertFailsWith<SerializationException> { newArrayCapacity(0, Int.MIN_VALUE) }
    }
}
