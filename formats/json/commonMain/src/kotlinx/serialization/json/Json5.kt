/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.json.internal.*
import kotlinx.serialization.json.internal.AbstractJsonLexer
import kotlinx.serialization.json.internal.lexer.*
import kotlinx.serialization.modules.*

public class Json5(configuration: JsonConfiguration, module: SerializersModule): Json(configuration, module) {
    override fun createLexer(input: String): AbstractJsonLexer {
        return Json5Lexer(input)
    }
}
