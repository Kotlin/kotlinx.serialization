/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.encoding

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.modules.*
import kotlin.jvm.*
import kotlin.test.*

class ReorderingCompositeEncoderTest {
    @Test
    fun shouldReorderWell() {
        val first = 42
        val second = "string"
        val third = true
        val value = OuterToBeReordered(Middle(Inner(second, third)), first)

        val encoder = ReorderingEncoder().apply {
            encodeSerializableValue(OuterToBeReordered.serializer(), value)
        }

        assertContentEquals(actual = encoder.encodedValues, expected = listOf(first, second, third))
    }

    private class ReorderingEncoder(
        val encodedValues: MutableList<Any?> = mutableListOf()
    ) : AbstractEncoder() {
        override val serializersModule: SerializersModule
            get() = EmptySerializersModule()

        override fun encodeValue(value: Any) {
            encodedValues.add(value)
        }

        override fun encodeNull() {
            encodedValues.add(null)
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
            return BasicCompositeEncoder().encodeReorderingElements(descriptor) { _, index ->
                // invert fields
                if (index == 0) 1 else 0
            }
        }

        private inner class BasicCompositeEncoder : AbstractEncoder() {
            override val serializersModule: SerializersModule
                get() = EmptySerializersModule()

            override fun encodeValue(value: Any) {
                encodedValues.add(value)
            }

            override fun encodeNull() {
                encodedValues.add(null)
            }
        }
    }

    @Serializable
    private data class OuterToBeReordered(val a: Middle, val b: Int)

    @JvmInline
    @Serializable
    private value class Middle(val i: Inner)

    @Serializable
    private data class Inner(val c: String, val d: Boolean)
}
