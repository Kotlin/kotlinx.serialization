package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json



@InternalSerializationApi
public interface JsonWriter {
    public fun writeLong(value: Long)
    public fun writeChar(char: Char)

    public fun write(text: String)

    public fun writeQuoted(text: String)

    public fun release()
}

@InternalSerializationApi
public interface SerialReader {
    public fun read(buffer: CharArray, bufferOffset: Int, count: Int): Int
}

@InternalSerializationApi
public fun <T> Json.encodeByWriter(writer: JsonWriter, serializer: SerializationStrategy<T>, value: T) {
    val encoder = StreamingJsonEncoder(
        writer, this,
        WriteMode.OBJ,
        arrayOfNulls(WriteMode.values().size)
    )
    encoder.encodeSerializableValue(serializer, value)
}

@InternalSerializationApi
public fun <T> Json.decodeByReader(
    deserializer: DeserializationStrategy<T>,
    reader: SerialReader
): T {
    val lexer = ReaderJsonLexer(reader)
    val input = StreamingJsonDecoder(this, WriteMode.OBJ, lexer, deserializer.descriptor, null)
    val result = input.decodeSerializableValue(deserializer)
    lexer.expectEof()
    return result
}

@InternalSerializationApi
@ExperimentalSerializationApi
public fun <T> Json.decodeToSequenceByReader(
    reader: SerialReader,
    deserializer: DeserializationStrategy<T>,
    format: DecodeSequenceMode = DecodeSequenceMode.AUTO_DETECT
): Sequence<T> {
    val lexer = ReaderJsonLexer(reader)
    val iter = JsonIterator(format, this, lexer, deserializer)
    return Sequence { iter }.constrainOnce()
}

@InternalSerializationApi
@ExperimentalSerializationApi
public inline fun <reified T> Json.decodeToSequenceByReader(
    reader: SerialReader,
    format: DecodeSequenceMode = DecodeSequenceMode.AUTO_DETECT
): Sequence<T> = decodeToSequenceByReader(reader, serializersModule.serializer(), format)
