package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json

/** @suppress */
@PublishedApi
internal interface JsonWriter {
    fun writeLong(value: Long)
    fun writeChar(char: Char)
    fun write(text: String)
    fun writeQuoted(text: String)
    fun release()
}

/** @suppress */
@PublishedApi
internal interface SerialReader {
    fun read(buffer: CharArray, bufferOffset: Int, count: Int): Int
}

/** @suppress */
@PublishedApi
internal fun <T> Json.encodeByWriter(writer: JsonWriter, serializer: SerializationStrategy<T>, value: T) {
    val encoder = StreamingJsonEncoder(
        writer, this,
        WriteMode.OBJ,
        arrayOfNulls(WriteMode.values().size)
    )
    encoder.encodeSerializableValue(serializer, value)
}

/** @suppress */
@PublishedApi
internal fun <T> Json.decodeByReader(
    deserializer: DeserializationStrategy<T>,
    reader: SerialReader
): T {
    val lexer = ReaderJsonLexer(reader)
    val input = StreamingJsonDecoder(this, WriteMode.OBJ, lexer, deserializer.descriptor, null)
    val result = input.decodeSerializableValue(deserializer)
    lexer.expectEof()
    return result
}

/** @suppress */
@PublishedApi
@ExperimentalSerializationApi
internal fun <T> Json.decodeToSequenceByReader(
    reader: SerialReader,
    deserializer: DeserializationStrategy<T>,
    format: DecodeSequenceMode = DecodeSequenceMode.AUTO_DETECT
): Sequence<T> {
    val lexer = ReaderJsonLexer(reader)
    val iter = JsonIterator(format, this, lexer, deserializer)
    return Sequence { iter }.constrainOnce()
}

/** @suppress */
@PublishedApi
@ExperimentalSerializationApi
internal inline fun <reified T> Json.decodeToSequenceByReader(
    reader: SerialReader,
    format: DecodeSequenceMode = DecodeSequenceMode.AUTO_DETECT
): Sequence<T> = decodeToSequenceByReader(reader, serializersModule.serializer(), format)
