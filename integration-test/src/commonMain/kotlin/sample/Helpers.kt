/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package sample

import kotlinx.io.PrintWriter
import kotlinx.io.Reader
import kotlinx.serialization.*
import kotlinx.serialization.internal.EnumDescriptor

class KeyValueOutput(val out: PrintWriter) : ElementValueEncoder() {
    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
        out.print('{')
        return this
    }

    override fun endStructure(desc: SerialDescriptor) = out.print('}')

    override fun encodeElement(desc: SerialDescriptor, index: Int): Boolean {
        if (index > 0) out.print(", ")
        out.print(desc.getElementName(index));
        out.print(':')
        return true
    }

    override fun encodeNull() = out.print("null")
    override fun encodeValue(value: Any) = out.print(value)

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

    override fun endStructure(desc: SerialDescriptor) = inp.expectAfterWhiteSpace('}')

    override fun decodeElementIndex(desc: SerialDescriptor): Int {
        inp.skipWhitespace(',')
        val name = inp.nextUntil(':', '}')
        if (name.isEmpty())
            return CompositeDecoder.READ_DONE
        val index = desc.getElementIndexOrThrow(name)
        inp.expect(':')
        return index
    }

    private fun readToken(): String {
        inp.skipWhitespace()
        return inp.nextUntil(' ', ',', '}')
    }

    override fun decodeNotNullMark(): Boolean {
        inp.skipWhitespace()
        if (inp.cur != 'n'.toInt()) return true
        return false
    }

    override fun decodeNull(): Nothing? {
        check(readToken() == "null") { "'null' expected" }
        return null
    }

    override fun decodeBoolean(): Boolean = readToken() == "true"
    override fun decodeByte(): Byte = readToken().toByte()
    override fun decodeShort(): Short = readToken().toShort()
    override fun decodeInt(): Int = readToken().toInt()
    override fun decodeLong(): Long = readToken().toLong()
    override fun decodeFloat(): Float = readToken().toFloat()
    override fun decodeDouble(): Double = readToken().toDouble()

    override fun decodeEnum(enumDescription: EnumDescriptor): Int {
        return readToken().toInt()
    }

    override fun decodeString(): String {
        inp.expectAfterWhiteSpace('"')
        val value = inp.nextUntil('"')
        inp.expect('"')
        return value
    }

    override fun decodeChar(): Char = decodeString().single()
}

// Parser

// Very simple char-by-char parser
class Parser(private val inp: Reader) {
    var cur: Int = inp.read()

    fun next() {
        cur = inp.read()
    }

    fun skipWhitespace(vararg c: Char) {
        while (cur >= 0 && (cur.toChar().isWhitespace() || cur.toChar() in c))
            next()
    }

    fun expect(c: Char) {
        check(cur == c.toInt()) { "Expected '$c'" }
        next()
    }

    fun expectAfterWhiteSpace(c: Char) {
        skipWhitespace()
        expect(c)
    }

    fun  nextUntil(vararg c: Char): String {
        val sb = StringBuilder()
        while (cur >= 0 && cur.toChar() !in c) {
            sb.append(cur.toChar())
            next()
        }
        return sb.toString()
    }
}
