package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
internal annotation class JsonFriendModuleApi

@JsonFriendModuleApi
public interface InternalJsonWriter {
    public fun writeLong(value: Long)
    public fun writeChar(char: Char)
    public fun write(text: String)
    public fun writeQuoted(text: String)
    public fun release()

    public companion object {
        public inline fun doWriteEscaping(text: String, writeImpl: (text: String, startIndex: Int, endIndex: Int) -> Unit) {
            var lastPos = 0
            for (i in text.indices) {
                val c = text[i].code
                if (c < ESCAPE_STRINGS.size && ESCAPE_STRINGS[c] != null) {
                    writeImpl(text, lastPos, i) // flush prev
                    val escape = ESCAPE_STRINGS[c]!!
                    writeImpl(escape, 0, escape.length)
                    lastPos = i + 1
                }
            }
            writeImpl(text, lastPos, text.length)
        }
    }
}

@JsonFriendModuleApi
public interface InternalJsonReader {
    public fun read(buffer: CharArray, bufferOffset: Int, count: Int): Int
}

@JsonFriendModuleApi
public fun <T> encodeByWriter(json: Json, writer: InternalJsonWriter, serializer: SerializationStrategy<T>, value: T) {
    val encoder = StreamingJsonEncoder(
        writer, json,
        WriteMode.OBJ,
        arrayOfNulls(WriteMode.entries.size)
    )
    encoder.encodeSerializableValue(serializer, value)
}

@JsonFriendModuleApi
public fun <T> decodeByReader(
    json: Json,
    deserializer: DeserializationStrategy<T>,
    reader: InternalJsonReader
): T {
    val lexer = ReaderJsonLexer(json, reader)
    try {
        val input = StreamingJsonDecoder(json, WriteMode.OBJ, lexer, deserializer.descriptor, null)
        val result = input.decodeSerializableValue(deserializer)
        lexer.expectEof()
        return result
    } finally {
        lexer.release()
    }
}

@JsonFriendModuleApi
@ExperimentalSerializationApi
public fun <T> decodeToSequenceByReader(
    json: Json,
    reader: InternalJsonReader,
    deserializer: DeserializationStrategy<T>,
    format: DecodeSequenceMode = DecodeSequenceMode.AUTO_DETECT
): Sequence<T> {
    val lexer = ReaderJsonLexer(json, reader, CharArray(BATCH_SIZE)) // Unpooled buffer due to lazy nature of sequence
    val iter = JsonIterator(format, json, lexer, deserializer)
    return Sequence { iter }.constrainOnce()
}

@JsonFriendModuleApi
@ExperimentalSerializationApi
public inline fun <reified T> decodeToSequenceByReader(
    json: Json,
    reader: InternalJsonReader,
    format: DecodeSequenceMode = DecodeSequenceMode.AUTO_DETECT
): Sequence<T> = decodeToSequenceByReader(json, reader, json.serializersModule.serializer(), format)
