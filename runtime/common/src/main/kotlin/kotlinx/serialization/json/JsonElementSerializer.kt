/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.internal.*

/**
 * External [Serializer] object providing [SerializationStrategy] and [DeserializationStrategy] for [JsonElement].
 * It can only be used by a [JsonInput] decoder for deserialization.
 * Example usage:
 * ```
 *   val string = Json.stringify(JsonElementSerializer, JsonLiteral(1.3))
 *   val literal = Json.parse(JsonElementSerializer, string)
 *
 *   assertEquals(JsonLiteral(1.3), literal)
 * ```
 *
 */
@Serializer(forClass = JsonElement::class)
public object JsonElementSerializer : KSerializer<JsonElement> {
    override val descriptor: SerialDescriptor = object : SerialClassDescImpl("JsonElementSerializer") {
        override val kind: SerialKind
            get() = UnionKind.SEALED

        init {
            addElement("JsonElement")
        }
    }

    override fun serialize(encoder: Encoder, obj: JsonElement) {
        when (obj) {
            is JsonPrimitive -> JsonPrimitiveSerializer.serialize(encoder, obj)
            is JsonObject -> JsonObjectSerializer.serialize(encoder, obj)
            is JsonArray -> JsonArraySerializer.serialize(encoder, obj)
        }
    }

    override fun deserialize(decoder: Decoder): JsonElement {
        val input = decoder as? JsonInput ?: error("JsonElement is deserializable only when used by Json")
        return input.decodeJson()
    }
}

/**
 * External [Serializer] object providing [SerializationStrategy] and [DeserializationStrategy] for [JsonPrimitive].
 */
@Serializer(forClass = JsonPrimitive::class)
public object JsonPrimitiveSerializer : KSerializer<JsonPrimitive> {
    override val descriptor: SerialDescriptor =
        JsonPrimitiveDescriptor

    override fun serialize(encoder: Encoder, obj: JsonPrimitive) {
        return if (obj is JsonNull) {
            JsonNullSerializer.serialize(encoder, JsonNull)
        } else {
            JsonLiteralSerializer.serialize(encoder, obj as JsonLiteral)
        }
    }

    override fun deserialize(decoder: Decoder): JsonPrimitive {
        return if (decoder.decodeNotNullMark()) JsonPrimitive(decoder.decodeString())
        else JsonNullSerializer.deserialize(decoder)
    }

    private object JsonPrimitiveDescriptor : SerialClassDescImpl("JsonPrimitive") {
        override val kind: SerialKind
            get() = PrimitiveKind.STRING

        override val isNullable: Boolean
            get() = true

        init {
            addElement("JsonPrimitive")
        }
    }
}

/**
 * External [Serializer] object providing [SerializationStrategy] and [DeserializationStrategy] for [JsonNull].
 */
@Serializer(forClass = JsonNull::class)
public object JsonNullSerializer : KSerializer<JsonNull> {
    override val descriptor: SerialDescriptor =
        JsonNullDescriptor

    override fun serialize(encoder: Encoder, obj: JsonNull) {
        encoder.encodeNull()
    }

    override fun deserialize(decoder: Decoder): JsonNull {
        decoder.decodeNull()
        return JsonNull
    }

    private object JsonNullDescriptor : SerialClassDescImpl("JsonNull") {
        override val kind: SerialKind
            get() = UnionKind.OBJECT

        override val isNullable: Boolean
            get() = true

        init {
            addElement("JsonNull")
        }
    }
}

/**
 * External [Serializer] object providing [SerializationStrategy] and [DeserializationStrategy] for [JsonLiteral].
 */
@Serializer(forClass = JsonLiteral::class)
public object JsonLiteralSerializer : KSerializer<JsonLiteral> {

    override val descriptor: SerialDescriptor =
        JsonLiteralDescriptor

    override fun serialize(encoder: Encoder, obj: JsonLiteral) {
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
        return JsonLiteral(decoder.decodeString())
    }

    private object JsonLiteralDescriptor : SerialClassDescImpl("JsonLiteral") {
        override val kind: SerialKind
            get() = PrimitiveKind.STRING

        init {
            addElement("JsonLiteral")
        }
    }
}

/**
 * External [Serializer] object providing [SerializationStrategy] and [DeserializationStrategy] for [JsonObject].
 */
@Serializer(forClass = JsonObject::class)
public object JsonObjectSerializer : KSerializer<JsonObject> {
    override val descriptor: SerialDescriptor =
        NamedMapClassDescriptor("JsonObject", StringSerializer.descriptor,
            JsonElementSerializer.descriptor)

    override fun serialize(encoder: Encoder, obj: JsonObject) {
        LinkedHashMapSerializer(StringSerializer, JsonElementSerializer).serialize(encoder, obj.content)
    }

    override fun deserialize(decoder: Decoder): JsonObject {
        return JsonObject(LinkedHashMapSerializer(StringSerializer, JsonElementSerializer).deserialize(decoder))
    }
}

/**
 * External [Serializer] object providing [SerializationStrategy] and [DeserializationStrategy] for [JsonArray].
 */
@Serializer(forClass = JsonArray::class)
public object JsonArraySerializer : KSerializer<JsonArray> {

    override val descriptor: SerialDescriptor = NamedListClassDescriptor("JsonArray",
        JsonElementSerializer.descriptor)

    override fun serialize(encoder: Encoder, obj: JsonArray) {
        ArrayListSerializer(JsonElementSerializer).serialize(encoder, obj)
    }

    override fun deserialize(decoder: Decoder): JsonArray {
        return JsonArray(ArrayListSerializer(JsonElementSerializer).deserialize(decoder))
    }
}
