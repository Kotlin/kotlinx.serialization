/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.internal.JsonDecodingException

/**
 * External [Serializer] object providing [SerializationStrategy] and [DeserializationStrategy] for [JsonElement].
 * It can only be used by with [Json] format an its input ([JsonDecoder] and [JsonEncoder]).
 * Currently, this hierarchy has no guarantees on descriptor content.
 *
 * Example usage:
 * ```
 * val string = Json.encodeToString(JsonElementSerializer, json { "key" to 1.0 })
 * val literal = Json.decodeFromString(JsonElementSerializer, string)
 * assertEquals(JsonObject(mapOf("key" to JsonLiteral(1.0))), literal)
 * ```
 */
@Serializer(forClass = JsonElement::class)
@PublishedApi
internal object JsonElementSerializer : KSerializer<JsonElement> {
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("kotlinx.serialization.json.JsonElement", PolymorphicKind.SEALED) {
            // Resolve cyclic dependency in descriptors by late binding
            element("JsonPrimitive", defer { JsonPrimitiveSerializer.descriptor })
            element("JsonNull", defer { JsonNullSerializer.descriptor })
            element("JsonLiteral", defer { JsonLiteralSerializer.descriptor })
            element("JsonObject", defer { JsonObjectSerializer.descriptor })
            element("JsonArray", defer { JsonArraySerializer.descriptor })
        }

    override fun serialize(encoder: Encoder, value: JsonElement) {
        verify(encoder)
        when (value) {
            is JsonPrimitive -> encoder.encodeSerializableValue(JsonPrimitiveSerializer, value)
            is JsonObject -> encoder.encodeSerializableValue(JsonObjectSerializer, value)
            is JsonArray -> encoder.encodeSerializableValue(JsonArraySerializer, value)
        }
    }

    override fun deserialize(decoder: Decoder): JsonElement {
        val input = decoder.asJsonDecoder()
        return input.decodeJsonElement()
    }
}

/**
 * External [Serializer] object providing [SerializationStrategy] and [DeserializationStrategy] for [JsonPrimitive].
 * It can only be used by with [Json] format an its input ([JsonDecoder] and [JsonEncoder]).
 */
@Serializer(forClass = JsonPrimitive::class)
@PublishedApi
internal object JsonPrimitiveSerializer : KSerializer<JsonPrimitive> {
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("kotlinx.serialization.json.JsonPrimitive", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: JsonPrimitive) {
        verify(encoder)
        return if (value is JsonNull) {
            encoder.encodeSerializableValue(JsonNullSerializer, JsonNull)
        } else {
            encoder.encodeSerializableValue(JsonLiteralSerializer, value as JsonLiteral)
        }
    }

    override fun deserialize(decoder: Decoder): JsonPrimitive {
        val result = decoder.asJsonDecoder().decodeJsonElement()
        if (result !is JsonPrimitive) throw JsonDecodingException(-1, "Unexpected JSON element, expected JsonPrimitive, had ${result::class}", result.toString())
        return result
    }
}

/**
 * External [Serializer] object providing [SerializationStrategy] and [DeserializationStrategy] for [JsonNull].
 * It can only be used by with [Json] format an its input ([JsonDecoder] and [JsonEncoder]).
 */
@Serializer(forClass = JsonNull::class)
@PublishedApi
internal object JsonNullSerializer : KSerializer<JsonNull> {
    // technically, JsonNull is an object, but it does not call beginStructure/endStructure at all
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("kotlinx.serialization.json.JsonNull", SerialKind.ENUM)

    override fun serialize(encoder: Encoder, value: JsonNull) {
        verify(encoder)
        encoder.encodeNull()
    }

    override fun deserialize(decoder: Decoder): JsonNull {
        verify(decoder)
        decoder.decodeNull()
        return JsonNull
    }
}

private object JsonLiteralSerializer : KSerializer<JsonLiteral> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.serialization.json.JsonLiteral", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: JsonLiteral) {
        verify(encoder)
        if (value.isString) {
            return encoder.encodeString(value.content)
        }

        val long = value.longOrNull
        if (long != null) {
            return encoder.encodeLong(long)
        }

        val double = value.doubleOrNull
        if (double != null) {
            return encoder.encodeDouble(double)
        }

        val boolean = value.booleanOrNull
        if (boolean != null) {
            return encoder.encodeBoolean(boolean)
        }
        encoder.encodeString(value.content)
    }

    override fun deserialize(decoder: Decoder): JsonLiteral {
        val result = decoder.asJsonDecoder().decodeJsonElement()
        if (result !is JsonLiteral) throw JsonDecodingException(-1, "Unexpected JSON element, expected JsonLiteral, had ${result::class}", result.toString())
        return result
    }
}

/**
 * External [Serializer] object providing [SerializationStrategy] and [DeserializationStrategy] for [JsonObject].
 * It can only be used by with [Json] format an its input ([JsonDecoder] and [JsonEncoder]).
 */
@Serializer(forClass = JsonObject::class)
@PublishedApi
internal object JsonObjectSerializer : KSerializer<JsonObject> {
    override val descriptor: SerialDescriptor =
        NamedMapClassDescriptor(
            "kotlinx.serialization.json.JsonObject",
            String.serializer().descriptor,
            JsonElementSerializer.descriptor
        )

    override fun serialize(encoder: Encoder, value: JsonObject) {
        verify(encoder)
        MapSerializer(String.serializer(), JsonElementSerializer).serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): JsonObject {
        verify(decoder)
        return JsonObject(MapSerializer(String.serializer(), JsonElementSerializer).deserialize(decoder))
    }
}

/**
 * External [Serializer] object providing [SerializationStrategy] and [DeserializationStrategy] for [JsonArray].
 * It can only be used by with [Json] format an its input ([JsonDecoder] and [JsonEncoder]).
 */
@Serializer(forClass = JsonArray::class)
@PublishedApi
internal object JsonArraySerializer : KSerializer<JsonArray> {

    override val descriptor: SerialDescriptor = NamedListClassDescriptor(
        "kotlinx.serialization.json.JsonArray",
        JsonElementSerializer.descriptor
    )

    override fun serialize(encoder: Encoder, value: JsonArray) {
        verify(encoder)
        ListSerializer(JsonElementSerializer).serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): JsonArray {
        verify(decoder)
        return JsonArray(ListSerializer(JsonElementSerializer).deserialize(decoder))
    }
}

private fun verify(encoder: Encoder) {
    encoder.asJsonEncoder()
}

private fun verify(decoder: Decoder) {
    decoder.asJsonDecoder()
}

internal fun Decoder.asJsonDecoder(): JsonDecoder = this as? JsonDecoder
    ?: throw IllegalStateException(
        "This serializer can be used only with Json format." +
                "Expected Decoder to be JsonDecoder, got ${this::class}"
    )

internal fun Encoder.asJsonEncoder() = this as? JsonEncoder
    ?: throw IllegalStateException(
        "This serializer can be used only with Json format." +
                "Expected Encoder to be JsonEncoder, got ${this::class}"
    )
