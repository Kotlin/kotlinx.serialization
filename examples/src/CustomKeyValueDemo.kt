import kotlinx.serialization.*
import utils.Parser
import utils.Result
import utils.testMethod
import java.io.PrintWriter
import java.io.StringReader
import java.io.StringWriter
import kotlin.reflect.KClass

/**
 * This demo shows how user can define his own custom text format.
 *
 * Because text formats usually record field names,
 * here we are using writeElement method, which provide information about current field.
 * Also, unlike binary demo, there are object separators, which are written in
 * writeBegin and writeEnd methods.
 *
 * @author Roman Elizarov
 */

class KeyValueOutput(val out: PrintWriter) : ElementValueOutput() {
    override fun writeBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KOutput {
        out.print('{')
        return this
    }

    override fun writeEnd(desc: KSerialClassDesc) {
        out.print('}')
    }

    /**
     * writeElement should return false, if this field must be skipped
     */
    override fun writeElement(desc: KSerialClassDesc, index: Int): Boolean {
        if (index > 0) out.print(", ")
        out.print(desc.getElementName(index));
        out.print(':')
        return true
    }

    override fun writeNullValue() = out.print("null")

    /**
     * writeValue is called by default, if primitives write methods
     * (like writeInt, etc) are not overridden.
     */
    override fun writeValue(value: Any) = out.print(value)

    override fun writeStringValue(value: String) {
        out.print('"')
        out.print(value)
        out.print('"')
    }

    override fun writeCharValue(value: Char) = writeStringValue(value.toString())
}

class KeyValueInput(val inp: Parser) : ElementValueInput() {
    override fun readBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KInput {
        inp.expectAfterWhiteSpace('{')
        return this
    }

    override fun readEnd(desc: KSerialClassDesc) {
        inp.expectAfterWhiteSpace('}')
    }

    /**
     * readElement must return index of field that should be read next.
     * If there are no more fields, return READ_DONE.
     * If you want to read fields in order without call to this method,
     * return READ_ALL (this is default behaviour).
     */
    override fun readElement(desc: KSerialClassDesc): Int {
        inp.skipWhitespace(',')
        val name = inp.nextUntil(':', '}')
        if (name.isEmpty())
            return READ_DONE
        val index = desc.getElementIndex(name)
        inp.expect(':')
        return index
    }

    private fun readToken(): String {
        inp.skipWhitespace()
        return inp.nextUntil(' ', ',', '}')
    }

    override fun readNotNullMark(): Boolean {
        inp.skipWhitespace()
        if (inp.cur != 'n'.toInt()) return true
        return false
    }

    override fun readNullValue(): Nothing? {
        check(readToken() == "null") { "'null' expected" }
        return null
    }

    override fun readBooleanValue(): Boolean = readToken().toBoolean()
    override fun readByteValue(): Byte = readToken().toByte()
    override fun readShortValue(): Short = readToken().toShort()
    override fun readIntValue(): Int = readToken().toInt()
    override fun readLongValue(): Long = readToken().toLong()
    override fun readFloatValue(): Float = readToken().toFloat()
    override fun readDoubleValue(): Double = readToken().toDouble()

    override fun <T : Enum<T>> readEnumValue(enumClass: KClass<T>): T =
            java.lang.Enum.valueOf(enumClass.java, readToken())

    override fun readStringValue(): String {
        inp.expectAfterWhiteSpace('"')
        val value = inp.nextUntil('"')
        inp.expect('"')
        return value
    }

    override fun readCharValue(): Char = readStringValue().single()
}

fun testKeyValueIO(serializer: KSerializer<Any>, obj: Any): Result {
    // save
    val sw = StringWriter()
    val out = KeyValueOutput(PrintWriter(sw))
    out.write(serializer, obj)
    // load
    val str = sw.toString()
    val inp = KeyValueInput(Parser(StringReader(str)))
    val other = inp.read(serializer)
    // result
    return Result(obj, other, "${str.length} chars $str")
}

fun main(args: Array<String>) {
    testMethod(::testKeyValueIO)
}