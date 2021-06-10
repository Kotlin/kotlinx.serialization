/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.json.internal.*
import java.io.*

@Suppress("DEPRECATION_ERROR")
internal actual val Json.schemaCache: DescriptorSchemaCache get() = this._schemaCache

public fun <T> Json.decodeFromStream(deserializer: DeserializationStrategy<T>, stream: InputStream): T {
    val lexer = JsonLexerJvm(stream)
    val input = StreamingJsonDecoder(this, WriteMode.OBJ, lexer)
    val result = input.decodeSerializableValue(deserializer)
    lexer.expectEof()
    return result
}
