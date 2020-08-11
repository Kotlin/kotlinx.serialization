/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

/**
 * Represents an instance of a serialization format
 * that can interact with [KSerializer] and is a supertype of all entry points for a serialization.
 * It does not impose any restrictions on a serialized form or underlying storage, neither it exposes them.
 *
 * Concrete data types and API for user-interaction are responsibility of a concrete subclass or subinterface,
 * for example [StringFormat], [BinaryFormat] or [Json].
 *
 * Typically, formats have their specific [Encoder] and [Decoder] implementations
 * as private classes and do not expose them.
 *
 * ### Not stable for inheritance
 *
 * `SerialFormat` interface is not stable for inheritance in 3rd party libraries, as new methods
 * might be added to this interface or contracts of the existing methods can be changed.
 *
 * It is safe to operate with instances of `BinaryFormat` and call its methods.
 */
@ExperimentalSerializationApi
public interface SerialFormat {
    /**
     * Contains all serializers registered by format user for [Contextual] and [Polymorphic] serialization.
     *
     * The same module should be exposed in the format's [Encoder] and [Decoder].
     */
    public val serializersModule: SerializersModule
}

/**
 * [SerialFormat] that allows conversions to and from [ByteArray] via [encodeToByteArray] and [decodeFromByteArray] methods.
 *
 * ### Not stable for inheritance
 *
 * `BinaryFormat` interface is not stable for inheritance in 3rd party libraries, as new methods
 * might be added to this interface or contracts of the existing methods can be changed.
 *
 * It is safe to operate with instances of `BinaryFormat` and call its methods.
 */
@ExperimentalSerializationApi
public interface BinaryFormat : SerialFormat {

    /**
     * Serializes and encodes the given [value] to byte array using the given [serializer].
     */
    public fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray

    /**
     * Decodes and deserializes the given [byte array][bytes] to to the value of type [T] using the given [deserializer]
     */
    public fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T
}

/**
 * [SerialFormat] that allows conversions to and from [String] via [encodeToString] and [decodeFromString] methods.
 *
 * ### Not stable for inheritance
 *
 * `StringFormat` interface is not stable for inheritance in 3rd party libraries, as new methods
 * might be added to this interface or contracts of the existing methods can be changed.
 *
 * It is safe to operate with instances of `StringFormat` and call its methods.
 */
@ExperimentalSerializationApi
public interface StringFormat : SerialFormat {

    /**
     * Serializes and encodes the given [value] to string using the given [serializer].
     */
    public fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String

    /**
     * Decodes and deserializes the given [string] to to the value of type [T] using the given [deserializer]
     */
    public fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T
}

/**
 * Serializes and encodes the given [value] to string using serializer retrieved from the reified type parameter.
 */
@OptIn(ExperimentalSerializationApi::class)
public inline fun <reified T> StringFormat.encodeToString(value: T): String =
    encodeToString(serializersModule.serializer(), value)

/**
 * Decodes and deserializes the given [string] to to the value of type [T] using deserializer
 * retrieved from the reified type parameter.
 */
@OptIn(ExperimentalSerializationApi::class)
public inline fun <reified T> StringFormat.decodeFromString(string: String): T =
    decodeFromString(serializersModule.serializer(), string)


/**
 * Serializes and encodes the given [value] to byte array, delegating it to the [BinaryFormat],
 * and then encodes resulting bytes to hex string.
 *
 * Hex representation does not interfere with serialization and encoding process of the format and
 * only applies transformation to the resulting array. It is recommended to use for debugging and
 * testing purposes.
 */
@OptIn(ExperimentalSerializationApi::class)
public fun <T> BinaryFormat.encodeToHexString(serializer: SerializationStrategy<T>, value: T): String =
    InternalHexConverter.printHexBinary(encodeToByteArray(serializer, value), lowerCase = true)

/**
 * Decodes byte array from the given [hex] string and the decodes and deserializes it
 * to the value of type [T], delegating it to the [BinaryFormat].
 *
 * This method is a counterpart to [encodeToHexString]
 */
@OptIn(ExperimentalSerializationApi::class)
public fun <T> BinaryFormat.decodeFromHexString(deserializer: DeserializationStrategy<T>, hex: String): T =
    decodeFromByteArray(deserializer, InternalHexConverter.parseHexBinary(hex))

/**
 * Serializes and encodes the given [value] to byte array, delegating it to the [BinaryFormat],
 * and then encodes resulting bytes to hex string.
 *
 * Hex representation does not interfere with serialization and encoding process of the format and
 * only applies transformation to the resulting array. It is recommended to use for debugging and
 * testing purposes.
 */
@OptIn(ExperimentalSerializationApi::class)
public inline fun <reified T> BinaryFormat.encodeToHexString(value: T): String =
    encodeToHexString(serializersModule.serializer(), value)

/**
 * Decodes byte array from the given [hex] string and the decodes and deserializes it
 * to the value of type [T], delegating it to the [BinaryFormat].
 *
 * This method is a counterpart to [encodeToHexString]
 */
@OptIn(ExperimentalSerializationApi::class)
public inline fun <reified T> BinaryFormat.decodeFromHexString(hex: String): T =
    decodeFromHexString(serializersModule.serializer(), hex)

/**
 * Serializes and encodes the given [value] to byte array using serializer
 * retrieved from the reified type parameter.
 */
@OptIn(ExperimentalSerializationApi::class)
public inline fun <reified T> BinaryFormat.encodeToByteArray(value: T): ByteArray =
    encodeToByteArray(serializersModule.serializer(), value)

/**
 * Decodes and deserializes the given [byte array][bytes] to to the value of type [T] using deserializer
 * retrieved from the reified type parameter.
 */
@OptIn(ExperimentalSerializationApi::class)
public inline fun <reified T> BinaryFormat.decodeFromByteArray(bytes: ByteArray): T =
    decodeFromByteArray(serializersModule.serializer(), bytes)
