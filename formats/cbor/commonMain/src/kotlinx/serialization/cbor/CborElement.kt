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

}

/**
 * Class representing signed CBOR integer (major type 1).
 */
@Serializable(with = CborIntSerializer::class)
public class CborNegativeInt(public val value: Long) : CborPrimitive() {
    init {
        require(value < 0) { "Number must be negative: $value" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as CborNegativeInt
        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()
}

/**
 * Class representing unsigned CBOR integer (major type 0).
 */
@Serializable(with = CborUIntSerializer::class)
public class CborPositiveInt(public val value: ULong) : CborPrimitive() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as CborPositiveInt
        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()
}

/**
 * Class representing CBOR floating point value (major type 7).
 */
@Serializable(with = CborDoubleSerializer::class)
public class CborDouble(public val value: Double) : CborPrimitive() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as CborDouble
        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()
}


/**
 * Class representing CBOR string value.
 */
@Serializable(with = CborStringSerializer::class)
public class CborString(public val value: String) : CborPrimitive() {

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
 * Convenience method to get current element as [CborNegativeInt]
 * @throws IllegalArgumentException if current element is not a [CborNegativeInt]
 */
public val CborElement.cborNegativeInt: CborNegativeInt
    get() = this as? CborNegativeInt ?: error("CborNegativeInt")

/**
 * Convenience method to get current element as [CborPositiveInt]
 * @throws IllegalArgumentException if current element is not a [CborPositiveInt]
 */
public val CborElement.cborPositiveInt: CborPositiveInt
    get() = this as? CborPositiveInt ?: error("CborPositiveInt")

/**
 * Convenience method to get current element as [CborDouble]
 * @throws IllegalArgumentException if current element is not a [CborDouble]
 */
public val CborElement.cborDouble: CborDouble
    get() = this as? CborDouble ?: error("CborDouble")

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
 * Creates a [CborMap] from the given map entries.
 */
public fun CborMap(vararg pairs: Pair<CborElement, CborElement>): CborMap = CborMap(mapOf(*pairs))

/**
 * Creates a [CborList] from the given elements.
 */
public fun CborList(vararg elements: CborElement): CborList = CborList(listOf(*elements))

private fun CborElement.error(element: String): Nothing =
    throw IllegalArgumentException("Element ${this::class} is not a $element")