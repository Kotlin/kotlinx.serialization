/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*

internal fun <T> JsonOutput.encodePolymorphically(
    serializer: PolymorphicSerializer<*>, value: T) { @Suppress("UNCHECKED_CAST")
    val actualSerializer = serializer.findPolymorphicSerializer(this, value as Any) as KSerializer<Any>
    if (actualSerializer is EnumSerializer<*>) {
        throw IllegalStateException("Enums cannot be serialized polymorphically")
    }
    actualSerializer.serialize(this, value)
}

internal fun getTypeNameProperty(json: Json): String {
    // Stub for the future
    return "\$type"
}

internal fun <T> JsonInput.decodeSerializableValuePolymorphic(deserializer: DeserializationStrategy<T>): T {
    if (deserializer !is PolymorphicSerializer<*>) {
        return deserializer.deserialize(this)
    }

    val jsonTree = cast<JsonObject>(decodeJson())
    val type = jsonTree[getTypeNameProperty(json)].content
    (jsonTree.content as MutableMap).remove("\$type")
    @Suppress("UNCHECKED_CAST")
    val actualSerializer = deserializer.findPolymorphicSerializer(this, type) as KSerializer<T>
    return json.readJson(jsonTree, actualSerializer)
}