package kotlinx.serialization.properties

/**
 * Converts string to [Map<String,String] in accordance
 * with [java.util.Properties](https://docs.oracle.com/javase/7/docs/api/java/util/Properties.html#load(java.io.Reader)).
 *
 * @receiver   String to pares
 * @throws     IllegalArgumentException if the input stream contains a
 * malformed Unicode escape sequence.
 */
internal fun String.decodeAsProperties(): Map<String, String> {
    val out = mutableMapOf<String, String>()
    var cursor = 0
    while(cursor < length) {
        cursor = skipWhitespace(cursor)
        if(cursor >= length) break
        if(this[cursor].isCommentStart) {
            cursor = findNewLine(cursor)
            continue
        }

        // analysis of key value locations
        val keyStart = cursor
        val keyEnd = findSeparator(keyStart)
        val valueStart = skipSeparators(keyEnd)
        val valueEnd = findNewLine(valueStart)
        cursor = valueEnd

        // extract values handling escapes and conversions
        val key = unescapedSubString(keyStart, keyEnd)
        val value = unescapedSubString(valueStart, valueEnd)

        out[key] = value
    }
    return out
}

private fun String.skipWhitespace(start: Int): Int {
    for(cursor in start until length) {
        if(!this[cursor].isWhitespace)
            return cursor
    }
    return length
}

private fun String.skipSeparators(start: Int): Int {
    var foundSeparator = false
    for(cursor in start until length) {
        val char = this[cursor]
        if(char.isSpaceOrTab) continue
        if(!char.isSeparator) return cursor
        if(foundSeparator) return cursor
        foundSeparator = true
    }
    return length
}

private fun String.findSeparator(start: Int): Int {
    var escape = false
    for(cursor in start until length) {
        val char = this[cursor]
        if(char.isKeyEnd && !escape) return cursor
        escape = if(char == '\\') !escape else false
    }
    return length
}

private fun String.findNewLine(start: Int): Int {
    var escape = false
    for(cursor in start until length) {
        val char = this[cursor]
        if(char.isNewLine) {
            if(!escape) return cursor
            // check if we have '\r' followed by '\n'
            if(char == '\r') {
                val nextCursor = cursor + 1
                if(nextCursor < length && this[nextCursor] == '\n') continue
            }
        }
        escape = if(char == '\\') !escape else false
    }
    return length
}

private fun String.unescapedSubString(start: Int, end: Int): String {
    if(start == end) return ""
    val buffer = StringBuilder(end-start)
    var cursor = start
    while (cursor < end) {
        val char = this[cursor++]
        if(char == '\\' && cursor < end) {
            val escaped = this[cursor++]
            // check if we need to decode unicode character
            if(escaped == 'u') {
                buffer.append(extractUnicode(cursor, end))
                cursor += 4
            } else if(escaped.isNewLine) {
                // skip remaining line break characters and any whitespace
                // before value on the next line
                cursor = skipWhitespace(cursor)
            } else {
                buffer.append(escaped.escapingChar)
            }
            continue
        }
        // non-escaping character - just append
        buffer.append(char)
    }
    return buffer.toString()
}

private fun String.extractUnicode(start: Int, end: Int): Char {
    var cursor = start
    if(end - cursor < 4) unicodeFormatError(start, end)
    var value = 0
    for (i in 0..3) {
        val hexDigit = this[cursor++]
        val hexValue = hexDigit.hexValue
        if(hexValue < 0) unicodeFormatError(start, end)
        value = (value shl 4) + hexValue
    }
    return value.toChar()
}

private fun String.unicodeFormatError(start: Int, end: Int): Nothing {
    val position = start - 2
    val problem = substring(position, (position + 6).coerceAtMost(end))
    throw IllegalArgumentException(
        "Malformed \\uxxxx encoding at position $position: '$problem'"
    )
}

private val Char.isCommentStart get() =
    this == '#' || this == '!'

private val Char.isSeparator get() =
    this == '=' || this == ':'

private val Char.isKeyEnd get() =
    isSeparator || isWhitespace

private val Char.isNewLine get() =
    this == '\n' || this == '\r'

private val Char.isSpaceOrTab get() =
    this == ' ' || this == '\t'

private val Char.isWhitespace get() = when(this) {
    '\n', '\r', '\t', ' ', formFeed -> true
    else -> false
}

private val Char.escapingChar get() = when(this) {
    't' -> '\t'
    'n' -> '\n'
    'r' -> '\r'
    'f' -> formFeed
    else -> this
}

// missing '\f' in Kotlin
private const val formFeed = 12.toChar()

private val Char.hexValue get() = when (this) {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> toInt() - '0'.toInt()
    'a', 'b', 'c', 'd', 'e', 'f' -> 10 + toInt() - 'a'.toInt()
    'A', 'B', 'C', 'D', 'E', 'F' -> 10 + toInt() - 'A'.toInt()
    else -> -1
}
