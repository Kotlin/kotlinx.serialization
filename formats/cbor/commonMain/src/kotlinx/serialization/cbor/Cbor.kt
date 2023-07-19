/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlinx.serialization.cbor.internal.*
import kotlinx.serialization.modules.*

/**
 * Implements [encoding][encodeToByteArray] and [decoding][decodeFromByteArray] classes to/from bytes
 * using [CBOR](https://tools.ietf.org/html/rfc7049) specification.
 * It is typically used by constructing an application-specific instance, with configured behaviour, and,
 * if necessary, registered custom serializers (in [SerializersModule] provided by [serializersModule] constructor parameter).
 *
 * ### Known caveats and limitations:
 * Supports reading collections of both definite and indefinite lengths; however,
 * serialization always writes maps and lists as [indefinite-length](https://tools.ietf.org/html/rfc7049#section-2.2.1) ones.
 * Does not support [optional tags](https://tools.ietf.org/html/rfc7049#section-2.4) representing datetime, bignums, etc.
 * Fully support CBOR maps, which, unlike JSON ones, may contain keys of non-primitive types, and may produce such maps
 * from corresponding Kotlin objects. However, other 3rd-party parsers (e.g. `jackson-dataformat-cbor`) may not accept such maps.
 *
 * @param encodeDefaults specifies whether default values of Kotlin properties are encoded.
 *                       False by default; meaning that properties with values equal to defaults will be elided.
 * @param ignoreUnknownKeys specifies if unknown CBOR elements should be ignored (skipped) when decoding.
 * @param writeKeyTags Specifies whether tags set using the [KeyTags] annotation should be written (or omitted)
 * @param writeValueTags Specifies whether tags set using the [ValueTags] annotation should be written (or omitted)
 * @param verifyKeyTags Specifies whether tags preceding map keys (i.e. properties) should be matched against the
 *                      [KeyTags] annotation during the deserialization process. Useful for lenient parsing
 * @param verifyValueTags Specifies whether tags preceding values should be matched against the [ValueTags]
 *                      annotation during the deserialization process. Useful for lenient parsing.
 */
@ExperimentalSerializationApi
public sealed class Cbor(
    internal val encodeDefaults: Boolean,
    internal val ignoreUnknownKeys: Boolean,
    internal val writeKeyTags: Boolean,
    internal val writeValueTags: Boolean,
    internal val verifyKeyTags: Boolean,
    internal val verifyValueTags: Boolean,
    override val serializersModule: SerializersModule
) : BinaryFormat {

    /**
     * The default instance of [Cbor]
     */
    public companion object Default : Cbor(false, false, true, true, true, true, EmptySerializersModule())

    override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray {
       // val tree = CborTree(this).pass1Accumulate(serializer, value)
    //    tree.pass2PruneNulls()
       // println(tree)

        val output = ByteArrayOutput()
        val dumper = CborWriter(this, CborEncoder(output))
        dumper.encodeSerializableValue(serializer, value)
        dumper.root.encode()
        return output.toByteArray().also {bytes->
            println(bytes.joinToString(separator = "", prefix = "", postfix = "") {
                it.toUByte().toString(16).let { if (it.length == 1) "0$it" else it }
            })

        }
    }

    override fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        val stream = ByteArrayInput(bytes)
        val reader = CborReader(this, CborDecoder(stream))
        return reader.decodeSerializableValue(deserializer)
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class CborImpl(
    encodeDefaults: Boolean, ignoreUnknownKeys: Boolean,
    writeKeyTags: Boolean,
    writeValueTags: Boolean,
    verifyKeyTags: Boolean,
    verifyValueTags: Boolean,
    serializersModule: SerializersModule
) :
    Cbor(
        encodeDefaults,
        ignoreUnknownKeys,
        writeKeyTags,
        writeValueTags,
        verifyKeyTags,
        verifyValueTags,
        serializersModule
    )

/**
 * Creates an instance of [Cbor] configured from the optionally given [Cbor instance][from]
 * and adjusted with [builderAction].
 */
@ExperimentalSerializationApi
public fun Cbor(from: Cbor = Cbor, builderAction: CborBuilder.() -> Unit): Cbor {
    val builder = CborBuilder(from)
    builder.builderAction()
    return CborImpl(
        builder.encodeDefaults,
        builder.ignoreUnknownKeys,
        builder.writeKeyTags,
        builder.writeValueTags,
        builder.verifyKeyTags,
        builder.verifyValueTags,
        builder.serializersModule
    )
}

/**
 * Builder of the [Cbor] instance provided by `Cbor` factory function.
 */
@ExperimentalSerializationApi
public class CborBuilder internal constructor(cbor: Cbor) {

    /**
     * Specifies whether default values of Kotlin properties should be encoded.
     */
    public var encodeDefaults: Boolean = cbor.encodeDefaults

    /**
     * Specifies whether encounters of unknown properties in the input CBOR
     * should be ignored instead of throwing [SerializationException].
     * `false` by default.
     */
    public var ignoreUnknownKeys: Boolean = cbor.ignoreUnknownKeys

    /**
     * Specifies whether tags set using the [KeyTags] annotation should be written (or omitted)
     */
    public var writeKeyTags: Boolean = cbor.writeKeyTags

    /**
     * Specifies whether tags set using the [ValueTags] annotation should be written (or omitted)
     */
    public var writeValueTags: Boolean = cbor.writeKeyTags

    /**
     * Specifies whether tags preceding map keys (i.e. properties) should be matched against the [KeyTags] annotation during the deserialization process
     */
    public var verifyKeyTags: Boolean = cbor.verifyKeyTags

    /**
     * Specifies whether tags preceding values should be matched against the [ValueTags] annotation during the deserialization process
     */
    public var verifyValueTags: Boolean = cbor.verifyValueTags

    /**
     * Module with contextual and polymorphic serializers to be used in the resulting [Cbor] instance.
     */
    public var serializersModule: SerializersModule = cbor.serializersModule
}
