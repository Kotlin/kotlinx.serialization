package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.json.internal.*
import java.io.*
import java.nio.charset.Charset

/**
 * Serializes the [value] with [serializer] into a [stream] using JSON format and given [charset].
 *
 * @throws [SerializationException] if the given value cannot be serialized to JSON.
 * @throws [IOException] If an I/O error occurs and stream can't be written to.
 */
@ExperimentalSerializationApi
public fun <T> Json.encodeToStream(
    serializer: SerializationStrategy<T>,
    value: T,
    stream: OutputStream,
    charset: Charset = Charsets.UTF_8
) {
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

/**
 * Serializes given [value] to [stream] using [charset] and serializer retrieved from the reified type parameter.
 *
 * @throws [SerializationException] if the given value cannot be serialized to JSON.
 * @throws [IOException] If an I/O error occurs and stream can't be written to.
 */
@ExperimentalSerializationApi
public inline fun <reified T> Json.encodeToStream(
    value: T,
    stream: OutputStream,
    charset: Charset = Charsets.UTF_8
): Unit =
    encodeToStream(serializersModule.serializer(), value, stream, charset)

/**
 * Deserializes JSON from [stream] using [charset] to a value of type [T] using [deserializer].
 *
 * @throws [SerializationException] if the given JSON input cannot be deserialized to the value of type [T].
 * @throws [IOException] If an I/O error occurs and stream can't be read from.
 */
@ExperimentalSerializationApi
public fun <T> Json.decodeFromStream(
    deserializer: DeserializationStrategy<T>,
    stream: InputStream,
    charset: Charset = Charsets.UTF_8
): T {
    val lexer = ReaderJsonLexer(stream, charset)
    val input = StreamingJsonDecoder(this, WriteMode.OBJ, lexer)
    return input.decodeSerializableValue(deserializer)
}

/**
 * Deserializes the contents of given [stream] to to the value of type [T] using [charset] and
 * deserializer retrieved from the reified type parameter.
 *
 * @throws [SerializationException] if the given JSON input cannot be deserialized to the value of type [T].
 * @throws [IOException] If an I/O error occurs and stream can't be read from.
 */
@ExperimentalSerializationApi
public inline fun <reified T> Json.decodeFromStream(stream: InputStream, charset: Charset = Charsets.UTF_8): T =
    decodeFromStream(serializersModule.serializer(), stream, charset)
