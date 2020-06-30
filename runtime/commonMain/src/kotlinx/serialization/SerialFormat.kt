/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.*
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
 */
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
 */
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
 */
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
public inline fun <reified T : Any> StringFormat.encodeToString(value: T): String =
    encodeToString(serializersModule.getContextualOrDefault(), value)

/**
 * Decodes and deserializes the given [string] to to the value of type [T] using deserializer
 * retrieved from the reified type parameter.
 */
public inline fun <reified T : Any> StringFormat.decodeFromString(string: String): T =
    decodeFromString(serializersModule.getContextualOrDefault(), string)


/**
 * Serializes and encodes the given [value] to byte array, delegating it to the [BinaryFormat],
 * and then encodes resulting bytes to hex string.
 *
 * Hex representation does not interfere with serialization and encoding process of the format and
 * only applies transformation to the resulting array. It is recommended to use for debugging and
 * testing purposes.
 */
public fun <T> BinaryFormat.encodeToHexString(serializer: SerializationStrategy<T>, value: T): String =
    InternalHexConverter.printHexBinary(encodeToByteArray(serializer, value), lowerCase = true)

/**
 * Decodes byte array from the given [hex] string and the decodes and deserializes it
 * to the value of type [T], delegating it to the [BinaryFormat].
 *
 * This method is a counterpart to [encodeToHexString]
 */
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
public inline fun <reified T : Any> BinaryFormat.encodeToHexString(value: T): String =
    encodeToHexString(serializersModule.getContextualOrDefault(), value)

/**
 * Decodes byte array from the given [hex] string and the decodes and deserializes it
 * to the value of type [T], delegating it to the [BinaryFormat].
 *
 * This method is a counterpart to [encodeToHexString]
 */
public inline fun <reified T : Any> BinaryFormat.decodeFromHexString(hex: String): T =
    decodeFromHexString(serializersModule.getContextualOrDefault(), hex)

/**
 * Serializes and encodes the given [value] to byte array using serializer
 * retrieved from the reified type parameter.
 */
public inline fun <reified T : Any> BinaryFormat.encodeToByteArray(value: T): ByteArray =
    encodeToByteArray(serializersModule.getContextualOrDefault(), value)

/**
 * Decodes and deserializes the given [byte array][bytes] to to the value of type [T] using deserializer
 * retrieved from the reified type parameter.
 */
public inline fun <reified T : Any> BinaryFormat.decodeFromByteArray(bytes: ByteArray): T =
    decodeFromByteArray(serializersModule.getContextualOrDefault(), bytes)
