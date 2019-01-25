/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*

internal inline fun <T> JsonOutput.encodePolymorphically(serializer: SerializationStrategy<T>, value: T, ifPolymorphic: () -> Unit) {
    if (serializer !is PolymorphicSerializer<*> || json.useArrayPolymorphism) {
        serializer.serialize(this, value)
        return
    }

    @Suppress("UNCHECKED_CAST")
    val actualSerializer = serializer.findPolymorphicSerializer(this, value as Any) as KSerializer<Any>
    if (actualSerializer is EnumSerializer<*>) {
        throw IllegalStateException("Enums cannot be serialized polymorphically")
    }
    ifPolymorphic()
    actualSerializer.serialize(this, value)
}

internal fun <T> JsonInput.decodeSerializableValuePolymorphic(deserializer: DeserializationStrategy<T>): T {
    if (deserializer !is PolymorphicSerializer<*> || json.useArrayPolymorphism) {
        return deserializer.deserialize(this)
    }

    val jsonTree = cast<JsonObject>(decodeJson())
    val type = jsonTree.getValue(json.classDiscriminator).content
    (jsonTree.content as MutableMap).remove(json.classDiscriminator)
    @Suppress("UNCHECKED_CAST")
    val actualSerializer = deserializer.findPolymorphicSerializer(this, type) as KSerializer<T>
    return json.readJson(jsonTree, actualSerializer)
}
