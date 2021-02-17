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

/*
 * In ASCII representation, upper and lower case letters are different
 * in 6-th bit and we leverage this fact
 */
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
internal class JsonReader(private val source: String) {

    @JvmField
    var currentPosition: Int = 0 // position in source

    // TODO this one should be built-in assert
    public val isDone: Boolean get() = consumeNextToken() == TC_EOF

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
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                ++current
                continue
            }
            val tc = charToTokenClass(c)
            currentPosition = current
            return tc == TC_STRING || tc == TC_OTHER || tc == TC_BEGIN_LIST || tc == TC_BEGIN_OBJ
        }
        currentPosition = current
        return false
    }

    /*
     * Peeked string for coerced enums.
     * If the value was picked, 'consumeString' will take it without scanning the source.
     */
    private var peekedString: String? = null
    private var length = 0 // length of string
    private var buf = CharArray(16) // only used for strings with escapes

    fun consumeNextToken(expected: Byte): Byte {
        val token = consumeNextToken()
        if (token != expected) {
            fail(expected)
        }
        return token
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

        fail("Expected $expected, but had '${source[currentPosition - 1]}' instead", currentPosition)
    }

    fun peekNextToken(): Byte {
        val source = source
        while (currentPosition < source.length) {
            val ch = source[currentPosition]
            return when (val tc = charToTokenClass(ch)) {
                TC_WHITESPACE -> {
                    ++currentPosition
                    continue
                }
                else -> tc
            }
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

    private fun failBeginningOfTheString() {
        // Try to guess if it's null for better error message
        --currentPosition
        if (!tryConsumeNotNull()) {
            fail("Expected string literal but 'null' literal was found.\n$coerceInputValuesHint", currentPosition - 4)
        } else {
            val symbol = if (++currentPosition == source.length) "EOF" else source[currentPosition]
            fail("Expected string literal but had $symbol instead")
        }
    }

    fun consumeString(): String {
        if (peekedString != null) {
            return takePeeked()
        }

        if (consumeNextToken() != TC_STRING) {
            failBeginningOfTheString()
        }
        var currentPosition = currentPosition
        if (currentPosition >= source.length) {
            fail("EOF", currentPosition)
        }
        val startPosition = currentPosition - 1
        var lastPosition = currentPosition
        length = 0
        var char = source[currentPosition] // Avoid two double checks visible in the profiler
        while (char != STRING) {
            if (char == STRING_ESC) {
                appendRange(source, lastPosition, currentPosition)
                val newPosition = appendEsc(source, currentPosition + 1)
                currentPosition = newPosition
                lastPosition = newPosition
            } else if (++currentPosition >= source.length) {
                fail("EOF", currentPosition)
            }
            char = source[currentPosition]
        }

        val string = if (lastPosition == startPosition + 1) {
            // there was no escaped chars
            source.substring(lastPosition, currentPosition)
        } else {
            // some escaped chars were there
            appendRange(source, lastPosition, currentPosition)
            buf.concatToString(0, length)
        }
        this.currentPosition = currentPosition + 1
        return string
    }

    private fun takePeeked(): String {
        return peekedString!!.also { peekedString = null }
    }

    /*
     * This method is a copy of consumeString, but used for key of json objects.
     * For them we know that escape symbols are _very_ unlikely and can optimistically do
     * quotation lookup via `indexOf` (which is a vectorized intrinsic), then substring and
     * `indexOf` for escape symbol. It works almost 20% faster for both large and small JSON strings.
     */
    fun consumeKeyString(): String {
        consumeNextToken(TC_STRING)
        val current = currentPosition
        val closingQuote = source.indexOf('"', current)
        if (closingQuote == -1) fail(TC_STRING) // Better error message?
        // TODO explain
        for (i in current until closingQuote) {
            // Encountered escape sequence, should fallback to "slow" path
            if (source[i] == '\\') TODO()
        }
        this.currentPosition = closingQuote + 1
        return source.substring(current, closingQuote)
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

    private fun append(ch: Char) {
        if (length >= buf.size) buf = buf.copyOf(2 * buf.size)
        buf[length++] = ch
    }

    // initializes buf usage upon the first encountered escaped char
    private fun appendRange(source: String, fromIndex: Int, toIndex: Int) {
        val addLength = toIndex - fromIndex
        val oldLength = length
        val newLength = oldLength + addLength
        if (newLength > buf.size) buf = buf.copyOf(newLength.coerceAtLeast(2 * buf.size))
        for (i in 0 until addLength) buf[oldLength + i] = source[fromIndex + i]
        length += addLength
    }

    private fun appendEsc(source: String, startPosition: Int): Int {
        var currentPosition = startPosition
        require(currentPosition < source.length, currentPosition) { "Unexpected EOF after escape character" }
        val currentChar = source[currentPosition++]
        if (currentChar == UNICODE_ESC) {
            return appendHex(source, currentPosition)
        }

        val c = escapeToChar(currentChar.toInt())
        require(c != INVALID, currentPosition) { "Invalid escaped char '$currentChar'" }
        append(c)
        return currentPosition
    }

    private fun appendHex(source: String, startPos: Int): Int {
        var curPos = startPos
        append(
            ((fromHexChar(source, curPos++) shl 12) +
                    (fromHexChar(source, curPos++) shl 8) +
                    (fromHexChar(source, curPos++) shl 4) +
                    fromHexChar(source, curPos++)).toChar()
        )
        return curPos
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

    private fun fromHexChar(source: String, currentPosition: Int): Int {
        require(currentPosition < source.length, currentPosition) { "Unexpected EOF during unicode escape" }
        return when (val curChar = source[currentPosition]) {
            in '0'..'9' -> curChar.toInt() - '0'.toInt()
            in 'a'..'f' -> curChar.toInt() - 'a'.toInt() + 10
            in 'A'..'F' -> curChar.toInt() - 'A'.toInt() + 10
            else -> fail("Invalid toHexChar char '$curChar' in unicode escape")
        }
    }

    //    fun consumeBoolean(allowQuotation: Boolean): Boolean {
//        skipWhitespaces()
//        var current = currentPosition
////        var hasQuote = false
////        if (allowQuotation && source[current] == STRING) {
////            hasQuote = true
////            ++current
////        }
//
//        // TODO handle EOF
//        val result = when (source[current++].toInt() or asciiCaseMask) {
//            't'.toInt() -> {
//                if (source.length - current < 3) fail("")
//                val r = source[current + 0].toInt() or asciiCaseMask == 'r'.toInt()
//                val u = source[current + 1].toInt() or asciiCaseMask == 'u'.toInt()
//                val e = source[current + 2].toInt() or asciiCaseMask == 'e'.toInt()
//                if (!(r and u and e)) fail("")
//
////                for ((i, c) in "rue".withIndex()) {
////                    if (c.toInt() != source[current + i].toInt() or asciiCaseMask) {
////                        fail("")
////                    }
////                }
//                currentPosition += 4
//                true
//            }
//            'f'.toInt() -> {
//                if (source.length - current < 4) fail("")
//                val a = source[current + 0].toInt() or asciiCaseMask == 'a'.toInt()
//                val l = source[current + 1].toInt() or asciiCaseMask == 'l'.toInt()
//                val s = source[current + 2].toInt() or asciiCaseMask == 's'.toInt()
//                val e = source[current + 3].toInt() or asciiCaseMask == 'e'.toInt()
//                if (!(a and l and s and e)) fail("")
////                for ((i, c) in "alse".withIndex()) {
////                    if (c.toInt() != source[current + i].toInt() or asciiCaseMask) {
////                        fail("")
////                    }
////                }
//                currentPosition += 5
//                false
//            }
//            else -> TODO()
//        }
//
////        if (hasQuote) {
////
////        }
//        return result
//    }
}
