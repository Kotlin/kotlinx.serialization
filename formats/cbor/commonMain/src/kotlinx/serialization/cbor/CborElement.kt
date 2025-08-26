/*
 * Copyright 2017-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("unused")
@file:OptIn(ExperimentalUnsignedTypes::class)

package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlinx.serialization.cbor.internal.*

/**
 * Class representing single CBOR element.
 * Can be [CborPrimitive], [CborMap] or [CborList].
 *
 * [CborElement.toString] properly prints CBOR tree as a human-readable representation.
 * Whole hierarchy is serializable, but only when used with [Cbor] as [CborElement] is purely CBOR-specific structure
 * which has meaningful schemaless semantics only for CBOR.
 *
 * The whole hierarchy is [serializable][Serializable] only by [Cbor] format.
 */
@Serializable(with = CborElementSerializer::class)
public sealed class CborElement(
    /**
     * CBOR tags associated with this element.
     * Tags are optional semantic tagging of other major types (major type 6).
     * See [RFC 8949 3.4. Tagging of Items](https://datatracker.ietf.org/doc/html/rfc8949#name-tagging-of-items).
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    tags: ULongArray = ulongArrayOf()

) {
    /**
     * CBOR tags associated with this element.
     * Tags are optional semantic tagging of other major types (major type 6).
     * See [RFC 8949 3.4. Tagging of Items](https://datatracker.ietf.org/doc/html/rfc8949#name-tagging-of-items).
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    public var tags: ULongArray = tags
        internal set //need this to collect

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CborElement) return false

        if (!tags.contentEquals(other.tags)) return false

        return true
    }

    override fun hashCode(): Int {
        return tags.contentHashCode()
    }

}

/**
 * Class representing CBOR primitive value.
 * CBOR primitives include numbers, strings, booleans, byte arrays and special null value [CborNull].
 */
@Serializable(with = CborPrimitiveSerializer::class)
public sealed class CborPrimitive<T : Any>(
    public val value: T,
    tags: ULongArray = ulongArrayOf()
) : CborElement(tags) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CborPrimitive<*>) return false
        if (!super.equals(other)) return false

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }

    override fun toString(): String {
        return "${this::class.simpleName}(" +
            "tags=${tags.joinToString(prefix = "[", postfix = "]")}, " +
            "value=$value" +
            ")"
    }
}

/**
 * Class representing either:
 * * signed CBOR integer (major type 1 encompassing `-2^64..-1`)
 * * unsigned CBOR integer (major type 0 encompassing `0..2^64-1`)
 *
 * depending on the value of [sign]. Note that [absoluteValue] **must** be `0` when sign is set to [Sign.ZERO]
 */
@Serializable(with = CborIntSerializer::class)
public class CborInt(
    absoluteValue: ULong,
    public val sign: Sign,
    vararg tags: ULong
) : CborPrimitive<ULong>(absoluteValue, tags) {

    init {
        if (sign == Sign.ZERO) require(absoluteValue == 0uL) { "Illegal absolute value $absoluteValue for Sign.ZERO" }
    }

    public enum class Sign {
        POSITIVE,
        NEGATIVE,
        ZERO
    }

    /**
     * **WARNING! Possible truncation/overflow!** E.g., `-2^64` -> `1`
     */
    public fun toLong(): Long = when (sign) {
        Sign.POSITIVE, Sign.ZERO -> value.toLong()
        Sign.NEGATIVE -> -value.toLong()
    }


    public companion object {
        /**
         * Creates:
         * * signed CBOR integer (major type 1 encompassing `-2^64..-1`)
         * * unsigned CBOR integer (major type 0 encompassing `0..2^64-1`)
         *
         * depending on whether a positive or a negative number was passed.
         * If you want to create a negative number exceeding [Long.MIN_VALUE], manually specify sign: `CborInt(ULong.MAX_VALUE, CborInt.Sign.NEGATIVE)`
         */
        public operator fun invoke(
            value: Long,
            vararg tags: ULong
        ): CborInt =
            if (value == 0L) CborInt(value.toULong(), Sign.ZERO, tags = tags)
            else if (value > 0L) CborInt(value.toULong(), Sign.POSITIVE, tags = tags)
            else CborInt(ULong.MAX_VALUE - value.toULong() + 1uL, Sign.NEGATIVE, tags = tags)

        /**
         * Creates an unsigned CBOR integer (major type 0).
         */
        public operator fun invoke(
            value: ULong,
            vararg tags: ULong
        ): CborInt = if (value == 0uL) CborInt(value, Sign.ZERO, tags = tags)
        else CborInt(value, Sign.POSITIVE, tags = tags)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CborInt) return false
        if (!super.equals(other)) return false

        if (sign != other.sign) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + sign.hashCode()
        return result
    }

    override fun toString(): String {
        return "CborInt(tags=${tags.joinToString(prefix = "[", postfix = "]")}, " +
            "value=" + when (sign) {
            Sign.POSITIVE, Sign.ZERO -> ""
            Sign.NEGATIVE -> "-"
        } +
            value +
            ")"
    }
}

/**
 * Class representing CBOR floating point value (major type 7).
 */
@Serializable(with = CborFloatSerializer::class)
public class CborFloat(
    value: Double,
    vararg tags: ULong
) : CborPrimitive<Double>(value, tags)

/**
 * Class representing CBOR string value.
 */
@Serializable(with = CborStringSerializer::class)
public class CborString(
    value: String,
    vararg tags: ULong
) : CborPrimitive<String>(value, tags)

/**
 * Class representing CBOR boolean value.
 */
@Serializable(with = CborBooleanSerializer::class)
public class CborBoolean(
    value: Boolean,
    vararg tags: ULong
) : CborPrimitive<Boolean>(value, tags)

/**
 * Class representing CBOR byte string value.
 */
@Serializable(with = CborByteStringSerializer::class)
public class CborByteString(
    value: ByteArray,
    vararg tags: ULong
) : CborPrimitive<ByteArray>(value, tags) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CborByteString) return false
        if (!tags.contentEquals(other.tags)) return false
        return value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        var result = tags.contentHashCode()
        result = 31 * result + (value.contentHashCode())
        return result
    }

    override fun toString(): String {
        return "CborByteString(" +
            "tags=${tags.joinToString(prefix = "[", postfix = "]")}, " +
            "value=h'${value.toHexString()}" +
            ")"
    }
}

/**
 * Class representing CBOR `null` value
 */
@Serializable(with = CborNullSerializer::class)
public class CborNull(vararg tags: ULong) : CborPrimitive<Unit>(Unit, tags)

/**
 * Class representing CBOR map, consisting of key-value pairs, where both key and value are arbitrary [CborElement]
 *
 * Since this class also implements [Map] interface, you can use
 * traditional methods like [Map.get] or [Map.getValue] to obtain CBOR elements.
 */
@Serializable(with = CborMapSerializer::class)
public class CborMap(
    private val content: Map<CborElement, CborElement>,
    vararg tags: ULong
) : CborElement(tags), Map<CborElement, CborElement> by content {

    public override fun equals(other: Any?): Boolean =
        other is CborMap && other.content == content && other.tags.contentEquals(tags)

    public override fun hashCode(): Int = content.hashCode() * 31 + tags.contentHashCode()

    override fun toString(): String {
        return "CborMap(" +
            "tags=${tags.joinToString(prefix = "[", postfix = "]")}, " +
            "content=$content" +
            ")"
    }

}

/**
 * Class representing CBOR array, consisting of CBOR elements.
 *
 * Since this class also implements [List] interface, you can use
 * traditional methods like [List.get] or [List.size] to obtain CBOR elements.
 */
@Serializable(with = CborListSerializer::class)
public class CborList(
    private val content: List<CborElement>,
    vararg tags: ULong
) : CborElement(tags), List<CborElement> by content {

    public override fun equals(other: Any?): Boolean =
        other is CborList && other.content == content && other.tags.contentEquals(tags)

    public override fun hashCode(): Int = content.hashCode() * 31 + tags.contentHashCode()

    override fun toString(): String {
        return "CborList(" +
            "tags=${tags.joinToString(prefix = "[", postfix = "]")}, " +
            "content=$content" +
            ")"
    }

}