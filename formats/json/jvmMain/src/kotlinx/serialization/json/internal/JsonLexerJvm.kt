/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import java.io.*
import java.nio.charset.*
import java.util.ArrayDeque

private typealias ReaderLike = SequenceReader

internal actual fun JsonLexer(s: String): JsonLexer = JsonLexerJvm(s)

/**
 * From Apache commons-io
 */
internal class SequenceReader(val readers: ArrayDeque<Reader>) : Reader() {
    constructor(vararg readers: Reader) : this(ArrayDeque(readers.asList()))

    private var reader: Reader? = null

    init {
        reader = nextReader()
    }

    override fun close() {
        readers.forEach { it.close() }
        reader?.close()
        reader = null
    }

    private fun nextReader(): Reader? {
        return readers.pollFirst()
    }

    fun prepend(r: Reader) {
        if (reader != null) readers.addFirst(reader!!)
        reader = r
    }

    override fun read(): Int {
        var c: Int = -1
        while (reader != null) {
            c = reader!!.read()
            if (c != -1) {
                break
            }
            reader = nextReader()
        }
        return c
    }

    @Suppress("NAME_SHADOWING")
    override fun read(cbuf: CharArray, off: Int, len: Int): Int {
        var off = off
        var len = len
        if (len < 0 || off < 0 || off + len > cbuf.size) {
            throw IndexOutOfBoundsException("Array Size=" + cbuf.size + ", offset=" + off + ", length=" + len)
        }
        var count = 0
        while (reader != null) {
            val readLen = reader!!.read(cbuf, off, len)
            if (readLen == -1) {
                reader = nextReader()
            } else {
                count += readLen
                off += readLen
                len -= readLen
                if (len <= 0) {
                    break
                }
            }
        }
        return if (count > 0) {
            count
        } else -1
    }
}

// do not forget to call lastRead = source.read() after!
internal fun ReaderLike.prepend(s: String): ReaderLike = apply { prepend(StringReader(s)) }

internal fun ReaderLike.readExactChars(n: Int): String {
    val c = CharArray(n)
    var read = 0
    while (read != n) {
        val actual = read(c, read, n - read)
        if (actual == -1) break
        read += actual
    }
    return String(c, 0, read)
}

// Streaming JSON reader
internal class JsonLexerJvm(private var source: ReaderLike): JsonLexer {

    constructor(s: String) : this(SequenceReader(StringReader(s).buffered()))
    constructor(i: InputStream, charset: Charset = Charsets.UTF_8) : this(SequenceReader(i.bufferedReader(charset)))

    private fun advance() {
        lastRead = source.read()
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun Int.isWs() = this == ' '.code || this == '\n'.code || this == '\r'.code || this == '\t'.code

    @JvmField
    var lastRead: Int = source.read()

    override fun expectEof() {
        val nextToken = consumeNextToken()
        if (nextToken != TC_EOF)
            fail("Expected EOF, but had ${lastRead.toChar()} instead")
    }

    override fun tryConsumeComma(): Boolean {
        val current = skipWhitespaces()
        if (current == -1) return false
        if (current == ','.code) {
            advance()
            return true
        }
        return false
    }

    override fun canConsumeValue(): Boolean {
        var current = lastRead
        while (current != -1) {
            // Inlined skipWhitespaces without field spill and nested loop. Also faster then char2TokenClass
            if (current.isWs()) {
                current = source.read()
                continue
            }
            lastRead = current
            return isValidValueStart(current)
        }
        lastRead = current
        return false
    }

    private fun isValidValueStart(c: Int): Boolean {
        return when (c) {
            '}'.code, ']'.code, ':'.code, ','.code -> false
            else -> true
        }
    }

    /*
     * Peeked string for coerced enums.
     * If the value was picked, 'consumeString' will take it without scanning the source.
     */
    private var peekedString: String? = null
    private var escapedString = StringBuilder()

    // TODO consider replacing usages of this method in JsonParser with char overload
    // CAN BE EXTENSION!
    override fun consumeNextToken(expected: Byte): Byte {
        val token = consumeNextToken()
        if (token != expected) {
            fail(expected)
        }
        return token
    }

    override fun consumeNextToken(expected: Char) {
        var current = lastRead
        while (current != -1) {
            val prev = current
            current = source.read()
            if (prev.isWs()) {
                continue
            }
            lastRead = current
            if (prev == expected.code) return
            unexpectedToken(expected)
        }
        lastRead = current
        unexpectedToken(expected) // EOF
    }

    private fun unexpectedToken(expected: Char) {
//        --currentPosition // To properly handle null
        if (expected == STRING && consumeStringLenient() == NULL) {
            fail("Expected string literal but 'null' literal was found.\n$coerceInputValuesHint" /*currentPosition - 4*/)
        }
        fail(charToTokenClass(expected.code))
    }

    private fun fail(expectedToken: Byte): Nothing {
        // We know that the token was consumed prior to this call
        // Slow path, never called in normal code, can avoid optimizing it
        val expected = when (expectedToken) {
            TC_STRING -> "quotation mark '\"'"
            TC_COMMA -> "comma ','"
            TC_COLON -> "semicolon ':'"
            TC_BEGIN_OBJ -> "start of the object '{'"
            TC_END_OBJ -> "end of the object '}'"
            TC_BEGIN_LIST -> "start of the array '['"
            TC_END_LIST -> "end of the array ']'"
            else -> "valid token" // should never happen
        }
        val s = if (lastRead == -1) "EOF" else lastRead.toChar().toString()
        fail("Expected $expected, but had '$s' instead" /*currentPosition - 1*/)
    }

    override fun peekNextToken(): Byte {
        var current = lastRead
        while (current != -1) {
//            val ch = source[currentPosition]
            if (current.isWs()) {
                current = source.read()
                continue
            }
            lastRead = current
            return charToTokenClass(current)
        }
        lastRead = current
        return TC_EOF
    }

    override fun consumeNextToken(): Byte {
        var current = lastRead
        while (current != -1) {
            val prev = current
            current = source.read()
            if (prev.isWs()) {
                continue
            }
            lastRead = current
            return charToTokenClass(prev)
        }
        lastRead = current
        return TC_EOF
    }

    /**
     * Tries to consume `null` token from input.
     * Returns `true` if the next 4 chars in input are not `null`,
     * `false` otherwise and consumes it.
     */
    override fun tryConsumeNotNull(): Boolean {
        skipWhitespaces()
        val maybeNull = lastRead.toChar().toString() + source.readExactChars(3)
        // Cannot consume null due to EOF, maybe something else
        if (maybeNull.length < 4 || NULL != maybeNull) {
            source = source.prepend(maybeNull)
            advance()
            return true
        }
        lastRead = source.read()
        return false
    }

    private fun skipWhitespaces(): Int {
        var c = lastRead
        // Skip whitespaces
        while (c != -1) {
            // Faster than char2TokenClass actually
            if (c.isWs()) {
                c = source.read()
                continue
            } else {
                break
            }
        }
        lastRead = c
        return c
    }

    override fun peekString(isLenient: Boolean): String? {
        val token = peekNextToken()
        val string = if (isLenient) {
            if (token != TC_STRING && token != TC_OTHER) return null
            consumeStringLenient()
        } else {
            if (token != TC_STRING) return null
            consumeString()
        }
        peekedString = string
        return string
    }

    /*
     * This method is a copy of consumeString, but used for key of json objects, so there
     * is no need to lookup peeked string.
     */
    override fun consumeKeyString(): String {
        consumeNextToken(STRING)
//      No indexOf here, so no fast path
        var char = lastRead
        while (char != STRING.code) {
            when (char) {
                STRING_ESC.code -> {
                    appendEscape()
                }
                -1 -> {
                    fail("EOF" /*currentPosition*/)
                }
                else -> {
                    escapedString.append(char.toChar())
                }
            }
            char = source.read()
        }
        val result = escapedString.toString()
        lastRead = source.read()
        escapedString.setLength(0)
        return result
    }

    override fun consumeString(): String {
        if (peekedString != null) {
            return takePeeked()
        }
        return consumeKeyString()
    }

    private fun takePeeked(): String {
        return peekedString!!.also { peekedString = null }
    }

    // Allows to consume unquoted string
    override fun consumeStringLenient(): String {
        if (peekedString != null) {
            return takePeeked()
        }
        var current = skipWhitespaces()
        if (current == -1) fail("EOF", current)
        // Skip leading quotation mark
//        current = source.read()
        lastRead = current
        val token = charToTokenClass(lastRead)
        if (token == TC_STRING) {
            return consumeString()
        }

        if (token != TC_OTHER) {
            fail("Expected beginning of the string, but got ${token}")
        }
        while (current != -1 && charToTokenClass(current) == TC_OTHER) {
            escapedString.append(current.toChar())
            current = source.read()
        }
        lastRead = current
        val result = escapedString.toString()
        escapedString.setLength(0)
        // Skip trailing quotation
        return result
    }

    private fun appendEscape(): Int {
        val currentChar = source.read()
        if (currentChar == UNICODE_ESC.code) {
            return appendHex()
        }

        val c = escapeToChar(currentChar)
        if (c == INVALID) fail("Invalid escaped char '$currentChar'")
        escapedString.append(c)
        return currentChar
    }

    private fun appendHex(): Int {
        val src = source.readExactChars(4)
        if (src.length != 4) fail("Unexpected EOF during unicode escape")
        val startPos = 0
        escapedString.append(
            ((fromHexChar(src, startPos) shl 12) +
                    (fromHexChar(src, startPos + 1) shl 8) +
                    (fromHexChar(src, startPos + 2) shl 4) +
                    fromHexChar(src, startPos + 3)).toChar()
        )
        return startPos + 4
    }

    private fun fromHexChar(source: String, currentPosition: Int): Int {
        return when (val character = source[currentPosition]) {
            in '0'..'9' -> character.code - '0'.code
            in 'a'..'f' -> character.code - 'a'.code + 10
            in 'A'..'F' -> character.code - 'A'.code + 10
            else -> fail("Invalid toHexChar char '$character' in unicode escape")
        }
    }

    override fun skipElement(allowLenientStrings: Boolean) {
        val tokenStack = mutableListOf<Byte>()
        var lastToken = peekNextToken()
        if (lastToken != TC_BEGIN_LIST && lastToken != TC_BEGIN_OBJ) {
            consumeStringLenient()
            return
        }
        while (true) {
            lastToken = peekNextToken()
            if (lastToken == TC_STRING) {
                if (allowLenientStrings) consumeStringLenient() else consumeKeyString()
                continue
            }
            when (lastToken) {
                TC_BEGIN_LIST, TC_BEGIN_OBJ -> {
                    tokenStack.add(lastToken)
                }
                TC_END_LIST -> {
                    if (tokenStack.last() != TC_BEGIN_LIST) throw JsonDecodingException(
//                        -1,
                        "found ] instead of }",
//                        source
                    )
                    tokenStack.removeLast()
                }
                TC_END_OBJ -> {
                    if (tokenStack.last() != TC_BEGIN_OBJ) throw JsonDecodingException(
//                        currentPosition,
                        "found } instead of ]",
//                        source
                    )
                    tokenStack.removeLast()
                }
                TC_EOF -> fail("Unexpected end of input due to malformed JSON during ignoring unknown keys")
            }
            consumeNextToken()
            if (tokenStack.size == 0) return
        }
    }

    override fun toString(): String {
        return "JsonReader(source='$source')"
    }

    override fun failOnUnknownKey(key: String) {
        // At this moment we already have both key and semicolon (and whitespaces! consumed),
        // but still would like an error to point to the beginning of the key, so we are backtracking it
//        val processed = source.substring(0, currentPosition)
//        val lastIndexOf = processed.lastIndexOf(key)
        fail("Encountered an unknown key '$key'.\n$ignoreUnknownKeysHint" /*lastIndexOf*/)
    }

    override fun fail(message: String, position: Int): Nothing {
        throw JsonDecodingException(position, message)
    }

    internal inline fun require(condition: Boolean, position: Int = -1, message: () -> String) {
        if (!condition) fail(message(), position)
    }

    override fun consumeNumericLiteral(): Long {
        /*
         * This is an optimized (~40% for numbers) version of consumeString().toLong()
         * that doesn't allocate and also doesn't support any radix but 10
         */
        var current = skipWhitespaces()
        if (current == -1) fail("EOF")
        val hasQuotation = if (current == STRING.code) {
            // Check it again
            current = source.read()
            if (current == -1) fail("EOF")
            true
        } else {
            false
        }
        var accumulator = 0L
        var isNegative = false
        var readChars = 0
        var hasChars = true
        while (hasChars) {
            val ch = current
            if (ch == '-'.code) {
                if (readChars != 0) fail("Unexpected symbol '-' in numeric literal")
                isNegative = true
                current = source.read()
                readChars++
                continue
            }

            val token = charToTokenClass(current)
            if (token != TC_OTHER) break
            current = source.read()
            readChars++
            hasChars = current != -1
            val digit = ch - '0'.code
            if (digit !in 0..9) fail("Unexpected symbol '$ch' in numeric literal")
            accumulator = accumulator * 10 - digit
            if (accumulator > 0) fail("Numeric value overflow")
        }
        if (readChars == 0 || (isNegative && readChars == 1)) {
            fail("Expected numeric literal")
        }
        if (hasQuotation) {
            if (!hasChars) fail("EOF")
            if (current != STRING.code) fail("Expected closing quotation mark")
            current = source.read()
        }
        lastRead = current
        return when {
            isNegative -> accumulator
            accumulator != Long.MIN_VALUE -> -accumulator
            else -> fail("Numeric value overflow")
        }
    }


    override fun consumeBoolean(): Boolean {
        return consumeBoolean(skipWhitespaces())
    }

    override fun consumeBooleanLenient(): Boolean {
        var current = skipWhitespaces()
        if (current == -1) fail("EOF")
        val hasQuotation = if (current == STRING.code) {
            current = source.read()
            true
        } else {
            false
        }
        val result = consumeBoolean(current)
        if (hasQuotation) {
            if (lastRead == -1) fail("EOF")
            if (lastRead != STRING.code)
                fail("Expected closing quotation mark")
            advance()
        }
        return result
    }

    private fun consumeBoolean(start: Int): Boolean {
        /*
         * In ASCII representation, upper and lower case letters are different
         * in 6-th bit and we leverage this fact, our implementation consumes boolean literals
         * in a case-insensitive manner.
         */
        var current = start
        if (current == -1) fail("EOF")
        return when (current or asciiCaseMask) {
            't'.code -> {
                consumeBooleanLiteral("rue")
                true
            }
            'f'.code -> {
                consumeBooleanLiteral("alse")
                false
            }
            else -> {
                fail("Expected valid boolean literal prefix, but had '${consumeStringLenient()}'")
            }
        }
    }

    private fun consumeBooleanLiteral(literalSuffix: String/*, current: Int*/) {
        val res = source.readExactChars(literalSuffix.length)
        if (res.length < literalSuffix.length) {
            fail("Unexpected end of boolean literal")
        }

        for (i in literalSuffix.indices) {
            val expected = literalSuffix[i]
            val actual = res[i]
            if (expected.code != actual.code or asciiCaseMask) {
                fail("Expected valid boolean literal prefix, but had '${consumeStringLenient()}'")
            }
        }

        lastRead = source.read()
    }
}
