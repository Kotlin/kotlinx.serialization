/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.okio

import kotlinx.serialization.*
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.json.okio.internal.JsonToOkioStreamWriter
import kotlinx.serialization.json.internal.decodeToSequence
import kotlinx.serialization.json.okio.internal.OkioSerialReader
import okio.BufferedSink
import okio.BufferedSource

/**
* Serializes the [value] with [serializer] into a [target] using JSON format and UTF-8 encoding.
*
* @throws [SerializationException] if the given value cannot be serialized to JSON.
*/
@ExperimentalSerializationApi
public fun <T> Json.encodeToOkio(
    serializer: SerializationStrategy<T>,
    value: T,
    target: BufferedSink
) {
    val writer = JsonToOkioStreamWriter(target)
    try {
        encodeByWriter(writer, serializer, value)
    } finally {
        writer.release()
    }
}

/**
 * Serializes given [value] to a [target] using UTF-8 encoding and serializer retrieved from the reified type parameter.
 *
 * @throws [SerializationException] if the given value cannot be serialized to JSON.
 */
@ExperimentalSerializationApi
public inline fun <reified T> Json.encodeToOkio(
    value: T,
    target: BufferedSink
): Unit =
    encodeToOkio(serializersModule.serializer(), value, target)



/**
 * Deserializes JSON from [source] using UTF-8 encoding to a value of type [T] using [deserializer].
 *
 * Note that this functions expects that exactly one object would be present in the source
 * and throws an exception if there are any dangling bytes after an object.
 *
 * @throws [SerializationException] if the given JSON input cannot be deserialized to the value of type [T].
 */
@ExperimentalSerializationApi
public fun <T> Json.decodeFromOkio(
    deserializer: DeserializationStrategy<T>,
    source: BufferedSource
): T {
    return decodeByReader(deserializer, OkioSerialReader(source))
}

/**
 * Deserializes the contents of given [source] to the value of type [T] using UTF-8 encoding and
 * deserializer retrieved from the reified type parameter.
 *
 * Note that this functions expects that exactly one object would be present in the stream
 * and throws an exception if there are any dangling bytes after an object.
 *
 * @throws [SerializationException] if the given JSON input cannot be deserialized to the value of type [T].
 */
@ExperimentalSerializationApi
public inline fun <reified T> Json.decodeFromOkio(source: BufferedSource): T =
    decodeFromOkio(serializersModule.serializer(), source)


/**
 * Transforms the given [source] into lazily deserialized sequence of elements of type [T] using UTF-8 encoding and [deserializer].
 * Unlike [decodeFromOkio], [source] is allowed to have more than one element, separated as [format] declares.
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
 */
@ExperimentalSerializationApi
public fun <T> Json.decodeOkioToSequence(
    source: BufferedSource,
    deserializer: DeserializationStrategy<T>,
    format: DecodeSequenceMode = DecodeSequenceMode.AUTO_DETECT
): Sequence<T> {
    return decodeToSequence(OkioSerialReader(source), deserializer, format)
}

/**
 * Transforms the given [source] into lazily deserialized sequence of elements of type [T] using UTF-8 encoding and deserializer retrieved from the reified type parameter.
 * Unlike [decodeFromOkio], [source] is allowed to have more than one element, separated as [format] declares.
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
 */
@ExperimentalSerializationApi
public inline fun <reified T> Json.decodeOkioToSequence(
    source: BufferedSource,
    format: DecodeSequenceMode = DecodeSequenceMode.AUTO_DETECT
): Sequence<T> = decodeOkioToSequence(source, serializersModule.serializer(), format)
