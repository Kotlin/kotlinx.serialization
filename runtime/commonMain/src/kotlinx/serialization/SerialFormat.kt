/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*

/**
 * A particular instance of a serialization format
 * that can interact with [KSerializer] and should be used as a main entry point
 * for serialization.
 *
 * Does not impose any particular restrictions on serialized form
 * or used storage; such restrictions are represented by
 * [SerialFormat]'s subinterfaces: [StringFormat] or [BinaryFormat].
 *
 * Typically, formats have their specific [Encoder] and [Decoder] implementations
 * as private inner classes and do not expose them.
 */
public interface SerialFormat {
    /**
     * Contains all serializers registered by format user for [ContextualSerialization] and [Polymorphic] serialization.
     *
     * The same context should be exposed in the format's [Encoder] and [Decoder].
     */
    public val context: SerialModule
}

@Deprecated(
    "Deprecated for removal since it is indistinguishable from SerialFormat interface. " +
            "Use SerialFormat instead.", ReplaceWith("SerialFormat"), DeprecationLevel.ERROR
)
public abstract class AbstractSerialFormat(override val context: SerialModule) : SerialFormat

/**
 * [SerialFormat] that allows conversion to and from [ByteArray]
 * via [dump] and [load] methods
 */
public interface BinaryFormat : SerialFormat {

    /**
     * Serializes [value] to [ByteArray] using given [serializer].
     */
    public fun <T> dump(serializer: SerializationStrategy<T>, value: T): ByteArray

    /**
     * Deserializes given [bytes] to an object of type [T] using given [deserializer].
     */
    public fun <T> load(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T
}

/**
 * Serializes [value] to [ByteArray] using given [serializer] and then
 * represents bytes in a human-readable form.
 *
 * Each byte from byte array represented by its hex value in range 00..FF
 * (i.e. each byte corresponds to two chars in resulting string).
 * Resulting string is lowercase and does not contain `0x` prefix.
 *
 * This method may be useful for debugging and testing.
 *
 * @see loads
 */
public fun <T> BinaryFormat.dumps(serializer: SerializationStrategy<T>, value: T): String =
    InternalHexConverter.printHexBinary(dump(serializer, value), lowerCase = true)

/**
 * Parses [hex] string as a byte array and then deserializes it to an object of type [T]
 * using given [deserializer].
 *
 * Hex string must be of even length, contain only 0-9 and A-F/a-f characters and does
 * not contain 0x prefix. Thus, each pair of characters is considered to be one byte, determined by
 * its hex value.
 *
 * This method may be useful for debugging and testing.
 *
 * @see dumps
 */
public fun <T> BinaryFormat.loads(deserializer: DeserializationStrategy<T>, hex: String): T =
    load(deserializer, InternalHexConverter.parseHexBinary(hex))

/**
 * [SerialFormat] that allows conversion to and from [String]
 * via [stringify] and [parse] methods
 */
public interface StringFormat : SerialFormat {
    /**
     * Serializes [value] to [String] using given [serializer].
     */
    public fun <T> stringify(serializer: SerializationStrategy<T>, value: T): String

    /**
     * Deserializes given [string] to an object of type [T] using given [deserializer].
     */
    public fun <T> parse(deserializer: DeserializationStrategy<T>, string: String): T
}
