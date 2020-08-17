/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.cbor.internal.ByteArrayInput
import kotlinx.serialization.cbor.internal.ByteArrayOutput
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
 */
@ExperimentalSerializationApi
public sealed class Cbor(
    internal val encodeDefaults: Boolean,
    override val serializersModule: SerializersModule,
    ctorMarker: Nothing? // Marker for the temporary migration
) : BinaryFormat {

    /**
     * The default instance of [Cbor]
     */
    public companion object Default : Cbor(true, EmptySerializersModule, null)

    override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray {
        val output = ByteArrayOutput()
        val dumper = CborWriter(this, CborEncoder(output))
        dumper.encodeSerializableValue(serializer, value)
        return output.toByteArray()
    }

    override fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        val stream = ByteArrayInput(bytes)
        val reader = CborReader(this, CborDecoder(stream))
        return reader.decodeSerializableValue(deserializer)
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class CborImpl(encodeDefaults: Boolean, serializersModule: SerializersModule) :
    Cbor(encodeDefaults, serializersModule, null)

/**
 * Creates an instance of [Cbor] configured from the optionally given [Cbor instance][from]
 * and adjusted with [builderAction].
 */
@ExperimentalSerializationApi
public fun Cbor(from: Cbor = Cbor, builderAction: CborBuilder.() -> Unit): Cbor {
    val builder = CborBuilder(from)
    builder.builderAction()
    return CborImpl(builder.encodeDefaults, builder.serializersModule)
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
     * Module with contextual and polymorphic serializers to be used in the resulting [Cbor] instance.
     */
    public var serializersModule: SerializersModule = cbor.serializersModule
}

@Deprecated(
    "Cbor constructor was deprecated in the favour of factory function during serialization 1.0 API stabilization",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("Cbor { this.encodeDefaults = encodeDefaults; this.serializersModule = serializersModule }")
)
public fun Cbor(encodeDefaults: Boolean, serializersModule: SerializersModule): Cbor = Cbor

@Deprecated(
    "Cbor constructor was deprecated in the favour of factory function during serialization 1.0 API stabilization",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("Cbor { this.serializersModule = serializersModule }")
)
public fun Cbor(serializersModule: SerializersModule): Cbor = Cbor
