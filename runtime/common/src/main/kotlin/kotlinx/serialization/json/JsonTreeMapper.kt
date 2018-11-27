/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*

@Deprecated(level = DeprecationLevel.WARNING, message = "Use Json methods instead")
class JsonTreeMapper(val encodeDefaults: Boolean = true) : AbstractSerialFormat() {

    @ImplicitReflectionSerializer
    @Deprecated(level = DeprecationLevel.WARNING, message = "Use Json.fromJson instead", replaceWith = ReplaceWith("Json.plain.fromJson(tree)"))
    inline fun <reified T : Any> readTree(tree: JsonElement): T = Json.plain.fromJson(tree)

    @Deprecated(level = DeprecationLevel.WARNING, message = "Use Json.fromJson instead", replaceWith = ReplaceWith("Json.plain.fromJson(tree, deserializer)"))
    fun <T> readTree(obj: JsonElement, deserializer: DeserializationStrategy<T>): T =
        Json.plain.fromJson(obj, deserializer)

    @Deprecated(level = DeprecationLevel.WARNING, message = "Use Json.toJson instead", replaceWith = ReplaceWith("Json.plain.toJson(obj, serializer)"))
    fun <T> writeTree(obj: T, serializer: SerializationStrategy<T>): JsonElement = Json.plain.toJson(obj, serializer)
}
