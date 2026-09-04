/*
 * Copyright 2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*
import kotlin.test.*

/*
 * Disclaimer: tests only verify that various collection serializers implementations
 * deserializes a collection with the expected content when the decoder returns the number
 * of elements it can deserialize in advance.
 */
@OptIn(InternalSerializationApi::class)
class SequentialDecodingTest {

    @Test
    fun mergeCollectionWhenSizeIsKnownInAdvance() {
        fun <E, C, T : C> check(serializer: AbstractCollectionSerializer<E, C, T>, initial: C?, block: (C) -> Unit) {
            val decoder = object : KnownSizeSequentialDecoder<Int>(3) {
                var counter = 1
                override fun decodeInt(): Int = counter++
            }

            val result = serializer.merge(decoder, initial)
            block(result)
        }

        check(ArrayListSerializer(Int.serializer()), arrayListOf(0)) {
            assertEquals(listOf(0, 1, 2, 3), it)
        }
        check(ArrayListSerializer(Int.serializer()), null) {
            assertEquals(listOf(1, 2, 3), it)
        }
        assertFailsWith<IllegalArgumentException> {
            ArrayListSerializer(Int.serializer()).merge(
                IllegalExpectedSizeDecoder, null
            )
        }

        check(HashSetSerializer(Int.serializer()), hashSetOf(0)) {
            assertEquals(setOf(0, 1, 2, 3), it)
        }
        check(HashSetSerializer(Int.serializer()), null) {
            assertEquals(setOf(1, 2, 3), it)
        }
        assertFailsWith<IllegalArgumentException> {
            HashSetSerializer(Int.serializer()).merge(
                IllegalExpectedSizeDecoder, null
            )
        }

        check(LinkedHashSetSerializer(Int.serializer()), hashSetOf(0)) {
            assertEquals(setOf(0, 1, 2, 3), it)
        }
        check(LinkedHashSetSerializer(Int.serializer()), null) {
            assertEquals(setOf(1, 2, 3), it)
        }
        assertFailsWith<IllegalArgumentException> {
            LinkedHashSetSerializer(Int.serializer()).merge(
                IllegalExpectedSizeDecoder, null
            )
        }

        check(HashMapSerializer(Int.serializer(), Int.serializer()), hashMapOf(0 to 0)) {
            assertEquals(mapOf(0 to 0, 1 to 2, 3 to 4, 5 to 6), it)
        }
        check(HashMapSerializer(Int.serializer(), Int.serializer()), null) {
            assertEquals(mapOf(1 to 2, 3 to 4, 5 to 6), it)
        }
        assertFailsWith<IllegalArgumentException> {
            HashMapSerializer(Int.serializer(), Int.serializer()).merge(
                IllegalExpectedSizeDecoder, null
            )
        }

        check(LinkedHashMapSerializer(Int.serializer(), Int.serializer()), hashMapOf(0 to 0)) {
            assertEquals(mapOf(0 to 0, 1 to 2, 3 to 4, 5 to 6), it)
        }
        check(LinkedHashMapSerializer(Int.serializer(), Int.serializer()), null) {
            assertEquals(mapOf(1 to 2, 3 to 4, 5 to 6), it)
        }
        assertFailsWith<IllegalArgumentException> {
            LinkedHashMapSerializer(Int.serializer(), Int.serializer()).merge(
                IllegalExpectedSizeDecoder, null
            )
        }
    }

    private fun <T> primitiveArrayChecks(
        serializer: PrimitiveArraySerializer<*, T, *>,
        assert: (T?, T?, String?) -> Unit,
        initial: T, expectedForInitial: T, expectedForNull: T
    ) {
        val decoder = PrimitivesDecoder(3)
        assert(expectedForInitial, serializer.merge(decoder, initial), null)
        assert(expectedForNull, serializer.merge(decoder, null), null)
        assertFailsWith<IllegalArgumentException> {
            serializer.merge(IllegalExpectedSizeDecoder, null)
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun mergePrimitiveArraysWhenSizeIsKnownInAdvance() {
        primitiveArrayChecks(
            ByteArraySerializer() as ByteArraySerializer, ::assertContentEquals,
            byteArrayOf(0), byteArrayOf(0, 1, 2, 3), byteArrayOf(4, 5, 6)
        )
        primitiveArrayChecks(
            ShortArraySerializer() as ShortArraySerializer, ::assertContentEquals,
            shortArrayOf(0), shortArrayOf(0, 1, 2, 3), shortArrayOf(4, 5, 6)
        )
        primitiveArrayChecks(
            IntArraySerializer() as IntArraySerializer, ::assertContentEquals,
            intArrayOf(0), intArrayOf(0, 1, 2, 3), intArrayOf(4, 5, 6)
        )
        primitiveArrayChecks(
            LongArraySerializer() as LongArraySerializer, ::assertContentEquals,
            longArrayOf(0), longArrayOf(0, 1, 2, 3), longArrayOf(4, 5, 6)
        )
        primitiveArrayChecks(
            FloatArraySerializer() as FloatArraySerializer, ::assertContentEquals,
            floatArrayOf(0f), floatArrayOf(0f, 1f, 2f, 3f), floatArrayOf(4f, 5f, 6f)
        )
        primitiveArrayChecks(
            DoubleArraySerializer() as DoubleArraySerializer, ::assertContentEquals,
            doubleArrayOf(0.0), doubleArrayOf(0.0, 1.0, 2.0, 3.0), doubleArrayOf(4.0, 5.0, 6.0)
        )
        primitiveArrayChecks(
            CharArraySerializer() as CharArraySerializer, ::assertContentEquals,
            charArrayOf('0'), charArrayOf('0', '1', '2', '3'), charArrayOf('4', '5', '6')
        )
        primitiveArrayChecks(
            UByteArraySerializer() as UByteArraySerializer, ::assertContentEquals,
            ubyteArrayOf(0u), ubyteArrayOf(0u, 1u, 2u, 3u), ubyteArrayOf(4u, 5u, 6u)
        )
        primitiveArrayChecks(
            UShortArraySerializer() as UShortArraySerializer, ::assertContentEquals,
            ushortArrayOf(0u), ushortArrayOf(0u, 1u, 2u, 3u), ushortArrayOf(4u, 5u, 6u)
        )
        primitiveArrayChecks(
            UIntArraySerializer() as UIntArraySerializer, ::assertContentEquals,
            uintArrayOf(0u), uintArrayOf(0u, 1u, 2u, 3u), uintArrayOf(4u, 5u, 6u)
        )
        primitiveArrayChecks(
            ULongArraySerializer() as ULongArraySerializer, ::assertContentEquals,
            ulongArrayOf(0u), ulongArrayOf(0u, 1u, 2u, 3u), ulongArrayOf(4u, 5u, 6u)
        )
    }

}

private abstract class KnownSizeSequentialDecoder<T>(
    private val collectionSize: Int,
) : AbstractDecoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule()

    var beginStructureCalled: Int = 0
        private set
    var endStructureCalled: Int = 0
        private set

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        beginStructureCalled++
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        endStructureCalled++
    }

    override fun decodeSequentially(): Boolean = true
    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = collectionSize
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = TODO()
}

private object IllegalExpectedSizeDecoder : KnownSizeSequentialDecoder<Unit>(-1000)

private class PrimitivesDecoder(size: Int) : KnownSizeSequentialDecoder<Unit>(size) {
    var counter = 1

    private fun inc(): Int = counter++
    override fun decodeBoolean(): Boolean = inc() % 2 != 0
    override fun decodeByte(): Byte = inc().toByte()
    override fun decodeShort(): Short = inc().toShort()
    override fun decodeInt(): Int = inc()
    override fun decodeLong(): Long = inc().toLong()
    override fun decodeFloat(): Float = inc().toFloat()
    override fun decodeDouble(): Double = inc().toDouble()
    override fun decodeChar(): Char = '0' + inc()
}
