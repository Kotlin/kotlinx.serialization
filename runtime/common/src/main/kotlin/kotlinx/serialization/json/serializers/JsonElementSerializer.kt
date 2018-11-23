/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.serializers

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*

@Serializer(forClass = JsonElement::class)
object JsonElementSerializer : KSerializer<JsonElement> {
    override fun serialize(encoder: Encoder, obj: JsonElement) {
        when (obj) {
            is JsonPrimitive -> JsonPrimitiveSerializer.serialize(encoder, obj)
            is JsonObject -> JsonObjectSerializer.serialize(encoder, obj)
            is JsonArray -> TODO()
        }
    }

    override fun deserialize(decoder: Decoder): JsonObject {
        TODO("Deserialization via generic interface is not yet supported, please use Json.toJson instead and report this issue")
    }

    override val descriptor: SerialDescriptor = object : SerialClassDescImpl("JsonElementSerializer") {
        override val kind: SerialKind
            get() = UnionKind.SEALED

        init {
            addElement("JsonObject")
            addElement("JsonLiteral")
            addElement("JsonArray")
        }
    }
}

private object JsonObjectSerializer : KSerializer<JsonObject> {

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
        TODO("Deserialization via generic interface is not yet supported, please use Json.toJson instead and report this issue")
    }

    private object JsonObjectDescriptor : SerialDescriptor {
        override val name: String = "JsonObject"
        override val kind: SerialKind get() = StructureKind.MAP
        override val elementsCount: Int = 2
        override fun getElementName(index: Int): String = index.toString()
        override fun getElementIndex(name: String): Int =
            name.toIntOrNull() ?: throw IllegalArgumentException("$name is not a valid map index")

        override fun getElementDescriptor(index: Int): SerialDescriptor =
            if (index % 2 == 0) StringDescriptor else TODO()

        override fun isElementOptional(index: Int): Boolean = false
    }
}

private object JsonPrimitiveSerializer : KSerializer<JsonPrimitive> {

    override fun serialize(encoder: Encoder, obj: JsonPrimitive) {
        // TODO JsonNull does not work as expected
        encoder.encodeString(obj.content)
    }

    override fun deserialize(decoder: Decoder): JsonPrimitive {
        TODO("Deserialization via generic interface is not yet supported, please use Json.toJson instead and report this issue")
    }

    override val descriptor: SerialDescriptor = JsonPrimitiveDescriptor

    object JsonPrimitiveDescriptor : SerialClassDescImpl("JsonPrimitive") {
        override val kind: SerialKind
            get() = PrimitiveKind.STRING

        init {
            // TODO what's going on
            addElement("JsonObject")
            addElement("JsonLiteral")
            addElement("JsonArray")
        }
    }
}
