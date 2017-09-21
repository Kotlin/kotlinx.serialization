import kotlinx.serialization.ElementValueInput
import kotlinx.serialization.ElementValueOutput
import kotlinx.serialization.KSerializer
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

class DataBinaryNullableOutput(val out: DataOutput) : ElementValueOutput() {
    override fun writeNullValue() = out.writeByte(0)
    override fun writeNotNullMark() = out.writeByte(1)
    override fun writeBooleanValue(value: Boolean) = out.writeByte(if (value) 1 else 0)
    override fun writeByteValue(value: Byte) = out.writeByte(value.toInt())
    override fun writeShortValue(value: Short) = out.writeShort(value.toInt())
    override fun writeIntValue(value: Int) = out.writeInt(value)
    override fun writeLongValue(value: Long) = out.writeLong(value)
    override fun writeFloatValue(value: Float) = out.writeFloat(value)
    override fun writeDoubleValue(value: Double) = out.writeDouble(value)
    override fun writeCharValue(value: Char) = out.writeChar(value.toInt())
    override fun writeStringValue(value: String) = out.writeUTF(value)
    override fun <T : Enum<T>> writeEnumValue(enumClass: KClass<T>, value: T) = out.writeInt(value.ordinal)
}

class DataBinaryNullableInput(val inp: DataInput) : ElementValueInput() {
    override fun readNotNullMark(): Boolean = inp.readByte() != 0.toByte()
    override fun readBooleanValue(): Boolean = inp.readByte().toInt() != 0
    override fun readByteValue(): Byte = inp.readByte()
    override fun readShortValue(): Short = inp.readShort()
    override fun readIntValue(): Int = inp.readInt()
    override fun readLongValue(): Long = inp.readLong()
    override fun readFloatValue(): Float = inp.readFloat()
    override fun readDoubleValue(): Double = inp.readDouble()
    override fun readCharValue(): Char = inp.readChar()
    override fun readStringValue(): String = inp.readUTF()
    override fun <T : Enum<T>> readEnumValue(enumClass: KClass<T>): T = enumClass.java.enumConstants[inp.readInt()]
}

fun testDataBinaryIO(serializer: KSerializer<Any>, obj: Any): Result {
    // save
    val baos = ByteArrayOutputStream()
    val out = DataBinaryNullableOutput(DataOutputStream(baos))
    out.write(serializer, obj)
    // load
    val bytes = baos.toByteArray()
    val inp = DataBinaryNullableInput(DataInputStream(ByteArrayInputStream(bytes)))
    val other = inp.read(serializer)
    // result
    return Result(obj, other, "${bytes.size} bytes ${HexConverter.printHexBinary(bytes)}")
}

fun main(args: Array<String>) {
    testMethod(::testDataBinaryIO)
}