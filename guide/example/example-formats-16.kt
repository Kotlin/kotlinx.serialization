// This file was automatically generated from formats.md by Knit tool. Do not edit.
package example.exampleFormats16

import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.encoding.*
import java.io.*

private val byteArraySerializer = serializer<ByteArray>()
class DataOutputEncoder(val output: DataOutput) : AbstractEncoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule
    override fun encodeBoolean(value: Boolean) = output.writeByte(if (value) 1 else 0)
    override fun encodeByte(value: Byte) = output.writeByte(value.toInt())
    override fun encodeShort(value: Short) = output.writeShort(value.toInt())
    override fun encodeInt(value: Int) = output.writeInt(value)
    override fun encodeLong(value: Long) = output.writeLong(value)
    override fun encodeFloat(value: Float) = output.writeFloat(value)
    override fun encodeDouble(value: Double) = output.writeDouble(value)
    override fun encodeChar(value: Char) = output.writeChar(value.code)
    override fun encodeString(value: String) = output.writeUTF(value)
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = output.writeInt(index)

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        encodeInt(collectionSize)
        return this
    }

    override fun encodeNull() = encodeBoolean(false)
    override fun encodeNotNullMark() = encodeBoolean(true)

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        if (serializer.descriptor == byteArraySerializer.descriptor)
            encodeByteArray(value as ByteArray)
        else
            super.encodeSerializableValue(serializer, value)
    }

    private fun encodeByteArray(bytes: ByteArray) {
        encodeCompactSize(bytes.size)
        output.write(bytes)
    }
    
    private fun encodeCompactSize(value: Int) {
        if (value < 0xff) {
            output.writeByte(value)
        } else {
            output.writeByte(0xff)
            output.writeInt(value)
        }
    }            
}

fun <T> encodeTo(output: DataOutput, serializer: SerializationStrategy<T>, value: T) {
    val encoder = DataOutputEncoder(output)
    encoder.encodeSerializableValue(serializer, value)
}

inline fun <reified T> encodeTo(output: DataOutput, value: T) = encodeTo(output, serializer(), value)

class DataInputDecoder(val input: DataInput, var elementsCount: Int = 0) : AbstractDecoder() {
    private var elementIndex = 0
    override val serializersModule: SerializersModule = EmptySerializersModule
    override fun decodeBoolean(): Boolean = input.readByte().toInt() != 0
    override fun decodeByte(): Byte = input.readByte()
    override fun decodeShort(): Short = input.readShort()
    override fun decodeInt(): Int = input.readInt()
    override fun decodeLong(): Long = input.readLong()
    override fun decodeFloat(): Float = input.readFloat()
    override fun decodeDouble(): Double = input.readDouble()
    override fun decodeChar(): Char = input.readChar()
    override fun decodeString(): String = input.readUTF()
    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = input.readInt()

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex == elementsCount) return CompositeDecoder.DECODE_DONE
        return elementIndex++
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        DataInputDecoder(input, descriptor.elementsCount)

    override fun decodeSequentially(): Boolean = true

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int =
        decodeInt().also { elementsCount = it }

    override fun decodeNotNullMark(): Boolean = decodeBoolean()

    @Suppress("UNCHECKED_CAST")
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>, previousValue: T?): T =
        if (deserializer.descriptor == byteArraySerializer.descriptor)
            decodeByteArray() as T
        else
            super.decodeSerializableValue(deserializer, previousValue)

    private fun decodeByteArray(): ByteArray {
        val bytes = ByteArray(decodeCompactSize())
        input.readFully(bytes)
        return bytes
    }

    private fun decodeCompactSize(): Int {
        val byte = input.readByte().toInt() and 0xff
        if (byte < 0xff) return byte
        return input.readInt()
    }
}

fun <T> decodeFrom(input: DataInput, deserializer: DeserializationStrategy<T>): T {
    val decoder = DataInputDecoder(input)
    return decoder.decodeSerializableValue(deserializer)
}

inline fun <reified T> decodeFrom(input: DataInput): T = decodeFrom(input, serializer())

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toInt().toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').uppercase()}}"
}

@Serializable
data class Project(val name: String, val attachment: ByteArray)

fun main() {
    val data = Project("kotlinx.serialization", byteArrayOf(0x0A, 0x0B, 0x0C, 0x0D))
    val output = ByteArrayOutputStream()
    encodeTo(DataOutputStream(output), data)
    val bytes = output.toByteArray()
    println(bytes.toAsciiHexString())
    val input = ByteArrayInputStream(bytes)
    val obj = decodeFrom<Project>(DataInputStream(input))
    println(obj)
}
