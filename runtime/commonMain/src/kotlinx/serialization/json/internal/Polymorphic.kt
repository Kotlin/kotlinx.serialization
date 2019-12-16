/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.internal.AbstractPolymorphicSerializer
import kotlinx.serialization.json.*

@Suppress("UNCHECKED_CAST")
internal inline fun <T> JsonOutput.encodePolymorphically(serializer: SerializationStrategy<T>, value: T, ifPolymorphic: () -> Unit) {
    if (serializer !is AbstractPolymorphicSerializer<*> || json.configuration.useArrayPolymorphism) {
        serializer.serialize(this, value)
        return
    }
    serializer as AbstractPolymorphicSerializer<Any> // PolymorphicSerializer <*> projects 2nd argument of findPolymorphic... to Nothing, so we need an additional cast
    val actualSerializer = serializer.findPolymorphicSerializer(this, value as Any) as KSerializer<Any>
    val kind = actualSerializer.descriptor.kind
    checkKind(kind)
    ifPolymorphic()
    actualSerializer.serialize(this, value)
}

internal fun checkKind(kind: SerialKind) {
    if (kind is UnionKind.ENUM_KIND) error("Enums cannot be serialized polymorphically with 'type' parameter. You can use 'JsonConfiguration.useArrayPolymorphism' instead")
    if (kind is PrimitiveKind) error("Primitives cannot be serialized polymorphically with 'type' parameter. You can use 'JsonConfiguration.useArrayPolymorphism' instead")
    if (kind is PolymorphicKind) error("Actual serializer for polymorphic cannot be polymorphic itself")
}

internal fun <T> JsonInput.decodeSerializableValuePolymorphic(deserializer: DeserializationStrategy<T>): T {
    if (deserializer !is AbstractPolymorphicSerializer<*> || json.configuration.useArrayPolymorphism) {
        return deserializer.deserialize(this)
    }

    val jsonTree = cast<JsonObject>(decodeJson())
    val type = jsonTree.getValue(json.configuration.classDiscriminator).content
    (jsonTree.content as MutableMap).remove(json.configuration.classDiscriminator)
    @Suppress("UNCHECKED_CAST")
    val actualSerializer = deserializer.findPolymorphicSerializer(this, type) as KSerializer<T>
    return json.readJson(jsonTree, actualSerializer)
}
