/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.*
import kotlin.test.*

/**
 * [PluginGeneratedSerialDescriptor] instances created without a generated serializer
 * (e.g. by the `@Serializer(forClass = ...)` companion shortcut) have no child serializers,
 * so identity operations must not touch element descriptors. Historically hashCode(),
 * equals() and toString() threw [IndexOutOfBoundsException] for such descriptors,
 * which made them unusable as cache keys (see the useAlternativeNames limitation).
 */
class IncompletePluginDescriptorTest {

    @Serializable
    @SerialName("IncompleteTestShape")
    data class CompleteTwin(val a: Int, val b: String)

    private fun incomplete(name: String, vararg elements: String) =
        PluginGeneratedSerialDescriptor(name, generatedSerializer = null, elementsCount = elements.size).apply {
            elements.forEach { addElement(it) }
        }

    @Test
    fun testHashCodeDoesNotThrowAndIsStable() {
        val descriptor = incomplete("IncompleteTestShape", "a", "b")
        val first = descriptor.hashCode()
        assertEquals(first, descriptor.hashCode())
    }

    @Test
    fun testToStringDoesNotThrow() {
        val descriptor = incomplete("IncompleteTestShape", "a", "b")
        val repr = descriptor.toString()
        assertTrue("IncompleteTestShape" in repr)
        assertTrue("a" in repr)
    }

    @Test
    fun testStructurallyEqualIncompleteDescriptors() {
        val d1 = incomplete("IncompleteTestShape", "a", "b")
        val d2 = incomplete("IncompleteTestShape", "a", "b")
        assertEquals(d1, d2)
        assertEquals(d2, d1)
        assertEquals(d1.hashCode(), d2.hashCode())
    }

    @Test
    fun testDifferentElementNamesNotEqual() {
        val d1 = incomplete("IncompleteTestShape", "a", "b")
        val d2 = incomplete("IncompleteTestShape", "a", "c")
        assertNotEquals(d1, d2)
    }

    @Test
    fun testIncompleteVsCompleteNotEqualAndDoesNotThrow() {
        val complete = CompleteTwin.serializer().descriptor
        val partial = incomplete("IncompleteTestShape", "a", "b")
        // Same serial name, same element names — but different construction.
        // Mixed comparison used to throw; now it must simply be unequal.
        assertNotEquals<Any>(complete, partial)
        assertNotEquals<Any>(partial, complete)
    }

    @Test
    fun testCompleteDescriptorBehaviorUnchanged() {
        val d1 = CompleteTwin.serializer().descriptor
        assertEquals(d1, d1)
        assertEquals(d1.hashCode(), d1.hashCode())
        assertTrue("a: " in d1.toString())
    }
}
