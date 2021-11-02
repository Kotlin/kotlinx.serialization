/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.json.internal.*
import java.io.*

/**
 * Serializes the [value] with [serializer] into a [stream] using JSON format and UTF-8 encoding.
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
 * Note that this functions expects that exactly one object would be present in the stream
 * and throws an exception if there are any dangling bytes after an object.
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
    val result = input.decodeSerializableValue(deserializer)
    lexer.expectEof()
    return result
}

/**
 * Deserializes the contents of given [stream] to the value of type [T] using UTF-8 encoding and
 * deserializer retrieved from the reified type parameter.
 *
 * Note that this functions expects that exactly one object would be present in the stream
 * and throws an exception if there are any dangling bytes after an object.
 *
 * @throws [SerializationException] if the given JSON input cannot be deserialized to the value of type [T].
 * @throws [IOException] If an I/O error occurs and stream can't be read from.
 */
@ExperimentalSerializationApi
public inline fun <reified T> Json.decodeFromStream(stream: InputStream): T =
    decodeFromStream(serializersModule.serializer(), stream)

/**
 * Description of [decodeToSequence]'s JSON input shape.
 *
 * The sequence represents a stream of objects parsed one by one;
 * [LazyStreamingFormat] defines a separator between these objects.
 * Normally, these objects are not separated by meaningful characters ([WHITESPACE_SEPARATED])
 * or the whole stream is a large array of objects separated with commas ([ARRAY_WRAPPED]).
 */
@ExperimentalSerializationApi
public enum class LazyStreamingFormat {
    /**
     * Declares that objects in the input stream are separated by whitespace characters.
     *
     * Stream must start with a first object and there are some (maybe none) whitespace chars between objects.
     * Whitespace character is either ' ', '\n', '\r' or '\t'.
     */
    WHITESPACE_SEPARATED,

    /**
     * Declares that objects in the input stream are wrapped in the JSON array. Elements of the array are still parsed lazily,
     * so there shouldn't be problems if the array total size is larger than available memory.
     *
     * Stream must start with json array start character and objects in it must be separated with commas and optional whitespaces.
     * Stream must end with array end character, otherwise, [JsonDecodingException] would be thrown.
     * Dangling chars after array end are not permitted.
     */
    ARRAY_WRAPPED,

    /**
     * Declares that parser itself should select between [WHITESPACE_SEPARATED] and [ARRAY_WRAPPED] modes.
     * Selection is performed by looking on the first meaningful character of the stream.
     *
     * In most cases, auto-detection is sufficient to correctly parse an input.
     * However, in the input is _whitespace-separated stream of the arrays_, parser would select incorrect mode.
     * For such cases, [LazyStreamingFormat] must be specified explicitly.
     *
     * Example of exceptional case:
     * `[1, 2, 3]   [4, 5, 6]\n[7, 8, 9]`
     */
    AUTO_DETECT;
}

/**
 * Transforms the given [stream] into lazily deserialized sequence of elements of type [T] using UTF-8 encoding and [deserializer].
 * Unlike [decodeFromStream], [stream] is allowed to have more than one element, separated as [format] declares.
 *
 * Elements must all be of type [T].
 * Elements are parsed lazily when resulting [Sequence] is evaluated.
 * Resulting sequence is tied to the stream and constrained to be evaluated only once.
 *
 * **Resource caution:** this method does not close [stream] when the parsing is finished neither provides method to close it manually.
 * It is a caller responsibility to hold a reference to a stream and close it. Moreover, because stream is parsed lazily,
 * closing it before returned sequence is evaluated fully would result in [IOException] from decoder.
 *
 * @throws [SerializationException] if the given JSON input cannot be deserialized to the value of type [T].
 * @throws [IOException] If an I/O error occurs and stream can't be read from.
 */
@ExperimentalSerializationApi
public fun <T> Json.decodeToSequence(
    stream: InputStream,
    deserializer: DeserializationStrategy<T>,
    format: LazyStreamingFormat = LazyStreamingFormat.AUTO_DETECT
): Sequence<T> {
    val lexer = ReaderJsonLexer(stream)
    val iter = JsonIterator(format, this, lexer, deserializer)
    return Sequence { iter }.constrainOnce()
}

/**
 * Transforms the given [stream] into lazily deserialized sequence of elements of type [T] using UTF-8 encoding and deserializer retrieved from the reified type parameter.
 * Unlike [decodeFromStream], [stream] is allowed to have more than one element, separated as [format] declares.
 *
 * Elements must all be of type [T].
 * Elements are parsed lazily when resulting [Sequence] is evaluated.
 * Resulting sequence is tied to the stream and constrained to be evaluated only once.
 *
 * **Resource caution:** this method does not close [stream] when the parsing is finished neither provides method to close it manually.
 * It is a caller responsibility to hold a reference to a stream and close it. Moreover, because stream is parsed lazily,
 * closing it before returned sequence is evaluated fully would result in [IOException] from decoder.
 *
 * @throws [SerializationException] if the given JSON input cannot be deserialized to the value of type [T].
 * @throws [IOException] If an I/O error occurs and stream can't be read from.
 */
@ExperimentalSerializationApi
public inline fun <reified T> Json.decodeToSequence(
    stream: InputStream,
    format: LazyStreamingFormat = LazyStreamingFormat.AUTO_DETECT
): Sequence<T> = decodeToSequence(stream, serializersModule.serializer(), format)

