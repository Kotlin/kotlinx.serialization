import kotlinx.serialization.*
import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlinx.serialization.internal.EnumDescriptor
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
 */

class KeyValueOutput(val out: PrintWriter) : ElementValueEncoder () {
    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
        out.print('{')
        return this
    }

    override fun endStructure(desc: SerialDescriptor) {
        out.print('}')
    }

    /**
     * encodeElement should return false, if this field must be skipped
     */
    override fun encodeElement(desc: SerialDescriptor, index: Int): Boolean {
        if (index > 0) out.print(", ")
        out.print(desc.getElementName(index));
        out.print(':')
        return true
    }

    override fun encodeNull() = out.print("null")

    /**
     * encodeValue is called by default, if primitives encode methods
     * (like encodeInt, etc) are not overridden.
     */
    override fun encodeValue(value: Any) = out.print(value)

    override fun encodeEnum(enumDescription: SerialDescriptor, ordinal: Int) {
        out.print(enumDescription.getElementName(ordinal))
    }

    override fun encodeString(value: String) {
        out.print('"')
        out.print(value)
        out.print('"')
    }

    override fun encodeChar(value: Char) = encodeString(value.toString())
}

class KeyValueInput(val inp: Parser) : ElementValueDecoder() {
    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        inp.expectAfterWhiteSpace('{')
        return this
    }

    override fun endStructure(desc: SerialDescriptor) {
        inp.expectAfterWhiteSpace('}')
    }

    /**
     * readElement must return index of field that should be read next.
     * If there are no more fields, return READ_DONE.
     * If you want to read fields in order without call to this method,
     * return READ_ALL (this is default behaviour).
     */
    override fun decodeElementIndex(desc: SerialDescriptor): Int {
        inp.skipWhitespace(',')
        val name = inp.nextUntil(':', '}')
        if (name.isEmpty())
            return READ_DONE
        val index = desc.getElementIndex(name)
        inp.expect(':')
        return index
    }

    private fun decodeToken(): String {
        inp.skipWhitespace()
        return inp.nextUntil(' ', ',', '}')
    }

    override fun decodeNotNullMark(): Boolean {
        inp.skipWhitespace()
        if (inp.cur != 'n'.toInt()) return true
        return false
    }

    override fun decodeNull(): Nothing? {
        check(decodeToken() == "null") { "'null' expected" }
        return null
    }

    override fun decodeBoolean(): Boolean = decodeToken().toBoolean()
    override fun decodeByte(): Byte = decodeToken().toByte()
    override fun decodeShort(): Short = decodeToken().toShort()
    override fun decodeInt(): Int = decodeToken().toInt()
    override fun decodeLong(): Long = decodeToken().toLong()
    override fun decodeFloat(): Float = decodeToken().toFloat()
    override fun decodeDouble(): Double = decodeToken().toDouble()

    override fun decodeEnum(enumDescription: SerialDescriptor): Int {
        return enumDescription.getElementIndexOrThrow(decodeToken())
    }

    override fun decodeString(): String {
        inp.expectAfterWhiteSpace('"')
        val value = inp.nextUntil('"')
        inp.expect('"')
        return value
    }

    override fun decodeChar(): Char = decodeString().single()
}

fun testKeyValueIO(serializer: KSerializer<Any>, obj: Any): Result {
    // save
    val sw = StringWriter()
    val out = KeyValueOutput(PrintWriter(sw))
    out.encode(serializer, obj)
    // load
    val str = sw.toString()
    val inp = KeyValueInput(Parser(StringReader(str)))
    val other = inp.decode(serializer)
    // result
    return Result(obj, other, "${str.length} chars $str")
}

fun main(args: Array<String>) {
    testMethod(::testKeyValueIO)
}
