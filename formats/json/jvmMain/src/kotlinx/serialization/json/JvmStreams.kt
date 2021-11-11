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
 * [DecodeSequenceMode] defines a separator between these objects.
 * Typically, these objects are not separated by meaningful characters ([WHITESPACE_SEPARATED]),
 * or the whole stream is a large array of objects separated with commas ([ARRAY_WRAPPED]).
 */
@ExperimentalSerializationApi
public enum class DecodeSequenceMode {
    /**
     * Declares that objects in the input stream are separated by whitespace characters.
     *
     * The stream is read as multiple JSON objects separated by any number of whitespace characters between objects. Starting and trailing whitespace characters are also permitted.
     * Each individual object is parsed lazily, when it is requested from the resulting sequence.
     *
     * Whitespace character is either ' ', '\n', '\r' or '\t'.
     *
     * Example of `WHITESPACE_SEPARATED` stream content:
     * ```
     * """{"key": "value"}{"key": "value2"}   {"key2": "value2"}"""
     * ```
     */
    WHITESPACE_SEPARATED,

    /**
     * Declares that objects in the input stream are wrapped in the JSON array.
     * Each individual object in the array is parsed lazily when it is requested from the resulting sequence.
     *
     * The stream is read as multiple JSON objects wrapped into a JSON array.
     * The stream must start with an array start character `[` and end with an array end character `]`,
     * otherwise, [JsonDecodingException] is thrown.
     *
     * Example of `ARRAY_WRAPPED` stream content:
     * ```
     * """[{"key": "value"}, {"key": "value2"},{"key2": "value2"}]"""
     * ```
     */
    ARRAY_WRAPPED,

    /**
     * Declares that parser itself should select between [WHITESPACE_SEPARATED] and [ARRAY_WRAPPED] modes.
     * The selection is performed by looking on the first meaningful character of the stream.
     *
     * In most cases, auto-detection is sufficient to correctly parse an input.
     * If the input is _whitespace-separated stream of the arrays_, parser could select an incorrect mode,
     * for that [DecodeSequenceMode] must be specified explicitly.
     *
     * Example of an exceptional case:
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
 * Resulting sequence is tied to the stream and can be evaluated only once.
 *
 * **Resource caution:** this method neither closes the [stream] when the parsing is finished nor provides a method to close it manually.
 * It is a caller responsibility to hold a reference to a stream and close it. Moreover, because stream is parsed lazily,
 * closing it before returned sequence is evaluated completely will result in [IOException] from decoder.
 *
 * @throws [SerializationException] if the given JSON input cannot be deserialized to the value of type [T].
 * @throws [IOException] If an I/O error occurs and stream can't be read from.
 */
@ExperimentalSerializationApi
public fun <T> Json.decodeToSequence(
    stream: InputStream,
    deserializer: DeserializationStrategy<T>,
    format: DecodeSequenceMode = DecodeSequenceMode.AUTO_DETECT
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
    format: DecodeSequenceMode = DecodeSequenceMode.AUTO_DETECT
): Sequence<T> = decodeToSequence(stream, serializersModule.serializer(), format)

