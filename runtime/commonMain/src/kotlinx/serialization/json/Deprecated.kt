/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:UseExperimental(UnstableDefault::class)

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.modules.EmptyModule

@Deprecated(deprecationText, ReplaceWith("Json"), DeprecationLevel.WARNING)
typealias JSON = Json

@Deprecated(level = DeprecationLevel.WARNING, message = "Use Json methods instead")
class JsonTreeMapper(val encodeDefaults: Boolean = true) : AbstractSerialFormat(EmptyModule) {

    @ImplicitReflectionSerializer
    @Deprecated(level = DeprecationLevel.WARNING, message = "Use Json.fromJson instead", replaceWith = ReplaceWith("Json.plain.fromJson(tree)"))
    inline fun <reified T : Any> readTree(tree: JsonElement): T = Json.plain.fromJson(tree)

    @Deprecated(level = DeprecationLevel.WARNING, message = "Use Json.fromJson instead", replaceWith = ReplaceWith("Json.plain.fromJson(obj, deserializer)"))
    fun <T> readTree(obj: JsonElement, deserializer: DeserializationStrategy<T>): T =
        Json.plain.fromJson(deserializer, obj)

    @Deprecated(level = DeprecationLevel.WARNING, message = "Use Json.toJson instead", replaceWith = ReplaceWith("Json.plain.toJson(obj, serializer)"))
    fun <T> writeTree(obj: T, serializer: SerializationStrategy<T>): JsonElement = Json.plain.toJson(serializer, obj)
}

@Deprecated(level = DeprecationLevel.WARNING, message = "Use Json methods instead")
class JsonTreeParser(private val input: String) {

    companion object {
        @Deprecated(level = DeprecationLevel.WARNING, message = "Use Json.parse instead", replaceWith = ReplaceWith("Json.plain.parseJson(input)"))
        fun parse(input: String): JsonObject = Json.plain.parseJson(input) as JsonObject
    }

    fun readFully(): JsonElement {
        @Suppress("DEPRECATION")
        return Json.plain.parseJson(input)
    }
}

@Deprecated("Replaced with JsonEncodingException", ReplaceWith("JsonEncodingException"), DeprecationLevel.ERROR)
typealias JsonInvalidValueInStrictModeException = JsonEncodingException

@Deprecated("Merged with JsonDecodingException", ReplaceWith("JsonDecodingException"), DeprecationLevel.ERROR)
typealias JsonUnknownKeyException = JsonDecodingException

@Deprecated("Replaced with JsonDecodingException", ReplaceWith("JsonDecodingException"), DeprecationLevel.ERROR)
typealias JsonParsingException = JsonDecodingException

@Deprecated("Merged with JsonException", ReplaceWith("JsonException"), DeprecationLevel.ERROR)
typealias JsonElementTypeMismatchException = JsonException
