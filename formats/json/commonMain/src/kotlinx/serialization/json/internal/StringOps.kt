/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlin.math.*
import kotlin.native.concurrent.*

private fun toHexChar(i: Int) : Char {
    val d = i and 0xf
    return if (d < 10) (d + '0'.code).toChar()
    else (d - 10 + 'a'.code).toChar()
}

@PublishedApi
@SharedImmutable
internal val ESCAPE_STRINGS: Array<String?> = arrayOfNulls<String>(93).apply {
    for (c in 0..0x1f) {
        val c1 = toHexChar(c shr 12)
        val c2 = toHexChar(c shr 8)
        val c3 = toHexChar(c shr 4)
        val c4 = toHexChar(c)
        this[c] = "\\u$c1$c2$c3$c4"
    }
    this['"'.code] = "\\\""
    this['\\'.code] = "\\\\"
    this['\t'.code] = "\\t"
    this['\b'.code] = "\\b"
    this['\n'.code] = "\\n"
    this['\r'.code] = "\\r"
    this[0x0c] = "\\f"
}

@SharedImmutable
internal val ESCAPE_MARKERS: ByteArray = ByteArray(93).apply {
    for (c in 0..0x1f) {
        this[c] = 1.toByte()
    }
    this['"'.code] = '"'.code.toByte()
    this['\\'.code] = '\\'.code.toByte()
    this['\t'.code] = 't'.code.toByte()
    this['\b'.code] = 'b'.code.toByte()
    this['\n'.code] = 'n'.code.toByte()
    this['\r'.code] = 'r'.code.toByte()
    this[0x0c] = 'f'.code.toByte()
}

internal fun StringBuilder.printQuoted(value: String) {
    append(STRING)
    var lastPos = 0
    for (i in value.indices) {
        val c = value[i].code
        if (c < ESCAPE_STRINGS.size && ESCAPE_STRINGS[c] != null) {
            append(value, lastPos, i) // flush prev
            append(ESCAPE_STRINGS[c])
            lastPos = i + 1
        }
    }

    if (lastPos != 0) append(value, lastPos, value.length)
    else append(value)
    append(STRING)
}

/**
 * Returns `true` if the contents of this string is equal to the word "true", ignoring case, `false` if content equals "false",
 * and returns `null` otherwise.
 */
internal fun String.toBooleanStrictOrNull(): Boolean? = when {
    this.equals("true", ignoreCase = true) -> true
    this.equals("false", ignoreCase = true) -> false
    else -> null
}

internal inline fun String.toLongExponent(failF : (String) -> Nothing) : Long {
    val source = this
    var current = 0
    var accumulator = 0L
    var exponentAccumulator = 0L
    var isNegative = false
    var isExponentPositive = false
    var hasExponent = false
    val start = current
    var hasChars = true
    while (hasChars) {
        val ch: Char = source[current]
        if((ch == 'e' || ch == 'E') && !hasExponent) {
            if (current == start) failF("Unexpected symbol $ch in numeric literal")
            isExponentPositive = true
            hasExponent = true
            ++current
            continue
        }
        if (ch == '-' && hasExponent) {
            if (current == start) failF("Unexpected symbol '-' in numeric literal")
            isExponentPositive = false
            ++current
            continue
        }
        if(ch == '+' && hasExponent) {
            if (current == start) failF("Unexpected symbol '+' in numeric literal")
            isExponentPositive = true
            ++current
            continue
        }
        if (ch == '-') {
            if (current != start) failF("Unexpected symbol '-' in numeric literal")
            isNegative = true
            ++current
            continue
        }
        val token = charToTokenClass(ch)
        if (token != TC_OTHER) break
        ++current
        hasChars = current != source.length
        val digit = ch - '0'
        if (digit !in 0..9) failF("Unexpected symbol '$ch' in numeric literal")
        if (hasExponent) {
            exponentAccumulator = exponentAccumulator * 10 + digit
            continue
        }
        accumulator = accumulator * 10 - digit
        if (accumulator > 0) failF("Numeric value overflow")
    }
    if (start == current || (isNegative && start == current - 1)) {
        failF("Expected numeric literal")
    }

    if(hasExponent) {
        val doubleAccumulator  = accumulator.toDouble() * calculateExponent(exponentAccumulator, isExponentPositive)
        if(doubleAccumulator > Long.MAX_VALUE || doubleAccumulator < Long.MIN_VALUE) failF("Numeric value overflow")
        accumulator = doubleAccumulator.toLong()
    }

    return when {
        isNegative -> accumulator
        accumulator != Long.MIN_VALUE -> -accumulator
        else -> failF("Numeric value overflow")
    }
}

private fun calculateExponent(exponentAccumulator: Long, isExponentPositive: Boolean): Double = when (isExponentPositive) {
    false -> 10.0.pow(-exponentAccumulator.toDouble())
    true -> 10.0.pow(exponentAccumulator.toDouble())
}
