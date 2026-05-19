@file:Suppress("unused")
@file:OptIn(ExperimentalUnsignedTypes::class)

package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlinx.serialization.cbor.internal.*

@OptIn(ExperimentalUnsignedTypes::class)
internal val EMPTY_TAGS: ULongArray = ULongArray(0)

/**
 * Class representing single CBOR element.
 * Can be [CborPrimitive], [CborMap] or [CborArray].
 *
 * [CborElement.toString] properly prints CBOR tree as a human-readable representation.
 * Whole hierarchy is serializable, but only when used with [Cbor] as [CborElement] is purely CBOR-specific structure
 * which has meaningful schemaless semantics only for CBOR.
 *
 * The whole hierarchy is [serializable][Serializable] only by [Cbor] format.
 */
@Serializable(with = CborElementSerializer::class)
@ExperimentalSerializationApi
public sealed class CborElement(
    /**
     * CBOR tags associated with this element.
     * Tags are optional semantic tagging of other major types (major type 6).
     * See [RFC 8949 3.4. Tagging of Items](https://datatracker.ietf.org/doc/html/rfc8949#name-tagging-of-items).
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    tags: ULongArray = EMPTY_TAGS

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
@ExperimentalSerializationApi
public sealed class CborPrimitive(
    tags: ULongArray = EMPTY_TAGS
) : CborElement(tags)

/**
 * Class representing either:
 * * signed CBOR integer (major type 1 encompassing `-2^64..-1`)
 * * unsigned CBOR integer (major type 0 encompassing `0..2^64-1`)
 *
 * depending on the value of [isPositive]. Note that [absoluteValue] **must not be** `0` when [isPositive] is set to `false`.
 */
@Serializable(with = CborIntSerializer::class)
@ExperimentalSerializationApi
public class CborInteger(
    public val absoluteValue: ULong,
    public val isPositive: Boolean,
    vararg tags: ULong
) : CborPrimitive(tags) {
    init {
        if (!isPositive) require(absoluteValue > 0uL) { "Illegal absolute value $absoluteValue for a negative number." }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CborInteger) return false
        if (!super.equals(other)) return false

        if (absoluteValue != other.absoluteValue) return false
        if (isPositive != other.isPositive) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + absoluteValue.hashCode()
        result = 31 * result + isPositive.hashCode()
        return result
    }

    override fun toString(): String {
        return "CborInt(tags=${tags.joinToString(prefix = "[", postfix = "]")}, " +
            "absoluteValue=" + when (isPositive) {
           true -> ""
           false -> "-"
        } +
            absoluteValue +
            ")"
    }
}

/**
 * Creates:
 * * signed CBOR integer (major type 1 encompassing `-2^64..-1`)
 * * unsigned CBOR integer (major type 0 encompassing `0..2^64-1`)
 *
 * depending on whether a positive or a negative number was passed.
 * If you want to create a negative number exceeding [Long.MIN_VALUE], manually specify sign: `CborInt(ULong.MAX_VALUE, isPositive = false)`.
 */
@ExperimentalSerializationApi
@Suppress("FunctionName")
public fun CborInteger(value: Long, vararg tags: ULong): CborInteger =
    if (value >= 0L) CborInteger(value.toULong(), isPositive = true, tags = tags)
    else CborInteger(ULong.MAX_VALUE - value.toULong() + 1uL, isPositive = false, tags = tags)

/**
 * Creates an unsigned CBOR integer (major type 0).
 */
@ExperimentalSerializationApi
@Suppress("FunctionName")
public fun CborInteger(value: ULong, vararg tags: ULong): CborInteger =
    CborInteger(value, isPositive = true, tags = tags)

/**
 * Converts this integer to [Long], throwing if it cannot be represented as [Long].
 */
@ExperimentalSerializationApi
public val CborInteger.long: Long
    get() = longOrNull ?: throw ArithmeticException("$this cannot be represented as Long")

/**
 * Converts this integer to [Long], or returns `null` if it cannot be represented as [Long].
 */
@ExperimentalSerializationApi
public val CborInteger.longOrNull: Long?
    get() {
        val max = Long.MAX_VALUE.toULong()
        return if (isPositive) {
            if (absoluteValue <= max) absoluteValue.toLong() else null
        } else {
            when {
                absoluteValue <= max -> -absoluteValue.toLong()
                absoluteValue == max + 1uL -> Long.MIN_VALUE
                else -> null
            }
        }
    }

/**
 * Converts this integer to [Int], throwing if it cannot be represented as [Int].
 */
@ExperimentalSerializationApi
public val CborInteger.int: Int
    get() = intOrNull ?: throw ArithmeticException("$this cannot be represented as Int")

/**
 * Converts this integer to [Int], or returns `null` if it cannot be represented as [Int].
 */
@ExperimentalSerializationApi
public val CborInteger.intOrNull: Int?
    get() {
        val longValue = longOrNull ?: return null
        if (longValue !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) return null
        return longValue.toInt()
    }

/**
 * Converts this integer to [Short], throwing if it cannot be represented as [Short].
 */
@ExperimentalSerializationApi
public val CborInteger.short: Short
    get() = shortOrNull ?: throw ArithmeticException("$this cannot be represented as Short")

/**
 * Converts this integer to [Short], or returns `null` if it cannot be represented as [Short].
 */
@ExperimentalSerializationApi
public val CborInteger.shortOrNull: Short?
    get() {
        val longValue = longOrNull ?: return null
        if (longValue !in Short.MIN_VALUE.toLong()..Short.MAX_VALUE.toLong()) return null
        return longValue.toShort()
    }

/**
 * Converts this integer to [Byte], throwing if it cannot be represented as [Byte].
 */
@ExperimentalSerializationApi
public val CborInteger.byte: Byte
    get() = byteOrNull ?: throw ArithmeticException("$this cannot be represented as Byte")

/**
 * Converts this integer to [Byte], or returns `null` if it cannot be represented as [Byte].
 */
@ExperimentalSerializationApi
public val CborInteger.byteOrNull: Byte?
    get() {
        val longValue = longOrNull ?: return null
        if (longValue !in Byte.MIN_VALUE.toLong()..Byte.MAX_VALUE.toLong()) return null
        return longValue.toByte()
    }

/**
 * Class representing CBOR floating point value (major type 7).
 */
@Serializable(with = CborFloatSerializer::class)
@ExperimentalSerializationApi
public class CborFloat(
    public val value: Double,
    vararg tags: ULong
) : CborPrimitive(tags) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CborFloat) return false
        if (!super.equals(other)) return false
        return value.equals(other.value)
    }

    override fun hashCode(): Int = 31 * super.hashCode() + value.hashCode()

    override fun toString(): String {
        return "CborFloat(" +
            "tags=${tags.joinToString(prefix = "[", postfix = "]")}, " +
            "value=$value" +
            ")"
    }
}

/**
 * Class representing CBOR string value.
 */
@Serializable(with = CborStringSerializer::class)
@ExperimentalSerializationApi
public class CborString(
    public val value: String,
    vararg tags: ULong
) : CborPrimitive(tags) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CborString) return false
        if (!super.equals(other)) return false
        return value == other.value
    }

    override fun hashCode(): Int = 31 * super.hashCode() + value.hashCode()

    override fun toString(): String {
        return "CborString(" +
            "tags=${tags.joinToString(prefix = "[", postfix = "]")}, " +
            "value=$value" +
            ")"
    }
}

/**
 * Class representing CBOR boolean value.
 */
@Serializable(with = CborBooleanSerializer::class)
@ExperimentalSerializationApi
public class CborBoolean(
    public val value: Boolean,
    vararg tags: ULong
) : CborPrimitive(tags) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CborBoolean) return false
        if (!super.equals(other)) return false
        return value == other.value
    }

    override fun hashCode(): Int = 31 * super.hashCode() + value.hashCode()

    override fun toString(): String {
        return "CborBoolean(" +
            "tags=${tags.joinToString(prefix = "[", postfix = "]")}, " +
            "value=$value" +
            ")"
    }
}

/**
 * Class representing CBOR byte string value.
 */
@Serializable(with = CborByteStringSerializer::class)
@ExperimentalSerializationApi
public class CborByteString(
    private val bytes: ByteArray,
    vararg tags: ULong
) : CborPrimitive(tags) {
    /**
     * Returns a copy of this CBOR byte string contents.
     */
    public fun toByteArray(): ByteArray = bytes.copyOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CborByteString) return false
        if (!tags.contentEquals(other.tags)) return false
        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = tags.contentHashCode()
        result = 31 * result + (bytes.contentHashCode())
        return result
    }

    override fun toString(): String {
        return "CborByteString(" +
            "tags=${tags.joinToString(prefix = "[", postfix = "]")}, " +
            "bytes=h'${bytes.toHexString()}" +
            ")"
    }

    internal fun getBytes(): ByteArray = bytes
}

/**
 * Class representing CBOR `null` value
 */
@Serializable(with = CborNullSerializer::class)
@ExperimentalSerializationApi
public class CborNull(vararg tags: ULong) : CborPrimitive(tags) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CborNull) return false
        return super.equals(other)
    }

    override fun hashCode(): Int = CborNull::class.hashCode() * 31 + super.hashCode()

    override fun toString(): String {
        return "CborNull(tags=${tags.joinToString(prefix = "[", postfix = "]")})"
    }
}

/**
 * Class representing CBOR `undefined` value
 */
@Serializable(with = CborUndefinedSerializer::class)
@ExperimentalSerializationApi
public class CborUndefined(vararg tags: ULong) : CborPrimitive(tags) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CborUndefined) return false
        return super.equals(other)
    }

    override fun hashCode(): Int = CborUndefined::class.hashCode() * 31 + super.hashCode()

    override fun toString(): String {
        return "CborUndefined(tags=${tags.joinToString(prefix = "[", postfix = "]")})"
    }
}

/**
 * Class representing CBOR map, consisting of key-value pairs, where both key and value are arbitrary [CborElement]
 *
 * Since this class also implements [Map] interface, you can use
 * traditional methods like [Map.get] or [Map.getValue] to obtain CBOR elements.
 */
@Serializable(with = CborMapSerializer::class)
@ExperimentalSerializationApi
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

    public operator fun get(key: String): CborElement? = content[CborString(key)]
    public fun getValue(key: String): CborElement = content.getValue(CborString(key))

    public operator fun get(key: Long): CborElement? = content[CborInteger(key)]
    public fun getValue(key: Long): CborElement = content.getValue(CborInteger(key))

    public operator fun get(key: Int): CborElement? = content[CborInteger(key.toLong())]
    public fun getValue(key: Int): CborElement = content.getValue(CborInteger(key.toLong()))

}

/**
 * Class representing CBOR array consisting of CBOR elements.
 *
 * Since this class also implements [List] interface, you can use
 * traditional methods like [List.get] or [List.size] to obtain CBOR elements.
 */
@Serializable(with = CborArraySerializer::class)
@ExperimentalSerializationApi
public class CborArray(
    private val content: List<CborElement>,
    vararg tags: ULong
) : CborElement(tags), List<CborElement> by content {

    public override fun equals(other: Any?): Boolean =
        other is CborArray && other.content == content && other.tags.contentEquals(tags)

    public override fun hashCode(): Int = content.hashCode() * 31 + tags.contentHashCode()

    override fun toString(): String {
        return "CborArray(" +
            "tags=${tags.joinToString(prefix = "[", postfix = "]")}, " +
            "content=$content" +
            ")"
    }

}
