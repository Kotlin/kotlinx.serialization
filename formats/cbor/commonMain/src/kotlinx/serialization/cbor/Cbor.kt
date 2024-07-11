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
 * Can be used to produce fully [COSE](https://datatracker.ietf.org/doc/html/rfc8812)-compliant data
 * but canonical sorting of map keys needs to be done manually by specifying members in appropriate order.
 * Fully support CBOR maps, which, unlike JSON ones, may contain keys of non-primitive types, and may produce such maps
 * from corresponding Kotlin objects. However, other 3rd-party parsers (e.g. `jackson-dataformat-cbor`) may not accept such maps.
 */
@ExperimentalSerializationApi
public sealed class Cbor(
    public val configuration: CborConfiguration,
    override val serializersModule: SerializersModule
) : BinaryFormat {

    /**
     * The default instance of [Cbor]. Neither writes nor verifies tags. Uses indefinite length encoding by default.
     */
    public companion object Default :
        Cbor(
            CborConfiguration(
                encodeDefaults = false,
                ignoreUnknownKeys = false,
                encodeKeyTags = false,
                encodeValueTags = false,
                encodeObjectTags = false,
                verifyKeyTags = false,
                verifyValueTags = false,
                verifyObjectTags = false,
                useDefiniteLengthEncoding = false,
                preferCborLabelsOverNames = false,
                alwaysUseByteString = false
            ), EmptySerializersModule()
        ) {

        /**
         * Preconfigured instance of [Cbor] for COSE compliance. Writes and verifies all tags, uses definite length
         * encoding and prefers labels to serial names. **DOES NOT** sort CBOR map keys; declare them in canonical order
         * for full cbor compliance!
         */
        public val CoseCompliant: Cbor =
            Cbor {
                encodeDefaults = false
                ignoreUnknownKeys = false
                writeKeyTags = true
                writeValueTags = true
                writeObjectTags = true
                verifyKeyTags = true
                verifyValueTags = true
                verifyObjectTags = true
                writeDefiniteLengths = true
                preferCborLabelsOverNames = true
                alwaysUseByteString = false
                serializersModule = EmptySerializersModule()
            }
    }

    override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray {
        val output = ByteArrayOutput()
        val dumper = if (configuration.useDefiniteLengthEncoding) DefiniteLengthCborWriter(
            this,
            output
        ) else IndefiniteLengthCborWriter(
            this,
            output
        )
        dumper.encodeSerializableValue(serializer, value)

        return output.toByteArray()

    }

    override fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        val stream = ByteArrayInput(bytes)
        val reader = CborReader(this, CborParser(stream, configuration.verifyObjectTags))
        return reader.decodeSerializableValue(deserializer)
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class CborImpl(
    encodeDefaults: Boolean, ignoreUnknownKeys: Boolean,
    writeKeyTags: Boolean,
    writeValueTags: Boolean,
    writeObjectTags: Boolean,
    verifyKeyTags: Boolean,
    verifyValueTags: Boolean,
    verifyObjectTags: Boolean,
    writeDefiniteLengths: Boolean,
    preferCborLabelsOverNames: Boolean,
    alwaysUseByteString: Boolean,
    serializersModule: SerializersModule
) :
    Cbor(
        CborConfiguration(
            encodeDefaults,
            ignoreUnknownKeys,
            writeKeyTags,
            writeValueTags,
            writeObjectTags,
            verifyKeyTags,
            verifyValueTags,
            verifyObjectTags,
            writeDefiniteLengths,
            preferCborLabelsOverNames,
            alwaysUseByteString
        ),
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
        builder.writeObjectTags,
        builder.verifyKeyTags,
        builder.verifyValueTags,
        builder.verifyObjectTags,
        builder.writeDefiniteLengths,
        builder.preferCborLabelsOverNames,
        builder.alwaysUseByteString,
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
    public var encodeDefaults: Boolean = cbor.configuration.encodeDefaults

    /**
     * Specifies whether encounters of unknown properties in the input CBOR
     * should be ignored instead of throwing [SerializationException].
     * `false` by default.
     */
    public var ignoreUnknownKeys: Boolean = cbor.configuration.ignoreUnknownKeys

    /**
     * Specifies whether tags set using the [KeyTags] annotation should be written (or omitted)
     */
    public var writeKeyTags: Boolean = cbor.configuration.encodeKeyTags

    /**
     * Specifies whether tags set using the [ValueTags] annotation should be written (or omitted)
     */
    public var writeValueTags: Boolean = cbor.configuration.encodeValueTags

    /**
     * Specifies whether tags set using the [ObjectTags] annotation should be written (or omitted)
     */
    public var writeObjectTags: Boolean = cbor.configuration.encodeObjectTags

    /**
     * Specifies whether tags preceding map keys (i.e. properties) should be matched against the [KeyTags] annotation during the deserialization process
     */
    public var verifyKeyTags: Boolean = cbor.configuration.verifyKeyTags

    /**
     * Specifies whether tags preceding values should be matched against the [ValueTags] annotation during the deserialization process
     */
    public var verifyValueTags: Boolean = cbor.configuration.verifyValueTags

    /**
     * Specifies whether tags preceding objects (maps) and arrays (as in [CborArray]) should be matched against the
     * specified tags during the deserialization process
     */
    public var verifyObjectTags: Boolean = cbor.configuration.verifyObjectTags

    /**
     * specifies whether structures (maps, object, lists, etc.) should be encoded using definite length encoding
     */
    public var writeDefiniteLengths: Boolean = cbor.configuration.useDefiniteLengthEncoding

    /**
     * Specifies whether to serialize element labels (i.e. Long from [CborLabel]) instead of the element names (i.e. String from [SerialName]) for map keys
     */
    public var preferCborLabelsOverNames: Boolean = cbor.configuration.preferCborLabelsOverNames

    /**
     * Specifies whether to always use the compact [ByteString] encoding when serializing or deserializing byte arrays.
     */
    public var alwaysUseByteString: Boolean = cbor.configuration.alwaysUseByteString

    /**
     * Module with contextual and polymorphic serializers to be used in the resulting [Cbor] instance.
     */
    public var serializersModule: SerializersModule = cbor.serializersModule
}
