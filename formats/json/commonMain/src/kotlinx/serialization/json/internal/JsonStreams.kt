package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json

@PublishedApi
internal interface JsonWriter {
    fun writeLong(value: Long)
    fun writeChar(char: Char)
    fun write(text: String)
    fun writeQuoted(text: String)
    fun release()
}

@PublishedApi
internal interface SerialReader {
    fun read(buffer: CharArray, bufferOffset: Int, count: Int): Int
}

@PublishedApi
internal fun <T> Json.encodeByWriter(writer: JsonWriter, serializer: SerializationStrategy<T>, value: T) {
    val encoder = StreamingJsonEncoder(
        writer, this,
        WriteMode.OBJ,
        arrayOfNulls(WriteMode.values().size)
    )
    encoder.encodeSerializableValue(serializer, value)
}

@PublishedApi
internal fun <T> Json.decodeByReader(
    deserializer: DeserializationStrategy<T>,
    reader: SerialReader
): T {
    val lexer = ReaderJsonLexer(reader)
    try {
        val input = StreamingJsonDecoder(this, WriteMode.OBJ, lexer, deserializer.descriptor, null)
        val result = input.decodeSerializableValue(deserializer)
        lexer.expectEof()
        return result
    } finally {
        lexer.release()
    }
}

@PublishedApi
@ExperimentalSerializationApi
internal fun <T> Json.decodeToSequenceByReader(
    reader: SerialReader,
    deserializer: DeserializationStrategy<T>,
    format: DecodeSequenceMode = DecodeSequenceMode.AUTO_DETECT
): Sequence<T> {
    val lexer = ReaderJsonLexer(reader, CharArray(BATCH_SIZE)) // Unpooled buffer due to lazy nature of sequence
    val iter = JsonIterator(format, this, lexer, deserializer)
    return Sequence { iter }.constrainOnce()
}

@PublishedApi
@ExperimentalSerializationApi
internal inline fun <reified T> Json.decodeToSequenceByReader(
    reader: SerialReader,
    format: DecodeSequenceMode = DecodeSequenceMode.AUTO_DETECT
): Sequence<T> = decodeToSequenceByReader(reader, serializersModule.serializer(), format)
