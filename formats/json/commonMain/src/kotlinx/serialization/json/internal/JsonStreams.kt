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

// Max value for a code  point placed in one Char
private const val SINGLE_CHAR_MAX_CODEPOINT = Char.MAX_VALUE.code
// Value added to the high UTF-16 surrogate after shifting
private const val HIGH_SURROGATE_HEADER = 0xd800 - (0x010000 ushr 10)
// Value added to the low UTF-16 surrogate after masking
private const val LOW_SURROGATE_HEADER = 0xdc00

@JsonFriendModuleApi
public abstract class InternalJsonReaderCodePointImpl: InternalJsonReader {
    public abstract fun exhausted(): Boolean
    public abstract fun nextCodePoint(): Int

    private var bufferedChar: Char? = null

    final override fun read(buffer: CharArray, bufferOffset: Int, count: Int): Int {
        var i = 0

        if (bufferedChar != null) {
            buffer[bufferOffset + i] = bufferedChar!!
            i++
            bufferedChar = null
        }

        while (i < count && !exhausted()) {
            val codePoint = nextCodePoint()
            if (codePoint <= SINGLE_CHAR_MAX_CODEPOINT) {
                buffer[bufferOffset + i] = codePoint.toChar()
                i++
            } else {
                // an example of working with surrogates is taken from okio library with minor changes, see https://github.com/square/okio
                // UTF-16 high surrogate: 110110xxxxxxxxxx (10 bits)
                // UTF-16 low surrogate:  110111yyyyyyyyyy (10 bits)
                // Unicode code point:    00010000000000000000 + xxxxxxxxxxyyyyyyyyyy (21 bits)
                val upChar = ((codePoint ushr 10) + HIGH_SURROGATE_HEADER).toChar()
                val lowChar = ((codePoint and 0x03ff) + LOW_SURROGATE_HEADER).toChar()

                buffer[bufferOffset + i] = upChar
                i++

                if (i < count) {
                    buffer[bufferOffset + i] = lowChar
                    i++
                } else {
                    // if char array is full - buffer lower surrogate
                    bufferedChar = lowChar
                }
            }
        }
        return if (i > 0) i else -1
    }
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
