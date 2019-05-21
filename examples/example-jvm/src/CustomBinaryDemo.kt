import kotlinx.serialization.*
import kotlinx.serialization.internal.EnumDescriptor
import kotlinx.serialization.internal.HexConverter
import utils.Result
import utils.testMethod
import java.io.*
import kotlin.reflect.KClass

/**
 * This demo shows how user can define his own custom binary format
 *
 * In most cases, it is sufficient to define how all primitive types are getting serialized.
 * If you want precise control over the fields and maybe record their names, you can use
 * `writeElement` methods, see CustomKeyValueDemo.kt
 */

class DataBinaryNullableOutput(val out: DataOutput) : ElementValueEncoder() {
    override fun beginCollection(
        desc: SerialDescriptor,
        collectionSize: Int,
        vararg typeParams: KSerializer<*>
    ): CompositeEncoder {
        return super.beginCollection(desc, collectionSize, *typeParams).also {
            out.writeInt(collectionSize)
        }
    }
    override fun encodeNull() = out.writeByte(0)
    override fun encodeNotNullMark() = out.writeByte(1)
    override fun encodeBoolean(value: Boolean) = out.writeByte(if (value) 1 else 0)
    override fun encodeByte(value: Byte) = out.writeByte(value.toInt())
    override fun encodeShort(value: Short) = out.writeShort(value.toInt())
    override fun encodeInt(value: Int) = out.writeInt(value)
    override fun encodeLong(value: Long) = out.writeLong(value)
    override fun encodeFloat(value: Float) = out.writeFloat(value)
    override fun encodeDouble(value: Double) = out.writeDouble(value)
    override fun encodeChar(value: Char) = out.writeChar(value.toInt())
    override fun encodeString(value: String) = out.writeUTF(value)
    override fun encodeEnum(enumDescription: EnumDescriptor, ordinal: Int) = out.writeInt(ordinal)
}

class DataBinaryNullableInput(val inp: DataInput) : ElementValueDecoder() {
    override fun decodeCollectionSize(desc: SerialDescriptor): Int = inp.readInt()
    override fun decodeNotNullMark(): Boolean = inp.readByte() != 0.toByte()
    override fun decodeBoolean(): Boolean = inp.readByte().toInt() != 0
    override fun decodeByte(): Byte = inp.readByte()
    override fun decodeShort(): Short = inp.readShort()
    override fun decodeInt(): Int = inp.readInt()
    override fun decodeLong(): Long = inp.readLong()
    override fun decodeFloat(): Float = inp.readFloat()
    override fun decodeDouble(): Double = inp.readDouble()
    override fun decodeChar(): Char = inp.readChar()
    override fun decodeString(): String = inp.readUTF()
    override fun decodeEnum(enumDescription: EnumDescriptor): Int = inp.readInt()
}

fun testDataBinaryIO(serializer: KSerializer<Any>, obj: Any): Result {
    // save
    val baos = ByteArrayOutputStream()
    val out = DataBinaryNullableOutput(DataOutputStream(baos))
    out.encode(serializer, obj)
    // load
    val bytes = baos.toByteArray()
    val inp = DataBinaryNullableInput(DataInputStream(ByteArrayInputStream(bytes)))
    val other = inp.decode(serializer)
    // result
    return Result(obj, other, "${bytes.size} bytes ${HexConverter.printHexBinary(bytes)}")
}

fun main(args: Array<String>) {
    testMethod(::testDataBinaryIO)
}
