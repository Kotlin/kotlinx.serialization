/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*

@Suppress("UNCHECKED_CAST")
internal inline fun <T> JsonEncoder.encodePolymorphically(serializer: SerializationStrategy<T>, value: T, ifPolymorphic: () -> Unit) {
    if (serializer !is AbstractPolymorphicSerializer<*> || json.configuration.useArrayPolymorphism) {
        serializer.serialize(this, value)
        return
    }
    val actualSerializer = findActualSerializer(serializer.cast(), value as Any)
    ifPolymorphic()
    actualSerializer.serialize(this, value)
}

private fun JsonEncoder.findActualSerializer(
    serializer: SerializationStrategy<Any>,
    value: Any
): SerializationStrategy<Any> {
    val casted = serializer as AbstractPolymorphicSerializer<Any>
    val actualSerializer = casted.findPolymorphicSerializer(this, value as Any)
    validateIfSealed(casted, actualSerializer, json.configuration.classDiscriminator)
    val kind = actualSerializer.descriptor.kind
    checkKind(kind)
    return actualSerializer
}

private fun validateIfSealed(
    serializer: SerializationStrategy<*>,
    actualSerializer: SerializationStrategy<Any>,
    classDiscriminator: String
) {
    if (serializer !is SealedClassSerializer<*>) return
    if (classDiscriminator in actualSerializer.descriptor.cachedSerialNames()) {
        val baseName = serializer.descriptor.serialName
        val actualName = actualSerializer.descriptor.serialName
        error(
            "Sealed class '$actualName' cannot be serialized as base class '$baseName' because" +
                    " it has property name that conflicts with JSON class discriminator '$classDiscriminator'. " +
                    "You can either change class discriminator in JsonConfiguration, " +
                    "rename property with @SerialName annotation or fall back to array polymorphism"
        )
    }
}

internal fun checkKind(kind: SerialKind) {
    if (kind is SerialKind.ENUM) error("Enums cannot be serialized polymorphically with 'type' parameter. You can use 'JsonBuilder.useArrayPolymorphism' instead")
    if (kind is PrimitiveKind) error("Primitives cannot be serialized polymorphically with 'type' parameter. You can use 'JsonBuilder.useArrayPolymorphism' instead")
    if (kind is PolymorphicKind) error("Actual serializer for polymorphic cannot be polymorphic itself")
}

internal fun <T> JsonDecoder.decodeSerializableValuePolymorphic(deserializer: DeserializationStrategy<T>): T {
    if (deserializer !is AbstractPolymorphicSerializer<*> || json.configuration.useArrayPolymorphism) {
        return deserializer.deserialize(this)
    }

    val jsonTree = cast<JsonObject>(decodeJsonElement(), deserializer.descriptor)
    val discriminator = json.configuration.classDiscriminator
    val type = jsonTree[discriminator]?.jsonPrimitive?.content
    val actualSerializer = deserializer.findPolymorphicSerializerOrNull(this, type)?.cast<T>()
        ?: throwSerializerNotFound(type, jsonTree)
    return json.readPolymorphicJson(discriminator, jsonTree, actualSerializer)
}

private fun throwSerializerNotFound(type: String?, jsonTree: JsonObject): Nothing {
    val suffix =
        if (type == null) "missing class discriminator ('null')"
        else "class discriminator '$type'"
    throw JsonDecodingException(-1, "Polymorphic serializer was not found for $suffix", jsonTree.toString())
}
