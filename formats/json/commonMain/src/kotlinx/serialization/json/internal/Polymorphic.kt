/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*

@Suppress("UNCHECKED_CAST")
internal inline fun <T> JsonEncoder.encodePolymorphically(
    serializer: SerializationStrategy<T>,
    value: T,
    ifPolymorphic: (String) -> Unit
) {
    if (serializer !is AbstractPolymorphicSerializer<*> || json.configuration.useArrayPolymorphism) {
        serializer.serialize(this, value)
        return
    }
    val casted = serializer as AbstractPolymorphicSerializer<Any>
    val baseClassDiscriminator = serializer.descriptor.classDiscriminator(json)
    val actualSerializer = casted.findPolymorphicSerializer(this, value as Any)
    validateIfSealed(casted, actualSerializer, baseClassDiscriminator)
    checkKind(actualSerializer.descriptor.kind)
    ifPolymorphic(baseClassDiscriminator)
    actualSerializer.serialize(this, value)
}

private fun validateIfSealed(
    serializer: SerializationStrategy<*>,
    actualSerializer: SerializationStrategy<Any>,
    classDiscriminator: String
) {
    if (serializer !is SealedClassSerializer<*>) return
    @Suppress("DEPRECATION_ERROR")
    if (classDiscriminator in actualSerializer.descriptor.jsonCachedSerialNames()) {
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
    val discriminator = deserializer.descriptor.classDiscriminator(json)
    val type = jsonTree[discriminator]?.jsonPrimitive?.content
    val actualSerializer = deserializer.findPolymorphicSerializerOrNull(this, type)
        ?: throwSerializerNotFound(type, jsonTree)

    @Suppress("UNCHECKED_CAST")
    return json.readPolymorphicJson(discriminator, jsonTree, actualSerializer as DeserializationStrategy<T>)
}

private fun throwSerializerNotFound(type: String?, jsonTree: JsonObject): Nothing {
    val suffix =
        if (type == null) "missing class discriminator ('null')"
        else "class discriminator '$type'"
    throw JsonDecodingException(-1, "Polymorphic serializer was not found for $suffix", jsonTree.toString())
}

internal fun SerialDescriptor.classDiscriminator(json: Json): String =
    annotations.filterIsInstance<JsonClassDiscriminator>().singleOrNull()?.discriminator
            ?: json.configuration.classDiscriminator

