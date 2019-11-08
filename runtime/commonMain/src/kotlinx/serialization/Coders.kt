/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.modules.*

interface Encoder {
    val context: SerialModule

    fun encodeNotNullMark()
    fun encodeNull()

    fun encodeUnit()
    fun encodeBoolean(value: Boolean)
    fun encodeByte(value: Byte)
    fun encodeShort(value: Short)
    fun encodeInt(value: Int)
    fun encodeLong(value: Long)
    fun encodeFloat(value: Float)
    fun encodeDouble(value: Double)
    fun encodeChar(value: Char)
    fun encodeString(value: String)

    fun encodeEnum(enumDescription: SerialDescriptor, ordinal: Int)

    fun <T : Any?> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        serializer.serialize(this, value)
    }

    fun <T : Any> encodeNullableSerializableValue(serializer: SerializationStrategy<T>, value: T?) {
        if (value == null) {
            encodeNull()
        } else {
            encodeNotNullMark()
            encodeSerializableValue(serializer, value)
        }
    }

    fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder

    fun beginCollection(desc: SerialDescriptor, collectionSize: Int, vararg typeParams: KSerializer<*>) =
        beginStructure(desc, *typeParams)
}

interface CompositeEncoder {
    val context: SerialModule

    fun endStructure(desc: SerialDescriptor) {}

    fun shouldEncodeElementDefault(desc: SerialDescriptor, index: Int): Boolean = true

    fun encodeUnitElement(desc: SerialDescriptor, index: Int)
    fun encodeBooleanElement(desc: SerialDescriptor, index: Int, value: Boolean)
    fun encodeByteElement(desc: SerialDescriptor, index: Int, value: Byte)
    fun encodeShortElement(desc: SerialDescriptor, index: Int, value: Short)
    fun encodeIntElement(desc: SerialDescriptor, index: Int, value: Int)
    fun encodeLongElement(desc: SerialDescriptor, index: Int, value: Long)
    fun encodeFloatElement(desc: SerialDescriptor, index: Int, value: Float)
    fun encodeDoubleElement(desc: SerialDescriptor, index: Int, value: Double)
    fun encodeCharElement(desc: SerialDescriptor, index: Int, value: Char)
    fun encodeStringElement(desc: SerialDescriptor, index: Int, value: String)

    fun <T : Any?> encodeSerializableElement(desc: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T)

    fun encodeNonSerializableElement(desc: SerialDescriptor, index: Int, value: Any)

    fun <T : Any> encodeNullableSerializableElement(desc: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T?)
}


interface Decoder {
    val context: SerialModule

    /**
     * Returns true if the current value in decoder is not null, false otherwise
     */
    fun decodeNotNullMark(): Boolean

    /**
     * Consumes null, returns null, will be called when [decodeNotNullMark] is false
     */
    fun decodeNull(): Nothing?

    fun decodeUnit()
    fun decodeBoolean(): Boolean
    fun decodeByte(): Byte
    fun decodeShort(): Short
    fun decodeInt(): Int
    fun decodeLong(): Long
    fun decodeFloat(): Float
    fun decodeDouble(): Double
    fun decodeChar(): Char
    fun decodeString(): String

    fun decodeEnum(enumDescription: SerialDescriptor): Int


    fun <T : Any?> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T = deserializer.deserialize(this)

    fun <T : Any> decodeNullableSerializableValue(deserializer: DeserializationStrategy<T?>): T? =
        if (decodeNotNullMark()) decodeSerializableValue(deserializer) else decodeNull()

    fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder

    val updateMode: UpdateMode

    fun <T> updateSerializableValue(deserializer: DeserializationStrategy<T>, old: T): T {
        return when(updateMode) {
            UpdateMode.BANNED -> throw UpdateNotSupportedException(deserializer.descriptor.serialName)
            UpdateMode.OVERWRITE -> decodeSerializableValue(deserializer)
            UpdateMode.UPDATE -> deserializer.patch(this, old)
        }
    }

    fun <T: Any> updateNullableSerializableValue(deserializer: DeserializationStrategy<T?>, old: T?): T? {
        return when {
            updateMode == UpdateMode.BANNED -> throw UpdateNotSupportedException(deserializer.descriptor.serialName)
            updateMode == UpdateMode.OVERWRITE || old == null -> decodeNullableSerializableValue(deserializer)
            decodeNotNullMark() -> deserializer.patch(this, old)
            else -> decodeNull().let { old }
        }
    }
}

interface CompositeDecoder {

    /**
     * Results of [decodeElementIndex]
     */
    companion object {
        const val READ_DONE = -1

        @Deprecated(
            message = "READ_ALL cannot be longer returned by 'decodeElementIndex', use 'decodeSequentially' instead",
            level = DeprecationLevel.WARNING
        )
        const val READ_ALL = -2
        const val UNKNOWN_NAME = -3
    }

    val context: SerialModule
    val updateMode: UpdateMode

    fun endStructure(desc: SerialDescriptor) {}

    /**
     * Checks whether the current decoder supports strictly ordered decoding of the data
     * without calling to [decodeElementIndex].
     * If the method returns `true`, the caller might skip [decodeElementIndex] calls
     * and start invoking `decode*Element` directly, incrementing the index of the element one by one.
     * This method can be called by serializers (either generated or user-defined) as a performance optimization,
     * but there is no guarantee that the method will be ever called. Practically, it means that implementations
     * that may benefit from sequential decoding should also support a regular [decodeElementIndex]-based decoding as well.
     *
     * Example of usage:
     * ```
     * class MyPair(i: Int, d: Double)
     *
     * object MyPairSerializer : KSerializer<MyPair> {
     *     // ... other methods omitted
     *
     *    fun deserialize(decoder: Decoder): MyPair {
     *        val composite = decoder.beginStructure(descriptor)
     *        if (composite.decodeSequentially()) {
     *            val i = composite.decodeIntElement(descriptor, index = 0) // Mind the sequential indexing
     *            val d = composite.decodeIntElement(descriptor, index = 1)
     *            composite.endStructure(descriptor)
     *            return MyPair(i, d)
     *        } else {
     *            // Fallback to `decodeElementIndex` loop, refer to its documentation for details
     *        }
     *    }
     * }
     * ```
     * This example is a rough equivalent of what serialization plugin generates for serializable pair class.
     *
     * Sequential decoding is a performance optimization for formats with strictly ordered schema,
     * usually binary ones. Regular formats such as JSON or ProtoBuf cannot use this optimization,
     * because e.g. in the latter example, the same data can be represented both as
     * `{"i": 1, "d": 1.0}`"` and `{"d": 1.0, "i": 1}` (thus, unordered).
     */
    public fun decodeSequentially(): Boolean = false

    /**
     *  Decodes the index of the next element to be decoded.
     *  Index represents a position of the current element in the serial descriptor element that can be found
     *  with [SerialDescriptor.getElementIndex].
     *
     *  If this method returns non-negative index, the caller should call one of the `decode*Element` methods
     *  with a resulting index.
     *  Apart from positive values, this method can return [READ_DONE] to indicate that no more elements
     *  are left or [UNKNOWN_NAME] to indicate that symbol with an unknown name was encountered.
     *
     * Example of usage:
     * ```
     * class MyPair(i: Int, d: Double)
     *
     * object MyPairSerializer : KSerializer<MyPair> {
     *     // ... other methods omitted
     *
     *    fun deserialize(decoder: Decoder): MyPair {
     *        val composite = decoder.beginStructure(descriptor)
     *        var i: Int? = null
     *        var d: Double? = null
     *        while (true) {
     *            val index = composite.decodeElementIndex(descriptor)
     *            if (index == READ_DONE) break // Input is over
     *            when (index) {
     *                0 -> {
     *                    i = composite.decodeIntElement(descriptor, 0)
     *                }
     *                1 -> {
     *                    d = composite.decodeDoubleElement(descriptor, 1)
     *                }
     *                else -> error("Unexpected index: $index)
     *            }
     *
     *        }
     *        composite.endStructure(descriptor)
     *        if (i == null || d == null) throwMissingFieldException()
     *        return MyPair(i, d)
     *    }
     * }
     * ```
     * This example is a rough equivalent of what serialization plugin generates for serializable pair class.
     *
     * The need in such loop comes from unstructured nature of most serialization formats.
     * For example, JSON for the following input `{"d": 2.0, "i": 1}`, will first read `d` key with index `1`
     * and only after `i` with index `0`.
     *
     * A potential implementation of this method for JSON format can be the following:
     * ```
     * fun decodeElementIndex(descriptor: SerialDescriptor): Int {
     *     // Ignore arrays
     *     val nextKey: String? = myStringJsonParser.nextKey()
     *     if (nextObjectKey == null) return READ_DONE
     *     return descriptor.getElementIndex(nextKey) // getElementIndex can return UNKNOWN_FIELD
     * }
     * ```
     */
    public fun decodeElementIndex(descriptor: SerialDescriptor): Int

    /**
     * Method to decode collection size that may be called before the collection decoding.
     * Collection type includes [Collection], [Map] and [Array] (including primitive arrays).
     * Method can return `-1` if the size is not known in advance, though for [sequential decoding][decodeSequentially]
     * precise size is a mandatory requirement.
     */
    public fun decodeCollectionSize(desc: SerialDescriptor): Int = -1

    fun decodeUnitElement(desc: SerialDescriptor, index: Int)
    fun decodeBooleanElement(desc: SerialDescriptor, index: Int): Boolean
    fun decodeByteElement(desc: SerialDescriptor, index: Int): Byte
    fun decodeShortElement(desc: SerialDescriptor, index: Int): Short
    fun decodeIntElement(desc: SerialDescriptor, index: Int): Int
    fun decodeLongElement(desc: SerialDescriptor, index: Int): Long
    fun decodeFloatElement(desc: SerialDescriptor, index: Int): Float
    fun decodeDoubleElement(desc: SerialDescriptor, index: Int): Double
    fun decodeCharElement(desc: SerialDescriptor, index: Int): Char
    fun decodeStringElement(desc: SerialDescriptor, index: Int): String

    fun <T : Any?> decodeSerializableElement(desc: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T>): T
    fun <T : Any> decodeNullableSerializableElement(desc: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T?>): T?


    fun <T> updateSerializableElement(desc: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T>, old: T): T
    fun <T: Any> updateNullableSerializableElement(desc: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T?>, old: T?): T?
}
