@file:Suppress("unused")
@file:OptIn(ExperimentalUnsignedTypes::class, DelicateCborApi::class)

package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlinx.serialization.cbor.internal.*
import kotlin.jvm.*

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
@ExperimentalSerializationApi
@Serializable(with = CborElementSerializer::class)
public sealed class CborElement(
    /**
     * CBOR tags associated with this element.
     * Tags are optional semantic tagging of other major types (major type 6).
     * See [RFC 8949 3.4. Tagging of Items](https://datatracker.ietf.org/doc/html/rfc8949#name-tagging-of-items).
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    tags: ULongArray = EMPTY_TAGS

) {

    @OptIn(ExperimentalUnsignedTypes::class)
    @DelicateCborApi
    internal var rawTags: ULongArray = tags

    /**
     * CBOR tags associated with this element.
     * Tags are optional semantic tagging of other major types (major type 6).
     * See [RFC 8949 3.4. Tagging of Items](https://datatracker.ietf.org/doc/html/rfc8949#name-tagging-of-items).
     */
    public val tags: List<ULong> by lazy { rawTags.toList() }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CborElement) return false

        if (!rawTags.contentEquals(other.rawTags)) return false

        return true
    }

    override fun hashCode(): Int {
        return rawTags.contentHashCode()
    }

}

/**
 * Class representing CBOR primitive value.
 * CBOR primitives include numbers, strings, booleans, byte arrays and special null value [CborNull].
 */
@ExperimentalSerializationApi
@Serializable(with = CborPrimitiveSerializer::class)
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
@ExperimentalSerializationApi
@Serializable(with = CborIntSerializer::class)
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
        return "CborInt(tags=${rawTags.joinToString(prefix = "[", postfix = "]")}, " +
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
public fun CborInteger(value: Long, vararg tags: ULong): CborInteger =
    if (value >= 0L) CborInteger(value.toULong(), isPositive = true, tags = tags)
    else CborInteger(ULong.MAX_VALUE - value.toULong() + 1uL, isPositive = false, tags = tags)

/**
 * Creates an unsigned CBOR integer (major type 0).
 */
@ExperimentalSerializationApi
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
@ExperimentalSerializationApi
@Serializable(with = CborFloatSerializer::class)
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
            "tags=${rawTags.joinToString(prefix = "[", postfix = "]")}, " +
            "value=$value" +
            ")"
    }
}

/**
 * Class representing CBOR string value.
 */
@ExperimentalSerializationApi
@Serializable(with = CborStringSerializer::class)
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
            "tags=${rawTags.joinToString(prefix = "[", postfix = "]")}, " +
            "value=$value" +
            ")"
    }
}

/**
 * Class representing CBOR boolean value.
 */
@ExperimentalSerializationApi
@Serializable(with = CborBooleanSerializer::class)
public class CborBoolean(
    public val value: Boolean,
    vararg tags: ULong
) : CborPrimitive(tags) {

    public constructor(value: Boolean, tags: List<ULong>) : this(value, *(tags.toULongArray()))

    /**
     * Creates a [CborBoolean] from the provided [CborBoolean]'s [value], and the specified [tags].
     *
     * @param value The [CborBoolean] instance whose boolean value is used to initialize this object.
     * @param tags A list of tags to be set with the new instance.
     */
    public constructor(value: CborBoolean, tags: List<ULong>) : this(value.value, *(tags.toULongArray()))
    public constructor(value: CborBoolean, vararg tags: ULong) : this(value.value, *tags)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CborBoolean) return false
        if (!super.equals(other)) return false
        return value == other.value
    }

    override fun hashCode(): Int = 31 * super.hashCode() + value.hashCode()

    override fun toString(): String {
        return "CborBoolean(" +
            "tags=${rawTags.joinToString(prefix = "[", postfix = "]")}, " +
            "value=$value" +
            ")"
    }
}

/**
 * Class representing CBOR byte string value.
 */
@ExperimentalSerializationApi
@Serializable(with = CborByteStringSerializer::class)
public class CborByteString(
    bytes: ByteArray,
    vararg tags: ULong
) : CborPrimitive(tags) {

    public constructor(bytes: ByteArray, tags: List<ULong>) : this(bytes, *(tags.toULongArray()))

    @DelicateCborApi
    public val bytes: ByteArray = bytes

    /**
     * Returns a deep copy of this CBOR byte string contents.
     */
    public fun toByteArray(): ByteArray = bytes.copyOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CborByteString) return false
        if (!rawTags.contentEquals(other.rawTags)) return false
        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = rawTags.contentHashCode()
        result = 31 * result + (bytes.contentHashCode())
        return result
    }

    override fun toString(): String {
        return "CborByteString(" +
            "tags=${rawTags.joinToString(prefix = "[", postfix = "]")}, " +
            "bytes=h'${bytes.toHexString()}" +
            ")"
    }

    internal fun getBytes(): ByteArray = bytes
}

/**
 * Class representing CBOR `null` value
 */
@ExperimentalSerializationApi
@Serializable(with = CborNullSerializer::class)
public class CborNull(vararg tags: ULong) : CborPrimitive(tags) {

    public constructor(tags: List<ULong>) : this(*(tags.toULongArray()))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CborNull) return false
        return super.equals(other)
    }

    override fun hashCode(): Int = CborNull::class.hashCode() * 31 + super.hashCode()

    override fun toString(): String {
        return "CborNull(tags=${rawTags.joinToString(prefix = "[", postfix = "]")})"
    }
}

/**
 * Class representing CBOR `undefined` value
 */
@ExperimentalSerializationApi
@Serializable(with = CborUndefinedSerializer::class)
public class CborUndefined(vararg tags: ULong) : CborPrimitive(tags) {

    public constructor(tags: List<ULong>) : this(*(tags.toULongArray()))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CborUndefined) return false
        return super.equals(other)
    }

    override fun hashCode(): Int = CborUndefined::class.hashCode() * 31 + super.hashCode()

    override fun toString(): String {
        return "CborUndefined(tags=${rawTags.joinToString(prefix = "[", postfix = "]")})"
    }
}

/**
 * Class representing CBOR map, consisting of key-value pairs, where both key and value are arbitrary [CborElement]
 *
 * Since this class also implements [Map] interface, you can use
 * traditional methods like [Map.get] or [Map.getValue] to obtain CBOR elements.
 */
@ExperimentalSerializationApi
@Serializable(with = CborMapSerializer::class)
public class CborMap(
    private val content: Map<CborElement, CborElement>,
    vararg tags: ULong
) : CborElement(tags), Map<CborElement, CborElement> by content {

    public constructor(content: Map<CborElement, CborElement>, tags: List<ULong>) : this(
        content,
        *(tags.toULongArray())
    )

    public override fun equals(other: Any?): Boolean =
        other is CborMap && other.content == content && other.rawTags.contentEquals(rawTags)

    public override fun hashCode(): Int = content.hashCode() * 31 + rawTags.contentHashCode()

    override fun toString(): String {
        return "CborMap(" +
            "tags=${rawTags.joinToString(prefix = "[", postfix = "]")}, " +
            "content=$content" +
            ")"
    }

    public operator fun get(key: String): CborElement? = content[CborString(key)]
    public fun getValue(key: String): CborElement = content.getValue(CborString(key))

    public operator fun get(key: Long): CborElement? = content[CborInteger(key)]
    public fun getValue(key: Long): CborElement = content.getValue(CborInteger(key))

    public operator fun get(key: Int): CborElement? = content[CborInteger(key.toLong())]
    public fun getValue(key: Int): CborElement = content.getValue(CborInteger(key.toLong()))

    public companion object {
        //these are inside the companion to avoid name clashes on the JVM

        @JvmName("invokeString")
        public operator fun invoke(content: Map<String, CborElement>, tags: List<ULong>): CborMap =
            CborMap(content.mapKeys { (k, _) -> CborString(k) }, *(tags.toULongArray()))

        @JvmName("invokeLong")
        public operator fun invoke(content: Map<Long, CborElement>, tags: List<ULong>): CborMap =
            CborMap(content.mapKeys { (k, _) -> CborInteger(k) }, *(tags.toULongArray()))

        @JvmName("invokeStringVarargs")
        public operator fun invoke(content: Map<String, CborElement>, vararg tags: ULong): CborMap =
            CborMap(content.mapKeys { (k, _) -> CborString(k) }, *tags)

        @JvmName("invokeLongVarargs")
        public operator fun invoke(content: Map<Long, CborElement>, vararg tags: ULong): CborMap =
            CborMap(content.mapKeys { (k, _) -> CborInteger(k) }, *tags)
    }

}

/**
 * Class representing CBOR array consisting of CBOR elements.
 *
 * Since this class also implements [List] interface, you can use
 * traditional methods like [List.get] or [List.size] to obtain CBOR elements.
 */
@ExperimentalSerializationApi
@Serializable(with = CborArraySerializer::class)
public class CborArray(
    private val content: List<CborElement>,
    vararg tags: ULong
) : CborElement(tags), List<CborElement> by content {

    public constructor(content: List<CborElement>, tags: List<ULong>) : this(content, *(tags.toULongArray()))
    public constructor(content: Array<CborElement>, vararg  tags: ULong) : this(content.toList(), *tags)
    public constructor(content: Array<CborElement>, tags: List<ULong>) : this(content, *(tags.toULongArray()))

    public override fun equals(other: Any?): Boolean =
        other is CborArray && other.content == content && other.rawTags.contentEquals(rawTags)

    public override fun hashCode(): Int = content.hashCode() * 31 + rawTags.contentHashCode()

    override fun toString(): String {
        return "CborArray(" +
            "tags=${rawTags.joinToString(prefix = "[", postfix = "]")}, " +
            "content=$content" +
            ")"
    }

}

/**
 * Creates a copy of this [CborPrimitive] with the specified [tags], discarding any existing tags.
 */
@ExperimentalSerializationApi
@Suppress("UNCHECKED_CAST")
public fun <T : CborElement> T.copy(vararg tags: ULong): T =
    when (this) {
        is CborBoolean -> CborBoolean(value, *tags)
        is CborByteString -> CborByteString(toByteArray(), *tags)
        is CborFloat -> CborFloat(value, *tags)
        is CborInteger -> CborInteger(absoluteValue, isPositive, *tags)
        is CborNull -> CborNull(*tags)
        is CborString -> CborString(value, *tags)
        is CborUndefined -> CborUndefined(*tags)
        is CborArray -> CborArray(toList(), *tags)
        is CborMap -> CborMap(toMap(), *tags)
    } as T

/**
 * Creates a copy of this [CborPrimitive] with the specified [tags], discarding any existing tags.
 */
@ExperimentalSerializationApi
public fun <T : CborPrimitive> T.copy(tags: List<ULong>): T = copy(*tags.toULongArray())


/*START BOOLEAN*/
/** Creates copy of this [CborBoolean] with the specified [value], copying all tags.*/
@ExperimentalSerializationApi
public fun CborBoolean.copy(value: Boolean): CborBoolean = CborBoolean(value, *(rawTags.copyOf()))
/** Creates copy of this [CborBoolean] with the specified [value] and [tags].*/
@ExperimentalSerializationApi
public fun CborBoolean.copy(value: Boolean, vararg tags: ULong): CborBoolean = CborBoolean(value, *tags)
/** Creates copy of this [CborBoolean] with the specified [value] and [tags].*/
@ExperimentalSerializationApi
public fun CborBoolean.copy(value: Boolean, tags: List<ULong>): CborBoolean = copy(value, *tags.toULongArray())
/*END BOOLEAN*/

/*START INTEGER*/
/** Creates copy of this [CborInteger] with the specified [value], copying all tags.*/
@ExperimentalSerializationApi
public fun CborInteger.copy(value: Long): CborInteger = CborInteger(value, *(rawTags.copyOf()))
/** Creates copy of this [CborInteger] with the specified [value] and [tags].*/
@ExperimentalSerializationApi
public fun CborInteger.copy(value: Long, vararg tags: ULong): CborInteger = CborInteger(value, *tags)
/** Creates copy of this [CborInteger] with the specified [value] and [tags].*/
@ExperimentalSerializationApi
public fun CborInteger.copy(value: Long, tags: List<ULong>): CborInteger = copy(value, *tags.toULongArray())

/** Creates copy of this [CborInteger] with the specified [absoluteValue] and [isPositive], copying all tags.*/
@ExperimentalSerializationApi
public fun CborInteger.copy(absoluteValue: ULong, isPositive: Boolean): CborInteger =
    CborInteger(absoluteValue, isPositive, *(rawTags.copyOf()))
/** Creates copy of this [CborInteger] with the specified [absoluteValue], [isPositive] and [tags].*/
@ExperimentalSerializationApi
public fun CborInteger.copy(absoluteValue: ULong, isPositive: Boolean, vararg tags: ULong): CborInteger =
    CborInteger(absoluteValue, isPositive, *tags)
/** Creates copy of this [CborInteger] with the specified [absoluteValue], [isPositive] and [tags].*/
@ExperimentalSerializationApi
public fun CborInteger.copy(absoluteValue: ULong, isPositive: Boolean, tags: List<ULong>): CborInteger =
    copy(absoluteValue, isPositive, *tags.toULongArray())
/*END INTEGER*/

/*START FLOAT*/
/** Creates copy of this [CborFloat] with the specified [value], copying all tags.*/
@ExperimentalSerializationApi
public fun CborFloat.copy(value: Double): CborFloat = CborFloat(value, *(rawTags.copyOf()))
/** Creates copy of this [CborFloat] with the specified [value] and [tags].*/
@ExperimentalSerializationApi
public fun CborFloat.copy(value: Double, vararg tags: ULong): CborFloat = CborFloat(value, *tags)
/** Creates copy of this [CborFloat] with the specified [value] and [tags].*/
@ExperimentalSerializationApi
public fun CborFloat.copy(value: Double, tags: List<ULong>): CborFloat = copy(value, *tags.toULongArray())
/*END FLOAT*/

/*START STRING*/
/** Creates copy of this [CborString] with the specified [value], copying all tags.*/
@ExperimentalSerializationApi
public fun CborString.copy(value: String): CborString = CborString(value, *(rawTags.copyOf()))
/** Creates copy of this [CborString] with the specified [value] and [tags].*/
@ExperimentalSerializationApi
public fun CborString.copy(value: String, vararg tags: ULong): CborString = CborString(value, *tags)
/** Creates copy of this [CborString] with the specified [value] and [tags].*/
@ExperimentalSerializationApi
public fun CborString.copy(value: String, tags: List<ULong>): CborString = copy(value, *tags.toULongArray())
/*END STRING*/

/*START BYTE STRING*/
/** Creates copy of this [CborByteString] with the specified [bytes], copying all tags.*/
@ExperimentalSerializationApi
public fun CborByteString.copy(bytes: ByteArray): CborByteString = CborByteString(bytes, *(rawTags.copyOf()))
/** Creates copy of this [CborByteString] with the specified [bytes] and [tags].*/
@ExperimentalSerializationApi
public fun CborByteString.copy(bytes: ByteArray, vararg tags: ULong): CborByteString = CborByteString(bytes, *tags)
/** Creates copy of this [CborByteString] with the specified [bytes] and [tags].*/
@ExperimentalSerializationApi
public fun CborByteString.copy(bytes: ByteArray, tags: List<ULong>): CborByteString =
    copy(bytes, *tags.toULongArray())
/*END BYTE STRING*/


/*START MAP*/
/** Creates copy of this [CborMap] with the specified [content], copying all tags.*/
@ExperimentalSerializationApi
public fun CborMap.copy(content: Map<CborElement, CborElement>): CborMap = CborMap(content, *(rawTags.copyOf()))
/** Creates copy of this [CborMap] with the specified [content] and [tags].*/
@ExperimentalSerializationApi
public fun CborMap.copy(content: Map<CborElement, CborElement>, vararg tags: ULong): CborMap = CborMap(content, *tags)
/** Creates copy of this [CborMap] with the specified [content] and [tags].*/
@ExperimentalSerializationApi
public fun CborMap.copy(content: Map<CborElement, CborElement>, tags: List<ULong>): CborMap =
    copy(content, *tags.toULongArray())

/** Creates copy of this [CborMap] with the specified [content], copying all tags.*/
@ExperimentalSerializationApi
@JvmName("copyStringMap")
public fun CborMap.copy(content: Map<String, CborElement>): CborMap =
    CborMap(content.mapKeys { (k, _) -> CborString(k) }, *(rawTags.copyOf()))

/** Creates copy of this [CborMap] with the specified [content] and [tags].*/
@ExperimentalSerializationApi
@JvmName("copyStringMapVarargs")
public fun CborMap.copy(content: Map<String, CborElement>, vararg tags: ULong): CborMap =
    CborMap(content.mapKeys { (k, _) -> CborString(k) }, *tags)

/** Creates copy of this [CborMap] with the specified [content] and [tags].*/
@ExperimentalSerializationApi
@JvmName("copyStringMapList")
public fun CborMap.copy(content: Map<String, CborElement>, tags: List<ULong>): CborMap =
    copy(content, *tags.toULongArray())

/** Creates copy of this [CborMap] with the specified [content], copying all tags.*/
@ExperimentalSerializationApi
@JvmName("copyLongMap")
public fun CborMap.copy(content: Map<Long, CborElement>): CborMap =
    CborMap(content.mapKeys { (k, _) -> CborInteger(k) }, *(rawTags.copyOf()))

/** Creates copy of this [CborMap] with the specified [content] and [tags].*/
@ExperimentalSerializationApi
@JvmName("copyLongMapVarargs")
public fun CborMap.copy(content: Map<Long, CborElement>, vararg tags: ULong): CborMap =
    CborMap(content.mapKeys { (k, _) -> CborInteger(k) }, *tags)

/** Creates copy of this [CborMap] with the specified [content] and [tags].*/
@ExperimentalSerializationApi
@JvmName("copyLongMapList")
public fun CborMap.copy(content: Map<Long, CborElement>, tags: List<ULong>): CborMap =
    copy(content, *tags.toULongArray())
/*END MAP*/


/*START ARRAY*/
/** Creates copy of this [CborArray] with the specified [content], copying all tags.*/
@ExperimentalSerializationApi
public fun CborArray.copy(content: List<CborElement>): CborArray = CborArray(content, *(rawTags.copyOf()))
/** Creates copy of this [CborArray] with the specified [content] and [tags].*/
@ExperimentalSerializationApi
public fun CborArray.copy(content: List<CborElement>, vararg tags: ULong): CborArray = CborArray(content, *tags)
/** Creates copy of this [CborArray] with the specified [content] and [tags].*/
@ExperimentalSerializationApi
public fun CborArray.copy(content: List<CborElement>, tags: List<ULong>): CborArray =
    copy(content, *tags.toULongArray())

/** Creates copy of this [CborArray] with the specified [content], copying all tags.*/
@ExperimentalSerializationApi
public fun CborArray.copy(content: Array<CborElement>): CborArray = CborArray(content, *(rawTags.copyOf()))
/** Creates copy of this [CborArray] with the specified [content] and [tags].*/
@ExperimentalSerializationApi
public fun CborArray.copy(content: Array<CborElement>, vararg tags: ULong): CborArray = CborArray(content, *tags)
/** Creates copy of this [CborArray] with the specified [content] and [tags].*/
@ExperimentalSerializationApi
public fun CborArray.copy(content: Array<CborElement>, tags: List<ULong>): CborArray =
    copy(content, *tags.toULongArray())
/*END ARRAY*/
