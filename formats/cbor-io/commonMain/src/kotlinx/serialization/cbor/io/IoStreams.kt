package kotlinx.serialization.cbor.io

import kotlinx.io.*
import kotlinx.serialization.*
import kotlinx.serialization.cbor.*
import kotlinx.serialization.cbor.io.internal.*

/**
 * Serializes the [value] with [serializer] into a [sink] using CBOR format.
 *
 * @throws [SerializationException] if the given value cannot be serialized to CBOR.
 * @throws [kotlinx.io.IOException] If an I/O error occurs and sink can't be written to.
 */
@ExperimentalSerializationApi
public fun <T> Cbor.encodeToSink(serializer: SerializationStrategy<T>, value: T, sink: Sink) {
    encodeToOutput(serializer, value, IoStreamOutput(sink))
}

/**
 * Serializes given [value] to a [sink] using CBOR format and serializer retrieved from the reified type parameter.
 *
 * @throws [SerializationException] if the given value cannot be serialized to CBOR.
 * @throws [kotlinx.io.IOException] If an I/O error occurs and sink can't be written to.
 */
@ExperimentalSerializationApi
public inline fun <reified T> Cbor.encodeToSink(
    value: T,
    sink: Sink
): Unit = encodeToSink(serializersModule.serializer(), value, sink)

/**
 * Deserializes CBOR from [source] to a value of type [T] using [deserializer].
 *
 * Note that this functions expects that exactly one object would be present in the source
 * and throws an exception if there are any dangling bytes after an object.
 *
 * @throws [SerializationException] if the given CBOR input cannot be deserialized to the value of type [T].
 * @throws [kotlinx.io.IOException] If an I/O error occurs and source can't be read from.
 */
@ExperimentalSerializationApi
public fun <T> Cbor.decodeFromSource(deserializer: DeserializationStrategy<T>, source: Source): T =
    decodeFromInput(deserializer, IoStreamInput(source))

/**
 * Deserializes CBOR from [source] to a value of type [T] using deserializer retrieved from the reified type parameter.
 *
 * Note that this functions expects that exactly one object would be present in the stream
 * and throws an exception if there are any dangling bytes after an object.
 *
 * @throws [SerializationException] if the given CBOR input cannot be deserialized to the value of type [T].
 * @throws [kotlinx.io.IOException] If an I/O error occurs and source can't be read from.
 */
@ExperimentalSerializationApi
public inline fun <reified T> Cbor.decodeFromSource(source: Source): T =
    decodeFromSource(serializersModule.serializer(), source)
