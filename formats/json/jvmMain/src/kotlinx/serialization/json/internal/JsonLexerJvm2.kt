package kotlinx.serialization.json.internal

import java.io.*
import java.nio.charset.Charset

internal class JsonLexerJvm2(private val reader: Reader) : JsonLexer {
    constructor(i: InputStream, charset: Charset = Charsets.UTF_8) : this(i.bufferedReader(charset))
    constructor(s: String) : this(StringReader(s))

    //    @JvmField
    private var currentPosition: Int = 0 // position in source

    private val batchSize = 2 * DEFAULT_BUFFER_SIZE
    private var threshold: Int = 1024 // chars
    private var source = CharArray(batchSize + threshold)

    init {
        preload(0)
    }

    val CharArray.length: Int get() = size

    fun CharArray.substring(fromIndex: Int, endIndex: Int): String {
        return String(this, fromIndex, endIndex - fromIndex)
    }

    fun CharArray.indexOf(char: Char, from: Int): Int {
        val src = this
        for (i in from until src.size) {
            if (src[i] == char) return i
        }
        return -1
    }

    fun preload(spaceLeft: Int) {
        val buffer = source
        System.arraycopy(buffer, currentPosition, buffer, 0, spaceLeft)
        var read = spaceLeft
        val sizeTotal = batchSize + threshold
        while (read != sizeTotal) {
            val actual = reader.read(buffer, read, sizeTotal - read)
            if (actual == -1) break
            read += actual
        }
        if (read != sizeTotal) {
            // EOF, resizing the array so it matches input size
            // todo: remove this crutch
            source = source.copyOf(read)
            threshold = -1
        }
        currentPosition = 0
    }

    fun ensureHaveChars() {
        val cur = currentPosition
        val oldSize = source.size
        val spaceLeft = oldSize - cur
        if (spaceLeft > threshold) return
        // warning: current position is not updated during string consumption
        // resizing
        preload(spaceLeft)
    }

    override fun expectEof() {
        val nextToken = consumeNextToken()
        if (nextToken != TC_EOF)
            fail("Expected EOF, but had ${source[currentPosition - 1]} instead")
    }

    override fun tryConsumeComma(): Boolean {
        val current = skipWhitespaces()
        if (current == source.length) return false
        if (source[current] == ',') {
            ++currentPosition
            return true
        }
        return false
    }

    override fun canConsumeValue(): Boolean {
        ensureHaveChars()
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
    override fun consumeNextToken(expected: Byte): Byte {
        val token = consumeNextToken()
        if (token != expected) {
            fail(expected)
        }
        return token
    }

    override fun consumeNextToken(expected: Char) {
        ensureHaveChars()
        val source = source
        var cpos = currentPosition
        while (cpos < source.length) {
            val c = source[cpos++]
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') continue
            currentPosition = cpos
            if (c == expected) return
            unexpectedToken(expected)
        }
        currentPosition = cpos
        unexpectedToken(expected) // EOF
    }

    private fun unexpectedToken(expected: Char) {
        --currentPosition // To properly handle null
        if (expected == STRING && consumeStringLenient() == NULL) {
            fail("Expected string literal but 'null' literal was found.\n$coerceInputValuesHint", currentPosition - 4)
        }
        fail(charToTokenClass(expected.code))
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

    override fun peekNextToken(): Byte {
        val source = source
        while (currentPosition < source.length) {
            val ch = source[currentPosition]
            if (ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t') {
                ++currentPosition
                continue
            }
            return charToTokenClass(ch.code)
        }
        return TC_EOF
    }

    override fun consumeNextToken(): Byte {
        ensureHaveChars()
        val source = source
        while (currentPosition < source.length) {
            val ch = source[currentPosition++]
            return when (val tc = charToTokenClass(ch.code)) {
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
    override fun tryConsumeNotNull(): Boolean {
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
        ensureHaveChars()
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

    override fun consumeString(): String {
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
        escapedString.append(source, lastPosition, current - lastPosition)
//        escapedString.append("", lastPosition, current)
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
    override fun consumeStringLenient(): String {
        if (peekedString != null) {
            return takePeeked()
        }
        var current = skipWhitespaces()
        if (current >= source.length) fail("EOF", current)
        // Skip leading quotation mark
        val token = charToTokenClass(source[current].code)
        if (token == TC_STRING) {
            return consumeString()
        }

        if (token != TC_OTHER) {
            fail("Expected beginning of the string, but got ${source[current]}")
        }
        while (current < source.length && charToTokenClass(source[current].code) == TC_OTHER) {
            ++current
        }
        val result = source.substring(currentPosition, current)
        // Skip trailing quotation
        currentPosition = current
        return result
    }

    // initializes buf usage upon the first encountered escaped char
    private fun appendRange(fromIndex: Int, toIndex: Int) {
        escapedString.append(source, fromIndex, toIndex - fromIndex)
    }

    private fun appendEsc(startPosition: Int): Int {
        var currentPosition = startPosition
        val currentChar = source[currentPosition++]
        if (currentChar == UNICODE_ESC) {
            return appendHex(source, currentPosition)
        }

        val c = escapeToChar(currentChar.code)
        if (c == INVALID) fail("Invalid escaped char '$currentChar'")
        escapedString.append(c)
        return currentPosition
    }

    private fun appendHex(source: CharArray, startPos: Int): Int {
        if (startPos + 4 >= source.length) fail("Unexpected EOF during unicode escape")
        escapedString.append(
            ((fromHexChar(source, startPos) shl 12) +
                    (fromHexChar(source, startPos + 1) shl 8) +
                    (fromHexChar(source, startPos + 2) shl 4) +
                    fromHexChar(source, startPos + 3)).toChar()
        )
        return startPos + 4
    }

    private fun fromHexChar(source: CharArray, currentPosition: Int): Int {
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
                        currentPosition,
                        "found ] instead of }",
                        /*source*/
                    )
                    tokenStack.removeLast()
                }
                TC_END_OBJ -> {
                    if (tokenStack.last() != TC_BEGIN_OBJ) throw JsonDecodingException(
                        currentPosition,
                        "found } instead of ]",
                        /*source*/
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
        return "JsonReader(source='$source', currentPosition=$currentPosition)"
    }

    override fun failOnUnknownKey(key: String) {
        // At this moment we already have both key and semicolon (and whitespaces! consumed),
        // but still would like an error to point to the beginning of the key, so we are backtracking it
        val processed = source.substring(0, currentPosition)
        val lastIndexOf = processed.lastIndexOf(key)
        fail("Encountered an unknown key '$key'.\n$ignoreUnknownKeysHint", lastIndexOf)
    }

    // todo: use currentPosition instead of -1
    override fun fail(message: String, position: Int): Nothing {
        throw JsonDecodingException(position, message, /*source*/)
    }

    override fun consumeNumericLiteral(): Long {
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
            val token = charToTokenClass(ch.code)
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


    override fun consumeBoolean(): Boolean {
        return consumeBoolean(skipWhitespaces())
    }

    override fun consumeBooleanLenient(): Boolean {
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
        return when (source[current++].code or asciiCaseMask) {
            't'.code -> {
                consumeBooleanLiteral("rue", current)
                true
            }
            'f'.code -> {
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
            if (expected.code != actual.code or asciiCaseMask) {
                fail("Expected valid boolean literal prefix, but had '${consumeStringLenient()}'")
            }
        }

        currentPosition = current + literalSuffix.length
    }
}

