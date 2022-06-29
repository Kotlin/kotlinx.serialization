/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.okio

import kotlinx.serialization.*
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.json.okio.internal.JsonToOkioStreamWriter
import kotlinx.serialization.json.internal.decodeToSequenceByReader
import kotlinx.serialization.json.okio.internal.OkioSerialReader
import okio.*

/**
 * Serializes the [value] with [serializer] into a [target] using JSON format and UTF-8 encoding.
 *
 * If [target] is not a [BufferedSink] then called [Sink.buffer] for it to create buffered wrapper.
 *
 * @throws [SerializationException] if the given value cannot be serialized to JSON.
 * @throws [okio.IOException] If an I/O error occurs and sink can't be written to.
 */
@ExperimentalSerializationApi
public fun <T> Json.encodeToSink(
    serializer: SerializationStrategy<T>,
    value: T,
    target: Sink
) {
    val buffered = if (target is BufferedSink) target else target.buffer()
    val writer = JsonToOkioStreamWriter(buffered)
    try {
        encodeByWriter(writer, serializer, value)
    } finally {
        writer.release()
    }
}

/**
 * Serializes given [value] to a [target] using UTF-8 encoding and serializer retrieved from the reified type parameter.
 *
 * If [target] is not a [BufferedSink] then called [Sink.buffer] for it to create buffered wrapper.
 *
 * @throws [SerializationException] if the given value cannot be serialized to JSON.
 * @throws [okio.IOException] If an I/O error occurs and sink can't be written to.
 */
@ExperimentalSerializationApi
public inline fun <reified T> Json.encodeToSink(
    value: T,
    target: Sink
): Unit = encodeToSink(serializersModule.serializer(), value, target)


/**
 * Deserializes JSON from [source] using UTF-8 encoding to a value of type [T] using [deserializer].
 *
 * If [source] is not a [BufferedSource] then called [Source.buffer] for it to create buffered wrapper.
 *
 * Note that this functions expects that exactly one object would be present in the source
 * and throws an exception if there are any dangling bytes after an object.
 *
 * @throws [SerializationException] if the given JSON input cannot be deserialized to the value of type [T].
 * @throws [okio.IOException] If an I/O error occurs and source can't be read from.
 */
@ExperimentalSerializationApi
public fun <T> Json.decodeFromSource(
    deserializer: DeserializationStrategy<T>,
    source: Source
): T {
    val buffered = if (source is BufferedSource) source else source.buffer()
    return decodeByReader(deserializer, OkioSerialReader(buffered))
}

/**
 * Deserializes the contents of given [source] to the value of type [T] using UTF-8 encoding and
 * deserializer retrieved from the reified type parameter.
 *
 * If [source] is not a [BufferedSource] then called [Source.buffer] for it to create buffered wrapper.
 *
 * Note that this functions expects that exactly one object would be present in the stream
 * and throws an exception if there are any dangling bytes after an object.
 *
 * @throws [SerializationException] if the given JSON input cannot be deserialized to the value of type [T].
 * @throws [okio.IOException] If an I/O error occurs and source can't be read from.
 */
@ExperimentalSerializationApi
public inline fun <reified T> Json.decodeFromSource(source: Source): T =
    decodeFromSource(serializersModule.serializer(), source)


/**
 * Transforms the given [source] into lazily deserialized sequence of elements of type [T] using UTF-8 encoding and [deserializer].
 * Unlike [decodeFromSource], [source] is allowed to have more than one element, separated as [format] declares.
 *
 * If [source] is not a [BufferedSource] then called [Source.buffer] for it to create buffered wrapper.
 *
 * Elements must all be of type [T].
 * Elements are parsed lazily when resulting [Sequence] is evaluated.
 * Resulting sequence is tied to the stream and can be evaluated only once.
 *
 * **Resource caution:** this method neither closes the [source] when the parsing is finished nor provides a method to close it manually.
 * It is a caller responsibility to hold a reference to a stream and close it. Moreover, because stream is parsed lazily,
 * closing it before returned sequence is evaluated completely will result in [Exception] from decoder.
 *
 * @throws [SerializationException] if the given JSON input cannot be deserialized to the value of type [T].
 * @throws [okio.IOException] If an I/O error occurs and source can't be read from.
 */
@ExperimentalSerializationApi
public fun <T> Json.decodeSourceToSequence(
    source: Source,
    deserializer: DeserializationStrategy<T>,
    format: DecodeSequenceMode = DecodeSequenceMode.AUTO_DETECT
): Sequence<T> {
    val buffered = if (source is BufferedSource) source else source.buffer()
    return decodeToSequenceByReader(OkioSerialReader(buffered), deserializer, format)
}

/**
 * Transforms the given [source] into lazily deserialized sequence of elements of type [T] using UTF-8 encoding and deserializer retrieved from the reified type parameter.
 * Unlike [decodeFromSource], [source] is allowed to have more than one element, separated as [format] declares.
 *
 * If [source] is not a [BufferedSource] then called [Source.buffer] for it to create buffered wrapper.
 *
 * Elements must all be of type [T].
 * Elements are parsed lazily when resulting [Sequence] is evaluated.
 * Resulting sequence is tied to the stream and constrained to be evaluated only once.
 *
 * **Resource caution:** this method does not close [source] when the parsing is finished neither provides method to close it manually.
 * It is a caller responsibility to hold a reference to a stream and close it. Moreover, because stream is parsed lazily,
 * closing it before returned sequence is evaluated fully would result in [Exception] from decoder.
 *
 * @throws [SerializationException] if the given JSON input cannot be deserialized to the value of type [T].
 * @throws [okio.IOException] If an I/O error occurs and source can't be read from.
 */
@ExperimentalSerializationApi
public inline fun <reified T> Json.decodeSourceToSequence(
    source: Source,
    format: DecodeSequenceMode = DecodeSequenceMode.AUTO_DETECT
): Sequence<T> = decodeSourceToSequence(source, serializersModule.serializer(), format)
