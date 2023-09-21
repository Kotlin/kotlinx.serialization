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
 * @throws [IOException] If an I/O error occurs and stream cannot be written to.
 */
@ExperimentalSerializationApi
public fun <T> Json.encodeToStream(
    serializer: SerializationStrategy<T>,
    value: T,
    stream: OutputStream
) {
    val writer = JsonToJavaStreamWriter(stream)
    try {
        encodeByWriter(this, writer, serializer, value)
    } finally {
        writer.release()
    }
}

/**
 * Serializes given [value] to [stream] using UTF-8 encoding and serializer retrieved from the reified type parameter.
 *
 * @throws [SerializationException] if the given value cannot be serialized to JSON.
 * @throws [IOException] If an I/O error occurs and stream cannot be written to.
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
 * @throws [IllegalArgumentException] if the decoded input cannot be represented as a valid instance of type [T]
 * @throws [IOException] If an I/O error occurs and stream cannot be read from.
 */
@ExperimentalSerializationApi
public fun <T> Json.decodeFromStream(
    deserializer: DeserializationStrategy<T>,
    stream: InputStream
): T {
    val reader = JavaStreamSerialReader(stream)
    try {
        return decodeByReader(this, deserializer, reader)
    } finally {
        reader.release()
    }
}

/**
 * Deserializes the contents of given [stream] to the value of type [T] using UTF-8 encoding and
 * deserializer retrieved from the reified type parameter.
 *
 * Note that this functions expects that exactly one object would be present in the stream
 * and throws an exception if there are any dangling bytes after an object.
 *
 * @throws [SerializationException] if the given JSON input cannot be deserialized to the value of type [T].
 * @throws [IllegalArgumentException] if the decoded input cannot be represented as a valid instance of type [T]
 * @throws [IOException] If an I/O error occurs and stream cannot be read from.
 */
@ExperimentalSerializationApi
public inline fun <reified T> Json.decodeFromStream(stream: InputStream): T =
    decodeFromStream(serializersModule.serializer(), stream)


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
 * @throws [IllegalArgumentException] if the decoded input cannot be represented as a valid instance of type [T]
 * @throws [IOException] If an I/O error occurs and stream cannot be read from.
 */
@ExperimentalSerializationApi
public fun <T> Json.decodeToSequence(
    stream: InputStream,
    deserializer: DeserializationStrategy<T>,
    format: DecodeSequenceMode = DecodeSequenceMode.AUTO_DETECT
): Sequence<T> {
    return decodeToSequenceByReader(this, JavaStreamSerialReader(stream), deserializer, format)
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
 * @throws [IllegalArgumentException] if the decoded input cannot be represented as a valid instance of type [T]
 * @throws [IOException] If an I/O error occurs and stream cannot be read from.
 */
@ExperimentalSerializationApi
public inline fun <reified T> Json.decodeToSequence(
    stream: InputStream,
    format: DecodeSequenceMode = DecodeSequenceMode.AUTO_DETECT
): Sequence<T> = decodeToSequence(stream, serializersModule.serializer(), format)
