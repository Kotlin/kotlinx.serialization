/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.serializers

import kotlinx.serialization.*
import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*

@Serializer(forClass = JsonElement::class)
public object JsonElementSerializer : KSerializer<JsonElement> {
    override fun serialize(encoder: Encoder, obj: JsonElement) {
        when (obj) {
            is JsonPrimitive -> JsonPrimitiveSerializer.serialize(encoder, obj)
            is JsonObject -> JsonObjectSerializer.serialize(encoder, obj)
            is JsonArray -> JsonArraySerializer.serialize(encoder, obj)
        }
    }

    override fun deserialize(decoder: Decoder): JsonElement {
        val input = decoder as? JsonInput ?: error("JsonElement is serializable only when used by Json")
        return input.readTree()
    }

    override val descriptor: SerialDescriptor = object : SerialClassDescImpl("JsonElementSerializer") {
        override val kind: SerialKind
            get() = UnionKind.SEALED

        init {
            addElement("JsonElement")
        }
    }
}

@Serializer(forClass = JsonObject::class)
public object JsonObjectSerializer : KSerializer<JsonObject> {

    override val descriptor: SerialDescriptor = JsonObjectDescriptor

    override fun serialize(encoder: Encoder, obj: JsonObject) {
        val composite = encoder.beginCollection(descriptor, obj.size, StringSerializer, JsonElementSerializer)
        var index = 0
        obj.content.onEach { (key, value) ->
            composite.encodeSerializableElement(descriptor, index++, StringSerializer, key)
            composite.encodeSerializableElement(descriptor, index++, JsonElementSerializer, value)
        }
        composite.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): JsonObject {
        val content = LinkedHashMap<String, JsonElement>()
        val composite = decoder.beginStructure(descriptor, StringSerializer, JsonElementSerializer)
        while (true) {
            val index = composite.decodeElementIndex(descriptor)
            if (index == READ_DONE) {
                composite.endStructure(descriptor)
                return JsonObject(content)
            }

            val key = composite.decodeStringElement(StringDescriptor, index)
            val valueIndex = composite.decodeElementIndex(descriptor)
            val value = composite.decodeSerializableElement(descriptor, valueIndex, JsonElementSerializer)
            content[key] = value
        }
    }

    private object JsonObjectDescriptor : SerialDescriptor {
        override val name: String = "JsonObject"
        override val kind: SerialKind get() = StructureKind.MAP
        override val elementsCount: Int = 2
        override fun getElementName(index: Int): String = index.toString()
        override fun getElementIndex(name: String): Int =
            name.toIntOrNull() ?: throw IllegalArgumentException("$name is not a valid map index")

        override fun getElementDescriptor(index: Int): SerialDescriptor =
            if (index % 2 == 0) StringDescriptor else JsonElementSerializer.descriptor

        override fun isElementOptional(index: Int): Boolean = false
    }
}

@Serializer(forClass = JsonPrimitive::class)
public object JsonPrimitiveSerializer : KSerializer<JsonPrimitive> {

    override fun serialize(encoder: Encoder, obj: JsonPrimitive) {
        return if (obj is JsonNull) {
            JsonNullSerializer.serialize(encoder, JsonNull)
        } else {
            JsonLiteralSerializer.serialize(encoder, obj as JsonLiteral)
        }
    }

    override fun deserialize(decoder: Decoder): JsonPrimitive {
        val nullable = decoder.decodeNullable(StringSerializer)
        return if (nullable == null) JsonNull else JsonPrimitive(nullable)
    }

    override val descriptor: SerialDescriptor = JsonPrimitiveDescriptor

    object JsonPrimitiveDescriptor : SerialClassDescImpl("JsonPrimitive") {
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
public object JsonNullSerializer : KSerializer<JsonNull> {

    override fun serialize(encoder: Encoder, obj: JsonNull) {
        encoder.encodeNull()
    }

    override fun deserialize(decoder: Decoder): JsonNull {
        decoder.decodeNull()
        return JsonNull
    }

    override val descriptor: SerialDescriptor = JsonNullDescriptor

    object JsonNullDescriptor : SerialClassDescImpl("JsonNull") {
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
public object JsonLiteralSerializer : KSerializer<JsonLiteral> {

    override fun serialize(encoder: Encoder, obj: JsonLiteral) {
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

    override val descriptor: SerialDescriptor = JsonLiteralDescriptor

    object JsonLiteralDescriptor : SerialClassDescImpl("JsonLiteral") {
        override val kind: SerialKind
            get() = PrimitiveKind.STRING

        init {
            addElement("JsonLiteral")
        }
    }
}


@Serializer(forClass = JsonArray::class)
public object JsonArraySerializer : KSerializer<JsonArray> {

    override fun serialize(encoder: Encoder, obj: JsonArray) {
        val composite = encoder.beginCollection(JsonObjectSerializer.descriptor, obj.size, JsonElementSerializer)
        var index = 0
        obj.content.onEach { value ->
            composite.encodeSerializableElement(JsonObjectSerializer.descriptor, index++, JsonElementSerializer, value)
        }
        composite.endStructure(JsonObjectSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): JsonArray {
        TODO("Deserialization via generic interface is not yet supported, please use Json.toJson instead and report this issue")
    }

    override val descriptor: SerialDescriptor = JsonArrayDescriptor

    object JsonArrayDescriptor : SerialClassDescImpl("JsonArray") {
        override val kind: SerialKind
            get() = StructureKind.LIST

        init {
            addElement("JsonArray")
        }
    }
}
