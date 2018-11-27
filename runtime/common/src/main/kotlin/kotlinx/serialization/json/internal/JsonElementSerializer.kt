/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*

@Serializer(forClass = JsonElement::class)
internal object JsonElementSerializer : KSerializer<JsonElement> {
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

@Serializer(forClass = JsonPrimitive::class)
internal object JsonPrimitiveSerializer : KSerializer<JsonPrimitive> {
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

@Serializer(forClass = JsonNull::class)
internal object JsonNullSerializer : KSerializer<JsonNull> {
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

@Serializer(forClass = JsonLiteral::class)
internal object JsonLiteralSerializer : KSerializer<JsonLiteral> {

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

@Serializer(forClass = JsonObject::class)
internal object JsonObjectSerializer : KSerializer<JsonObject> {
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

@Serializer(forClass = JsonArray::class)
internal object JsonArraySerializer : KSerializer<JsonArray> {

    override val descriptor: SerialDescriptor = NamedListClassDescriptor("JsonArray",
        JsonElementSerializer.descriptor)

    override fun serialize(encoder: Encoder, obj: JsonArray) {
        ArrayListSerializer(JsonElementSerializer).serialize(encoder, obj)
    }

    override fun deserialize(decoder: Decoder): JsonArray {
        return JsonArray(ArrayListSerializer(JsonElementSerializer).deserialize(decoder))
    }
}
