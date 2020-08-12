/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.*
import kotlinx.serialization.encoding.updateModeDeprecated

/**
 * KSerializer is responsible for the representation of a serial form of a type [T]
 * in terms of [encoders][Encoder] and [decoders][Decoder] and for constructing and deconstructing [T]
 * from/to a sequence of encoding primitives. For classes marked with [@Serializable][Serializable], can be
 * obtained from generated companion extension `.serializer()` or from [serializer<T>()][serializer] function.
 *
 * Serialization is decoupled from the encoding process to make it completely format-agnostic.
 * Serialization represents a type as its serial form and is abstracted from the actual
 * format (whether its JSON, ProtoBuf or a hashing) and unaware of the underlying storage
 * (whether it is a string builder, byte array or a network socket), while
 * encoding/decoding is abstracted from a particular type and its serial form and is responsible
 * for transforming primitives ("here in an int property 'foo'" call from a serializer) into a particular
 * format-specific representation ("for a given int, append a property name in quotation marks,
 * then append a colon, then append an actual value" for JSON) and how to retrieve a primitive
 * ("give me an int that is 'foo' property") from the underlying representation ("expect the next string to be 'foo',
 * parse it, then parse colon, then parse a string until the next comma as an int and return it).
 *
 * Serial form consists of a structural description, declared by the [descriptor] and
 * actual serialization and deserialization processes, defined by the corresponding
 * [serialize] and [deserialize] methods implementation.
 *
 * Structural description specifies how the [T] is represented in the serial form:
 * its [kind][SerialKind] (e.g. whether it is represented as a primitive, a list or a class),
 * its [elements][SerialDescriptor.elementNames] and their [positional names][SerialDescriptor.getElementName].
 *
 * Serialization process is defined as a sequence of calls to an [Encoder], and transforms a type [T]
 * into a stream of format-agnostic primitives that represent [T], such as "here is an int, here is a double
 * and here is another nested object". It can be demonstrated by the example:
 * ```
 * class MyData(int: Int, stringList: List<String>, alwaysZero: Long)
 *
 * // .. serialize method of a corresponding serializer
 * fun serialize(encoder: Encoder, value: MyData): Unit = encoder.encodeStructure(descriptor) {
 *     // encodeStructure encodes beginning and end of the structure
 *     // encode 'int' property as Int
 *     encodeIntElement(descriptor, index = 0, value.int)
 *     // encode 'stringList' property as List<String>
 *     encodeSerializableElement(descriptor, index = 1, serializer<List<String>>, value.stringList)
 *     // don't encode 'alwaysZero' property because we decided to do so
 * } // end of the structure
 * ```
 *
 * Deserialization process is symmetric and uses [Decoder].
 */
public interface KSerializer<T> : SerializationStrategy<T>, DeserializationStrategy<T> {
    /**
     * Describes the structure of the serializable representation of [T], produced
     * by this serializer. Knowing the structure of the descriptor is required to determine
     * the shape of the serialized form (e.g. what elements are encoded as lists and what as primitives)
     * along with its metadata such as alternative names.
     *
     * The descriptor is used during serialization by encoders and decoders
     * to introspect the type and metadata of [T]'s elements being encoded or decoded, and
     * to introspect the type, infer the schema or to compare against the predefined schema.
     */
    override val descriptor: SerialDescriptor

    @Deprecated(patchDeprecated, level = DeprecationLevel.ERROR)
    override fun patch(decoder: Decoder, old: T): T = throw UnsupportedOperationException("Not implemented, should not be called")
}

/**
 * Serialization strategy defines the serial form of a type [T], including its structural description,
 * declared by the [descriptor] and the actual serialization process, defined by the implementation
 * of the [serialize] method.
 *
 * [serialize] method takes an instance of [T] and transforms it into its serial form (a sequence of primitives),
 * calling the corresponding [Encoder] methods.
 *
 * A serial form of the type is a transformation of the concrete instance into a sequence of primitive values
 * and vice versa. The serial form is not required to completely mimic the structure of the class, for example,
 * a specific implementation may represent multiple integer values as a single string, omit or add some
 * values that are present in the type, but not in the instance.
 *
 * For a more detailed explanation of the serialization process, please refer to [KSerializer] documentation.
 */
public interface SerializationStrategy<in T> {
    /**
     * Describes the structure of the serializable representation of [T], produced
     * by this serializer.
     */
    public val descriptor: SerialDescriptor

    /**
     * Serializes the [value] of type [T] using the format that is represented by the given [encoder].
     * [serialize] method is format-agnostic and operates with a high-level structured [Encoder] API.
     * Throws [SerializationException] if value cannot be serialized.
     *
     * Example of serialize method:
     * ```
     * class MyData(int: Int, stringList: List<String>, alwaysZero: Long)
     *
     * fun serialize(encoder: Encoder, value: MyData): Unit = encoder.encodeStructure(descriptor) {
     *     // encodeStructure encodes beginning and end of the structure
     *     // encode 'int' property as Int
     *     encodeIntElement(descriptor, index = 0, value.int)
     *     // encode 'stringList' property as List<String>
     *     encodeSerializableElement(descriptor, index = 1, serializer<List<String>>, value.stringList)
     *     // don't encode 'alwaysZero' property because we decided to do so
     * } // end of the structure
     * ```
     */
    public fun serialize(encoder: Encoder, value: T)
}

/**
 * Deserialization strategy defines the serial form of a type [T], including its structural description,
 * declared by the [descriptor] and the actual deserialization process, defined by the implementation
 * of the [deserialize] method.
 *
 * [deserialize] method takes an instance of [Decoder], and, knowing the serial form of the [T],
 * invokes primitive retrieval methods on the decoder and then transforms the received primitives
 * to an instance of [T].
 *
 * A serial form of the type is a transformation of the concrete instance into a sequence of primitive values
 * and vice versa. The serial form is not required to completely mimic the structure of the class, for example,
 * a specific implementation may represent multiple integer values as a single string, omit or add some
 * values that are present in the type, but not in the instance.
 *
 * For a more detailed explanation of the serialization process, please refer to [KSerializer] documentation.
 */
public interface DeserializationStrategy<T> {
    /**
     * Describes the structure of the serializable representation of [T], that current
     * deserializer is able to deserialize.
     */
    public val descriptor: SerialDescriptor

    /**
     * Deserializes the value of type [T] using the format that is represented by the given [decoder].
     * [deserialize] method is format-agnostic and operates with a high-level structured [Decoder] API.
     * As long as most of the formats imply an arbitrary order of properties, deserializer should be able
     * to decode these properties in an arbitrary order and in a format-agnostic way.
     * For that purposes, [CompositeDecoder.decodeElementIndex]-based loop is used: decoder firstly
     * signals property at which index it is ready to decode and then expects caller to decode
     * property with the given index.
     *
     * Throws [SerializationException] if value cannot be deserialized.
     *
     * Example of serialize method:
     * ```
     * class MyData(int: Int, stringList: List<String>, alwaysZero: Long)
     *
     * fun deserialize(decoder: Decoder): MyData = decoder.decodeStructure(descriptor) {
     *     // decodeStructure decodes beginning and end of the structure
     *     var int: Int? = null
     *     var list: List<String>? = null
     *     loop@ while (true) {
     *         when (val index = decodeElementIndex(descriptor)) {
     *             DECODE_DONE -> break@loop
     *             0 -> {
     *                 // Decode 'int' property as Int
     *                 int = decodeIntElement(descriptor, index = 0)
     *             }
     *             1 -> {
     *                 // Decode 'stringList' property as List<String>
     *                 list = decodeSerializableElement(descriptor, index = 1, serializer<List<String>>())
     *             }
     *             else -> throw SerializationException("Unexpected index $index")
     *         }
     *      }
     *     if (int == null || list == null) throwMissingFieldException()
     *     // Always use 0 as a value for alwaysZero property because we decided to do so.
     *     return MyData(int, list, alwaysZero = 0L)
     * }
     * ```
     */
    public fun deserialize(decoder: Decoder): T

    @Deprecated(patchDeprecated, level = DeprecationLevel.ERROR)
    public fun patch(decoder: Decoder, old: T): T
}

// Can't be error yet because it's impossible to add default implementations for `val updateMode` in Decoder:
// 'Class JsonDecoder must implement updateMode because it inherits it from multiple interfaces'
// so users will have it in signature until we delete updateMode
@Deprecated(updateModeDeprecated, level = DeprecationLevel.WARNING)
@Suppress("NO_EXPLICIT_VISIBILITY_IN_API_MODE_WARNING")
public enum class UpdateMode {
    BANNED, OVERWRITE, UPDATE
}

internal const val patchDeprecated =
    "Patch function is deprecated for removal since this functionality is no longer supported by serializer." +
            "Some formats may provide implementation-specific patching in their Decoders."
