/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.modules.*

/**
 * Decoder is a core deserialization primitive that encapsulates the knowledge of the underlying
 * format and an underlying storage, exposing only structural methods to the deserializer, making it completely
 * format-agnostic. Deserialization process takes a decoder and asks him for a sequence of primitive elements,
 * defined by a deserializer serial form, while decoder knows how to retrieve these primitive elements from an actual format
 * representations.
 *
 * Decoder provides high-level API that operates with basic primitive types, collections
 * and nested structures. Internally, the decoder represents input storage, and operates with its state
 * and lower level format-specific details.
 *
 * To be more specific, serialization asks a decoder for a sequence of "give me an int, give me
 * a double, give me a list of strings and give me another object that is a nested int", while decoding
 * transforms this sequence into a format-specific commands such as "parse the part of the string until the next quotation mark
 * as an int to retrieve an int, parse everything within the next curly braces to retrieve elements of a nested object etc."
 *
 * The symmetric interface for the serialization process is [Encoder].
 *
 * ### Deserialization. Primitives
 *
 * If a class is represented as a single [primitive][PrimitiveKind] value in its serialized form,
 * then one of the `decode*` methods (e.g. [decodeInt]) can be used directly.
 *
 * ### Deserialization. Structured types
 *
 * If a class is represented as a structure or has multiple values in its serialized form,
 * `decode*` methods are not that helpful, because format may not require a strict order of data
 * (e.g. JSON or XML), do not allow to work with collection types or establish structure boundaries.
 * All these capabilities are delegated to the [CompositeDecoder] interface with a more specific API surface.
 * To denote a structure start, [beginStructure] should be used.
 * ```
 * // Denote the structure start,
 * val composite = decoder.beginStructure(descriptor)
 * // Decode all elements within the structure using 'composite'
 * ...
 * // Denote the structure end
 * composite.endStructure(descriptor)
 * ```
 *
 * E.g. if the decoder belongs to JSON format, then [beginStructure] will parse an opening bracket
 * (`{` or `[`, depending on the descriptor kind), returning the [CompositeDecoder] that is aware of colon separator,
 * that should be read after each key-value pair, whilst [CompositeDecoder.endStructure] will parse a closing bracket.
 *
 * ### Exception guarantees.
 * For the regular exceptions, such as invalid input, missing control symbols or attributes and unknown symbols,
 * [SerializationException] can be thrown by any decoder methods. It is recommended to declare a format-specific
 * subclass of [SerializationException] and throw it.
 *
 * ### Format encapsulation
 *
 * For example, for the following deserializer:
 * ```
 * class StringHolder(val stringValue: String)
 *
 * object StringPairDeserializer : DeserializationStrategy<StringHolder> {
 *    override val descriptor = ...
 *
 *    override fun deserializer(decoder: Decoder): StringHolder {
 *        // Denotes start of the structure, StringHolder is not a "plain" data type
 *        val composite = decoder.beginStructure(descriptor)
 *        if (composite.decodeElementIndex(descriptor) != 0)
 *            throw MissingFieldException("Field 'stringValue' is missing")
 *        // Decode the nested string value
 *        val value = composite.decodeStringElement(descriptor, index = 0)
 *        // Denotes end of the structure
 *        composite.endStructure(descriptor)
 *    }
 * }
 * ```
 *
 * This deserializer does not know anything about the underlying data and will work with any properly-implemented decoder.
 * JSON, for example, parses an opening bracket `{` during the `beginStructure` call, checks that the next key
 * after this bracket is `stringValue` (using the descriptor), returns the value after the colon as string value
 * and parses closing bracket `}` during the `endStructure`.
 * XML would do the roughly the same, but with different separators and parsing structures, while ProtoBuf
 * machinery could be completely different.
 * In any case, all these parsing details are encapsulated by a decoder.
 *
 * ### Decoder implementation
 *
 * While being strictly typed, an underlying format can transform actual types in the way it wants.
 * For example, a format can support only string types and encode/decode all primitives in a string form:
 * ```
 * StringFormatDecoder : Decoder {
 *
 *     ...
 *     override fun decodeDouble(): Double = decodeString().toDouble()
 *     override fun decodeInt(): Double = decodeString().toInt()
 *     ...
 * }
 * ```
 */
public interface Decoder {
    /**
     * Context of the current serialization process, including contextual and polymorphic serialization and,
     * potentially, a format-specific configuration.
     */
    public val context: SerialModule

    // Not documented, not decided on
    public val updateMode: UpdateMode

    /**
     * Returns `true` if the current value in decoder is not null, false otherwise.
     * This method is usually used to decode potentially nullable data:
     * ```
     * // Could be String? deserialize() method
     * public fun deserialize(decoder: Decoder): String? {
     *     if (decoder.decodeNotNullMark()) {
     *         return decoder.decodeString()
     *     } else {
     *         return decoder.decodeNull()
     *     }
     * }
     * ```
     */
    public fun decodeNotNullMark(): Boolean

    /**
     * Decodes the `null` value and returns it.
     */
    public fun decodeNull(): Nothing?

    // Not documented, will be reworked
    public fun decodeUnit()

    /**
     * Decodes a boolean value.
     * Corresponding kind is [PrimitiveKind.BOOLEAN].
     */
    public fun decodeBoolean(): Boolean

    /**
     * Decodes a single byte value.
     * Corresponding kind is [PrimitiveKind.BYTE].
     */
    public fun decodeByte(): Byte

    /**
     * Decodes a 16-bit short value.
     * Corresponding kind is [PrimitiveKind.SHORT].
     */
    public fun decodeShort(): Short

    /**
     * Decodes a 16-bit unicode character value.
     * Corresponding kind is [PrimitiveKind.CHAR].
     */
    public fun decodeChar(): Char

    /**
     * Decodes a 32-bit integer value.
     * Corresponding kind is [PrimitiveKind.INT].
     */
    public fun decodeInt(): Int

    /**
     * Decodes a 64-bit integer value.
     * Corresponding kind is [PrimitiveKind.LONG].
     */
    public fun decodeLong(): Long

    /**
     * Decodes a 32-bit IEEE 754 floating point value.
     * Corresponding kind is [PrimitiveKind.FLOAT].
     */
    public fun decodeFloat(): Float

    /**
     * Decodes a 64-bit IEEE 754 floating point value.
     * Corresponding kind is [PrimitiveKind.DOUBLE].
     */
    public fun decodeDouble(): Double

    /**
     * Decodes a string value.
     * Corresponding kind is [PrimitiveKind.STRING].
     */
    public fun decodeString(): String

    /**
     * Decodes a enum value and returns its index in [enumDescriptor] elements collection.
     * Corresponding kind is [UnionKind.ENUM_KIND].
     *
     * E.g. for the enum `enum class Letters { A, B, C, D }` and
     * underlying input "C", [decodeEnum] method should return `2` as a result.
     *
     * This method does not imply any restrictions on the input format,
     * the format is free to store the enum by its name, index, ordinal or any other enum representation.
     */
    public fun decodeEnum(enumDescriptor: SerialDescriptor): Int

    /**
     * Decodes the beginning of the nested structure in a serialized form
     * and returns [CompositeDecoder] responsible for decoding this very structure.
     *
     * Typically, classes, collections and maps are represented as a nested structure in a serialized form.
     * E.g. the following JSON
     * ```
     * {
     *     "a": 2,
     *     "b": { "nested": "c" }
     *     "c": [1, 2, 3],
     *     "d": null
     * }
     * ```
     * has three nested structures: the very beginning of the data, "b" value and "c" value.
     */
    @Suppress("DEPRECATION_ERROR", "RemoveRedundantSpreadOperator")
    public fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder = beginStructure(descriptor, *arrayOf<KSerializer<*>>())

    @Deprecated(
        "Parameter typeSerializers is deprecated for removal. Please migrate to beginStructure method with one argument.",
        ReplaceWith("beginStructure(descriptor)"),
        DeprecationLevel.ERROR
    )
    public fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder

    /**
     * Decodes the value of type [T] by delegating the decoding process to the given [deserializer].
     * For example, `decodeInt` call us equivalent to delegating integer decoding to [IntSerializer]:
     * `decodeSerializableValue(IntSerializer)`
     */
    public fun <T : Any?> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T =
        deserializer.deserialize(this)

    /**
     * Decodes the nullable value of type [T] by delegating the decoding process to the given [deserializer].
     */
    public fun <T : Any> decodeNullableSerializableValue(deserializer: DeserializationStrategy<T?>): T? =
        if (decodeNotNullMark()) decodeSerializableValue(deserializer) else decodeNull()

    // Not documented
    public fun <T> updateSerializableValue(deserializer: DeserializationStrategy<T>, old: T): T {
        return when (updateMode) {
            UpdateMode.BANNED -> throw UpdateNotSupportedException(deserializer.descriptor.serialName)
            UpdateMode.OVERWRITE -> decodeSerializableValue(deserializer)
            UpdateMode.UPDATE -> deserializer.patch(this, old)
        }
    }

    // Not documented
    public fun <T : Any> updateNullableSerializableValue(deserializer: DeserializationStrategy<T?>, old: T?): T? {
        return when {
            updateMode == UpdateMode.BANNED -> throw UpdateNotSupportedException(deserializer.descriptor.serialName)
            updateMode == UpdateMode.OVERWRITE || old == null -> decodeNullableSerializableValue(deserializer)
            decodeNotNullMark() -> deserializer.patch(this, old)
            else -> decodeNull().let { old }
        }
    }
}

/**
 * [CompositeDecoder] is a part of decoding process that is bound to a particular structured part of
 * the serialized form, described by the serial descriptor passed to [Decoder.beginStructure].
 *
 * Typically, for unordered data, [CompositeDecoder] is used by a serializer withing a [decodeElementIndex]-based
 * loop that decodes all the required data one-by-one in any order and then terminates by calling [endStructure].
 * Please refer to [decodeElementIndex] for example of such loop.
 *
 * All `decode*` methods have `index` and `serialDescriptor` parameters with a strict semantics and constraints:
 *   * `descriptor` argument is always the same as one used in [Decoder.beginStructure].
 *   * `index` of the element being decoded. For [sequential][decodeSequentially] decoding, it is always a monotonic
 *      sequence from `0` to `descriptor.elementsCount` and for indexing-loop it is always an index that [decodeElementIndex]
 *      has returned from the last call.
 *
 * The symmetric interface for the serialization process is [CompositeEncoder].
 */
public interface CompositeDecoder {

    /**
     * Results of [decodeElementIndex] used for decoding control flow.
     */
    public companion object {
        /**
         * Value returned by [decodeElementIndex] when the underlying input has no more data
         * (apart from the end of the structure).
         * When this value is returned, no methods of the decoder should be called but [endStructure].
         */
        public const val READ_DONE: Int = -1

        @Deprecated(
            message = "READ_ALL cannot be longer returned by 'decodeElementIndex', use 'decodeSequentially' instead",
            level = DeprecationLevel.WARNING
        )
        @Suppress("UNUSED")
        public const val READ_ALL: Int = -2

        /**
         * Value returned by [decodeElementIndex] when the format encountered an unknown element
         * (expected neither by the structure of serial descriptor, nor by the format itself).
         */
        public const val UNKNOWN_NAME: Int = -3
    }

    /**
     * Context of the current decoding process, including contextual and polymorphic serialization and,
     * potentially, a format-specific configuration.
     */
    public val context: SerialModule

    // Not documented, not decided on
    public val updateMode: UpdateMode

    /**
     * Denotes the end of the structure associated with current decoder.
     * For example, composite decoder of JSON format will expect (and parse)
     * a closing bracket in the underlying input.
     */
    public fun endStructure(descriptor: SerialDescriptor)

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
     *     if (nextKey == null) return READ_DONE
     *     return descriptor.getElementIndex(nextKey) // getElementIndex can return UNKNOWN_NAME
     * }
     * ```
     */
    public fun decodeElementIndex(descriptor: SerialDescriptor): Int

    /**
     * Method to decode collection size that may be called before the collection decoding.
     * Collection type includes [Collection], [Map] and [Array] (including primitive arrays).
     * Method can return `-1` if the size is not known in advance, though for [sequential decoding][decodeSequentially]
     * knowing precise size is a mandatory requirement.
     */
    public fun decodeCollectionSize(descriptor: SerialDescriptor): Int = -1

    // Not documented, was reworked
    public fun decodeUnitElement(descriptor: SerialDescriptor, index: Int)

    /**
     * Decodes a boolean value from the underlying input.
     * The resulting value is associated with the [descriptor] element at the given [index].
     * The element at the given index should have [PrimitiveKind.BOOLEAN] kind.
     */
    public fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean

    /**
     * Decodes a single byte value from the underlying input.
     * The resulting value is associated with the [descriptor] element at the given [index].
     * The element at the given index should have [PrimitiveKind.BYTE] kind.
     */
    public fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte

    /**
     * Decodes a 16-bit unicode character value from the underlying input.
     * The resulting value is associated with the [descriptor] element at the given [index].
     * The element at the given index should have [PrimitiveKind.CHAR] kind.
     */
    public fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char

    /**
     * Decodes a 16-bit short value from the underlying input.
     * The resulting value is associated with the [descriptor] element at the given [index].
     * The element at the given index should have [PrimitiveKind.SHORT] kind.
     */
    public fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short

    /**
     * Decodes a 32-bit integer value from the underlying input.
     * The resulting value is associated with the [descriptor] element at the given [index].
     * The element at the given index should have [PrimitiveKind.INT] kind.
     */
    public fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int

    /**
     * Decodes a 64-bit integer value from the underlying input.
     * The resulting value is associated with the [descriptor] element at the given [index].
     * The element at the given index should have [PrimitiveKind.LONG] kind.
     */
    public fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long

    /**
     * Decodes a 32-bit IEEE 754 floating point value from the underlying input.
     * The resulting value is associated with the [descriptor] element at the given [index].
     * The element at the given index should have [PrimitiveKind.FLOAT] kind.
     */
    public fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float

    /**
     * Decodes a 64-bit IEEE 754 floating point value from the underlying input.
     * The resulting value is associated with the [descriptor] element at the given [index].
     * The element at the given index should have [PrimitiveKind.DOUBLE] kind.
     */
    public fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double

    /**
     * Decodes a string value from the underlying input.
     * The resulting value is associated with the [descriptor] element at the given [index].
     * The element at the given index should have [PrimitiveKind.STRING] kind.
     */
    public fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String

    /**
     * Delegates decoding value of the type [T] to the given [deserializer].
     */
    public fun <T : Any?> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>
    ): T

    /**
     * Delegates decoding nullable value of the type [T] to the given [deserializer].
     */
    public fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>
    ): T?

    // Not documented
    public fun <T> updateSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        old: T
    ): T

    // Not documented
    public fun <T : Any> updateNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        old: T?
    ): T?
}

/**
 * Alias for [Decoder.decodeSerializableValue]
 */
public fun <T : Any?> Decoder.decode(deserializer: DeserializationStrategy<T>): T =
    decodeSerializableValue(deserializer)

/**
 * [typeOf]-based version of [Decoder.decodeSerializableValue]
 */
@ImplicitReflectionSerializer
public inline fun <reified T : Any> Decoder.decode(): T = decode(T::class.serializer())

/**
 * Begins a structure, decodes it using the given [block], ends it and returns decoded element.
 */
public inline fun <T> Decoder.decodeStructure(descriptor: SerialDescriptor, crossinline block: CompositeDecoder.() -> T): T {
    val composite = beginStructure(descriptor)
    val result = composite.block()
    composite.endStructure(descriptor)
    return result
}
