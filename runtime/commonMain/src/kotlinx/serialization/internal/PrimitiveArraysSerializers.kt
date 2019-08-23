/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*

private const val INITIAL_SIZE = 10

/**
 * Serializer for [ByteArray].
 *
 * Encode elements one-by-one, as regular list,
 * unless format's Encoder/Decoder have special handling for this serializer.
 */
public object ByteArraySerializer :
    PrimitiveArraySerializer<Byte, ByteArray, ByteArrayBuilder>(ByteSerializer, ByteDescriptor),
    KSerializer<ByteArray> {

    override fun ByteArray.collectionSize(): Int = size
    override fun builder(): ByteArrayBuilder = ByteArrayBuilder()
    override fun ByteArray.toBuilder(): ByteArrayBuilder = ByteArrayBuilder(this)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: ByteArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeByteElement(descriptor, index))
    }

    override fun writeContent(encoder: CompositeEncoder, content: ByteArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeByteElement(descriptor, i, content[i])
    }
}

public class ByteArrayBuilder internal constructor() :
    PrimitiveArrayBuilder<ByteArray>() {

    private var buf: ByteArray = ByteArray(INITIAL_SIZE)
    override var position: Int = 0
        private set

    constructor(filledBuf: ByteArray): this() {
        buf = filledBuf
        position = filledBuf.size
    }

    override fun ensureCapacity(requiredCapacity: Int) {
        if (buf.size < requiredCapacity)
            buf = buf.copyOf(requiredCapacity)
    }

    internal fun append(c: Byte) {
        ensureCapacity()
        buf[position++] = c
    }

    override fun build() = buf.copyOf(position)
}

// the rest of the serializers are merely copy-paste
/**
 * Serializer for [ShortArray].
 *
 * Encode elements one-by-one, as regular list,
 * unless format's Encoder/Decoder have special handling for this serializer.
 */
public object ShortArraySerializer :
    PrimitiveArraySerializer<Short, ShortArray, ShortArrayBuilder>(ShortSerializer, ShortDescriptor),
    KSerializer<ShortArray> {

    override fun ShortArray.collectionSize(): Int = size
    override fun builder(): ShortArrayBuilder = ShortArrayBuilder()
    override fun ShortArray.toBuilder(): ShortArrayBuilder = ShortArrayBuilder(this)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: ShortArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeShortElement(descriptor, index))
    }

    override fun writeContent(encoder: CompositeEncoder, content: ShortArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeShortElement(descriptor, i, content[i])
    }
}

public class ShortArrayBuilder internal constructor() :
    PrimitiveArrayBuilder<ShortArray>() {

    private var buf: ShortArray = ShortArray(INITIAL_SIZE)
    override var position: Int = 0
        private set

    constructor(filledBuf: ShortArray) : this() {
        buf = filledBuf
        position = filledBuf.size
    }

    override fun ensureCapacity(requiredCapacity: Int) {
        if (buf.size < requiredCapacity)
            buf = buf.copyOf(requiredCapacity)
    }

    internal fun append(c: Short) {
        ensureCapacity()
        buf[position++] = c
    }

    override fun build() = buf.copyOf(position)
}

/**
 * Serializer for [IntArray].
 *
 * Encode elements one-by-one, as regular list,
 * unless format's Encoder/Decoder have special handling for this serializer.
 */
public object IntArraySerializer :
    PrimitiveArraySerializer<Int, IntArray, IntArrayBuilder>(IntSerializer, IntDescriptor),
    KSerializer<IntArray> {

    override fun IntArray.collectionSize(): Int = size
    override fun builder(): IntArrayBuilder = IntArrayBuilder()
    override fun IntArray.toBuilder(): IntArrayBuilder = IntArrayBuilder(this)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: IntArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeIntElement(descriptor, index))
    }

    override fun writeContent(encoder: CompositeEncoder, content: IntArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeIntElement(descriptor, i, content[i])
    }
}

public class IntArrayBuilder internal constructor() :
    PrimitiveArrayBuilder<IntArray>() {

    private var buf: IntArray = IntArray(INITIAL_SIZE)
    override var position: Int = 0
        private set

    constructor(filledBuf: IntArray) : this() {
        buf = filledBuf
        position = filledBuf.size
    }

    override fun ensureCapacity(requiredCapacity: Int) {
        if (buf.size < requiredCapacity)
            buf = buf.copyOf(requiredCapacity)
    }

    internal fun append(c: Int) {
        ensureCapacity()
        buf[position++] = c
    }

    override fun build() = buf.copyOf(position)
}


/**
 * Serializer for [LongArray].
 *
 * Encode elements one-by-one, as regular list,
 * unless format's Encoder/Decoder have special handling for this serializer.
 */
public object LongArraySerializer :
    PrimitiveArraySerializer<Long, LongArray, LongArrayBuilder>(LongSerializer, LongDescriptor),
    KSerializer<LongArray> {

    override fun LongArray.collectionSize(): Int = size
    override fun builder(): LongArrayBuilder = LongArrayBuilder()
    override fun LongArray.toBuilder(): LongArrayBuilder = LongArrayBuilder(this)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: LongArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeLongElement(descriptor, index))
    }

    override fun writeContent(encoder: CompositeEncoder, content: LongArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeLongElement(descriptor, i, content[i])
    }
}

public class LongArrayBuilder internal constructor() :
    PrimitiveArrayBuilder<LongArray>() {

    private var buf: LongArray = LongArray(INITIAL_SIZE)
    override var position: Int = 0
        private set

    constructor(filledBuf: LongArray) : this() {
        buf = filledBuf
        position = filledBuf.size
    }

    override fun ensureCapacity(requiredCapacity: Int) {
        if (buf.size < requiredCapacity)
            buf = buf.copyOf(requiredCapacity)
    }

    internal fun append(c: Long) {
        ensureCapacity()
        buf[position++] = c
    }

    override fun build() = buf.copyOf(position)
}

/**
 * Serializer for [CharArray].
 *
 * Encode elements one-by-one, as regular list,
 * unless format's Encoder/Decoder have special handling for this serializer.
 */
public object CharArraySerializer :
    PrimitiveArraySerializer<Char, CharArray, CharArrayBuilder>(CharSerializer, CharDescriptor),
    KSerializer<CharArray> {

    override fun CharArray.collectionSize(): Int = size
    override fun builder(): CharArrayBuilder = CharArrayBuilder()
    override fun CharArray.toBuilder(): CharArrayBuilder = CharArrayBuilder(this)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: CharArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeCharElement(descriptor, index))
    }

    override fun writeContent(encoder: CompositeEncoder, content: CharArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeCharElement(descriptor, i, content[i])
    }
}

public class CharArrayBuilder internal constructor() :
    PrimitiveArrayBuilder<CharArray>() {

    private var buf: CharArray = CharArray(INITIAL_SIZE)
    override var position: Int = 0
        private set

    constructor(filledBuf: CharArray) : this() {
        buf = filledBuf
        position = filledBuf.size
    }

    override fun ensureCapacity(requiredCapacity: Int) {
        if (buf.size < requiredCapacity)
            buf = buf.copyOf(requiredCapacity)
    }

    internal fun append(c: Char) {
        ensureCapacity()
        buf[position++] = c
    }

    override fun build() = buf.copyOf(position)
}

/**
 * Serializer for [FloatArray].
 *
 * Encode elements one-by-one, as regular list,
 * unless format's Encoder/Decoder have special handling for this serializer.
 */
public object FloatArraySerializer :
    PrimitiveArraySerializer<Float, FloatArray, FloatArrayBuilder>(FloatSerializer, FloatDescriptor),
    KSerializer<FloatArray> {

    override fun FloatArray.collectionSize(): Int = size
    override fun builder(): FloatArrayBuilder = FloatArrayBuilder()
    override fun FloatArray.toBuilder(): FloatArrayBuilder = FloatArrayBuilder(this)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: FloatArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeFloatElement(descriptor, index))
    }

    override fun writeContent(encoder: CompositeEncoder, content: FloatArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeFloatElement(descriptor, i, content[i])
    }
}

public class FloatArrayBuilder internal constructor() :
    PrimitiveArrayBuilder<FloatArray>() {

    private var buf: FloatArray = FloatArray(INITIAL_SIZE)
    override var position: Int = 0
        private set

    constructor(filledBuf: FloatArray) : this() {
        buf = filledBuf
        position = filledBuf.size
    }

    override fun ensureCapacity(requiredCapacity: Int) {
        if (buf.size < requiredCapacity)
            buf = buf.copyOf(requiredCapacity)
    }

    internal fun append(c: Float) {
        ensureCapacity()
        buf[position++] = c
    }

    override fun build() = buf.copyOf(position)
}

/**
 * Serializer for [DoubleArray].
 *
 * Encode elements one-by-one, as regular list,
 * unless format's Encoder/Decoder have special handling for this serializer.
 */
public object DoubleArraySerializer :
    PrimitiveArraySerializer<Double, DoubleArray, DoubleArrayBuilder>(DoubleSerializer, DoubleDescriptor),
    KSerializer<DoubleArray> {

    override fun DoubleArray.collectionSize(): Int = size
    override fun builder(): DoubleArrayBuilder = DoubleArrayBuilder()
    override fun DoubleArray.toBuilder(): DoubleArrayBuilder = DoubleArrayBuilder(this)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: DoubleArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeDoubleElement(descriptor, index))
    }

    override fun writeContent(encoder: CompositeEncoder, content: DoubleArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeDoubleElement(descriptor, i, content[i])
    }
}

public class DoubleArrayBuilder internal constructor() :
    PrimitiveArrayBuilder<DoubleArray>() {

    private var buf: DoubleArray = DoubleArray(INITIAL_SIZE)
    override var position: Int = 0
        private set

    constructor(filledBuf: DoubleArray) : this() {
        buf = filledBuf
        position = filledBuf.size
    }

    override fun ensureCapacity(requiredCapacity: Int) {
        if (buf.size < requiredCapacity)
            buf = buf.copyOf(requiredCapacity)
    }

    internal fun append(c: Double) {
        ensureCapacity()
        buf[position++] = c
    }

    override fun build() = buf.copyOf(position)
}

/**
 * Serializer for [BooleanArray].
 *
 * Encode elements one-by-one, as regular list,
 * unless format's Encoder/Decoder have special handling for this serializer.
 */
public object BooleanArraySerializer :
    PrimitiveArraySerializer<Boolean, BooleanArray, BooleanArrayBuilder>(BooleanSerializer, BooleanDescriptor),
    KSerializer<BooleanArray> {

    override fun BooleanArray.collectionSize(): Int = size
    override fun builder(): BooleanArrayBuilder = BooleanArrayBuilder()
    override fun BooleanArray.toBuilder(): BooleanArrayBuilder = BooleanArrayBuilder(this)

    override fun readElement(decoder: CompositeDecoder, index: Int, builder: BooleanArrayBuilder, checkIndex: Boolean) {
        builder.append(decoder.decodeBooleanElement(descriptor, index))
    }

    override fun writeContent(encoder: CompositeEncoder, content: BooleanArray, size: Int) {
        for (i in 0 until size)
            encoder.encodeBooleanElement(descriptor, i, content[i])
    }
}

public class BooleanArrayBuilder internal constructor() :
    PrimitiveArrayBuilder<BooleanArray>() {

    private var buf: BooleanArray = BooleanArray(INITIAL_SIZE)
    override var position: Int = 0
        private set

    constructor(filledBuf: BooleanArray) : this() {
        buf = filledBuf
        position = filledBuf.size
    }

    override fun ensureCapacity(requiredCapacity: Int) {
        if (buf.size < requiredCapacity)
            buf = buf.copyOf(requiredCapacity)
    }

    internal fun append(c: Boolean) {
        ensureCapacity()
        buf[position++] = c
    }

    override fun build() = buf.copyOf(position)
}
