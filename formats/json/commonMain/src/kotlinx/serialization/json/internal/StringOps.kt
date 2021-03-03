/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlin.native.concurrent.*

private fun toHexChar(i: Int) : Char {
    val d = i and 0xf
    return if (d < 10) (d + '0'.toInt()).toChar()
    else (d - 10 + 'a'.toInt()).toChar()
}

@SharedImmutable
internal val ESCAPE_STRINGS: Array<String?> = arrayOfNulls<String>(93).apply {
    for (c in 0..0x1f) {
        val c1 = toHexChar(c shr 12)
        val c2 = toHexChar(c shr 8)
        val c3 = toHexChar(c shr 4)
        val c4 = toHexChar(c)
        this[c] = "\\u$c1$c2$c3$c4"
    }
    this['"'.toInt()] = "\\\""
    this['\\'.toInt()] = "\\\\"
    this['\t'.toInt()] = "\\t"
    this['\b'.toInt()] = "\\b"
    this['\n'.toInt()] = "\\n"
    this['\r'.toInt()] = "\\r"
    this[0x0c] = "\\f"
}

@SharedImmutable
internal val ESCAPE_MARKERS: ByteArray = ByteArray(93).apply {
    for (c in 0..0x1f) {
        this[c] = 1.toByte()
    }
    this['"'.toInt()] = '"'.toByte()
    this['\\'.toInt()] = '\\'.toByte()
    this['\t'.toInt()] = 't'.toByte()
    this['\b'.toInt()] = 'b'.toByte()
    this['\n'.toInt()] = 'n'.toByte()
    this['\r'.toInt()] = 'r'.toByte()
    this[0x0c] = 'f'.toByte()
}

internal fun StringBuilder.printQuoted(value: String) {
    append(STRING)
    var lastPos = 0
    for (i in value.indices) {
        val c = value[i].toInt()
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
