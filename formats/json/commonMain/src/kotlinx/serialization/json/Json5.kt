/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.json.internal.AbstractJsonLexer
import kotlinx.serialization.json.internal.lexer.*
import kotlinx.serialization.modules.*

public class Json5(configuration: JsonConfiguration, module: SerializersModule): Json(configuration, module) {
    override fun createLexer(input: String): AbstractJsonLexer {
        return Json5Lexer(input)
    }

    public companion object {
        private val conf = Json {
            isLenient = false
            allowTrailingComma = true
            allowSpecialFloatingPointValues = true
            ignoreUnknownKeys = false
            encodeDefaults = false
        }.configuration

        public val Default: Json5 = Json5(conf, EmptySerializersModule())
    }
}
