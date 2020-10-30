package kotlinx.serialization.properties

/**
 * Method encodes map as [String] in [java.util.Properties] format.
 *
 * Every entry is converted to a single line by concatenating
 * key and value with the '=' character in-between.
 * For the key, all space characters are preceded by the `\` character.
 * For the value, leading space characters, but not embedded or trailing
 * space characters, are written with a preceding `\`  character.
 * The key and element characters `#`, `!`, `=`, and `:` are also
 * preceded by the '\' character to avoid misinterpretation by the parser.
 *
 * Optionally characters less than `\u0020` and characters greater than `\u007E`
 * in keys or values are written as `\u*xxxx*' for their appropriate
 * hexadecimal value *xxxx*.
 *
 * @receiver map to be converted into [String]
 * @param escapeUnicode whether or not to escape unicode characters
 */
internal fun Map<String, String>.encodeAsString(escapeUnicode: Boolean = false): String {
    val builder = StringBuilder()
    for((k,v) in this) with(builder) {
        appendConverted(k, escapeSpace = true, escapeUnicode)
        append("=")
        appendConverted(v, escapeSpace = false, escapeUnicode)
        append("\n")
    }
    return builder.toString()
}


private fun StringBuilder.appendConverted(string: String, escapeSpace: Boolean, escapeUnicode: Boolean) {
    for (char in string) when {
        char.needsNoEscaping -> append(char)
        char == ' ' -> if(escapeSpace) appendEscaped(char) else append(char)
        char.needsEscaping -> append('\\').append(char.escapedSymbol)
        else -> if(escapeUnicode) appendEscapedUnicode(char) else append(char)
    }
}

private fun StringBuilder.appendEscaped(c: Char) {
    append('\\').append(c.escapedSymbol)
}

private fun StringBuilder.appendEscapedUnicode(c: Char) {
    append('\\').append('u')
    val intVal = c.toInt()
    append(toHex(intVal shr 12))
    append(toHex(intVal shr 8))
    append(toHex(intVal shr 4))
    append(toHex(intVal))
}

private val Char.needsNoEscaping get() =
    this in '>'..'~' && this != '\\'

private val Char.needsEscaping get() = when(this) {
    '\\', '\t', '\n', '\r', '=', ':', '#', '!', formFeed -> true
    else -> false
}

private val Char.escapedSymbol get() = when(this) {
    '\t' -> 't'
    '\n' -> 'n'
    '\r' -> 'r'
    formFeed -> 'f'
    else -> this
}

// missing '\f' in Kotlin
private const val formFeed = 12.toChar()

private fun toHex(nibble: Int): Char =
    hexDigit[nibble and 0xF]

private val hexDigit = charArrayOf(
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
)
