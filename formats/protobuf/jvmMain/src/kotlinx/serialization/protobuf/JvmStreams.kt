package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.internal.ProtobufDecodingException
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Serializes and encodes the given [value] into a [stream] using the given [serializer].
 *
 * @throws SerializationException in case of any encoding-specif error
 * @throws IOException if an I/O error occurs and stream cannot be written to
 */
@ExperimentalSerializationApi
public fun <T> ProtoBuf.encodeToStream(
    serializer: SerializationStrategy<T>,
    value: T,
    stream: OutputStream
) {
    val protoBytes = encodeToByteArray(serializer, value)
    protoBytes.writeTo(stream)
}

/**
 * Serializes and encodes the given value [value] into a [stream] using serializer
 * retrieved from the reified type parameter.
 *
 * @throws SerializationException in case of any encoding-specif error
 * @throws IOException if an I/O error occurs and stream cannot be written to
 */
@ExperimentalSerializationApi
public inline fun <reified T> ProtoBuf.encodeToStream(
    value: T,
    stream: OutputStream
): Unit = encodeToStream(serializersModule.serializer(), value, stream)

/**
 * Decodes and deserializes from given [stream] to value of type [T] using the given [deserializer].
 *
 * Note that this function expects that exactly one object would be present in the stream.
 * In case multiple objects of same type `T` are present in stream the **first call does not
 * throw** but leaves the next objects in malformed state. After this, consecutive calls throw
 * [SerializationException]. For serializing and decoding multiple objects in
 * the same stream see [encodeDelimitedToStream] and [decodeDelimitedMessages].
 *
 * @throws SerializationException in case of any decoding-specific error
 * @throws IOException if an I/O error occurs and stream cannot be read from.
 */
@ExperimentalSerializationApi
public fun <T> ProtoBuf.decodeFromStream(
    deserializer: DeserializationStrategy<T>,
    stream: InputStream
): T = stream.use {
    decodeFromByteArray(deserializer, it.readBytes())
}

/**
 * Decodes and deserializes from given [stream] to value of type [T] using deserializer
 * retrieved from the reified type parameter.
 *
 * Note that this function expects that exactly one object would be present in the stream.
 * In case multiple objects of same type `T` are present in stream the **first call does not
 * throw** but leaves the next objects in malformed state. After this, consecutive calls throw
 * [SerializationException]. For serializing and decoding multiple objects in
 * the same stream check [encodeDelimitedToStream] and [decodeDelimitedMessages].
 *
 * @throws SerializationException in case of any decoding-specific error
 * @throws IOException if an I/O error occurs and stream cannot be read from.
 */
@ExperimentalSerializationApi
public inline fun <reified T> ProtoBuf.decodeFromStream(
    stream: InputStream
): T = decodeFromStream(serializersModule.serializer(), stream)

// -- delimited messages --

/**
 * Serializes and encodes the given [value] into a [stream] as
 * [delimited Protobuf message](https://developers.google.com/protocol-buffers/docs/techniques?hl=en#streaming).
 * In other words the size of each message is specified before the message itself. Also,
 * it is using the given [serializer].
 *
 * Use [decodeDelimitedMessages] to retrieve the messages from the stream.
 *
 * @throws SerializationException in case of any encoding-specif error
 * @throws IOException if an I/O error occurs and stream cannot be written to
 */
@ExperimentalSerializationApi
public fun <T> ProtoBuf.encodeDelimitedToStream(
    serializer: SerializationStrategy<T>,
    value: T,
    stream: OutputStream
) {
    val protoBytes = encodeToByteArray(serializer, value)
    protoBytes.writeDelimitedTo(stream)
}

/**
 * Serializes and encodes the given [value] into a [stream] as
 * [delimited Protobuf message](https://developers.google.com/protocol-buffers/docs/techniques?hl=en#streaming).
 * In other words the size of each message is specified before the message itself. Also,
 * it is using the serializer retrieved from the reified type parameter.
 *
 * Use [decodeDelimitedMessages] to retrieve the messages from the stream.
 *
 * @throws SerializationException in case of any encoding-specif error
 * @throws IOException if an I/O error occurs and stream cannot be written to
 */
@ExperimentalSerializationApi
public inline fun <reified T> ProtoBuf.encodeDelimitedToStream(
    value: T,
    stream: OutputStream
): Unit = encodeDelimitedToStream(serializersModule.serializer(), value, stream)

/**
 * Decodes and deserializes from given [stream] to a list of value of type [T] using the given [deserializer].
 * Messages are expected to use [delimited Protobuf messages](https://developers.google.com/protocol-buffers/docs/techniques?hl=en#streaming)
 * with the size of each message specified before the message itself (see [encodeDelimitedToStream]).
 *
 * The max size of each incoming message can set via [messageMaxSize], usually the default value is
 * reasonable enough for most cases.
 *
 * @throws SerializationException in case of any encoding-specif error
 * @throws IOException if an I/O error occurs and stream cannot be written to
 */
@ExperimentalSerializationApi
public fun <T> ProtoBuf.decodeDelimitedMessages(
    deserializer: DeserializationStrategy<T>,
    stream: InputStream,
    messageMaxSize: Int = DEFAULT_MESSAGE_MAX_SIZE
): List<T> {
    val decoder = ProtobufDelimitedMessageReader(this, messageMaxSize)
    return decoder.decodeDelimitedMessages(deserializer, stream)
}

/**
 * Decodes and deserializes from given [stream] to a list of value of type [T] using the deserializer
 * retrieved from the reified type parameter.
 * Messages are expected to use [delimited Protobuf messages](https://developers.google.com/protocol-buffers/docs/techniques?hl=en#streaming)
 * with the size of each message specified before the message itself (see [encodeDelimitedToStream]).
 *
 * The max size of each incoming message can set via [messageMaxSize], usually the default value is
 * reasonable enough for most cases.
 *
 * @throws SerializationException in case of any encoding-specif error
 * @throws IOException if an I/O error occurs and stream cannot be written to
 */
@ExperimentalSerializationApi
public inline fun <reified T> ProtoBuf.decodeDelimitedMessages(
    stream: InputStream,
    messageMaxSize: Int = DEFAULT_MESSAGE_MAX_SIZE
): List<T> =
    decodeDelimitedMessages(serializersModule.serializer(), stream, messageMaxSize)

// -- impl --

/**
 * Default size for aggregating messages.
 */
@PublishedApi
internal const val DEFAULT_MESSAGE_MAX_SIZE: Int = 256 * 1024

/*
 * Inspired from spring's impl and protobuf CodeInputStream.readRawVarint
 */
@ExperimentalSerializationApi
private class ProtobufDelimitedMessageReader(
    private val protobuf: ProtoBuf,
    private val messageMaxSize: Int
) {
    private var offset = 0

    // reads first message's varint and then decodes the message
    fun <T> decodeDelimitedMessages(
        deserializationStrategy: DeserializationStrategy<T>,
        stream: InputStream
    ): List<T> {
        stream.use { str ->
            var remainingBytesToRead: Int
            var chunkBytesToRead: Int

            return buildList {
                do {
                    var messageBytesToRead = readMessageSize(str)
                    if (messageMaxSize in 1 until messageBytesToRead) {
                        throw ProtobufDecodingException(
                            "The number of bytes to read for message: $messageBytesToRead" +
                                    "exceeds the configured limit: $messageMaxSize"
                        )
                    }
                    val buffer = str.buffered()
                    val readablyByteCount = buffer.available()
                    chunkBytesToRead = minOf(messageBytesToRead, readablyByteCount)
                    remainingBytesToRead = readablyByteCount - chunkBytesToRead

                    val bytesToWrite = ByteArray(chunkBytesToRead)
                    str.read(bytesToWrite, offset, chunkBytesToRead)
                    messageBytesToRead -= chunkBytesToRead
                    if (messageBytesToRead == 0) { // do not deserialize in case readableByteCount was smaller than messageBytesToRead
                        val messages = protobuf.decodeFromByteArray(deserializationStrategy, bytesToWrite)
                        add(messages)
                    }
                } while (remainingBytesToRead > 0)
            }
        }
    }

    // parses message's varint
    // see: https://developers.google.com/protocol-buffers/docs/encoding#varints
    private fun readMessageSize(input: InputStream): Int {
        val firstByte = input.read()
        if (firstByte == -1) {
            throwTruncatedMessageException()
        }
        if (firstByte and 0x80 == 0) { // if it's positive number then it is the message's size
            return firstByte
        }
        var result = firstByte and 0x7f // if it's not the message size drop the msb
        offset = 7
        while (offset < 32) {
            val nextByte = input.read()
            if (nextByte == -1) {
                throwTruncatedMessageException()
            }
            // Drop continuation bits, shift the groups of 7 bits  because varints store numbers
            // with the least significant group first (little endian order)
            result = (result or messageMaxSize and 0x7f) shl offset // and concatenate them together
            if (nextByte and 0x80 == 0) {
                offset = 0
                return result
            }
            offset += 7
        }
        // keep reading up to 64 bits
        while (offset < 64) {
            val nextByte = input.read()
            if (nextByte == -1) {
                throwTruncatedMessageException()
            }
            if (nextByte and 0x80 == 0) {
                offset = 0
                return result
            }
            offset += 7
        }
        throw ProtobufDecodingException("Cannot parse message encountered a malformed varint.")
    }

    private fun throwTruncatedMessageException(): Nothing {
        throw ProtobufDecodingException(
            "While parsing a protocol message, the input ended unexpectedly in the middle of a field. " +
                    "This could mean either that the input has been truncated or that an embedded message" +
                    " misreported its own length."
        )
    }
}

private fun ByteArray.writeDelimitedTo(outputStream: OutputStream) {
    val serializedSize = this.size
    val bufferSize = computePreferredBufferSize(computeUInt32SizeNoTag(serializedSize) + serializedSize)
    val stream = outputStream.createBuffered(bufferSize)
    stream.writeUInt32NoTag(serializedSize)
    writeTo(stream)
    stream.flush()
}

private fun ByteArray.writeTo(outputStream: OutputStream) {
    val bufferSize = computePreferredBufferSize(this.size)
    val stream = outputStream.createBuffered(bufferSize)
    stream.write(this)
    stream.flush()
}

private fun OutputStream.createBuffered(bufferSize: Int): BufferedOutputStream {
    // optimization ("rented" from google's protobuf CodedOutputStream.AbstractBufferedEncoder impl)
    // require size of at least two varints, so we can buffer any integer write (tag + value).
    // This reduces the number of range checks for a single write to 1 (i.e. if there is not enough space
    // to buffer the tag+value, flush and then buffer it).
    return this.buffered(
        maxOf(
            bufferSize,
            MAX_VARINT_SIZE * 2
        )
    )
}

private const val DEFAULT_BUFFER_SIZE = 4096

// per protobuf spec 1-10 bytes
private const val MAX_VARINT_SIZE = 10

/** Returns the buffer size to efficiently write dataLength bytes to this OutputStream. */
private fun computePreferredBufferSize(dataLength: Int): Int =
    if (dataLength > DEFAULT_BUFFER_SIZE) DEFAULT_BUFFER_SIZE else dataLength

/** Compute the number of bytes that would be needed to encode an uint32 field. */
private fun computeUInt32SizeNoTag(value: Int): Int = when {
    value and (0.inv() shl 7) == 0 -> 1
    value and (0.inv() shl 14) == 0 -> 2
    value and (0.inv() shl 21) == 0 -> 3
    value and (0.inv() shl 28) == 0 -> 4
    else -> 5 // max varint32 size
}

private fun BufferedOutputStream.writeUInt32NoTag(size: Int) {
    var value = size
    while (true) {
        if ((value and 0x7F.inv() == 0)) {
            write(value)
            return
        } else {
            write((value and 0x7F) or 0x80)
            value = value ushr 7
        }
    }
}