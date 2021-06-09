/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.json.internal.*
import java.io.*
import java.nio.charset.Charset

@Suppress("DEPRECATION_ERROR")
internal actual val Json.schemaCache: DescriptorSchemaCache get() = this._schemaCache

public fun <T> Json.decodeFromStream(deserializer: DeserializationStrategy<T>, stream: InputStream, charset: Charset = Charsets.UTF_8): T {
    val lexer = JsonReaderLexer(stream, charset)
    val input = StreamingJsonDecoder(this, WriteMode.OBJ, lexer)
    val result = input.decodeSerializableValue(deserializer)
    lexer.expectEof()
    return result
}

public fun <T> Json.encodeToStream(serializer: SerializationStrategy<T>, value: T, stream: OutputStream, charset: Charset = Charsets.UTF_8) {
    val result = JsonToWriterStringBuilder(stream, charset)
    try {
        val encoder = StreamingJsonEncoder(
            result, this,
            WriteMode.OBJ,
            arrayOfNulls(WriteMode.values().size)
        )
        encoder.encodeSerializableValue(serializer, value)
    } finally {
        result.release()
    }
}
