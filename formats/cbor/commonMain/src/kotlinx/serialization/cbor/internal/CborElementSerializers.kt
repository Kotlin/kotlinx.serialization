/*
 * Copyright 2017-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalSerializationApi::class, ExperimentalUnsignedTypes::class)

package kotlinx.serialization.cbor.internal

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.cbor.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

internal interface CborSerializer

/**
 * Serializer object providing [SerializationStrategy] and [DeserializationStrategy] for [CborElement].
 * It can only be used by with [Cbor] format and its input ([CborDecoder] and [CborEncoder]).
 */
internal object CborElementSerializer : KSerializer<CborElement>, CborSerializer {
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("kotlinx.serialization.cbor.CborElement", PolymorphicKind.SEALED) {
            // Resolve cyclic dependency in descriptors by late binding
            element("CborPrimitive", defer { CborPrimitiveSerializer.descriptor })
            element("CborNull", defer { CborNullSerializer.descriptor })
            element("CborUndefined", defer { CborUndefinedSerializer.descriptor })
            element("CborString", defer { CborStringSerializer.descriptor })
            element("CborBoolean", defer { CborBooleanSerializer.descriptor })
            element("CborByteString", defer { CborByteStringSerializer.descriptor })
            element("CborMap", defer { CborMapSerializer.descriptor })
            element("CborList", defer { CborListSerializer.descriptor })
            element("CborDouble", defer { CborFloatSerializer.descriptor })
            element("CborInt", defer { CborIntSerializer.descriptor })
        }

    override fun serialize(encoder: Encoder, value: CborElement) {
        encoder.asCborEncoder()

        // Encode the value
        when (value) {
            is CborPrimitive<*> -> encoder.encodeSerializableValue(CborPrimitiveSerializer, value)
            is CborMap -> encoder.encodeSerializableValue(CborMapSerializer, value)
            is CborList -> encoder.encodeSerializableValue(CborListSerializer, value)
        }
    }

    override fun deserialize(decoder: Decoder): CborElement {
        val input = decoder.asCborDecoder()
        return input.decodeCborElement()
    }
}

/**
 * Serializer object providing [SerializationStrategy] and [DeserializationStrategy] for [CborPrimitive].
 * It can only be used by with [Cbor] format an its input ([CborDecoder] and [CborEncoder]).
 */
internal object CborPrimitiveSerializer : KSerializer<CborPrimitive<*>>, CborSerializer {
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("kotlinx.serialization.cbor.CborPrimitive", PolymorphicKind.SEALED)

    override fun serialize(encoder: Encoder, value: CborPrimitive<*>) {
        when (value) {
            is CborNull -> encoder.encodeSerializableValue(CborNullSerializer, value)
            is CborUndefined -> encoder.encodeSerializableValue(CborUndefinedSerializer, value)
            is CborString -> encoder.encodeSerializableValue(CborStringSerializer, value)
            is CborBoolean -> encoder.encodeSerializableValue(CborBooleanSerializer, value)
            is CborByteString -> encoder.encodeSerializableValue(CborByteStringSerializer, value)
            is CborFloat -> encoder.encodeSerializableValue(CborFloatSerializer, value)
            is CborInt -> encoder.encodeSerializableValue(CborIntSerializer, value)
        }
    }

    override fun deserialize(decoder: Decoder): CborPrimitive<*> {
        val result = decoder.asCborDecoder().decodeCborElement()
        if (result !is CborPrimitive<*>) throw CborDecodingException("Unexpected CBOR element, expected CborPrimitive, had ${result::class}")
        return result
    }
}

/**
 * Serializer object providing [SerializationStrategy] and [DeserializationStrategy] for [CborNull].
 * It can only be used by with [Cbor] format an its input ([CborDecoder] and [CborEncoder]).
 */
internal object CborNullSerializer : KSerializer<CborNull>, CborSerializer {

    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("kotlinx.serialization.cbor.CborNull", SerialKind.ENUM)

    override fun serialize(encoder: Encoder, value: CborNull) {
        val cborEncoder = encoder.asCborEncoder()
        cborEncoder.encodeTags(value.tags)
        encoder.encodeNull()
    }

    override fun deserialize(decoder: Decoder): CborNull {
        val element = decoder.asCborDecoder().decodeCborElement()
        if (element !is CborNull) throw CborDecodingException("Unexpected CBOR element, expected CborNull, had ${element::class}")
        return element
    }
}

internal object CborUndefinedSerializer : KSerializer<CborUndefined>, CborSerializer {
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("kotlinx.serialization.cbor.CborUndefined", SerialKind.ENUM)

    override fun serialize(encoder: Encoder, value: CborUndefined) {
        val cborEncoder = encoder.asCborEncoder()
        cborEncoder.encodeTags(value.tags)
        cborEncoder.encodeUndefined()
    }

    override fun deserialize(decoder: Decoder): CborUndefined {
        val element = decoder.asCborDecoder().decodeCborElement()
        if (element !is CborUndefined) throw CborDecodingException("Unexpected CBOR element, expected CborUndefined, had ${element::class}")
        return element
    }
}


internal object CborIntSerializer : KSerializer<CborInt>, CborSerializer {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.serialization.cbor.CborInt", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: CborInt) {
        val cborEncoder = encoder.asCborEncoder()
        cborEncoder.encodeTags(value.tags)
        when (value.isPositive) {
            //@formatter:off
            true -> cborEncoder.encodePositive(value.value)
            false                    -> cborEncoder.encodeNegative(value.value)
            //@formatter:on
        }
    }

    override fun deserialize(decoder: Decoder): CborInt {
        val result = decoder.asCborDecoder().decodeCborElement()
        if (result !is CborInt) throw CborDecodingException("Unexpected CBOR element, expected CborInt, had ${result::class}")
        return result
    }
}

internal object CborFloatSerializer : KSerializer<CborFloat>, CborSerializer {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.serialization.cbor.CborDouble", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: CborFloat) {
        val cborEncoder = encoder.asCborEncoder()
        cborEncoder.encodeTags(value.tags)
        encoder.encodeDouble(value.value)
    }

    override fun deserialize(decoder: Decoder): CborFloat {
        val element = decoder.asCborDecoder().decodeCborElement()
        if (element !is CborFloat) throw CborDecodingException("Unexpected CBOR element, expected CborFloat, had ${element::class}")
        return element
    }
}

/**
 * Serializer object providing [SerializationStrategy] and [DeserializationStrategy] for [CborString].
 * It can only be used by with [Cbor] format an its input ([CborDecoder] and [CborEncoder]).
 */
internal object CborStringSerializer : KSerializer<CborString>, CborSerializer {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.serialization.cbor.CborString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: CborString) {
        val cborEncoder = encoder.asCborEncoder()
        cborEncoder.encodeTags(value.tags)
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): CborString {
        val cborDecoder = decoder.asCborDecoder()
        val element = cborDecoder.decodeCborElement()
        if (element !is CborString) throw CborDecodingException("Unexpected CBOR element, expected CborString, had ${element::class}")
        return element
    }
}

/**
 * Serializer object providing [SerializationStrategy] and [DeserializationStrategy] for [CborBoolean].
 * It can only be used by with [Cbor] format an its input ([CborDecoder] and [CborEncoder]).
 */
internal object CborBooleanSerializer : KSerializer<CborBoolean>, CborSerializer {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.serialization.cbor.CborBoolean", PrimitiveKind.BOOLEAN)

    override fun serialize(encoder: Encoder, value: CborBoolean) {
        val cborEncoder = encoder.asCborEncoder()
        cborEncoder.encodeTags(value.tags)
        encoder.encodeBoolean(value.value)
    }

    override fun deserialize(decoder: Decoder): CborBoolean {
        val cborDecoder = decoder.asCborDecoder()
        val element = cborDecoder.decodeCborElement()
        if (element !is CborBoolean) throw CborDecodingException("Unexpected CBOR element, expected CborBoolean, had ${element::class}")
        return element
    }
}

/**
 * Serializer object providing [SerializationStrategy] and [DeserializationStrategy] for [CborByteString].
 * It can only be used by with [Cbor] format and its input ([CborDecoder] and [CborEncoder]).
 */
internal object CborByteStringSerializer : KSerializer<CborByteString>, CborSerializer {
    override val descriptor: SerialDescriptor =
        SerialDescriptor("kotlinx.serialization.cbor.CborByteString", ByteArraySerializer().descriptor)

    override fun serialize(encoder: Encoder, value: CborByteString) {
        val cborEncoder = encoder.asCborEncoder()
        cborEncoder.encodeTags(value.tags)
        cborEncoder.encodeByteString(value.value)
    }

    override fun deserialize(decoder: Decoder): CborByteString {
        val cborDecoder = decoder.asCborDecoder()
        val element = cborDecoder.decodeCborElement()
        if (element !is CborByteString) throw CborDecodingException("Unexpected CBOR element, expected CborByteString, had ${element::class}")
        return element
    }
}

/**
 * Serializer object providing [SerializationStrategy] and [DeserializationStrategy] for [CborMap].
 * It can only be used by with [Cbor] format and its input ([CborDecoder] and [CborEncoder]).
 */
internal object CborMapSerializer : KSerializer<CborMap>, CborSerializer {
    override val descriptor: SerialDescriptor =
        SerialDescriptor(
            serialName = "kotlinx.serialization.cbor.CborMap",
            original = MapSerializer(CborElementSerializer, CborElementSerializer).descriptor
        )

    override fun serialize(encoder: Encoder, value: CborMap) {
        val cborEncoder = encoder.asCborEncoder()
        cborEncoder.encodeTags(value.tags)
        MapSerializer(CborElementSerializer, CborElementSerializer).serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): CborMap {
        val element = decoder.asCborDecoder().decodeCborElement()
        if (element !is CborMap) throw CborDecodingException("Unexpected CBOR element, expected CborMap, had ${element::class}")
        return element
    }
}

/**
 * Serializer object providing [SerializationStrategy] and [DeserializationStrategy] for [CborList].
 * It can only be used by with [Cbor] format an its input ([CborDecoder] and [CborEncoder]).
 */
internal object CborListSerializer : KSerializer<CborList>, CborSerializer {
    override val descriptor: SerialDescriptor =
        SerialDescriptor(
            serialName = "kotlinx.serialization.cbor.CborList",
            original = ListSerializer(CborElementSerializer).descriptor
        )

    override fun serialize(encoder: Encoder, value: CborList) {
        val cborEncoder = encoder.asCborEncoder()
        cborEncoder.encodeTags(value.tags)
        ListSerializer(CborElementSerializer).serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): CborList {
        val element = decoder.asCborDecoder().decodeCborElement()
        if (element !is CborList) throw CborDecodingException("Unexpected CBOR element, expected CborList, had ${element::class}")
        return element
    }
}


internal fun Decoder.asCborDecoder(): CborDecoder = this as? CborDecoder
    ?: throw IllegalStateException(
        "This serializer can be used only with Cbor format. " +
            "Expected Decoder to be CborDecoder, got ${this::class}"
    )

/*need to expose writer to access encodeTag()*/
internal fun Encoder.asCborEncoder() = this as? CborEncoder
    ?: throw IllegalStateException(
        "This serializer can be used only with Cbor format. " +
            "Expected Encoder to be CborEncoder, got ${this::class}"
    )

/**
 * Returns serial descriptor that delegates all the calls to descriptor returned by [deferred] block.
 * Used to resolve cyclic dependencies between recursive serializable structures.
 */
@OptIn(ExperimentalSerializationApi::class)
private fun defer(deferred: () -> SerialDescriptor): SerialDescriptor = object : SerialDescriptor {
    private val original: SerialDescriptor by lazy(deferred)

    override val serialName: String
        get() = original.serialName
    override val kind: SerialKind
        get() = original.kind
    override val elementsCount: Int
        get() = original.elementsCount

    override fun getElementName(index: Int): String = original.getElementName(index)
    override fun getElementIndex(name: String): Int = original.getElementIndex(name)
    override fun getElementAnnotations(index: Int): List<Annotation> = original.getElementAnnotations(index)
    override fun getElementDescriptor(index: Int): SerialDescriptor = original.getElementDescriptor(index)
    override fun isElementOptional(index: Int): Boolean = original.isElementOptional(index)
}
