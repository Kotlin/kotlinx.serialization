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
@Deprecated(
    message = "Deprecated in the favour of ByteArraySerializer() factory",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("ByteArraySerializer()", imports = ["kotlinx.serialization.builtins.ByteArraySerializer"])
)
public object ByteArraySerializer : KSerializer<ByteArray>,
    PrimitiveArraySerializer<Byte, ByteArray, ByteArrayBuilder>(Byte.serializer()) {

    override fun ByteArray.collectionSize(): Int = size
    override fun ByteArray.toBuilder(): ByteArrayBuilder = ByteArrayBuilder(this)
    override fun empty(): ByteArray = ByteArray(0)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: ByteArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeByteElement(descriptor, index))
    }

    override fun writeContent(encoder: CompositeEncoder, content: ByteArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeByteElement(descriptor, i, content[i])
    }
}

@InternalSerializationApi
@Deprecated(message = "For internal use", level = DeprecationLevel.HIDDEN)
public class ByteArrayBuilder internal constructor(
    bufferWithData: ByteArray
) : PrimitiveArrayBuilder<ByteArray>() {

    private var buffer: ByteArray = bufferWithData
    override var position: Int = bufferWithData.size
        private set

    init {
        ensureCapacity(INITIAL_SIZE)
    }

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
@Deprecated(
    message = "Deprecated in the favour of ShortArraySerializer() factory",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("ShortArraySerializer()", imports = ["kotlinx.serialization.builtins.ShortArraySerializer"])
)
public object ShortArraySerializer : KSerializer<ShortArray>,
    PrimitiveArraySerializer<Short, ShortArray, ShortArrayBuilder>(Short.serializer()) {

    override fun ShortArray.collectionSize(): Int = size
    override fun ShortArray.toBuilder(): ShortArrayBuilder = ShortArrayBuilder(this)
    override fun empty(): ShortArray = ShortArray(0)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: ShortArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeShortElement(descriptor, index))
    }

    override fun writeContent(encoder: CompositeEncoder, content: ShortArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeShortElement(descriptor, i, content[i])
    }
}

@InternalSerializationApi
@Deprecated(message = "For internal use", level = DeprecationLevel.HIDDEN)
public class ShortArrayBuilder internal constructor(
    bufferWithData: ShortArray
) : PrimitiveArrayBuilder<ShortArray>() {

    private var buffer: ShortArray = bufferWithData
    override var position: Int = bufferWithData.size
        private set

    init {
        ensureCapacity(INITIAL_SIZE)
    }

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
@Deprecated(
    message = "Deprecated in the favour of IntArraySerializer() factory",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("IntArraySerializer()", imports = ["kotlinx.serialization.builtins.IntArraySerializer"])
)
public object IntArraySerializer : KSerializer<IntArray>,
    PrimitiveArraySerializer<Int, IntArray, IntArrayBuilder>(Int.serializer()) {

    override fun IntArray.collectionSize(): Int = size
    override fun IntArray.toBuilder(): IntArrayBuilder = IntArrayBuilder(this)
    override fun empty(): IntArray = IntArray(0)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: IntArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeIntElement(descriptor, index))
    }

    override fun writeContent(encoder: CompositeEncoder, content: IntArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeIntElement(descriptor, i, content[i])
    }
}

@InternalSerializationApi
@Deprecated(message = "For internal use", level = DeprecationLevel.HIDDEN)
public class IntArrayBuilder internal constructor(
    bufferWithData: IntArray
) : PrimitiveArrayBuilder<IntArray>() {

    private var buffer: IntArray = bufferWithData
    override var position: Int = bufferWithData.size
        private set

    init {
        ensureCapacity(INITIAL_SIZE)
    }

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
@Deprecated(
    message = "Deprecated in the favour of LongArraySerializer() factory",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("LongArraySerializer()", imports = ["kotlinx.serialization.builtins.LongArraySerializer"])
)
public object LongArraySerializer : KSerializer<LongArray>,
    PrimitiveArraySerializer<Long, LongArray, LongArrayBuilder>(Long.serializer()) {

    override fun LongArray.collectionSize(): Int = size
    override fun LongArray.toBuilder(): LongArrayBuilder = LongArrayBuilder(this)
    override fun empty(): LongArray = LongArray(0)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: LongArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeLongElement(descriptor, index))
    }

    override fun writeContent(encoder: CompositeEncoder, content: LongArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeLongElement(descriptor, i, content[i])
    }
}

@InternalSerializationApi
@Deprecated(message = "For internal use", level = DeprecationLevel.HIDDEN)
public class LongArrayBuilder internal constructor(
    bufferWithData: LongArray
) : PrimitiveArrayBuilder<LongArray>() {

    private var buffer: LongArray = bufferWithData
    override var position: Int = bufferWithData.size
        private set

    init {
        ensureCapacity(INITIAL_SIZE)
    }

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
@Deprecated(
    message = "Deprecated in the favour of FloatArraySerializer() factory",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("FloatArraySerializer()", imports = ["kotlinx.serialization.builtins.FloatArraySerializer"])
)
public object FloatArraySerializer : KSerializer<FloatArray>,
    PrimitiveArraySerializer<Float, FloatArray, FloatArrayBuilder>(Float.serializer()) {

    override fun FloatArray.collectionSize(): Int = size
    override fun FloatArray.toBuilder(): FloatArrayBuilder = FloatArrayBuilder(this)
    override fun empty(): FloatArray = FloatArray(0)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: FloatArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeFloatElement(descriptor, index))
    }

    override fun writeContent(encoder: CompositeEncoder, content: FloatArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeFloatElement(descriptor, i, content[i])
    }
}

@InternalSerializationApi
@Deprecated(message = "For internal use", level = DeprecationLevel.HIDDEN)
public class FloatArrayBuilder internal constructor(
    bufferWithData: FloatArray
) : PrimitiveArrayBuilder<FloatArray>() {

    private var buffer: FloatArray = bufferWithData
    override var position: Int = bufferWithData.size
        private set

    init {
        ensureCapacity(INITIAL_SIZE)
    }

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
@Deprecated(
    message = "Deprecated in the favour of DoubleArraySerializer() factory",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("DoubleArraySerializer()", imports = ["kotlinx.serialization.builtins.DoubleArraySerializer"])
)
public object DoubleArraySerializer : KSerializer<DoubleArray>,
    PrimitiveArraySerializer<Double, DoubleArray, DoubleArrayBuilder>(Double.serializer()) {

    override fun DoubleArray.collectionSize(): Int = size
    override fun DoubleArray.toBuilder(): DoubleArrayBuilder = DoubleArrayBuilder(this)
    override fun empty(): DoubleArray = DoubleArray(0)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: DoubleArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeDoubleElement(descriptor, index))
    }

    override fun writeContent(encoder: CompositeEncoder, content: DoubleArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeDoubleElement(descriptor, i, content[i])
    }
}

@InternalSerializationApi
@Deprecated(message = "For internal use", level = DeprecationLevel.HIDDEN)
public class DoubleArrayBuilder internal constructor(
    bufferWithData: DoubleArray
) : PrimitiveArrayBuilder<DoubleArray>() {

    private var buffer: DoubleArray = bufferWithData
    override var position: Int = bufferWithData.size
        private set

    init {
        ensureCapacity(INITIAL_SIZE)
    }

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
@Deprecated(
    message = "Deprecated in the favour of CharArraySerializer() factory",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("CharArraySerializer()", imports = ["kotlinx.serialization.builtins.CharArraySerializer"])
)
public object CharArraySerializer : KSerializer<CharArray>,
    PrimitiveArraySerializer<Char, CharArray, CharArrayBuilder>(Char.serializer()) {

    override fun CharArray.collectionSize(): Int = size
    override fun CharArray.toBuilder(): CharArrayBuilder = CharArrayBuilder(this)
    override fun empty(): CharArray = CharArray(0)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: CharArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeCharElement(descriptor, index))
    }

    override fun writeContent(encoder: CompositeEncoder, content: CharArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeCharElement(descriptor, i, content[i])
    }
}

@InternalSerializationApi
@Deprecated(message = "For internal use", level = DeprecationLevel.HIDDEN)
public class CharArrayBuilder internal constructor(
    bufferWithData: CharArray
) : PrimitiveArrayBuilder<CharArray>() {

    private var buffer: CharArray = bufferWithData
    override var position: Int = bufferWithData.size
        private set

    init {
        ensureCapacity(INITIAL_SIZE)
    }

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
@Deprecated(
    message = "Deprecated in the favour of BooleanArraySerializer() factory",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("BooleanArraySerializer()", imports = ["kotlinx.serialization.builtins.BooleanArraySerializer"])
)
public object BooleanArraySerializer : KSerializer<BooleanArray>,
    PrimitiveArraySerializer<Boolean, BooleanArray, BooleanArrayBuilder>(Boolean.serializer()) {

    override fun BooleanArray.collectionSize(): Int = size
    override fun BooleanArray.toBuilder(): BooleanArrayBuilder = BooleanArrayBuilder(this)
    override fun empty(): BooleanArray = BooleanArray(0)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: BooleanArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeBooleanElement(descriptor, index))
    }

    override fun writeContent(encoder: CompositeEncoder, content: BooleanArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeBooleanElement(descriptor, i, content[i])
    }
}

@InternalSerializationApi
@Deprecated(message = "For internal use", level = DeprecationLevel.HIDDEN)
public class BooleanArrayBuilder internal constructor(
    bufferWithData: BooleanArray
) : PrimitiveArrayBuilder<BooleanArray>() {

    private var buffer: BooleanArray = bufferWithData
    override var position: Int = bufferWithData.size
        private set

    init {
        ensureCapacity(INITIAL_SIZE)
    }

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
