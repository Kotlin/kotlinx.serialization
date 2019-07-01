/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.SharedImmutable
import kotlinx.serialization.json.JsonDecodingException
import kotlinx.serialization.json.internal.EscapeCharMappings.ESCAPE_2_CHAR
import kotlin.jvm.JvmField

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
internal const val TC_WS: Byte = 3
internal const val TC_COMMA: Byte = 4
internal const val TC_COLON: Byte = 5
internal const val TC_BEGIN_OBJ: Byte = 6
internal const val TC_END_OBJ: Byte = 7
internal const val TC_BEGIN_LIST: Byte = 8
internal const val TC_END_LIST: Byte = 9
internal const val TC_NULL: Byte = 10
internal const val TC_INVALID: Byte = 11
internal const val TC_EOF: Byte = 12

// mapping from chars to token classes
private const val CTC_MAX = 0x7e

// mapping from escape chars real chars
private const val ESC2C_MAX = 0x75

@SharedImmutable
internal val C2TC = ByteArray(CTC_MAX).apply {
    for (i in 0..0x20) {
        initC2TC(i, TC_INVALID)
    }

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

// object instead of @SharedImmutable because there is mutual initialization in [initC2ESC]
internal object EscapeCharMappings {
    @JvmField
    public val ESCAPE_2_CHAR = CharArray(ESC2C_MAX)

    init {
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

    private fun initC2ESC(c: Int, esc: Char) {
        if (esc != UNICODE_ESC) ESCAPE_2_CHAR[esc.toInt()] = c.toChar()
    }

    private fun initC2ESC(c: Char, esc: Char) = initC2ESC(c.toInt(), esc)
}

private fun ByteArray.initC2TC(c: Int, cl: Byte) {
    this[c] = cl
}

private fun ByteArray.initC2TC(c: Char, cl: Byte) {
    initC2TC(c.toInt(), cl)
}

internal fun charToTokenClass(c: Char) = if (c.toInt() < CTC_MAX) C2TC[c.toInt()] else TC_OTHER

internal fun escapeToChar(c: Int): Char = if (c < ESC2C_MAX) ESCAPE_2_CHAR[c] else INVALID


// Streaming JSON reader
internal class JsonReader(private val source: String) {

    @JvmField
    var currentPosition: Int = 0 // position in source

    @JvmField
    var tokenClass: Byte = TC_EOF

    public val isDone: Boolean get() = tokenClass == TC_EOF

    public val canBeginValue: Boolean
        get() = when (tokenClass) {
            TC_BEGIN_LIST, TC_BEGIN_OBJ, TC_OTHER, TC_STRING, TC_NULL -> true
            else -> false
        }

    // updated by nextToken
    private var tokenPosition: Int = 0

    // update by nextString/nextLiteral
    private var offset = -1 // when offset >= 0 string is in source, otherwise in buf
    private var length = 0 // length of string
    private var buf = CharArray(16) // only used for strings with escapes

    init {
        nextToken()
    }

    internal inline fun requireTokenClass(expected: Byte, errorMessage: (Char) -> String) {
        if (tokenClass != expected) fail(tokenPosition, errorMessage(tokenClass.toChar()))
    }

    fun takeString(): String {
        if (tokenClass != TC_OTHER && tokenClass != TC_STRING) fail(tokenPosition, "Expected string or non-null literal")
        val prevStr = if (offset < 0)
            String(buf, 0, length) else
            source.substring(offset, offset + length)
        nextToken()
        return prevStr
    }

    private fun append(ch: Char) {
        if (length >= buf.size) buf = buf.copyOf(2 * buf.size)
        buf[length++] = ch
    }

    // initializes buf usage upon the first encountered escaped char
    private fun appendRange(source: String, fromIndex: Int, toIndex: Int) {
        val addLen = toIndex - fromIndex
        val oldLen = length
        val newLen = oldLen + addLen
        if (newLen > buf.size) buf = buf.copyOf(newLen.coerceAtLeast(2 * buf.size))
        for (i in 0 until addLen) buf[oldLen + i] = source[fromIndex + i]
        length += addLen
    }

    fun nextToken() {
        val source = source
        var curPos = currentPosition
        val maxLen = source.length
        while (true) {
            if (curPos >= maxLen) {
                tokenPosition = curPos
                tokenClass = TC_EOF
                return
            }
            val ch = source[curPos]
            val tc = charToTokenClass(ch)
            when (tc) {
                TC_WS -> curPos++ // skip whitespace
                TC_OTHER -> {
                    nextLiteral(source, curPos)
                    return
                }
                TC_STRING -> {
                    nextString(source, curPos)
                    return
                }
                else -> {
                    this.tokenPosition = curPos
                    this.tokenClass = tc
                    this.currentPosition = curPos + 1
                    return
                }
            }
        }
    }

    private fun nextLiteral(source: String, startPos: Int) {
        tokenPosition = startPos
        offset = startPos
        var curPos = startPos
        val maxLen = source.length
        while (true) {
            curPos++
            if (curPos >= maxLen || charToTokenClass(source[curPos]) != TC_OTHER) break
        }
        this.currentPosition = curPos
        length = curPos - offset
        tokenClass = if (rangeEquals(source, offset, length, NULL)) TC_NULL else TC_OTHER
    }

    private fun nextString(source: String, startPos: Int) {
        tokenPosition = startPos
        length = 0 // in buffer
        var curPos = startPos + 1
        var lastPos = curPos
        val maxLen = source.length
        parse@ while (true) {
            if (curPos >= maxLen) fail(curPos, "Unexpected end in string")
            if (source[curPos] == STRING) {
                break@parse
            } else if (source[curPos] == STRING_ESC) {
                appendRange(source, lastPos, curPos)
                val newPos = appendEsc(source, curPos + 1)
                curPos = newPos
                lastPos = newPos
            } else {
                curPos++
            }
        }
        if (lastPos == startPos + 1) {
            // there was no escaped chars
            this.offset = lastPos
            this.length = curPos - lastPos
        } else {
            // some escaped chars were there
            appendRange(source, lastPos, curPos)
            this.offset = -1
        }
        this.currentPosition = curPos + 1
        tokenClass = TC_STRING
    }

    private fun appendEsc(source: String, startPos: Int): Int {
        var curPos = startPos
        require(curPos < source.length, curPos) { "Unexpected end after escape char" }
        val curChar = source[curPos++]
        if (curChar == UNICODE_ESC) {
            curPos = appendHex(source, curPos)
        } else {
            val c = escapeToChar(curChar.toInt())
            require(c != INVALID, curPos) { "Invalid escaped char '$curChar'" }
            append(c)
        }
        return curPos
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
        if (tokenClass != TC_BEGIN_OBJ && tokenClass != TC_BEGIN_LIST) {
            nextToken()
            return
        }
        val tokenStack = mutableListOf<Byte>()
        do {
            when (tokenClass) {
                TC_BEGIN_LIST, TC_BEGIN_OBJ -> tokenStack.add(tokenClass)
                TC_END_LIST -> {
                    if (tokenStack.last() != TC_BEGIN_LIST) throw JsonDecodingException(
                        currentPosition,
                        "found ] instead of }"
                    )
                    tokenStack.removeAt(tokenStack.size - 1)
                }
                TC_END_OBJ -> {
                    if (tokenStack.last() != TC_BEGIN_OBJ) throw JsonDecodingException(
                        currentPosition,
                        "found } instead of ]"
                    )
                    tokenStack.removeAt(tokenStack.size - 1)
                }
            }
            nextToken()
        } while (tokenStack.isNotEmpty())
    }

    override fun toString(): String {
        return "JsonReader(source='$source', currentPosition=$currentPosition, tokenClass=$tokenClass, tokenPosition=$tokenPosition, offset=$offset)"
    }
}

// Utility functions
private fun fromHexChar(source: String, curPos: Int): Int {
    require(curPos < source.length, curPos) { "Unexpected end in unicode escape" }
    val curChar = source[curPos]
    return when (curChar) {
        in '0'..'9' -> curChar.toInt() - '0'.toInt()
        in 'a'..'f' -> curChar.toInt() - 'a'.toInt() + 10
        in 'A'..'F' -> curChar.toInt() - 'A'.toInt() + 10
        else -> fail(curPos, "Invalid toHexChar char '$curChar' in unicode escape")
    }
}

private fun rangeEquals(source: String, start: Int, length: Int, str: String): Boolean {
    val n = str.length
    if (length != n) return false
    for (i in 0 until n) if (source[start + i] != str[i]) return false
    return true
}

internal inline fun require(condition: Boolean, position: Int, msg: () -> String) {
    if (!condition) fail(position, msg())
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun fail(position: Int, msg: String): Nothing {
    throw JsonDecodingException(position, msg)
}
