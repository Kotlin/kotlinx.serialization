/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*
import kotlin.test.*

/*
 * Test ensures that type that aggregate all basic (primitive/collection/maps/arrays)
 * types is properly serialized/deserialized with dummy format that supports only classes and primitives as
 * first-class citizens.
 */
class BasicTypesSerializationTest {

    // KeyValue Input/Output
    class KeyValueOutput(private val sb: StringBuilder) : AbstractEncoder() {
        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
            sb.append('{')
            return this
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            sb.append('}')
        }

        override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
            if (index > 0) sb.append(", ")
            sb.append(descriptor.getElementName(index))
            sb.append(':')
            return true
        }

        override fun encodeNull() {
            sb.append("null")
        }

        override fun encodeValue(value: Any) {
            sb.append(value)
        }

        override fun encodeString(value: String) {
            sb.append('"')
            sb.append(value)
            sb.append('"')
        }

        override fun encodeChar(value: Char) = encodeString(value.toString())
    }

    class KeyValueInput(private val inp: Parser) : AbstractDecoder() {
        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
            inp.expectAfterWhiteSpace('{')
            return this
        }

        override fun endStructure(descriptor: SerialDescriptor) = inp.expectAfterWhiteSpace('}')

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            inp.skipWhitespace(',')
            val name = inp.nextUntil(':', '}')
            if (name.isEmpty())
                return CompositeDecoder.DECODE_DONE
            val index = descriptor.getElementIndexOrThrow(name)
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

        override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
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

    // Very simple char-by-char parser
    class Parser(private val inp: StringReader) {
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

        fun nextUntil(vararg c: Char): String {
            val sb = StringBuilder()
            while (cur >= 0 && cur.toChar() !in c) {
                sb.append(cur.toChar())
                next()
            }
            return sb.toString()
        }
    }


    class StringReader(val str: String) {
        private var position: Int = 0
        fun read(): Int = when (position) {
            str.length -> -1
            else -> str[position++].toInt()
        }
    }

    @Test
    fun testKvSerialization() {
        // serialize to string
        val sb = StringBuilder()
        val out = KeyValueOutput(sb)
        out.encodeSerializableValue(TypesUmbrella.serializer(), umbrellaInstance)
        // deserialize from string
        val str = sb.toString()
        val inp = KeyValueInput(Parser(StringReader(str)))
        val other = inp.decodeSerializableValue(TypesUmbrella.serializer())
        // assert we've got it back from string
        assertEquals(umbrellaInstance, other)
        assertNotSame(umbrellaInstance, other)
    }
}
