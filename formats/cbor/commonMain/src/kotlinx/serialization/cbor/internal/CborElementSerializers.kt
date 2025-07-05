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

/**
 * Serializer object providing [SerializationStrategy] and [DeserializationStrategy] for [CborElement].
 * It can only be used by with [Cbor] format and its input ([CborDecoder] and [CborEncoder]).
 */
internal object CborElementSerializer : KSerializer<CborElement> {
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("kotlinx.serialization.cbor.CborElement", PolymorphicKind.SEALED) {
            // Resolve cyclic dependency in descriptors by late binding
            element("CborPrimitive", defer { CborPrimitiveSerializer.descriptor })
            element("CborNull", defer { CborNullSerializer.descriptor })
            element("CborString", defer { CborStringSerializer.descriptor })
            element("CborBoolean", defer { CborBooleanSerializer.descriptor })
            element("CborByteString", defer { CborByteStringSerializer.descriptor })
            element("CborMap", defer { CborMapSerializer.descriptor })
            element("CborList", defer { CborListSerializer.descriptor })
            element("CborDouble", defer { CborDoubleSerializer.descriptor })
            element("CborInt", defer { CborIntSerializer.descriptor })
            element("CborUInt", defer { CborUIntSerializer.descriptor })
        }

    override fun serialize(encoder: Encoder, value: CborElement) {
        verify(encoder)
        when (value) {
            is CborPrimitive -> encoder.encodeSerializableValue(CborPrimitiveSerializer, value)
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
internal object CborPrimitiveSerializer : KSerializer<CborPrimitive> {
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("kotlinx.serialization.cbor.CborPrimitive", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: CborPrimitive) {
        verify(encoder)
        when (value) {
            is CborNull -> encoder.encodeSerializableValue(CborNullSerializer, CborNull)
            is CborString -> encoder.encodeSerializableValue(CborStringSerializer, value)
            is CborBoolean -> encoder.encodeSerializableValue(CborBooleanSerializer, value)
            is CborByteString -> encoder.encodeSerializableValue(CborByteStringSerializer, value)
            is CborDouble -> encoder.encodeSerializableValue(CborDoubleSerializer, value)
            is CborNegativeInt -> encoder.encodeSerializableValue(CborIntSerializer, value)
            is CborPositiveInt -> encoder.encodeSerializableValue(CborUIntSerializer, value)
        }
    }

    override fun deserialize(decoder: Decoder): CborPrimitive {
        val result = decoder.asCborDecoder().decodeCborElement()
        if (result !is CborPrimitive) throw CborDecodingException("Unexpected CBOR element, expected CborPrimitive, had ${result::class}")
        return result
    }
}

/**
 * Serializer object providing [SerializationStrategy] and [DeserializationStrategy] for [CborNull].
 * It can only be used by with [Cbor] format an its input ([CborDecoder] and [CborEncoder]).
 */
internal object CborNullSerializer : KSerializer<CborNull> {
    // technically, CborNull is an object, but it does not call beginStructure/endStructure at all
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("kotlinx.serialization.cbor.CborNull", SerialKind.ENUM)

    override fun serialize(encoder: Encoder, value: CborNull) {
        verify(encoder)
        encoder.encodeNull()
    }

    override fun deserialize(decoder: Decoder): CborNull {
        verify(decoder)
        if (decoder.decodeNotNullMark()) {
            throw CborDecodingException("Expected 'null' literal")
        }
        decoder.decodeNull()
        return CborNull
    }
}

public object CborIntSerializer : KSerializer<CborNegativeInt> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlinx.serialization.cbor.CborInt", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: CborNegativeInt) {
        encoder.encodeLong(value.value)
    }

    override fun deserialize(decoder: Decoder): CborNegativeInt {
        return CborNegativeInt( decoder.decodeLong())
    }
}

public object CborUIntSerializer : KSerializer<CborPositiveInt> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("CborUInt", PrimitiveKind.LONG)
    
    override fun serialize(encoder: Encoder, value: CborPositiveInt) {
        encoder.encodeInline(descriptor).encodeSerializableValue(ULong.serializer(), value.value)
    }
    
    override fun deserialize(decoder: Decoder): CborPositiveInt {
        return CborPositiveInt(decoder.decodeInline(descriptor).decodeSerializableValue(ULong.serializer()))
    }
}

public object CborDoubleSerializer : KSerializer<CborDouble> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlinx.serialization.cbor.CborDouble", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: CborDouble) {
        encoder.encodeDouble(value.value)
    }

    override fun deserialize(decoder: Decoder): CborDouble {
        return CborDouble(decoder.decodeDouble())
    }
}

/**
 * Serializer object providing [SerializationStrategy] and [DeserializationStrategy] for [CborString].
 * It can only be used by with [Cbor] format an its input ([CborDecoder] and [CborEncoder]).
 */
public object CborStringSerializer : KSerializer<CborString> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.serialization.cbor.CborString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: CborString) {
        verify(encoder)
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): CborString {
        val input = decoder.asCborDecoder()
        val element = input.decodeCborElement()
        if (element !is CborString) throw CborDecodingException("Unexpected CBOR element, expected CborString, had ${element::class}")
        return element
    }
}

/**
 * Serializer object providing [SerializationStrategy] and [DeserializationStrategy] for [CborBoolean].
 * It can only be used by with [Cbor] format an its input ([CborDecoder] and [CborEncoder]).
 */
public object CborBooleanSerializer : KSerializer<CborBoolean> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.serialization.cbor.CborBoolean", PrimitiveKind.BOOLEAN)

    override fun serialize(encoder: Encoder, value: CborBoolean) {
        verify(encoder)
        encoder.encodeBoolean(value.boolean)
    }

    override fun deserialize(decoder: Decoder): CborBoolean {
        val input = decoder.asCborDecoder()
        val element = input.decodeCborElement()
        if (element !is CborBoolean) throw CborDecodingException("Unexpected CBOR element, expected CborBoolean, had ${element::class}")
        return element
    }
}

/**
 * Serializer object providing [SerializationStrategy] and [DeserializationStrategy] for [CborByteString].
 * It can only be used by with [Cbor] format an its input ([CborDecoder] and [CborEncoder]).
 */
public object CborByteStringSerializer : KSerializer<CborByteString> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.serialization.cbor.CborByteString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: CborByteString) {
        verify(encoder)
        val cborEncoder = encoder.asCborEncoder()
        cborEncoder.encodeByteArray(value.bytes)
    }

    override fun deserialize(decoder: Decoder): CborByteString {
        val input = decoder.asCborDecoder()
        val element = input.decodeCborElement()
        if (element !is CborByteString) throw CborDecodingException("Unexpected CBOR element, expected CborByteString, had ${element::class}")
        return element
    }
}

/**
 * Serializer object providing [SerializationStrategy] and [DeserializationStrategy] for [CborMap].
 * It can only be used by with [Cbor] format an its input ([CborDecoder] and [CborEncoder]).
 */
public object CborMapSerializer : KSerializer<CborMap> {
    private object CborMapDescriptor :
        SerialDescriptor by MapSerializer(CborElementSerializer, CborElementSerializer).descriptor {
        @ExperimentalSerializationApi
        override val serialName: String = "kotlinx.serialization.cbor.CborMap"
    }

    override val descriptor: SerialDescriptor = CborMapDescriptor

    override fun serialize(encoder: Encoder, value: CborMap) {
        verify(encoder)
        MapSerializer(CborElementSerializer, CborElementSerializer).serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): CborMap {
        verify(decoder)
        return CborMap(MapSerializer(CborElementSerializer, CborElementSerializer).deserialize(decoder))
    }
}

/**
 * Serializer object providing [SerializationStrategy] and [DeserializationStrategy] for [CborList].
 * It can only be used by with [Cbor] format an its input ([CborDecoder] and [CborEncoder]).
 */
public object CborListSerializer : KSerializer<CborList> {
    private object CborListDescriptor : SerialDescriptor by ListSerializer(CborElementSerializer).descriptor {
        @ExperimentalSerializationApi
        override val serialName: String = "kotlinx.serialization.cbor.CborList"
    }

    override val descriptor: SerialDescriptor = CborListDescriptor

    override fun serialize(encoder: Encoder, value: CborList) {
        verify(encoder)
        ListSerializer(CborElementSerializer).serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): CborList {
        verify(decoder)
        return CborList(ListSerializer(CborElementSerializer).deserialize(decoder))
    }
}

private fun verify(encoder: Encoder) {
    encoder.asCborEncoder()
}

private fun verify(decoder: Decoder) {
    decoder.asCborDecoder()
}

internal fun Decoder.asCborDecoder(): CborDecoder = this as? CborDecoder
    ?: throw IllegalStateException(
        "This serializer can be used only with Cbor format." +
            "Expected Decoder to be CborDecoder, got ${this::class}"
    )

internal fun Encoder.asCborEncoder() = this as? CborEncoder
    ?: throw IllegalStateException(
        "This serializer can be used only with Cbor format." +
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