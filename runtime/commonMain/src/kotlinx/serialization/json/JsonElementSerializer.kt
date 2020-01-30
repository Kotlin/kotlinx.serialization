/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.internal.*

/**
 * External [Serializer] object providing [SerializationStrategy] and [DeserializationStrategy] for [JsonElement].
 * It can only be used by with [Json] format an its input ([JsonInput] and [JsonOutput]).
 * Currently, this hierarchy has no guarantees on descriptor content.
 *
 * Example usage:
 * ```
 * val string = Json.stringify(JsonElementSerializer, json { "key" to 1.0 })
 * val literal = Json.parse(JsonElementSerializer, string)
 * assertEquals(JsonObject(mapOf("key" to JsonLiteral(1.0))), literal)
 * ```
 */
@Serializer(forClass = JsonElement::class)
public object JsonElementSerializer : KSerializer<JsonElement> {
    override val descriptor: SerialDescriptor =
        SerialDescriptor("kotlinx.serialization.json.JsonElement", PolymorphicKind.SEALED) {
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
        verify(decoder)
        val input = decoder as JsonInput
        return input.decodeJson()
    }
}

/**
 * External [Serializer] object providing [SerializationStrategy] and [DeserializationStrategy] for [JsonPrimitive].
 * It can only be used by with [Json] format an its input ([JsonInput] and [JsonOutput]).
 */
@Serializer(forClass = JsonPrimitive::class)
public object JsonPrimitiveSerializer : KSerializer<JsonPrimitive> {
    override val descriptor: SerialDescriptor =
        SerialDescriptor("kotlinx.serialization.json.JsonPrimitive", PrimitiveKind.STRING) {}

    override fun serialize(encoder: Encoder, value: JsonPrimitive) {
        verify(encoder)
        return if (value is JsonNull) {
            encoder.encodeSerializableValue(JsonNullSerializer, JsonNull)
        } else {
            encoder.encodeSerializableValue(JsonLiteralSerializer, value as JsonLiteral)
        }
    }

    override fun deserialize(decoder: Decoder): JsonPrimitive {
        verify(decoder)
        return if (decoder.decodeNotNullMark()) JsonPrimitive(decoder.decodeString())
        else decoder.decodeSerializableValue(JsonNullSerializer)
    }
}

/**
 * External [Serializer] object providing [SerializationStrategy] and [DeserializationStrategy] for [JsonNull].
 * It can only be used by with [Json] format an its input ([JsonInput] and [JsonOutput]).
 */
@Serializer(forClass = JsonNull::class)
public object JsonNullSerializer : KSerializer<JsonNull> {
    // technically, JsonNull is an object, but it does not call beginStructure/endStructure at all
    override val descriptor: SerialDescriptor =
        SerialDescriptor("kotlinx.serialization.json.JsonNull", UnionKind.ENUM_KIND) {}

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

/**
 * External [Serializer] object providing [SerializationStrategy] and [DeserializationStrategy] for [JsonLiteral].
 * It can only be used by with [Json] format an its input ([JsonInput] and [JsonOutput]).
 */
@Serializer(forClass = JsonLiteral::class)
public object JsonLiteralSerializer : KSerializer<JsonLiteral> {

    override val descriptor: SerialDescriptor =
        SerialDescriptor("kotlinx.serialization.json.JsonLiteral", PrimitiveKind.STRING) {}

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
        verify(decoder)
        return JsonLiteral(decoder.decodeString())
    }
}

/**
 * External [Serializer] object providing [SerializationStrategy] and [DeserializationStrategy] for [JsonObject].
 * It can only be used by with [Json] format an its input ([JsonInput] and [JsonOutput]).
 */
@Serializer(forClass = JsonObject::class)
public object JsonObjectSerializer : KSerializer<JsonObject> {
    override val descriptor: SerialDescriptor =
        NamedMapClassDescriptor(
            "kotlinx.serialization.json.JsonObject",
            StringSerializer.descriptor,
            JsonElementSerializer.descriptor
        )

    override fun serialize(encoder: Encoder, value: JsonObject) {
        verify(encoder)
        LinkedHashMapSerializer(StringSerializer, JsonElementSerializer).serialize(encoder, value.content)
    }

    override fun deserialize(decoder: Decoder): JsonObject {
        verify(decoder)
        return JsonObject(LinkedHashMapSerializer(StringSerializer, JsonElementSerializer).deserialize(decoder))
    }
}

/**
 * External [Serializer] object providing [SerializationStrategy] and [DeserializationStrategy] for [JsonArray].
 * It can only be used by with [Json] format an its input ([JsonInput] and [JsonOutput]).
 */
@Serializer(forClass = JsonArray::class)
public object JsonArraySerializer : KSerializer<JsonArray> {

    override val descriptor: SerialDescriptor = NamedListClassDescriptor(
        "kotlinx.serialization.json.JsonArray",
        JsonElementSerializer.descriptor
    )

    override fun serialize(encoder: Encoder, value: JsonArray) {
        verify(encoder)
        ArrayListSerializer(JsonElementSerializer).serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): JsonArray {
        verify(decoder)
        return JsonArray(ArrayListSerializer(JsonElementSerializer).deserialize(decoder))
    }
}

private fun verify(encoder: Encoder) {
    if (encoder !is JsonOutput) error("Json element serializer can be used only by Json format")
}

private fun verify(decoder: Decoder) {
    if (decoder !is JsonInput) error("Json element serializer can be used only by Json format")
}
