/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.json.internal.CharMappings.CHAR_TO_TOKEN
import kotlinx.serialization.json.internal.CharMappings.ESCAPE_2_CHAR
import kotlin.jvm.*

internal const val lenientHint = "Use 'isLenient = true' in 'Json {}` builder to accept non-compliant JSON."
internal const val coerceInputValuesHint = "Use 'coerceInputValues = true' in 'Json {}` builder to coerce nulls to default values."
internal const val specialFlowingValuesHint =
    "It is possible to deserialize them using 'JsonBuilder.allowSpecialFloatingPointValues = true'"
internal const val ignoreUnknownKeysHint = "Use 'ignoreUnknownKeys = true' in 'Json {}' builder to ignore unknown keys."
internal const val allowStructuredMapKeysHint =
    "Use 'allowStructuredMapKeys = true' in 'Json {}' builder to convert such maps to [key1, value1, key2, value2,...] arrays."

// special strings
internal const val NULL = "null"

// special chars
internal const val COMMA = ','
internal const val COLON = ':'
internal const val BEGIN_OBJ = '{'
internal const val END_OBJ = '}'
internal const val BEGIN_LIST = '['
internal const val END_LIST = ']'
internal const val STRING = '"'
internal const val STRING_ESC = '\\'

internal const val INVALID = 0.toChar()
internal const val UNICODE_ESC = 'u'

// token classes
internal const val TC_OTHER: Byte = 0
internal const val TC_STRING: Byte = 1
internal const val TC_STRING_ESC: Byte = 2
internal const val TC_WHITESPACE: Byte = 3
internal const val TC_COMMA: Byte = 4
internal const val TC_COLON: Byte = 5
internal const val TC_BEGIN_OBJ: Byte = 6
internal const val TC_END_OBJ: Byte = 7
internal const val TC_BEGIN_LIST: Byte = 8
internal const val TC_END_LIST: Byte = 9
internal const val TC_EOF: Byte = 10
internal const val TC_INVALID: Byte = Byte.MAX_VALUE

// mapping from chars to token classes
private const val CTC_MAX = 0x7e

// mapping from escape chars real chars
private const val ESC2C_MAX = 0x75

private const val asciiCaseMask = 1 shl 5

// object instead of @SharedImmutable because there is mutual initialization in [initC2ESC] and [initC2TC]
internal object CharMappings {
    @JvmField
    val ESCAPE_2_CHAR = CharArray(ESC2C_MAX)

    @JvmField
    val CHAR_TO_TOKEN = ByteArray(CTC_MAX)

    init {
        initEscape()
        initCharToToken()
    }

    private fun initEscape() {
        for (i in 0x00..0x1f) {
            initC2ESC(i, UNICODE_ESC)
        }

        initC2ESC(0x08, 'b')
        initC2ESC(0x09, 't')
        initC2ESC(0x0a, 'n')
        initC2ESC(0x0c, 'f')
        initC2ESC(0x0d, 'r')
        initC2ESC('/', '/')
        initC2ESC(STRING, STRING)
        initC2ESC(STRING_ESC, STRING_ESC)
    }

    private fun initCharToToken() {
        for (i in 0..0x20) {
            initC2TC(i, TC_INVALID)
        }

        initC2TC(0x09, TC_WHITESPACE)
        initC2TC(0x0a, TC_WHITESPACE)
        initC2TC(0x0d, TC_WHITESPACE)
        initC2TC(0x20, TC_WHITESPACE)
        initC2TC(COMMA, TC_COMMA)
        initC2TC(COLON, TC_COLON)
        initC2TC(BEGIN_OBJ, TC_BEGIN_OBJ)
        initC2TC(END_OBJ, TC_END_OBJ)
        initC2TC(BEGIN_LIST, TC_BEGIN_LIST)
        initC2TC(END_LIST, TC_END_LIST)
        initC2TC(STRING, TC_STRING)
        initC2TC(STRING_ESC, TC_STRING_ESC)
    }

    private fun initC2ESC(c: Int, esc: Char) {
        if (esc != UNICODE_ESC) ESCAPE_2_CHAR[esc.toInt()] = c.toChar()
    }

    private fun initC2ESC(c: Char, esc: Char) = initC2ESC(c.toInt(), esc)

    private fun initC2TC(c: Int, cl: Byte) {
        CHAR_TO_TOKEN[c] = cl
    }

    private fun initC2TC(c: Char, cl: Byte) = initC2TC(c.toInt(), cl)
}

internal fun charToTokenClass(c: Char) = if (c.toInt() < CTC_MAX) CHAR_TO_TOKEN[c.toInt()] else TC_OTHER

internal fun escapeToChar(c: Int): Char = if (c < ESC2C_MAX) ESCAPE_2_CHAR[c] else INVALID

// Streaming JSON reader
internal class JsonLexer(private val source: String) {

    @JvmField
    var currentPosition: Int = 0 // position in source

    fun expectEof() {
        val nextToken = consumeNextToken()
        if (nextToken != TC_EOF)
            fail("Expected EOF, but had ${source[currentPosition - 1]} instead")
    }

    fun tryConsumeComma(): Boolean {
        val current = skipWhitespaces()
        if (current == source.length) return false
        if (source[current] == ',') {
            ++currentPosition
            return true
        }
        return false
    }

    fun canConsumeValue(): Boolean {
        var current = currentPosition
        while (current < source.length) {
            val c = source[current]
            // Inlined skipWhitespaces without field spill and nested loop. Also faster then char2TokenClass
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                ++current
                continue
            }
            currentPosition = current
            return isValidValueStart(c)
        }
        currentPosition = current
        return false
    }

    private fun isValidValueStart(c: Char): Boolean {
        return when (c) {
            '}', ']', ':', ',' -> false
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
    fun consumeNextToken(expected: Byte): Byte {
        val token = consumeNextToken()
        if (token != expected) {
            fail(expected)
        }
        return token
    }

    fun consumeNextToken(expected: Char) {
        val source = source
        while (currentPosition < source.length) {
            val c = source[currentPosition++]
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') continue
            if (c == expected) return
            unexpectedToken(expected)
        }
        unexpectedToken(expected) // EOF
    }

    private fun unexpectedToken(expected: Char) {
        --currentPosition // To properly handle null
        if (expected == STRING && consumeStringLenient() == NULL) {
            fail("Expected string literal but 'null' literal was found.\n$coerceInputValuesHint", currentPosition - 4)
        }
        fail(charToTokenClass(expected))
    }

    private fun fail(expectedToken: Byte) {
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
        val s = if (currentPosition == source.length || currentPosition <= 0) "EOF" else source[currentPosition - 1].toString()
        fail("Expected $expected, but had '$s' instead", currentPosition - 1)
    }

    fun peekNextToken(): Byte {
        val source = source
        while (currentPosition < source.length) {
            val ch = source[currentPosition]
            if (ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t') {
                ++currentPosition
                continue
            }
            return charToTokenClass(ch)
        }
        return TC_EOF
    }

    fun consumeNextToken(): Byte {
        val source = source
        while (currentPosition < source.length) {
            val ch = source[currentPosition++]
            return when (val tc = charToTokenClass(ch)) {
                TC_WHITESPACE -> continue
                else -> tc
            }
        }
        return TC_EOF
    }

    /**
     * Tries to consume `null` token from input.
     * Returns `true` if the next 4 chars in input are not `null`,
     * `false` otherwise and consumes it.
     */
    fun tryConsumeNotNull(): Boolean {
        val current = skipWhitespaces()
        // Cannot consume null due to EOF, maybe something else
        if (source.length - current < 4) return true
        for (i in 0..3) {
            if (NULL[i] != source[current + i]) return true
        }
        currentPosition = current + 4
        return false
    }

    private fun skipWhitespaces(): Int {
        var current = currentPosition
        // Skip whitespaces
        while (current < source.length) {
            val c = source[current]
            // Faster than char2TokenClass actually
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                ++current
            } else {
                break
            }
        }
        currentPosition = current
        return current
    }

    fun peekString(isLenient: Boolean): String? {
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
    fun consumeKeyString(): String {
        /*
         * For strings we assume that escaped symbols are rather an exception, so firstly
         * we optimistically scan for closing quote via intrinsified and blazing-fast 'indexOf',
         * than do our pessimistic check for backslash and fallback to slow-path if necessary.
         */
        consumeNextToken(STRING)
        val current = currentPosition
        val closingQuote = source.indexOf('"', current)
        if (closingQuote == -1) fail(TC_STRING)
        // Now we _optimistically_ know where the string ends (it might have been an escaped quote)
        for (i in current until closingQuote) {
            // Encountered escape sequence, should fallback to "slow" path and symmbolic scanning
            if (source[i] == STRING_ESC) {
                return consumeString(currentPosition, i)
            }
        }
        this.currentPosition = closingQuote + 1
        return source.substring(current, closingQuote)
    }

    fun consumeString(): String {
        if (peekedString != null) {
            return takePeeked()
        }

        return consumeKeyString()
    }

    private fun consumeString(startPosition: Int, current: Int): String {
        var currentPosition = current
        var lastPosition = startPosition
        val source = source
        var char = source[currentPosition] // Avoid two range checks visible in the profiler
        while (char != STRING) {
            if (char == STRING_ESC) {
                currentPosition = appendEscape(lastPosition, currentPosition)
                lastPosition = currentPosition
            } else if (++currentPosition >= source.length) {
                fail("EOF", currentPosition)
            }
            char = source[currentPosition]
        }

        val string = if (lastPosition == startPosition) {
            // there was no escaped chars
            source.substring(lastPosition, currentPosition)
        } else {
            // some escaped chars were there
            decodedString(lastPosition, currentPosition)
        }
        this.currentPosition = currentPosition + 1
        return string
    }

    private fun appendEscape(lastPosition: Int, current: Int): Int {
        escapedString.append(source, lastPosition, current)
        return appendEsc(current + 1)
    }

    private fun decodedString(lastPosition: Int, currentPosition: Int): String {
        appendRange(lastPosition, currentPosition)
        val result = escapedString.toString()
        escapedString.setLength(0)
        return result
    }

    private fun takePeeked(): String {
        return peekedString!!.also { peekedString = null }
    }

    // Allows to consume unquoted string
    fun consumeStringLenient(): String {
        if (peekedString != null) {
            return takePeeked()
        }
        var current = skipWhitespaces()
        // Skip leading quotation mark
        val token = charToTokenClass(source[current])
        if (token == TC_STRING) {
            return consumeString()
        }

        if (token != TC_OTHER) {
            fail("Expected beginning of the string, but got ${source[current]}")
        }
        while (current < source.length && charToTokenClass(source[current]) == TC_OTHER) {
            ++current
        }
        val result = source.substring(currentPosition, current)
        // Skip trailing quotation
        currentPosition = current
        return result
    }

    // initializes buf usage upon the first encountered escaped char
    private fun appendRange(fromIndex: Int, toIndex: Int) {
        escapedString.append(source, fromIndex, toIndex)
    }

    private fun appendEsc(startPosition: Int): Int {
        var currentPosition = startPosition
        val currentChar = source[currentPosition++]
        if (currentChar == UNICODE_ESC) {
            return appendHex(source, currentPosition)
        }

        val c = escapeToChar(currentChar.toInt())
        if (c == INVALID) fail("Invalid escaped char '$currentChar'")
        escapedString.append(c)
        return currentPosition
    }

    private fun appendHex(source: String, startPos: Int): Int {
        if (startPos + 4 >= source.length) fail("Unexpected EOF during unicode escape")
        escapedString.append(
            ((fromHexChar(source, startPos) shl 12) +
                    (fromHexChar(source, startPos + 1) shl 8) +
                    (fromHexChar(source, startPos + 2) shl 4) +
                    fromHexChar(source, startPos + 3)).toChar()
        )
        return startPos + 4
    }

    private fun fromHexChar(source: String, currentPosition: Int): Int {
        return when (val character = source[currentPosition]) {
            in '0'..'9' -> character.toInt() - '0'.toInt()
            in 'a'..'f' -> character.toInt() - 'a'.toInt() + 10
            in 'A'..'F' -> character.toInt() - 'A'.toInt() + 10
            else -> fail("Invalid toHexChar char '$character' in unicode escape")
        }
    }

    fun skipElement() {
        val tokenStack = mutableListOf<Byte>()
        var lastToken = peekNextToken()
        if (lastToken != TC_BEGIN_LIST && lastToken != TC_BEGIN_OBJ) {
            consumeStringLenient()
            return
        }
        while (true) {
            lastToken = consumeNextToken()
            when (lastToken) {
                TC_BEGIN_LIST, TC_BEGIN_OBJ -> {
                    tokenStack.add(lastToken)
                }
                TC_END_LIST -> {
                    if (tokenStack.last() != TC_BEGIN_LIST) throw JsonDecodingException(
                        currentPosition,
                        "found ] instead of }",
                        source
                    )
                    tokenStack.removeAt(tokenStack.size - 1)
                    if (tokenStack.size == 0) return
                }
                TC_END_OBJ -> {
                    if (tokenStack.last() != TC_BEGIN_OBJ) throw JsonDecodingException(
                        currentPosition,
                        "found } instead of ]",
                        source
                    )
                    tokenStack.removeAt(tokenStack.size - 1)
                    if (tokenStack.size == 0) return
                }
            }
        }
    }

    override fun toString(): String {
        return "JsonReader(source='$source', currentPosition=$currentPosition)"
    }

    fun failOnUnknownKey(key: String) {
        // At this moment we already have both key and semicolon (and whitespaces! consumed),
        // but still would like an error to point to the beginning of the key, so we are backtracking it
        val processed = source.substring(0, currentPosition)
        val lastIndexOf = processed.lastIndexOf(key)
        fail("Encountered an unknown key '$key'.\n$ignoreUnknownKeysHint", lastIndexOf)
    }

    fun fail(message: String, position: Int = currentPosition): Nothing {
        throw JsonDecodingException(position, message, source)
    }

    internal inline fun require(condition: Boolean, position: Int = currentPosition, message: () -> String) {
        if (!condition) fail(message(), position)
    }

    fun consumeNumericLiteral(): Long {
        /*
         * This is an optimized (~40% for numbers) version of consumeString().toLong()
         * that doesn't allocate and also doesn't support any radix but 10
         */
        var current = skipWhitespaces()
        if (current == source.length) fail("EOF")
        val hasQuotation = if (source[current] == STRING) {
            // Check it again
            if (++current == source.length) fail("EOF")
            true
        } else {
            false
        }
        var accumulator = 0L
        var isNegative = false
        val start = current
        var hasChars = true
        while (hasChars) {
            val ch: Char = source[current]
            if (ch == '-') {
                if (current != start) fail("Unexpected symbol '-' in numeric literal")
                isNegative = true
                ++current
                continue
            }
            val token = charToTokenClass(ch)
            if (token != TC_OTHER) break
            ++current
            hasChars = current != source.length
            val digit = ch - '0'
            if (digit !in 0..9) fail("Unexpected symbol '$ch' in numeric literal")
            accumulator = accumulator * 10 - digit
            if (accumulator > 0) fail("Numeric value overflow")
        }
        if (start == current || (isNegative && start == current - 1)) {
            fail("Expected numeric literal")
        }
        if (hasQuotation) {
            if (!hasChars) fail("EOF")
            if (source[current] != STRING) fail("Expected closing quotation mark")
            ++current
        }
        currentPosition = current
        return when {
            isNegative -> accumulator
            accumulator != Long.MIN_VALUE -> -accumulator
            else -> fail("Numeric value overflow")
        }
    }


    fun consumeBoolean(): Boolean {
        return consumeBoolean(skipWhitespaces())
    }

    fun consumeBooleanLenient(): Boolean {
        var current = skipWhitespaces()
        if (current == source.length) fail("EOF")
        val hasQuotation = if (source[current] == STRING) {
            ++current
            true
        } else {
            false
        }
        val result = consumeBoolean(current)
        if (hasQuotation) {
            if (currentPosition == source.length) fail("EOF")
            if (source[currentPosition] != STRING)
                fail("Expected closing quotation mark")
            ++currentPosition
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
        if (current == source.length) fail("EOF")
        return when (source[current++].toInt() or asciiCaseMask) {
            't'.toInt() -> {
                consumeBooleanLiteral("rue", current)
                true
            }
            'f'.toInt() -> {
                consumeBooleanLiteral("alse", current)
                false
            }
            else -> {
                fail("Expected valid boolean literal prefix, but had '${consumeStringLenient()}'")
            }
        }
    }

    private fun consumeBooleanLiteral(literalSuffix: String, current: Int) {
        if (source.length - current < literalSuffix.length) {
            fail("Unexpected end of boolean literal")
        }

        for (i in literalSuffix.indices) {
            val expected = literalSuffix[i]
            val actual = source[current + i]
            if (expected.toInt() != actual.toInt() or asciiCaseMask) {
                fail("Expected valid boolean literal prefix, but had '${consumeStringLenient()}'")
            }
        }

        currentPosition = current + literalSuffix.length
    }
}
