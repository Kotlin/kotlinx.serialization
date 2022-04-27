/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.encoding

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.modules.*

/**
 * Encoder is a core serialization primitive that encapsulates the knowledge of the underlying
 * format and its storage, exposing only structural methods to the serializer, making it completely
 * format-agnostic. Serialization process transforms a single value into the sequence of its
 * primitive elements, also called its serial form, while encoding transforms these primitive elements into an actual
 * format representation: JSON string, ProtoBuf ByteArray, in-memory map representation etc.
 *
 * Encoder provides high-level API that operates with basic primitive types, collections
 * and nested structures. Internally, encoder represents output storage and operates with its state
 * and lower level format-specific details.
 *
 * To be more specific, serialization transforms a value into a sequence of "here is an int, here is
 * a double, here a list of strings and here is another object that is a nested int", while encoding
 * transforms this sequence into a format-specific commands such as "insert opening curly bracket
 * for a nested object start, insert a name of the value, and the value separated with colon for an int etc."
 *
 * The symmetric interface for the deserialization process is [Decoder].
 *
 * ### Serialization. Primitives
 *
 * If a class is represented as a single [primitive][PrimitiveKind] value in its serialized form,
 * then one of the `encode*` methods (e.g. [encodeInt]) can be used directly.
 *
 * ### Serialization. Structured types.
 *
 * If a class is represented as a structure or has multiple values in its serialized form,
 * `encode*` methods are not that helpful, because they do not allow working with collection types or establish structure boundaries.
 * All these capabilities are delegated to the [CompositeEncoder] interface with a more specific API surface.
 * To denote a structure start, [beginStructure] should be used.
 * ```
 * // Denote the structure start,
 * val composite = encoder.beginStructure(descriptor)
 * // Encoding all elements within the structure using 'composite'
 * ...
 * // Denote the structure end
 * composite.endStructure(descriptor)
 * ```
 *
 * E.g. if the encoder belongs to JSON format, then [beginStructure] will write an opening bracket
 * (`{` or `[`, depending on the descriptor kind), returning the [CompositeEncoder] that is aware of colon separator,
 * that should be appended between each key-value pair, whilst [CompositeEncoder.endStructure] will write a closing bracket.
 *
 * ### Exception guarantees.
 * For the regular exceptions, such as invalid input, conflicting serial names,
 * [SerializationException] can be thrown by any encoder methods.
 * It is recommended to declare a format-specific subclass of [SerializationException] and throw it.
 *
 * ### Format encapsulation
 *
 * For example, for the following serializer:
 * ```
 * class StringHolder(val stringValue: String)
 *
 * object StringPairDeserializer : SerializationStrategy<StringHolder> {
 *    override val descriptor = ...
 *
 *    override fun serializer(encoder: Encoder, value: StringHolder) {
 *        // Denotes start of the structure, StringHolder is not a "plain" data type
 *        val composite = encoder.beginStructure(descriptor)
 *        // Encode the nested string value
 *        composite.encodeStringElement(descriptor, index = 0)
 *        // Denotes end of the structure
 *        composite.endStructure(descriptor)
 *    }
 * }
 * ```
 *
 * This serializer does not know anything about the underlying storage and will work with any properly-implemented encoder.
 * JSON, for example, writes an opening bracket `{` during the `beginStructure` call, writes 'stringValue` key along
 * with its value in `encodeStringElement` and writes the closing bracket `}` during the `endStructure`.
 * XML would do roughly the same, but with different separators and structures, while ProtoBuf
 * machinery could be completely different.
 * In any case, all these parsing details are encapsulated by an encoder.
 *
 * ### Exception safety
 *
 * In general, catching [SerializationException] from any of `encode*` methods is not allowed and produces unspecified behaviour.
 * After thrown exception, current encoder is left in an arbitrary state, no longer suitable for further encoding.
 *
 * ### Encoder implementation.
 *
 * While being strictly typed, an underlying format can transform actual types in the way it wants.
 * For example, a format can support only string types and encode/decode all primitives in a string form:
 * ```
 * StringFormatEncoder : Encoder {
 *
 *     ...
 *     override fun encodeDouble(value: Double) = encodeString(value.toString())
 *     override fun encodeInt(value: Int) = encodeString(value.toString())
 *     ...
 * }
 * ```
 *
 * ### Not stable for inheritance
 *
 * `Encoder` interface is not stable for inheritance in 3rd party libraries, as new methods
 * might be added to this interface or contracts of the existing methods can be changed.
 */
public interface Encoder {
    /**
     * Context of the current serialization process, including contextual and polymorphic serialization and,
     * potentially, a format-specific configuration.
     */
    public val serializersModule: SerializersModule

    /**
     * Notifies the encoder that value of a nullable type that is
     * being serialized is not null. It should be called before writing a non-null value
     * of nullable type:
     * ```
     * // Could be String? serialize method
     * if (value != null) {
     *     encoder.encodeNotNullMark()
     *     encoder.encodeStringValue(value)
     * } else {
     *     encoder.encodeNull()
     * }
     * ```
     *
     * This method has a use in highly-performant binary formats and can
     * be safely ignore by most of the regular formats.
     */
    @ExperimentalSerializationApi
    public fun encodeNotNullMark() {}

    /**
     * Encodes `null` value.
     */
    @ExperimentalSerializationApi
    public fun encodeNull()

    /**
     * Encodes a boolean value.
     * Corresponding kind is [PrimitiveKind.BOOLEAN].
     */
    public fun encodeBoolean(value: Boolean)

    /**
     * Encodes a single byte value.
     * Corresponding kind is [PrimitiveKind.BYTE].
     */
    public fun encodeByte(value: Byte)

    /**
     * Encodes a 16-bit short value.
     * Corresponding kind is [PrimitiveKind.SHORT].
     */
    public fun encodeShort(value: Short)

    /**
     * Encodes a 16-bit unicode character value.
     * Corresponding kind is [PrimitiveKind.CHAR].
     */
    public fun encodeChar(value: Char)

    /**
     * Encodes a 32-bit integer value.
     * Corresponding kind is [PrimitiveKind.INT].
     */
    public fun encodeInt(value: Int)

    /**
     * Encodes a 64-bit integer value.
     * Corresponding kind is [PrimitiveKind.LONG].
     */
    public fun encodeLong(value: Long)

    /**
     * Encodes a 32-bit IEEE 754 floating point value.
     * Corresponding kind is [PrimitiveKind.FLOAT].
     */
    public fun encodeFloat(value: Float)

    /**
     * Encodes a 64-bit IEEE 754 floating point value.
     * Corresponding kind is [PrimitiveKind.DOUBLE].
     */
    public fun encodeDouble(value: Double)

    /**
     * Encodes a string value.
     * Corresponding kind is [PrimitiveKind.STRING].
     */
    public fun encodeString(value: String)

    /**
     * Encodes a enum value that is stored at the [index] in [enumDescriptor] elements collection.
     * Corresponding kind is [SerialKind.ENUM].
     *
     * E.g. for the enum `enum class Letters { A, B, C, D }` and
     * serializable value "C", [encodeEnum] method should be called with `2` as am index.
     *
     * This method does not imply any restrictions on the output format,
     * the format is free to store the enum by its name, index, ordinal or any other
     */
    public fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int)

    /**
     * Returns [Encoder] for encoding an underlying type of an inline class.
     * [inlineDescriptor] describes a serializable inline class.
     *
     * Namely, for the `@Serializable inline class MyInt(val my: Int)`,
     * the following sequence is used:
     * ```
     * thisEncoder.encodeInline(MyInt.serializer().descriptor).encodeInt(my)
     * ```
     *
     * Current encoder may return any other instance of [Encoder] class,
     * depending on the provided [inlineDescriptor].
     * For example, when this function is called on Json encoder with
     * `UInt.serializer().descriptor`, the returned encoder is able
     * to encode unsigned integers.
     *
     * Note that this function returns [Encoder] instead of [CompositeEncoder]
     * because inline classes always have one property.
     * Calling [Encoder.beginStructure] on returned instance leads to an undefined behavior.
     */
    @ExperimentalSerializationApi
    public fun encodeInline(inlineDescriptor: SerialDescriptor): Encoder

    /**
     * Encodes the beginning of the nested structure in a serialized form
     * and returns [CompositeDecoder] responsible for encoding this very structure.
     * E.g the hierarchy:
     * ```
     * class StringHolder(val stringValue: String)
     * class Holder(val stringHolder: StringHolder)
     * ```
     *
     * with the following serialized form in JSON:
     * ```
     * {
     *   "stringHolder" : { "stringValue": "value" }
     * }
     * ```
     *
     * will be roughly represented as the following sequence of calls:
     * ```
     * // Holder serializer
     * fun serialize(encoder: Encoder, value: Holder) {
     *     val composite = encoder.beginStructure(descriptor) // the very first opening bracket '{'
     *     composite.encodeSerializableElement(descriptor, 0, value.stringHolder) // Serialize nested StringHolder
     *     composite.endStructure(descriptor) // The very last closing bracket
     * }
     *
     * // StringHolder serializer
     * fun serialize(encoder: Encoder, value: StringHolder) {
     *     val composite = encoder.beginStructure(descriptor) // One more '{' when the key "stringHolder" is already written
     *     composite.encodeStringElement(descriptor, 0, value.stringValue) // Serialize actual value
     *     composite.endStructure(descriptor) // Closing bracket
     * }
     * ```
     */
    public fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder

    /**
     * Encodes the beginning of the collection with size [collectionSize] and the given serializer of its type parameters.
     * This method has to be implemented only if you need to know collection size in advance, otherwise, [beginStructure] can be used.
     */
    public fun beginCollection(
        descriptor: SerialDescriptor,
        collectionSize: Int
    ): CompositeEncoder = beginStructure(descriptor)

    /**
     * Encodes the [value] of type [T] by delegating the encoding process to the given [serializer].
     * For example, `encodeInt` call us equivalent to delegating integer encoding to [Int.serializer][Int.Companion.serializer]:
     * `encodeSerializableValue(Int.serializer())`
     */
    public fun <T : Any?> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        serializer.serialize(this, value)
    }

    /**
     * Encodes the nullable [value] of type [T] by delegating the encoding process to the given [serializer].
     */
    @Suppress("UNCHECKED_CAST")
    @ExperimentalSerializationApi
    public fun <T : Any> encodeNullableSerializableValue(serializer: SerializationStrategy<T>, value: T?) {
        val isNullabilitySupported = serializer.descriptor.isNullable
        if (isNullabilitySupported) {
            // Instead of `serializer.serialize` to be able to intercept this
            return encodeSerializableValue(serializer as SerializationStrategy<T?>, value)
        }

        // Else default path used to avoid allocation of NullableSerializer
        if (value == null) {
            encodeNull()
        } else {
            encodeNotNullMark()
            encodeSerializableValue(serializer, value)
        }
    }
}

/**
 * [CompositeEncoder] is a part of encoding process that is bound to a particular structured part of
 * the serialized form, described by the serial descriptor passed to [Encoder.beginStructure].
 *
 * All `encode*` methods have `index` and `serialDescriptor` parameters with a strict semantics and constraints:
 *   * `descriptor` is always the same as one used in [Encoder.beginStructure]. While this parameter may seem redundant,
 *      it is required for efficient serialization process to avoid excessive field spilling.
 *      If you are writing your own format, you can safely ignore this parameter and use one used in `beginStructure`
 *      for simplicity.
 *   * `index` of the element being encoded. This element at this index in the descriptor should be associated with
 *      the one being written.
 *
 * The symmetric interface for the deserialization process is [CompositeDecoder].
 *
 * ### Not stable for inheritance
 *
 * `CompositeEncoder` interface is not stable for inheritance in 3rd party libraries, as new methods
 * might be added to this interface or contracts of the existing methods can be changed.
 */
public interface CompositeEncoder {
    /**
     * Context of the current serialization process, including contextual and polymorphic serialization and,
     * potentially, a format-specific configuration.
     */
    public val serializersModule: SerializersModule

    /**
     * Denotes the end of the structure associated with current encoder.
     * For example, composite encoder of JSON format will write
     * a closing bracket in the underlying input and reduce the number of nesting for pretty printing.
     */
    public fun endStructure(descriptor: SerialDescriptor)

    /**
     * Whether the format should encode values that are equal to the default values.
     * This method is used by plugin-generated serializers for properties with default values:
     * ```
     * @Serializable
     * class WithDefault(val int: Int = 42)
     * // serialize method
     * if (value.int != 42 || output.shouldEncodeElementDefault(serialDesc, 0)) {
     *    encoder.encodeIntElement(serialDesc, 0, value.int);
     * }
     * ```
     *
     * This method is never invoked for properties annotated with [EncodeDefault].
     */
    @ExperimentalSerializationApi
    public fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean = true

    /**
     * Encodes a boolean [value] associated with an element at the given [index] in [serial descriptor][descriptor].
     * The element at the given [index] should have [PrimitiveKind.BOOLEAN] kind.
     */
    public fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean)

    /**
     * Encodes a single byte [value] associated with an element at the given [index] in [serial descriptor][descriptor].
     * The element at the given [index] should have [PrimitiveKind.BYTE] kind.
     */
    public fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte)

    /**
     * Encodes a 16-bit short [value] associated with an element at the given [index] in [serial descriptor][descriptor].
     * The element at the given [index] should have [PrimitiveKind.SHORT] kind.
     */
    public fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short)

    /**
     * Encodes a 16-bit unicode character [value] associated with an element at the given [index] in [serial descriptor][descriptor].
     * The element at the given [index] should have [PrimitiveKind.CHAR] kind.
     */
    public fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char)

    /**
     * Encodes a 32-bit integer [value] associated with an element at the given [index] in [serial descriptor][descriptor].
     * The element at the given [index] should have [PrimitiveKind.INT] kind.
     */
    public fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int)

    /**
     * Encodes a 64-bit integer [value] associated with an element at the given [index] in [serial descriptor][descriptor].
     * The element at the given [index] should have [PrimitiveKind.LONG] kind.
     */
    public fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long)

    /**
     * Encodes a 32-bit IEEE 754 floating point [value] associated with an element
     * at the given [index] in [serial descriptor][descriptor].
     * The element at the given [index] should have [PrimitiveKind.FLOAT] kind.
     */
    public fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float)

    /**
     * Encodes a 64-bit IEEE 754 floating point [value] associated with an element
     * at the given [index] in [serial descriptor][descriptor].
     * The element at the given [index] should have [PrimitiveKind.DOUBLE] kind.
     */
    public fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double)

    /**
     * Encodes a string [value] associated with an element at the given [index] in [serial descriptor][descriptor].
     * The element at the given [index] should have [PrimitiveKind.STRING] kind.
     */
    public fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String)

    /**
     * Returns [Encoder] for decoding an underlying type of an inline class.
     * Serializable inline class is described by the [child descriptor][SerialDescriptor.getElementDescriptor]
     * of given [descriptor] at [index].
     *
     * Namely, for the `@Serializable inline class MyInt(val my: Int)`,
     * and `@Serializable class MyData(val myInt: MyInt)`
     * the following sequence is used:
     * ```
     * thisEncoder.encodeInlineElement(MyData.serializer.descriptor, 0).encodeInt(my)
     * ```
     *
     * This method is an optimization and its invocation should have the exact same result as
     * ```
     * thisEncoder.encodeSerializableElement(MyData.serializer.descriptor, 0, MyInt.serializer(), myInt)
     * ```
     *
     * Current encoder may return any other instance of [Encoder] class,
     * depending on provided descriptor.
     * For example, when this function is called on Json encoder with descriptor that has
     * `UInt.serializer().descriptor` at the given [index], the returned encoder is able
     * to encode unsigned integers.
     *
     * Note that this function returns [Encoder] instead of [CompositeEncoder]
     * because inline classes always have one property.
     * Calling [Encoder.beginStructure] on returned instance leads to an undefined behavior.
     *
     * @see Encoder.encodeInline
     * @see SerialDescriptor.getElementDescriptor
     */
    @ExperimentalSerializationApi
    public fun encodeInlineElement(
        descriptor: SerialDescriptor,
        index: Int
    ): Encoder

    /**
     * Delegates [value] encoding of the type [T] to the given [serializer].
     * [value] is associated with an element at the given [index] in [serial descriptor][descriptor].
     */
    public fun <T : Any?> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    )

    /**
     * Delegates nullable [value] encoding of the type [T] to the given [serializer].
     * [value] is associated with an element at the given [index] in [serial descriptor][descriptor].
     */
    @ExperimentalSerializationApi
    public fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    )
}

/**
 * Begins a structure, encodes it using the given [block] and ends it.
 */
public inline fun Encoder.encodeStructure(
    descriptor: SerialDescriptor,
    crossinline block: CompositeEncoder.() -> Unit
) {
    val composite = beginStructure(descriptor)
    composite.block()
    composite.endStructure(descriptor)
}

/**
 * Begins a collection, encodes it using the given [block] and ends it.
 */
public inline fun Encoder.encodeCollection(
    descriptor: SerialDescriptor,
    collectionSize: Int,
    crossinline block: CompositeEncoder.() -> Unit
) {
    val composite = beginCollection(descriptor, collectionSize)
    composite.block()
    composite.endStructure(descriptor)
}

/**
 * Begins a collection, calls [block] with each item and ends the collections.
 */
public inline fun <E> Encoder.encodeCollection(
    descriptor: SerialDescriptor,
    collection: Collection<E>,
    crossinline block: CompositeEncoder.(index: Int, E) -> Unit
) {
    encodeCollection(descriptor, collection.size) {
        collection.forEachIndexed { index, e ->
            block(index, e)
        }
    }
}
