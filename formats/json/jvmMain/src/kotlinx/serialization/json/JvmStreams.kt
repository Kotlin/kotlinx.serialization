package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.json.internal.*
import java.io.*

/**
 * Serializes the [value] with [serializer] into a [stream] using JSON format and UTF-8 encoding..
 *
 * @throws [SerializationException] if the given value cannot be serialized to JSON.
 * @throws [IOException] If an I/O error occurs and stream can't be written to.
 */
@ExperimentalSerializationApi
public fun <T> Json.encodeToStream(
    serializer: SerializationStrategy<T>,
    value: T,
    stream: OutputStream
) {
    val result = JsonToWriterStringBuilder(stream)
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
 * Serializes given [value] to [stream] using UTF-8 encoding and serializer retrieved from the reified type parameter.
 *
 * @throws [SerializationException] if the given value cannot be serialized to JSON.
 * @throws [IOException] If an I/O error occurs and stream can't be written to.
 */
@ExperimentalSerializationApi
public inline fun <reified T> Json.encodeToStream(
    value: T,
    stream: OutputStream
): Unit =
    encodeToStream(serializersModule.serializer(), value, stream)

/**
 * Deserializes JSON from [stream] using UTF-8 encoding to a value of type [T] using [deserializer].
 *
 * @throws [SerializationException] if the given JSON input cannot be deserialized to the value of type [T].
 * @throws [IOException] If an I/O error occurs and stream can't be read from.
 */
@ExperimentalSerializationApi
public fun <T> Json.decodeFromStream(
    deserializer: DeserializationStrategy<T>,
    stream: InputStream
): T {
    val lexer = ReaderJsonLexer(stream)
    val input = StreamingJsonDecoder(this, WriteMode.OBJ, lexer, deserializer.descriptor)
    return input.decodeSerializableValue(deserializer)
}

/**
 * Deserializes the contents of given [stream] to to the value of type [T] using UTF-8 encoding and
 * deserializer retrieved from the reified type parameter.
 *
 * @throws [SerializationException] if the given JSON input cannot be deserialized to the value of type [T].
 * @throws [IOException] If an I/O error occurs and stream can't be read from.
 */
@ExperimentalSerializationApi
public inline fun <reified T> Json.decodeFromStream(stream: InputStream): T =
    decodeFromStream(serializersModule.serializer(), stream)
