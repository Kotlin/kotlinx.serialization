/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.protobuf.internal.*
import kotlin.js.*

/**
 * Implements [encoding][encodeToByteArray] and [decoding][decodeFromByteArray] classes to/from bytes
 * using [Proto2][https://developers.google.com/protocol-buffers/docs/proto] specification.
 * It is typically used by constructing an application-specific instance, with configured specific behaviour
 * and, if necessary, registered custom serializers (in [SerializersModule] provided by [serializersModule] constructor parameter).
 *
 * ### Correspondence between Protobuf message definitions and Kotlin classes
 * Given a ProtoBuf definition with one required field, one optional field and one optional field with a custom default
 * value:
 * ```
 * message MyMessage {
 *     required int32 first = 1;
 *     optional int32 second = 2;
 *     optional int32 third = 3 [default = 42];
 * }
 * ```
 *
 * The corresponding [Serializable] class should match the ProtoBuf definition and should use the same default values:
 * ```
 * @Serializable
 * data class MyMessage(val first: Int, val second: Int = 0, val third: Int = 42)
 * ```
 *
 * By default, protobuf fields ids are being assigned to Kotlin properties in incremental order, i.e.
 * the first property in the class has id 1, the second has id 2, and so forth.
 * If you need a more stable order (e.g. to avoid breaking changes when reordering properties),
 * provide custom ids using [ProtoNumber] annotation.
 *
 * By default, all integer numbers are encoded using [varint][https://developers.google.com/protocol-buffers/docs/encoding#varints]
 * encoding. This behaviour can be changed via [ProtoType] annotation.
 *
 * ### Known caveats and limitations
 * Lists are represented as repeated fields. Because format spec says that if the list is empty,
 * there are no elements in the stream with such tag, you must explicitly mark any
 * field of list type with default = emptyList(). Same for maps.
 * There's no special support for `oneof` protobuf fields. However, this implementation
 * supports standard kotlinx.serialization's polymorphic and sealed serializers,
 * using their default form (message of serialName: string and other embedded message with actual content).
 *
 * ### Proto3 support
 * This implementation does not support repeated packed fields, so you won't be able to deserialize
 * Proto3 lists. However, other messages could be decoded. You have to remember that since fields in Proto3
 * messages by default are implicitly optional,
 * corresponding Kotlin properties have to be nullable with default value `null`.
 *
 * ### Usage example
 * ```
 * // Serialize to ProtoBuf hex string
 * val encoded = ProtoBuf.encodeToHexString(MyMessage(15)) // "080f1000182a"
 *
 * // Deserialize from ProtoBuf hex string
 * val decoded = ProtoBuf.decodeFromHexString<MyMessage>(encoded) // MyMessage(first=15, second=0, third=42)
 *
 * // Serialize to ProtoBuf bytes (omitting default values)
 * val encoded2 = ProtoBuf(encodeDefaults = false).encodeToByteArray( MyMessage(15)) // [0x08, 0x0f]
 *
 * // Deserialize ProtoBuf bytes will use default values of the MyMessage class
 * val decoded2 = ProtoBuf.decodeFromByteArray<MyMessage>(encoded2) // MyMessage(first=15, second=0, third=42)
 * ```
 *
 * ### Check existence of optional fields
 * Null values can be used as the default value for optional fields to implement more complex use-cases that rely on
 * checking if a field was set or not. This requires the use of a custom ProtoBuf instance with
 * `ProtoBuf(encodeDefaults = false)`.
 *
 * ```
 * @Serializable
 * data class MyMessage(val first: Int, private val _second: Int? = null, private val _third: Int? = null) {
 *
 *     val second: Int
 *         get() = _second ?: 0
 *
 *     val third: Int
 *         get() = _third ?: 42
 *
 *     fun hasSecond() = _second != null
 *
 *     fun hasThird() = _third != null
 * }
 *
 * // Serialize to ProtoBuf bytes (encodeDefaults = false is required if null values are used)
 * val encoded = ProtoBuf(encodeDefaults = false).encodeToByteArray(MyMessage(15)) // [0x08, 0x0f]
 *
 * // Deserialize ProtoBuf bytes
 * val decoded = ProtoBuf.decodeFromByteArray<MyMessage>(encoded) // MyMessage(first = 15, _second = null, _third = null)
 * decoded.hasSecond()     // false
 * decoded.second          // 0
 * decoded.hasThird()      // false
 * decoded.third           // 42
 *
 * // Serialize to ProtoBuf bytes
 * val encoded2 = ProtoBuf(encodeDefaults = false).encodeToByteArray(MyMessage(15, 0, 0)) // [0x08, 0x0f, 0x10, 0x00, 0x18, 0x00]
 *
 * // Deserialize ProtoBuf bytes
 * val decoded2 = ProtoBuf.decodeFromByteArray<MyMessage>(encoded2) // MyMessage(first=15, _second=0, _third=0)
 * decoded.hasSecond()     // true
 * decoded.second          // 0
 * decoded.hasThird()      // true
 * decoded.third           // 0
 * ```
 *
 * @param encodeDefaults specifies whether default values are encoded.
 * @param serializersModule application-specific [SerializersModule] to provide custom serializers.
 */
@ExperimentalSerializationApi
public sealed class ProtoBuf(
    internal val encodeDefaults: Boolean,
    override val serializersModule: SerializersModule,
    marker: Nothing? // Marker for deprecated ctor
) : BinaryFormat {

    /**
     * The default instance of [ProtoBuf].
     */
    public companion object Default : ProtoBuf(true, EmptySerializersModule, null)

    override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray {
        val output = ByteArrayOutput()
        val encoder = ProtobufEncoder(this, ProtobufWriter(output), serializer.descriptor)
        encoder.encodeSerializableValue(serializer, value)
        return output.toByteArray()
    }

    override fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        val input = ByteArrayInput(bytes)
        val decoder = ProtobufDecoder(this, ProtobufReader(input), deserializer.descriptor)
        return decoder.decodeSerializableValue(deserializer)
    }
}

/**
 * Creates an instance of [ProtoBuf] configured from the optionally given [ProtoBuf instance][from]
 * and adjusted with [builderAction].
 */
@ExperimentalSerializationApi
public fun ProtoBuf(from: ProtoBuf = ProtoBuf, builderAction: ProtoBufBuilder.() -> Unit): ProtoBuf {
    val b = ProtoBufBuilder(from)
    b.builderAction()
    return ProtoBufImpl(b.encodeDefaults, b.serializersModule)
}

/**
 * Builder of the [ProtoBuf] instance provided by `ProtoBuf` factory function.
 */
@ExperimentalSerializationApi
public class ProtoBufBuilder internal constructor(proto: ProtoBuf) {

    /**
     * Specifies whether default values of Kotlin properties should be encoded.
     */
    public var encodeDefaults: Boolean = proto.encodeDefaults

    /**
     * Module with contextual and polymorphic serializers to be used in the resulting [ProtoBuf] instance.
     */
    public var serializersModule: SerializersModule = proto.serializersModule
}

@Deprecated(
    "Constructor was deprecated in the favor of builder function during serialization 1.0 API stabilization",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("ProtoBuf { this.encodeDefaults = encodeDefaults; this.serializersModule = serializersModule }")
)
public fun ProtoBuf(
    encodeDefaults: Boolean,
    serializersModule: SerializersModule = EmptySerializersModule
): ProtoBuf = ProtoBufImpl(encodeDefaults, serializersModule)

@Deprecated(
    "Constructor was deprecated in the favor of builder function during serialization 1.0 API stabilization",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("ProtoBuf { this.serializersModule = serializersModule }")
)
public fun ProtoBuf(
    serializersModule: SerializersModule
): ProtoBuf = ProtoBufImpl(true, serializersModule)

@Deprecated(
    "Empty constructor was deprecated in the favor of Default instance during serialization 1.0 API stabilization",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("ProtoBuf")
)
@JsName("_ProtoBuf")
public fun ProtoBuf(): ProtoBuf = ProtoBuf

private class ProtoBufImpl(encodeDefaults: Boolean, serializersModule: SerializersModule) :
    ProtoBuf(encodeDefaults, serializersModule, null)
