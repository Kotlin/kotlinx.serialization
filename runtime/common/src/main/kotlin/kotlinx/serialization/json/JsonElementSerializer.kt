/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
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
 *   val string = Json.stringify(JsonElementSerializer, json { "key" to 1.0 })
 *   val literal = Json.parse(JsonElementSerializer, string)
 *   assertEquals(JsonObject(mapOf("key" to JsonLiteral(1.0))), literal)
 * ```
 */
@Serializer(forClass = JsonElement::class)
public object JsonElementSerializer : KSerializer<JsonElement> {
    override val descriptor: SerialDescriptor = object : SerialClassDescImpl("JsonElementSerializer") {
        override val kind: SerialKind get() = UnionKind.SEALED
    }

    override fun serialize(encoder: Encoder, obj: JsonElement) {
        verify(encoder)
        when (obj) {
            is JsonPrimitive -> JsonPrimitiveSerializer.serialize(encoder, obj)
            is JsonObject -> JsonObjectSerializer.serialize(encoder, obj)
            is JsonArray -> JsonArraySerializer.serialize(encoder, obj)
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
    override val descriptor: SerialDescriptor get() = JsonPrimitiveDescriptor

    override fun serialize(encoder: Encoder, obj: JsonPrimitive) {
        verify(encoder)
        return if (obj is JsonNull) {
            JsonNullSerializer.serialize(encoder, JsonNull)
        } else {
            JsonLiteralSerializer.serialize(encoder, obj as JsonLiteral)
        }
    }

    override fun deserialize(decoder: Decoder): JsonPrimitive {
        verify(decoder)
        return if (decoder.decodeNotNullMark()) JsonPrimitive(decoder.decodeString())
        else JsonNullSerializer.deserialize(decoder)
    }

    private object JsonPrimitiveDescriptor : SerialClassDescImpl("JsonPrimitive") {
        override val kind: SerialKind
            get() = PrimitiveKind.STRING
    }
}

/**
 * External [Serializer] object providing [SerializationStrategy] and [DeserializationStrategy] for [JsonNull].
 * It can only be used by with [Json] format an its input ([JsonInput] and [JsonOutput]).
 */
@Serializer(forClass = JsonNull::class)
public object JsonNullSerializer : KSerializer<JsonNull> {
    override val descriptor: SerialDescriptor get() = JsonNullDescriptor

    override fun serialize(encoder: Encoder, obj: JsonNull) {
        verify(encoder)
        encoder.encodeNull()
    }

    override fun deserialize(decoder: Decoder): JsonNull {
        verify(decoder)
        decoder.decodeNull()
        return JsonNull
    }

    private object JsonNullDescriptor : SerialClassDescImpl("JsonNull") {
        override val kind: SerialKind get() = UnionKind.OBJECT
    }
}

/**
 * External [Serializer] object providing [SerializationStrategy] and [DeserializationStrategy] for [JsonLiteral].
 * It can only be used by with [Json] format an its input ([JsonInput] and [JsonOutput]).
 */
@Serializer(forClass = JsonLiteral::class)
public object JsonLiteralSerializer : KSerializer<JsonLiteral> {

    override val descriptor: SerialDescriptor get() = JsonLiteralDescriptor

    override fun serialize(encoder: Encoder, obj: JsonLiteral) {
        verify(encoder)
        if (obj.isString) {
            return encoder.encodeString(obj.content)
        }

        val integer = obj.intOrNull
        if (integer != null) {
            return encoder.encodeInt(integer)
        }

        val double = obj.doubleOrNull
        if (double != null) {
            return encoder.encodeDouble(double)
        }

        val boolean = obj.booleanOrNull
        if (boolean != null) {
            return encoder.encodeBoolean(boolean)
        }

        encoder.encodeString(obj.content)
    }

    override fun deserialize(decoder: Decoder): JsonLiteral {
        verify(decoder)
        return JsonLiteral(decoder.decodeString())
    }

    private object JsonLiteralDescriptor : SerialClassDescImpl("JsonLiteral") {
        override val kind: SerialKind
            get() = PrimitiveKind.STRING
    }
}

/**
 * External [Serializer] object providing [SerializationStrategy] and [DeserializationStrategy] for [JsonObject].
 * It can only be used by with [Json] format an its input ([JsonInput] and [JsonOutput]).
 */
@Serializer(forClass = JsonObject::class)
public object JsonObjectSerializer : KSerializer<JsonObject> {
    override val descriptor: SerialDescriptor =
        NamedMapClassDescriptor("JsonObject", StringSerializer.descriptor, JsonElementSerializer.descriptor)

    override fun serialize(encoder: Encoder, obj: JsonObject) {
        verify(encoder)
        LinkedHashMapSerializer(StringSerializer, JsonElementSerializer).serialize(encoder, obj.content)
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

    override val descriptor: SerialDescriptor = NamedListClassDescriptor("JsonArray",
        JsonElementSerializer.descriptor)

    override fun serialize(encoder: Encoder, obj: JsonArray) {
        verify(encoder)
        ArrayListSerializer(JsonElementSerializer).serialize(encoder, obj)
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