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
 * which has a meaningful schemaless semantics only for CBOR.
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
        internal set

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
        return "CborPrimitive(" +
            "kind=${value::class.simpleName}, " +
            "tags=${tags.joinToString()}, " +
            "value=$value" +
            ")"
    }
}

public sealed class CborInt<T : Any>(
    tags: ULongArray = ulongArrayOf(),
    value: T,
) : CborPrimitive<T>(value, tags) {
    public companion object {
        public operator fun invoke(
            value: Long,
            tags: ULongArray = ulongArrayOf()
        ): CborInt<*> = if (value >= 0) CborPositiveInt(value.toULong(), tags) else CborNegativeInt(value, tags)

        public operator fun invoke(
            value: ULong,
            tags: ULongArray = ulongArrayOf()
        ): CborInt<ULong> = CborPositiveInt(value, tags)
    }
}

/**
 * Class representing signed CBOR integer (major type 1).
 */
@Serializable(with = CborIntSerializer::class)
public class CborNegativeInt(
    value: Long,
    tags: ULongArray = ulongArrayOf()
) : CborInt<Long>(tags, value) {
    init {
        require(value < 0) { "Number must be negative: $value" }
    }
}

/**
 * Class representing unsigned CBOR integer (major type 0).
 */
@Serializable(with = CborUIntSerializer::class)
public class CborPositiveInt(
    value: ULong,
    tags: ULongArray = ulongArrayOf()
) : CborInt<ULong>(tags, value)

/**
 * Class representing CBOR floating point value (major type 7).
 */
@Serializable(with = CborDoubleSerializer::class)
public class CborDouble(
    value: Double,
    tags: ULongArray = ulongArrayOf()
) : CborPrimitive<Double>(value, tags)

/**
 * Class representing CBOR string value.
 */
@Serializable(with = CborStringSerializer::class)
public class CborString(
    value: String,
    tags: ULongArray = ulongArrayOf()
) : CborPrimitive<String>(value, tags)

/**
 * Class representing CBOR boolean value.
 */
@Serializable(with = CborBooleanSerializer::class)
public class CborBoolean(
    value: Boolean,
    tags: ULongArray = ulongArrayOf()
) : CborPrimitive<Boolean>(value, tags)

/**
 * Class representing CBOR byte string value.
 */
@Serializable(with = CborByteStringSerializer::class)
public class CborByteString(
    value: ByteArray,
    tags: ULongArray = ulongArrayOf()
) : CborPrimitive<ByteArray>(value, tags) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CborByteString) return false
        if (!super.equals(other)) return false
        return value.contentEquals(other.value)
    }
    override fun toString(): String {
        return "CborPrimitive(" +
            "kind=${value::class.simpleName}, " +
            "tags=${tags.joinToString()}, " +
            "value=h'${value.toHexString()}" +
            ")"
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (value.contentHashCode())
        return result
    }

}

/**
 * Class representing CBOR `null` value
 */
@Serializable(with = CborNullSerializer::class)
public class CborNull(tags: ULongArray = ulongArrayOf()) : CborPrimitive<Unit>(Unit, tags)

/**
 * Class representing CBOR map, consisting of key-value pairs, where both key and value are arbitrary [CborElement]
 *
 * Since this class also implements [Map] interface, you can use
 * traditional methods like [Map.get] or [Map.getValue] to obtain CBOR elements.
 */
@Serializable(with = CborMapSerializer::class)
public class CborMap(
    private val content: Map<CborElement, CborElement>,
    tags: ULongArray = ulongArrayOf()
) : CborElement(tags), Map<CborElement, CborElement> by content {

    public override fun equals(other: Any?): Boolean =
        other is CborMap && other.content == content && other.tags.contentEquals(tags)

    public override fun hashCode(): Int = content.hashCode() * 31 + tags.contentHashCode()
    override fun toString(): String {
        return "CborMap(" +
            "tags=${tags.joinToString()}, " +
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
    tags: ULongArray = ulongArrayOf()
) : CborElement(tags), List<CborElement> by content {

    public override fun equals(other: Any?): Boolean =
        other is CborList && other.content == content && other.tags.contentEquals(tags)

    public override fun hashCode(): Int = content.hashCode() * 31 + tags.contentHashCode()
    override fun toString(): String {
        return "CborList(" +
            "tags=${tags.joinToString()}, " +
            "content=$content" +
            ")"
    }

}