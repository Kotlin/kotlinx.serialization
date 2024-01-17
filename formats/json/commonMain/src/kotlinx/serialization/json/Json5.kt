/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.json.internal.AbstractJsonLexer
import kotlinx.serialization.json.internal.lexer.*
import kotlinx.serialization.modules.*

public class Json5 internal constructor(configuration: JsonConfiguration, module: SerializersModule) :
    Json(configuration, module) {
    override fun createLexer(input: String): AbstractJsonLexer {
        return Json5Lexer(input)
    }
    public companion object Default : Json(defaultJson5Config, EmptySerializersModule())
}

private val defaultJson5Config: JsonConfiguration =
    JsonConfiguration(allowSpecialFloatingPointValues = true, allowTrailingComma = true)

public fun Json5(from: Json = Json5.Default, builderAction: Json5Builder.() -> Unit): Json {
    val builder = Json5Builder(from)
    builder.builderAction()
    val conf = builder.build()
    return Json5(conf, builder.serializersModule)
}

public class Json5Builder internal constructor(json: Json) :
    JsonBuilderBase(json.configuration, json.serializersModule) {
    // TODO: config
    override fun build(): JsonConfiguration {
        validateBase()
        return JsonConfiguration(
            encodeDefaults, ignoreUnknownKeys, false,
            allowStructuredMapKeys, prettyPrint, explicitNulls, prettyPrintIndent,
            coerceInputValues, useArrayPolymorphism,
            classDiscriminator, true, useAlternativeNames,
            namingStrategy, decodeEnumsCaseInsensitive, true, classDiscriminatorMode
        )
    }
}
