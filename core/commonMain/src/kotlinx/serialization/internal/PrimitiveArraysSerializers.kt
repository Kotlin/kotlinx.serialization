/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("DEPRECATION_ERROR")
package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.encoding.*

private const val INITIAL_SIZE = 10

/**
 * Serializer for [ByteArray].
 *
 * Encode elements one-by-one, as regular list,
 * unless format's Encoder/Decoder have special handling for this serializer.
 */
@PublishedApi
internal object ByteArraySerializer : KSerializer<ByteArray>,
    PrimitiveArraySerializer<Byte, ByteArray, ByteArrayBuilder>(Byte.serializer()) {

    override fun ByteArray.collectionSize(): Int = size
    override fun ByteArray.toBuilder(): ByteArrayBuilder = ByteArrayBuilder(this)
    override fun ofSize(size: Int): ByteArrayBuilder = ByteArrayBuilder(size)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: ByteArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeByteElement(descriptor, index))
    }

    override fun writeContent(encoder: CompositeEncoder, content: ByteArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeByteElement(descriptor, i, content[i])
    }
}

@PublishedApi
internal class ByteArrayBuilder private constructor(
    bufferWithData: ByteArray, initialPosition: Int
) : PrimitiveArrayBuilder<ByteArray>() {
    internal constructor(bufferWithData: ByteArray) : this(bufferWithData, bufferWithData.size) {
        ensureCapacity(INITIAL_SIZE)
    }
    internal constructor(initialCapacity: Int) : this(ByteArray(initialCapacity), 0)

    private var buffer: ByteArray = bufferWithData
    override var position: Int = initialPosition
        private set

    override fun ensureCapacity(requiredCapacity: Int) {
        if (buffer.size < requiredCapacity)
            buffer = buffer.copyOf(requiredCapacity.coerceAtLeast(buffer.size * 2))
    }

    internal fun append(c: Byte) {
        ensureCapacity()
        buffer[position++] = c
    }

    override fun build() = buffer.copyOf(position)
}

// the rest of the serializers are merely copy-paste
/**
 * Serializer for [ShortArray].
 *
 * Encode elements one-by-one, as regular list,
 * unless format's Encoder/Decoder have special handling for this serializer.
 */
@PublishedApi
internal object ShortArraySerializer : KSerializer<ShortArray>,
    PrimitiveArraySerializer<Short, ShortArray, ShortArrayBuilder>(Short.serializer()) {

    override fun ShortArray.collectionSize(): Int = size
    override fun ShortArray.toBuilder(): ShortArrayBuilder = ShortArrayBuilder(this)
    override fun ofSize(size: Int): ShortArrayBuilder = ShortArrayBuilder(size)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: ShortArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeShortElement(descriptor, index))
    }

    override fun writeContent(encoder: CompositeEncoder, content: ShortArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeShortElement(descriptor, i, content[i])
    }
}

@PublishedApi
internal class ShortArrayBuilder private constructor(
    bufferWithData: ShortArray, initialPosition: Int
) : PrimitiveArrayBuilder<ShortArray>() {
    internal constructor(bufferWithData: ShortArray) : this(bufferWithData, bufferWithData.size) {
        ensureCapacity(INITIAL_SIZE)
    }
    internal constructor(initialCapacity: Int) : this(ShortArray(initialCapacity), 0)

    private var buffer: ShortArray = bufferWithData
    override var position: Int = initialPosition
        private set

    override fun ensureCapacity(requiredCapacity: Int) {
        if (buffer.size < requiredCapacity)
            buffer = buffer.copyOf(requiredCapacity.coerceAtLeast(buffer.size * 2))
    }

    internal fun append(c: Short) {
        ensureCapacity()
        buffer[position++] = c
    }

    override fun build() = buffer.copyOf(position)
}

/**
 * Serializer for [IntArray].
 *
 * Encode elements one-by-one, as regular list,
 * unless format's Encoder/Decoder have special handling for this serializer.
 */
@PublishedApi
internal object IntArraySerializer : KSerializer<IntArray>,
    PrimitiveArraySerializer<Int, IntArray, IntArrayBuilder>(Int.serializer()) {

    override fun IntArray.collectionSize(): Int = size
    override fun IntArray.toBuilder(): IntArrayBuilder = IntArrayBuilder(this)
    override fun ofSize(size: Int): IntArrayBuilder = IntArrayBuilder(size)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: IntArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeIntElement(descriptor, index))
    }

    override fun writeContent(encoder: CompositeEncoder, content: IntArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeIntElement(descriptor, i, content[i])
    }
}

@PublishedApi
internal class IntArrayBuilder private constructor(
    bufferWithData: IntArray, initialPosition: Int
) : PrimitiveArrayBuilder<IntArray>() {
    internal constructor(bufferWithData: IntArray) : this(bufferWithData, bufferWithData.size) {
        ensureCapacity(INITIAL_SIZE)
    }
    internal constructor(initialCapacity: Int) : this(IntArray(initialCapacity), 0)

    private var buffer: IntArray = bufferWithData
    override var position: Int = initialPosition
        private set

    override fun ensureCapacity(requiredCapacity: Int) {
        if (buffer.size < requiredCapacity)
            buffer = buffer.copyOf(requiredCapacity.coerceAtLeast(buffer.size * 2))
    }

    internal fun append(c: Int) {
        ensureCapacity()
        buffer[position++] = c
    }

    override fun build() = buffer.copyOf(position)
}

/**
 * Serializer for [LongArray].
 *
 * Encode elements one-by-one, as regular list,
 * unless format's Encoder/Decoder have special handling for this serializer.
 */
@PublishedApi
internal object LongArraySerializer : KSerializer<LongArray>,
    PrimitiveArraySerializer<Long, LongArray, LongArrayBuilder>(Long.serializer()) {

    override fun LongArray.collectionSize(): Int = size
    override fun LongArray.toBuilder(): LongArrayBuilder = LongArrayBuilder(this)
    override fun ofSize(size: Int): LongArrayBuilder = LongArrayBuilder(size)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: LongArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeLongElement(descriptor, index))
    }

    override fun writeContent(encoder: CompositeEncoder, content: LongArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeLongElement(descriptor, i, content[i])
    }
}

@PublishedApi
internal class LongArrayBuilder private constructor(
    bufferWithData: LongArray, initialPosition: Int
) : PrimitiveArrayBuilder<LongArray>() {
    internal constructor(bufferWithData: LongArray) : this(bufferWithData, bufferWithData.size) {
        ensureCapacity(INITIAL_SIZE)
    }
    internal constructor(initialCapacity: Int) : this(LongArray(initialCapacity), 0)

    private var buffer: LongArray = bufferWithData
    override var position: Int = initialPosition
        private set

    override fun ensureCapacity(requiredCapacity: Int) {
        if (buffer.size < requiredCapacity)
            buffer = buffer.copyOf(requiredCapacity.coerceAtLeast(buffer.size * 2))
    }

    internal fun append(c: Long) {
        ensureCapacity()
        buffer[position++] = c
    }

    override fun build() = buffer.copyOf(position)
}

/**
 * Serializer for [FloatArray].
 *
 * Encode elements one-by-one, as regular list,
 * unless format's Encoder/Decoder have special handling for this serializer.
 */
@PublishedApi
internal object FloatArraySerializer : KSerializer<FloatArray>,
    PrimitiveArraySerializer<Float, FloatArray, FloatArrayBuilder>(Float.serializer()) {

    override fun FloatArray.collectionSize(): Int = size
    override fun FloatArray.toBuilder(): FloatArrayBuilder = FloatArrayBuilder(this)
    override fun ofSize(size: Int): FloatArrayBuilder = FloatArrayBuilder(size)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: FloatArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeFloatElement(descriptor, index))
    }

    override fun writeContent(encoder: CompositeEncoder, content: FloatArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeFloatElement(descriptor, i, content[i])
    }
}

@PublishedApi
internal class FloatArrayBuilder private constructor(
    bufferWithData: FloatArray, initialPosition: Int
) : PrimitiveArrayBuilder<FloatArray>() {
    internal constructor(bufferWithData: FloatArray) : this(bufferWithData, bufferWithData.size) {
        ensureCapacity(INITIAL_SIZE)
    }
    internal constructor(initialCapacity: Int) : this(FloatArray(initialCapacity), 0)

    private var buffer: FloatArray = bufferWithData
    override var position: Int = initialPosition
        private set

    override fun ensureCapacity(requiredCapacity: Int) {
        if (buffer.size < requiredCapacity)
            buffer = buffer.copyOf(requiredCapacity.coerceAtLeast(buffer.size * 2))
    }

    internal fun append(c: Float) {
        ensureCapacity()
        buffer[position++] = c
    }

    override fun build() = buffer.copyOf(position)
}

/**
 * Serializer for [DoubleArray].
 *
 * Encode elements one-by-one, as regular list,
 * unless format's Encoder/Decoder have special handling for this serializer.
 */
@PublishedApi
internal object DoubleArraySerializer : KSerializer<DoubleArray>,
    PrimitiveArraySerializer<Double, DoubleArray, DoubleArrayBuilder>(Double.serializer()) {

    override fun DoubleArray.collectionSize(): Int = size
    override fun DoubleArray.toBuilder(): DoubleArrayBuilder = DoubleArrayBuilder(this)
    override fun ofSize(size: Int): DoubleArrayBuilder = DoubleArrayBuilder(size)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: DoubleArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeDoubleElement(descriptor, index))
    }

    override fun writeContent(encoder: CompositeEncoder, content: DoubleArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeDoubleElement(descriptor, i, content[i])
    }
}

@PublishedApi
internal class DoubleArrayBuilder private constructor(
    bufferWithData: DoubleArray, initialPosition: Int
) : PrimitiveArrayBuilder<DoubleArray>() {
    internal constructor(bufferWithData: DoubleArray) : this(bufferWithData, bufferWithData.size) {
        ensureCapacity(INITIAL_SIZE)
    }
    internal constructor(initialCapacity: Int) : this(DoubleArray(initialCapacity), 0)

    private var buffer: DoubleArray = bufferWithData
    override var position: Int = initialPosition
        private set

    override fun ensureCapacity(requiredCapacity: Int) {
        if (buffer.size < requiredCapacity)
            buffer = buffer.copyOf(requiredCapacity.coerceAtLeast(buffer.size * 2))
    }

    internal fun append(c: Double) {
        ensureCapacity()
        buffer[position++] = c
    }

    override fun build() = buffer.copyOf(position)
}

/**
 * Serializer for [CharArray].
 *
 * Encode elements one-by-one, as regular list,
 * unless format's Encoder/Decoder have special handling for this serializer.
 */
@PublishedApi
internal object CharArraySerializer : KSerializer<CharArray>,
    PrimitiveArraySerializer<Char, CharArray, CharArrayBuilder>(Char.serializer()) {

    override fun CharArray.collectionSize(): Int = size
    override fun CharArray.toBuilder(): CharArrayBuilder = CharArrayBuilder(this)
    override fun ofSize(size: Int): CharArrayBuilder = CharArrayBuilder(size)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: CharArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeCharElement(descriptor, index))
    }

    override fun writeContent(encoder: CompositeEncoder, content: CharArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeCharElement(descriptor, i, content[i])
    }
}

@PublishedApi
internal class CharArrayBuilder private constructor(
    bufferWithData: CharArray, initialPosition: Int
) : PrimitiveArrayBuilder<CharArray>() {
    internal constructor(bufferWithData: CharArray) : this(bufferWithData, bufferWithData.size) {
        ensureCapacity(INITIAL_SIZE)
    }
    internal constructor(initialCapacity: Int) : this(CharArray(initialCapacity), 0)

    private var buffer: CharArray = bufferWithData
    override var position: Int = initialPosition
        private set

    override fun ensureCapacity(requiredCapacity: Int) {
        if (buffer.size < requiredCapacity)
            buffer = buffer.copyOf(requiredCapacity.coerceAtLeast(buffer.size * 2))
    }

    internal fun append(c: Char) {
        ensureCapacity()
        buffer[position++] = c
    }

    override fun build() = buffer.copyOf(position)
}

/**
 * Serializer for [BooleanArray].
 * Encode elements one-by-one, as regular list, unless format's Encoder/Decoder have a special support for this serializer.
 */
@PublishedApi
internal object BooleanArraySerializer : KSerializer<BooleanArray>,
    PrimitiveArraySerializer<Boolean, BooleanArray, BooleanArrayBuilder>(Boolean.serializer()) {

    override fun BooleanArray.collectionSize(): Int = size
    override fun BooleanArray.toBuilder(): BooleanArrayBuilder = BooleanArrayBuilder(this)
    override fun ofSize(size: Int): BooleanArrayBuilder = BooleanArrayBuilder(size)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: BooleanArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeBooleanElement(descriptor, index))
    }

    override fun writeContent(encoder: CompositeEncoder, content: BooleanArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeBooleanElement(descriptor, i, content[i])
    }
}

@PublishedApi
internal class BooleanArrayBuilder private constructor(bufferWithData: BooleanArray, initialPosition: Int)
    : PrimitiveArrayBuilder<BooleanArray>() {
    internal constructor(bufferWithData: BooleanArray) : this(bufferWithData, bufferWithData.size) {
        ensureCapacity(INITIAL_SIZE)
    }

    internal constructor(initialCapacity: Int) : this(BooleanArray(initialCapacity), 0)

    private var buffer: BooleanArray = bufferWithData
    override var position: Int = initialPosition
        private set

    override fun ensureCapacity(requiredCapacity: Int) {
        if (buffer.size < requiredCapacity)
            buffer = buffer.copyOf(requiredCapacity.coerceAtLeast(buffer.size * 2))
    }

    internal fun append(c: Boolean) {
        ensureCapacity()
        buffer[position++] = c
    }

    override fun build() = buffer.copyOf(position)
}


// Unsigned arrays

/**
 * Serializer for [UByteArray].
 *
 * Encode elements one-by-one, as regular list,
 * unless format's Encoder/Decoder have special handling for this serializer.
 */
@PublishedApi
@ExperimentalUnsignedTypes
internal object UByteArraySerializer : KSerializer<UByteArray>,
    PrimitiveArraySerializer<UByte, UByteArray, UByteArrayBuilder>(UByte.serializer()) {

    override fun UByteArray.collectionSize(): Int = size
    override fun UByteArray.toBuilder(): UByteArrayBuilder = UByteArrayBuilder(this)
    override fun ofSize(size: Int): UByteArrayBuilder = UByteArrayBuilder(size)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: UByteArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeInlineElement(descriptor, index).decodeByte().toUByte())
    }

    override fun writeContent(encoder: CompositeEncoder, content: UByteArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeInlineElement(descriptor, i).encodeByte(content[i].toByte())
    }
}

@PublishedApi
@ExperimentalUnsignedTypes
internal class UByteArrayBuilder private constructor(
    bufferWithData: UByteArray, initialPosition: Int
) : PrimitiveArrayBuilder<UByteArray>() {
    internal constructor(bufferWithData: UByteArray) : this(bufferWithData, bufferWithData.size) {
        ensureCapacity(INITIAL_SIZE)
    }
    internal constructor(initialCapacity: Int) : this(UByteArray(initialCapacity), 0)

    private var buffer: UByteArray = bufferWithData
    override var position: Int = initialPosition
        private set

    override fun ensureCapacity(requiredCapacity: Int) {
        if (buffer.size < requiredCapacity)
            buffer = buffer.copyOf(requiredCapacity.coerceAtLeast(buffer.size * 2))
    }

    internal fun append(c: UByte) {
        ensureCapacity()
        buffer[position++] = c
    }

    override fun build() = buffer.copyOf(position)
}

/**
 * Serializer for [UShortArray].
 *
 * Encode elements one-by-one, as regular list,
 * unless format's Encoder/Decoder have special handling for this serializer.
 */
@PublishedApi
@ExperimentalUnsignedTypes
internal object UShortArraySerializer : KSerializer<UShortArray>,
    PrimitiveArraySerializer<UShort, UShortArray, UShortArrayBuilder>(UShort.serializer()) {

    override fun UShortArray.collectionSize(): Int = size
    override fun UShortArray.toBuilder(): UShortArrayBuilder = UShortArrayBuilder(this)
    override fun ofSize(size: Int): UShortArrayBuilder = UShortArrayBuilder(size)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: UShortArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeInlineElement(descriptor, index).decodeShort().toUShort())
    }

    override fun writeContent(encoder: CompositeEncoder, content: UShortArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeInlineElement(descriptor, i).encodeShort(content[i].toShort())
    }
}

@PublishedApi
@ExperimentalUnsignedTypes
internal class UShortArrayBuilder private constructor(
    bufferWithData: UShortArray, initialPosition: Int
) : PrimitiveArrayBuilder<UShortArray>() {
    internal constructor(bufferWithData: UShortArray) : this(bufferWithData, bufferWithData.size) {
        ensureCapacity(INITIAL_SIZE)
    }
    internal constructor(initialCapacity: Int) : this(UShortArray(initialCapacity), 0)

    private var buffer: UShortArray = bufferWithData
    override var position: Int = initialPosition
        private set

    override fun ensureCapacity(requiredCapacity: Int) {
        if (buffer.size < requiredCapacity)
            buffer = buffer.copyOf(requiredCapacity.coerceAtLeast(buffer.size * 2))
    }

    internal fun append(c: UShort) {
        ensureCapacity()
        buffer[position++] = c
    }

    override fun build() = buffer.copyOf(position)
}

/**
 * Serializer for [UIntArray].
 *
 * Encode elements one-by-one, as regular list,
 * unless format's Encoder/Decoder have special handling for this serializer.
 */
@PublishedApi
@ExperimentalUnsignedTypes
internal object UIntArraySerializer : KSerializer<UIntArray>,
    PrimitiveArraySerializer<UInt, UIntArray, UIntArrayBuilder>(UInt.serializer()) {

    override fun UIntArray.collectionSize(): Int = size
    override fun UIntArray.toBuilder(): UIntArrayBuilder = UIntArrayBuilder(this)
    override fun ofSize(size: Int): UIntArrayBuilder = UIntArrayBuilder(size)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: UIntArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeInlineElement(descriptor, index).decodeInt().toUInt())
    }

    override fun writeContent(encoder: CompositeEncoder, content: UIntArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeInlineElement(descriptor, i).encodeInt(content[i].toInt())
    }
}

@PublishedApi
@ExperimentalUnsignedTypes
internal class UIntArrayBuilder private constructor(
    bufferWithData: UIntArray, initialPosition: Int
) : PrimitiveArrayBuilder<UIntArray>() {
    internal constructor(bufferWithData: UIntArray) : this(bufferWithData, bufferWithData.size) {
        ensureCapacity(INITIAL_SIZE)
    }
    internal constructor(initialCapacity: Int) : this(UIntArray(initialCapacity), 0)

    private var buffer: UIntArray = bufferWithData
    override var position: Int = initialPosition
        private set

    override fun ensureCapacity(requiredCapacity: Int) {
        if (buffer.size < requiredCapacity)
            buffer = buffer.copyOf(requiredCapacity.coerceAtLeast(buffer.size * 2))
    }

    internal fun append(c: UInt) {
        ensureCapacity()
        buffer[position++] = c
    }

    override fun build() = buffer.copyOf(position)
}

/**
 * Serializer for [ULongArray].
 *
 * Encode elements one-by-one, as regular list,
 * unless format's Encoder/Decoder have special handling for this serializer.
 */
@PublishedApi
@ExperimentalUnsignedTypes
internal object ULongArraySerializer : KSerializer<ULongArray>,
    PrimitiveArraySerializer<ULong, ULongArray, ULongArrayBuilder>(ULong.serializer()) {

    override fun ULongArray.collectionSize(): Int = size
    override fun ULongArray.toBuilder(): ULongArrayBuilder = ULongArrayBuilder(this)
    override fun ofSize(size: Int): ULongArrayBuilder = ULongArrayBuilder(size)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: ULongArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeInlineElement(descriptor, index).decodeLong().toULong())
    }

    override fun writeContent(encoder: CompositeEncoder, content: ULongArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeInlineElement(descriptor, i).encodeLong(content[i].toLong())
    }
}

@PublishedApi
@ExperimentalUnsignedTypes
internal class ULongArrayBuilder private constructor(
    bufferWithData: ULongArray, initialPosition: Int
) : PrimitiveArrayBuilder<ULongArray>() {
    internal constructor(bufferWithData: ULongArray) : this(bufferWithData, bufferWithData.size) {
        ensureCapacity(INITIAL_SIZE)
    }
    internal constructor(initialCapacity: Int) : this(ULongArray(initialCapacity), 0)

    private var buffer: ULongArray = bufferWithData
    override var position: Int = initialPosition
        private set

    override fun ensureCapacity(requiredCapacity: Int) {
        if (buffer.size < requiredCapacity)
            buffer = buffer.copyOf(requiredCapacity.coerceAtLeast(buffer.size * 2))
    }

    internal fun append(c: ULong) {
        ensureCapacity()
        buffer[position++] = c
    }

    override fun build() = buffer.copyOf(position)
}
