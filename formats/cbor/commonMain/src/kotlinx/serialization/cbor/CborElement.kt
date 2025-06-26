/*
 * Copyright 2017-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("unused")

package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlinx.serialization.cbor.internal.*

/**
 * Class representing single CBOR element.
 * Can be [CborPrimitive], [CborMap] or [CborList].
 *
 * [CborElement.toString] properly prints CBOR tree as a human-readable representation.
 * Whole hierarchy is serializable, but only when used with [Cbor] as [CborElement] is purely CBOR-specific structure
 * which has a meaningful schemaless semantics only for CBOR.
 *
 * The whole hierarchy is [serializable][Serializable] only by [Cbor] format.
 */
@Serializable(with = CborElementSerializer::class)
public sealed class CborElement

/**
 * Class representing CBOR primitive value.
 * CBOR primitives include numbers, strings, booleans, byte arrays and special null value [CborNull].
 */
@Serializable(with = CborPrimitiveSerializer::class)
public sealed class CborPrimitive : CborElement() {
    /**
     * Content of given element as string. For [CborNull], this method returns a "null" string.
     * [CborPrimitive.contentOrNull] should be used for [CborNull] to get a `null`.
     */
    public abstract val content: String

    public override fun toString(): String = content
}

/**
 * Sealed class representing CBOR number value.
 * Can be either [Signed] or [Unsigned].
 */
@Serializable(with = CborNumberSerializer::class)
public sealed class CborNumber : CborPrimitive() {
    /**
     * Returns the value as a [Byte].
     */
    public abstract val byte: Byte

    /**
     * Returns the value as a [Short].
     */
    public abstract val short: Short

    /**
     * Returns the value as an [Int].
     */
    public abstract val int: Int

    /**
     * Returns the value as a [Long].
     */
    public abstract val long: Long

    /**
     * Returns the value as a [Float].
     */
    public abstract val float: Float

    /**
     * Returns the value as a [Double].
     */
    public abstract val double: Double

    /**
     * Class representing a signed CBOR number value.
     */
    public class Signed(@Contextual private val value: Number) : CborNumber() {
        override val content: String get() = value.toString()
        override val byte: Byte get() = value.toByte()
        override val short: Short get() = value.toShort()
        override val int: Int get() = value.toInt()
        override val long: Long get() = value.toLong()
        override val float: Float get() = value.toFloat()
        override val double: Double get() = value.toDouble()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null) return false

            when (other) {
                is Signed -> {
                    // Compare as double to handle different numeric types
                    return when {
                        // For integers, compare as long to avoid precision loss
                        value is Byte || value is Short || value is Int || value is Long ||
                        other.value is Byte || other.value is Short || other.value is Int || other.value is Long -> {
                            value.toLong() == other.value.toLong()
                        }
                        // For floating point, compare as double
                        else -> {
                            value.toDouble() == other.value.toDouble()
                        }
                    }
                }
                is Unsigned -> {
                    // Only compare if both are non-negative integers
                    if (value is Byte || value is Short || value is Int || value is Long) {
                        val longValue = value.toLong()
                        return longValue >= 0 && longValue.toULong() == other.long.toULong()
                    }
                    return false
                }
                else -> return false
            }
        }

        override fun hashCode(): Int = value.hashCode()
    }

    /**
     * Class representing an unsigned CBOR number value.
     */
    public class Unsigned(private val value: ULong) : CborNumber() {
        override val content: String get() = value.toString()
        override val byte: Byte get() = value.toByte()
        override val short: Short get() = value.toShort()
        override val int: Int get() = value.toInt()
        override val long: Long get() = value.toLong()
        override val float: Float get() = value.toFloat()
        override val double: Double get() = value.toDouble()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null) return false

            when (other) {
                is Unsigned -> {
                    return value == other.long.toULong()
                }
                is Signed -> {
                    // Only compare if the signed value is non-negative
                    val otherLong = other.long
                    return otherLong >= 0 && value == otherLong.toULong()
                }
                else -> return false
            }
        }

        override fun hashCode(): Int = value.hashCode()
    }
}

/**
 * Class representing CBOR string value.
 */
@Serializable(with = CborStringSerializer::class)
public class CborString(private val value: String) : CborPrimitive() {
    override val content: String get() = value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as CborString
        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()
}

/**
 * Class representing CBOR boolean value.
 */
@Serializable(with = CborBooleanSerializer::class)
public class CborBoolean(private val value: Boolean) : CborPrimitive() {
    override val content: String get() = value.toString()

    /**
     * Returns the boolean value.
     */
    public val boolean: Boolean get() = value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as CborBoolean
        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()
}

/**
 * Class representing CBOR byte string value.
 */
@Serializable(with = CborByteStringSerializer::class)
public class CborByteString(private val value: ByteArray) : CborPrimitive() {
    override val content: String get() = value.contentToString()

    /**
     * Returns the byte array value.
     */
    public val bytes: ByteArray get() = value.copyOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as CborByteString
        return value.contentEquals(other.value)
    }

    override fun hashCode(): Int = value.contentHashCode()
}

/**
 * Class representing CBOR `null` value
 */
@Serializable(with = CborNullSerializer::class)
public object CborNull : CborPrimitive() {
    override val content: String = "null"
}

/**
 * Class representing CBOR map, consisting of key-value pairs, where both key and value are arbitrary [CborElement]
 *
 * Since this class also implements [Map] interface, you can use
 * traditional methods like [Map.get] or [Map.getValue] to obtain CBOR elements.
 */
@Serializable(with = CborMapSerializer::class)
public class CborMap(
    private val content: Map<CborElement, CborElement>
) : CborElement(), Map<CborElement, CborElement> by content {
    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as CborMap
        return content == other.content
    }
    public override fun hashCode(): Int = content.hashCode()
    public override fun toString(): String {
        return content.entries.joinToString(
            separator = ", ",
            prefix = "{",
            postfix = "}",
            transform = { (k, v) -> "$k: $v" }
        )
    }
}

/**
 * Class representing CBOR array, consisting of indexed values, where value is arbitrary [CborElement]
 *
 * Since this class also implements [List] interface, you can use
 * traditional methods like [List.get] or [List.getOrNull] to obtain CBOR elements.
 */
@Serializable(with = CborListSerializer::class)
public class CborList(private val content: List<CborElement>) : CborElement(), List<CborElement> by content {
    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as CborList
        return content == other.content
    }
    public override fun hashCode(): Int = content.hashCode()
    public override fun toString(): String = content.joinToString(prefix = "[", postfix = "]", separator = ", ")
}

/**
 * Convenience method to get current element as [CborPrimitive]
 * @throws IllegalArgumentException if current element is not a [CborPrimitive]
 */
public val CborElement.cborPrimitive: CborPrimitive
    get() = this as? CborPrimitive ?: error("CborPrimitive")

/**
 * Convenience method to get current element as [CborMap]
 * @throws IllegalArgumentException if current element is not a [CborMap]
 */
public val CborElement.cborMap: CborMap
    get() = this as? CborMap ?: error("CborMap")

/**
 * Convenience method to get current element as [CborList]
 * @throws IllegalArgumentException if current element is not a [CborList]
 */
public val CborElement.cborList: CborList
    get() = this as? CborList ?: error("CborList")

/**
 * Convenience method to get current element as [CborNull]
 * @throws IllegalArgumentException if current element is not a [CborNull]
 */
public val CborElement.cborNull: CborNull
    get() = this as? CborNull ?: error("CborNull")

/**
 * Convenience method to get current element as [CborNumber]
 * @throws IllegalArgumentException if current element is not a [CborNumber]
 */
public val CborElement.cborNumber: CborNumber
    get() = this as? CborNumber ?: error("CborNumber")

/**
 * Convenience method to get current element as [CborString]
 * @throws IllegalArgumentException if current element is not a [CborString]
 */
public val CborElement.cborString: CborString
    get() = this as? CborString ?: error("CborString")

/**
 * Convenience method to get current element as [CborBoolean]
 * @throws IllegalArgumentException if current element is not a [CborBoolean]
 */
public val CborElement.cborBoolean: CborBoolean
    get() = this as? CborBoolean ?: error("CborBoolean")

/**
 * Convenience method to get current element as [CborByteString]
 * @throws IllegalArgumentException if current element is not a [CborByteString]
 */
public val CborElement.cborByteString: CborByteString
    get() = this as? CborByteString ?: error("CborByteString")

/**
 * Content of the given element as string or `null` if current element is [CborNull]
 */
public val CborPrimitive.contentOrNull: String? get() = if (this is CborNull) null else content

/**
 * Creates a [CborMap] from the given map entries.
 */
public fun CborMap(vararg pairs: Pair<CborElement, CborElement>): CborMap = CborMap(mapOf(*pairs))

/**
 * Creates a [CborList] from the given elements.
 */
public fun CborList(vararg elements: CborElement): CborList = CborList(listOf(*elements))

private fun CborElement.error(element: String): Nothing =
    throw IllegalArgumentException("Element ${this::class} is not a $element")
