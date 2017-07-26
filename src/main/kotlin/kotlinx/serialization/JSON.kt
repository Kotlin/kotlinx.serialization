/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.serialization

import java.io.*
import kotlin.reflect.KClass

data class JSON(
        private val unquoted: Boolean = false,
        private val indented: Boolean = false,
        private val indent: String = "    "
) {

    fun <T> stringify(saver: KSerialSaver<T>, obj: T): String {
        val sw = StringWriter()
        val output = JsonOutput(Mode.OBJ, Composer(sw))
        output.write(saver, obj)
        return sw.toString()
    }

    inline fun <reified T : Any> stringify(obj: T): String = stringify(T::class.serializer(), obj)

    fun <T> parse(loader: KSerialLoader<T>, str: String): T {
        val parser = Parser(StringReader(str))
        val input = JsonInput(Mode.OBJ, parser)
        val result = input.read(loader)
        check(parser.curTc == TC_EOF) { "Shall parse complete string"}
        return result
    }

    inline fun <reified T : Any> parse(str: String): T = parse(T::class.serializer(), str)

    companion object {
        fun <T> stringify(saver: KSerialSaver<T>, obj: T): String = plain.stringify(saver, obj)
        inline fun <reified T : Any> stringify(obj: T): String = stringify(T::class.serializer(), obj)
        fun <T> parse(loader: KSerialLoader<T>, str: String): T = plain.parse(loader, str)
        inline fun <reified T : Any> parse(str: String): T = parse(T::class.serializer(), str)

        val plain = JSON()
        val unquoted = JSON(unquoted = true)
        val indented = JSON(indented = true)

        //================================= implementation =================================

        // special strings
        private const val NULL = "null"

        // special chars
        private const val COMMA = ','
        private const val COLON = ':'
        private const val BEGIN_OBJ = '{'
        private const val END_OBJ = '}'
        private const val BEGIN_LIST = '['
        private const val END_LIST = ']'
        private const val STRING = '"'
        private const val STRING_ESC = '\\'

        private const val INVALID = 0.toChar()
        private const val UNICODE_ESC = 'u'

        // token classes
        private const val TC_OTHER: Byte = 0
        private const val TC_EOF: Byte = 1
        private const val TC_INVALID: Byte = 2
        private const val TC_WS: Byte = 3
        private const val TC_COMMA: Byte = 4
        private const val TC_COLON: Byte = 5
        private const val TC_BEGIN_OBJ: Byte = 6
        private const val TC_END_OBJ: Byte = 7
        private const val TC_BEGIN_LIST: Byte = 8
        private const val TC_END_LIST: Byte = 9
        private const val TC_STRING: Byte = 10
        private const val TC_STRING_ESC: Byte = 11
        private const val TC_NULL: Byte = 12

        // mapping from chars to token classes
        private const val CTC_MAX = 0x7e
        private const val CTC_OFS = 1

        private val C2TC = ByteArray(CTC_MAX + CTC_OFS)

        fun initC2TC(c : Int, cl: Byte) { C2TC[c + CTC_OFS] = cl }
        fun initC2TC(c: Char, cl: Byte) {
            initC2TC(c.toInt(), cl)
        }

        fun c2tc(c: Int): Byte = if (c < CTC_MAX + CTC_OFS) C2TC[c + CTC_OFS] else TC_OTHER

        init {
            initC2TC(-1, TC_EOF)
            for (i in 0..0x20)
                initC2TC(i, TC_INVALID)
            initC2TC(0x09, TC_WS)
            initC2TC(0x0a, TC_WS)
            initC2TC(0x0d, TC_WS)
            initC2TC(0x20, TC_WS)
            initC2TC(COMMA, TC_COMMA)
            initC2TC(COLON, TC_COLON)
            initC2TC(BEGIN_OBJ, TC_BEGIN_OBJ)
            initC2TC(END_OBJ, TC_END_OBJ)
            initC2TC(BEGIN_LIST, TC_BEGIN_LIST)
            initC2TC(END_LIST, TC_END_LIST)
            initC2TC(STRING, TC_STRING)
            initC2TC(STRING_ESC, TC_STRING_ESC)
        }

        private fun mustBeQuoted(str: String): Boolean = str.any { c2tc(it.toInt()) != TC_OTHER } || str == NULL

        // mapping from chars to their escape chars and back
        private const val C2ESC_MAX = 0x5d
        private const val ESC2C_MAX = 0x75

        private val C2ESC = ByteArray(C2ESC_MAX)
        private val ESC2C = ByteArray(ESC2C_MAX)

        fun initC2ESC(c: Int, esc: Char) {
            C2ESC[c] = esc.toByte()
            if (esc != UNICODE_ESC) ESC2C[esc.toInt()] = c.toByte()
        }

        fun initC2ESC(c: Char, esc: Char) {
            initC2ESC(c.toInt(), esc)
        }

        fun c2esc(c: Char): Char = if (c.toInt() < C2ESC_MAX) C2ESC[c.toInt()].toChar() else INVALID
        fun esc2c(c: Int): Char = if (c < ESC2C_MAX) ESC2C[c].toChar() else INVALID

        init {
            for (i in 0x00..0x1f)
                initC2ESC(i, UNICODE_ESC)
            initC2ESC(0x08, 'b')
            initC2ESC(0x09, 't')
            initC2ESC(0x0a, 'n')
            initC2ESC(0x0c, 'f')
            initC2ESC(0x0d, 'r')
            initC2ESC(STRING, STRING)
            initC2ESC(STRING_ESC, STRING_ESC)
        }

        fun hex(i: Int) : Char {
            val d = i and 0xf
            return if (d < 10) (d + '0'.toInt()).toChar()
            else (d - 10 + 'a'.toInt()).toChar()
        }

        private fun switchMode(mode: Mode, desc: KSerialClassDesc, typeParams: Array<out KSerializer<*>>): Mode =
                when (desc.kind) {
                    KSerialClassKind.POLYMORPHIC -> Mode.POLY
                    KSerialClassKind.LIST, KSerialClassKind.SET -> Mode.LIST
                    KSerialClassKind.MAP -> {
                        val keyKind = typeParams[0].serialClassDesc.kind
                        if (keyKind == KSerialClassKind.PRIMITIVE || keyKind == KSerialClassKind.ENUM)
                            Mode.MAP
                        else Mode.LIST
                    }
                    KSerialClassKind.ENTRY -> if (mode == Mode.MAP) Mode.ENTRY else Mode.OBJ
                    else -> Mode.OBJ
                }


        private fun require(condition: Boolean, pos: Int, msg: () -> String ) {
            if (!condition)
                fail(pos, msg())
        }

        private fun fail(pos: Int, msg: String): Nothing {
            throw IllegalArgumentException("JSON at $pos: $msg")
        }
    }

    private enum class Mode(val begin: Char, val end: Char) {
        OBJ(BEGIN_OBJ, END_OBJ),
        LIST(BEGIN_LIST, END_LIST),
        MAP(BEGIN_OBJ, END_OBJ),
        POLY(BEGIN_LIST, END_LIST),
        ENTRY(INVALID, INVALID);

        val beginTc: Byte = c2tc(begin.toInt())
        val endTc: Byte = c2tc(end.toInt())
    }

    private inner class JsonOutput(val mode: Mode, val w: Composer) : ElementValueOutput() {
        private var forceStr: Boolean = false

        override fun writeBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KOutput {
            val newMode = switchMode(mode, desc, typeParams)
            if (newMode.begin != INVALID) {
                w.print(newMode.begin)
                w.indent()
            }
            return if (mode == newMode) this else JsonOutput(newMode, w) // todo: reuse instance per mode
        }

        override fun writeEnd(desc: KSerialClassDesc) {
            if (mode.end != INVALID) {
                w.unIndent()
                w.nextItem()
                w.print(mode.end)
            }
        }

        override fun writeElement(desc: KSerialClassDesc, index: Int): Boolean {
            when (mode) {
                Mode.LIST, Mode.MAP -> {
                    if (index == 0) return false
                    if (index > 1)
                        w.print(COMMA)
                    w.nextItem()
                }
                Mode.ENTRY, Mode.POLY -> {
                    if (index == 0)
                        forceStr = true
                    if (index == 1) {
                        w.print(if (mode == Mode.ENTRY) COLON else COMMA)
                        w.space()
                        forceStr = false
                    }
                }
                else -> {
                    if (index > 0)
                        w.print(COMMA)
                    w.nextItem()
                    writeStringValue(desc.getElementName(index))
                    w.print(COLON)
                    w.space()
                }
            }
            return true
        }

        override fun writeNullValue() {
            w.print(NULL)
        }

        override fun writeBooleanValue(value: Boolean) { if (forceStr) writeStringValue(value.toString()) else w.print(value) }
        override fun writeByteValue(value: Byte) { if (forceStr) writeStringValue(value.toString()) else w.print(value) }
        override fun writeShortValue(value: Short) { if (forceStr) writeStringValue(value.toString()) else w.print(value) }
        override fun writeIntValue(value: Int) { if (forceStr) writeStringValue(value.toString()) else w.print(value) }
        override fun writeLongValue(value: Long) { if (forceStr) writeStringValue(value.toString()) else w.print(value) }

        override fun writeFloatValue(value: Float) {
            if (forceStr || !value.isFinite()) writeStringValue(value.toString()) else
                w.print(value)
        }

        override fun writeDoubleValue(value: Double) {
            if (forceStr || !value.isFinite()) writeStringValue(value.toString()) else
                w.print(value)
        }

        override fun writeCharValue(value: Char) {
            writeStringValue(value.toString())
        }

        override fun writeStringValue(value: String) {
            if (unquoted && !mustBeQuoted(value)) {
                w.print(value)
                return
            }
            w.print(STRING)
            for (c in value) {
                val esc = c2esc(c)
                when (esc) {
                    INVALID -> w.print(c) // no need to escape
                    UNICODE_ESC -> {
                        w.print(STRING_ESC)
                        w.print(UNICODE_ESC)
                        val code = c.toInt()
                        w.print(hex(code shr 12))
                        w.print(hex(code shr 8))
                        w.print(hex(code shr 4))
                        w.print(hex(code))
                    }
                    else -> {
                        w.print(STRING_ESC)
                        w.print(esc)
                    }
                }
            }
            w.print(STRING)
        }

        override fun writeValue(value: Any) {
            writeStringValue(value.toString())
        }

    }

    private inner class Composer(w: Writer) : PrintWriter(w) {
        var level = 0
        fun indent() { level++ }
        fun unIndent() { level-- }

        fun nextItem() {
            if (indented) {
                println()
                repeat(level) { print(indent) }
            }
        }

        fun space() {
            if (indented)
                print(' ')
        }
    }

    private class JsonInput(val mode: Mode, val p: Parser) : ElementValueInput() {
        var curIndex = 0
        var entryIndex = 0

        override fun readBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KInput {
            val newMode = switchMode(mode, desc, typeParams)
            if (newMode.begin != INVALID) {
                require(p.curTc == newMode.beginTc, p.tokenPos) { "Expected '${newMode.begin}, kind: ${desc.kind}'" }
                p.nextToken()
            }
            return when (newMode) {
                Mode.LIST, Mode.MAP, Mode.POLY -> JsonInput(newMode, p) // need fresh cur index
                else -> if (mode == newMode) this else
                    JsonInput(newMode, p) // todo: reuse instance per mode
            }
        }

        override fun readEnd(desc: KSerialClassDesc) {
            if (mode.end != INVALID) {
                require(p.curTc == mode.endTc, p.tokenPos) { "Expected '${mode.end}'" }
                p.nextToken()
            }
        }

        override fun readNotNullMark(): Boolean {
            return p.curTc != TC_NULL
        }

        override fun readNullValue(): Nothing? {
            require(p.curTc == TC_NULL, p.tokenPos) { "Expected 'null' literal" }
            p.nextToken()
            return null
        }

        override fun readElement(desc: KSerialClassDesc): Int {
//            println(p.state())
            if (p.curTc == TC_COMMA) p.nextToken()
            when (mode) {
                Mode.LIST, Mode.MAP -> {
                    if (!p.canBeginValue)
                        return READ_DONE
                    return ++curIndex
                }
                Mode.POLY -> {
                    when (entryIndex++) {
                        0 -> return 0
                        1 -> {
                            return 1
                        }
                        else -> {
                            entryIndex = 0
                            return READ_DONE
                        }
                    }
                }
                Mode.ENTRY -> {
                    when (entryIndex++) {
                        0 -> return 0
                        1 -> {
                            require(p.curTc == TC_COLON, p.tokenPos) { "Expected ':'" }
                            p.nextToken()
                            return 1
                        }
                        else -> {
                            entryIndex = 0
                            return READ_DONE
                        }
                    }
                }
                else -> {
                    if (!p.canBeginValue)
                        return READ_DONE
                    val key = p.takeStr()
                    require(p.curTc == TC_COLON, p.tokenPos) { "Expected ':'" }
                    p.nextToken()
                    return desc.getElementIndex(key)
                }
            }
        }

        override fun readBooleanValue(): Boolean = p.takeStr().toBoolean()
        override fun readByteValue(): Byte = p.takeStr().toByte()
        override fun readShortValue(): Short = p.takeStr().toShort()
        override fun readIntValue(): Int = p.takeStr().toInt()
        override fun readLongValue(): Long = p.takeStr().toLong()
        override fun readFloatValue(): Float = p.takeStr().toFloat()
        override fun readDoubleValue(): Double = p.takeStr().toDouble()
        override fun readCharValue(): Char = p.takeStr().single()
        override fun readStringValue(): String = p.takeStr()

        override fun <T : Enum<T>> readEnumValue(enumClass: KClass<T>): T =
                java.lang.Enum.valueOf(enumClass.java, p.takeStr())
    }

    private class Parser(val r: Reader) {
        // updated by nextChar
        var charPos: Int = 0
        var curChar: Int = -1
        // updated by nextToken
        var tokenPos: Int = 0
        var curTc: Byte = TC_EOF
        var curStr: String? = null
        var sb = StringBuilder()

        init {
            nextChar()
            nextToken()
        }

        val canBeginValue: Boolean get() = when (curTc) {
            TC_BEGIN_LIST, TC_BEGIN_OBJ, TC_OTHER, TC_STRING, TC_NULL -> true
            else -> false
        }

        fun takeStr(): String {
            val prevStr = curStr ?: fail(tokenPos, "Expected string or non-null literal")
            nextToken()
            return prevStr
        }

        fun nextToken() {
            while(true) {
                tokenPos = charPos
                curTc = c2tc(curChar)
                when (curTc) {
                    TC_WS -> nextChar() // skip whitespace
                    TC_OTHER -> {
                        nextLiteral()
                        return
                    }
                    TC_STRING -> {
                        nextString()
                        return
                    }
                    else -> {
                        nextChar()
                        curStr = null
                        return
                    }
                }
            }
        }

        private fun nextChar() {
            curChar = r.read()
            charPos++
        }

        private fun nextLiteral() {
            sb.setLength(0)
            while(true) {
                sb.append(curChar.toChar())
                nextChar()
                if (c2tc(curChar) != TC_OTHER) break
            }
            if (NULL.contentEquals(sb)) {
                curStr = null
                curTc = TC_NULL
            } else {
                curStr = sb.toString()
                curTc = TC_OTHER
            }
        }

        private fun nextString() {
            sb.setLength(0)
            parse@ while(true) {
                nextChar()
                when (c2tc(curChar)) {
                    TC_EOF -> fail(charPos, "Unexpected end in string")
                    TC_STRING -> {
                        nextChar()
                        break@parse
                    }
                    TC_STRING_ESC -> {
                        nextChar()
                        require(curChar >= 0, charPos) { "Unexpected end after escape char" }
                        if (curChar == UNICODE_ESC.toInt()) {
                            sb.append(((hex() shl 12) + (hex() shl 8) + (hex() shl 4) + hex()).toChar())
                        } else {
                            val c = esc2c(curChar)
                            require(c != INVALID, charPos) { "Invalid escaped char '${curChar.toChar()}'" }
                            sb.append(c)
                        }
                    }
                    else -> sb.append(curChar.toChar())
                }
            }
            curStr = sb.toString()
            curTc = TC_STRING
        }

        private fun hex(): Int {
            nextChar()
            require(curChar >= 0, charPos) { "Unexpected end in unicode escape " }
            when (curChar.toChar()) {
                in '0'..'9' -> return curChar - '0'.toInt()
                in 'a'..'f' -> return curChar - 'a'.toInt() + 10
                in 'A'..'F' -> return curChar - 'A'.toInt() + 10
                else -> throw fail(charPos, "Invalid hex char '${curChar.toChar()}' in unicode escape")
            }
        }

        internal fun state(): String {
            return "Parser(charPos=$charPos, curChar=$curChar, tokenPos=$tokenPos, curTc=$curTc, curStr=$curStr)"
        }
    }
}
