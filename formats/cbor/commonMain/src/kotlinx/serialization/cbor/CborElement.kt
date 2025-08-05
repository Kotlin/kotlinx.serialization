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

}

/**
 * Class representing CBOR primitive value.
 * CBOR primitives include numbers, strings, booleans, byte arrays and special null value [CborNull].
 */
@Serializable(with = CborPrimitiveSerializer::class)
public sealed class CborPrimitive(
    tags: ULongArray = ulongArrayOf()
) : CborElement(tags)

/**
 * Class representing signed CBOR integer (major type 1).
 */
@Serializable(with = CborIntSerializer::class)
public class CborNegativeInt(
    public val value: Long,
    tags: ULongArray = ulongArrayOf()
) : CborPrimitive(tags) {
    init {
        require(value < 0) { "Number must be negative: $value" }
    }

    override fun equals(other: Any?): Boolean =
        other is CborNegativeInt && other.value == value && other.tags.contentEquals(tags)

    override fun hashCode(): Int = value.hashCode() * 31 + tags.contentHashCode()
}

/**
 * Class representing unsigned CBOR integer (major type 0).
 */
@Serializable(with = CborUIntSerializer::class)
public class CborPositiveInt(
    public val value: ULong,
    tags: ULongArray = ulongArrayOf()
) : CborPrimitive(tags) {

    override fun equals(other: Any?): Boolean =
        other is CborPositiveInt && other.value == value && other.tags.contentEquals(tags)

    override fun hashCode(): Int = value.hashCode() * 31 + tags.contentHashCode()
}

public fun CborInt(
    value: Long,
    tags: ULongArray = ulongArrayOf()
): CborPrimitive = if (value >= 0) CborPositiveInt(value.toULong(), tags) else CborNegativeInt(value, tags)

/**
 * Class representing CBOR floating point value (major type 7).
 */
@Serializable(with = CborDoubleSerializer::class)
public class CborDouble(
    public val value: Double,
    tags: ULongArray = ulongArrayOf()
) : CborPrimitive(tags) {

    override fun equals(other: Any?): Boolean =
        other is CborDouble && other.value == value && other.tags.contentEquals(tags)

    override fun hashCode(): Int = value.hashCode() * 31 + tags.contentHashCode()
}

/**
 * Class representing CBOR string value.
 */
@Serializable(with = CborStringSerializer::class)
public class CborString(
    public val value: String,
    tags: ULongArray = ulongArrayOf()
) : CborPrimitive(tags) {

    override fun equals(other: Any?): Boolean =
        other is CborString && other.value == value && other.tags.contentEquals(tags)

    override fun hashCode(): Int = value.hashCode() * 31 + tags.contentHashCode()
}

/**
 * Class representing CBOR boolean value.
 */
@Serializable(with = CborBooleanSerializer::class)
public class CborBoolean(
    private val value: Boolean,
    tags: ULongArray = ulongArrayOf()
) : CborPrimitive(tags) {

    /**
     * Returns the boolean value.
     */
    public val boolean: Boolean get() = value

    override fun equals(other: Any?): Boolean =
        other is CborBoolean && other.value == value && other.tags.contentEquals(tags)

    override fun hashCode(): Int = value.hashCode() * 31 + tags.contentHashCode()
}

/**
 * Class representing CBOR byte string value.
 */
@Serializable(with = CborByteStringSerializer::class)
public class CborByteString(
    private val value: ByteArray,
    tags: ULongArray = ulongArrayOf()
) : CborPrimitive(tags) {

    /**
     * Returns the byte array value.
     */
    public val bytes: ByteArray get() = value.copyOf()

    override fun equals(other: Any?): Boolean =
        other is CborByteString && other.value.contentEquals(value) && other.tags.contentEquals(tags)

    override fun hashCode(): Int = value.contentHashCode() * 31 + tags.contentHashCode()
}

/**
 * Class representing CBOR `null` value
 */
@Serializable(with = CborNullSerializer::class)
public class CborNull(tags: ULongArray = ulongArrayOf()) : CborPrimitive(tags) {
    // Note: CborNull is an object, so it cannot have constructor parameters for tags
    // If tags are needed for null values, this would need to be changed to a class
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CborNull) return false
        return true
    }

    override fun hashCode(): Int {
        return this::class.hashCode()
    }
}

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

    public override fun toString(): String = content.toString()
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

    public override fun toString(): String = content.toString()
}