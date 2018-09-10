/*
 * Copyright 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.serialization.json

private fun toHexChar(i: Int) : Char {
    val d = i and 0xf
    return if (d < 10) (d + '0'.toInt()).toChar()
    else (d - 10 + 'a'.toInt()).toChar()
}

/*
 * Even though the actual size of this array is 92, it has to be the power of two, otherwise
 * JVM cannot perform advanced range-check elimination and vectorization in printQuoted
 */
private val ESCAPE_CHARS: Array<String?> = arrayOfNulls<String>(128).apply {
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

internal fun StringBuilder.printQuoted(value: String)  {
    append(STRING)
    var lastPos = 0
    val length = value.length
    for (i in 0 until length) {
        val c = value[i].toInt()
        // Do not replace this constant with C2ESC_MAX (which is smaller than ESCAPE_CHARS size),
        // otherwise JIT won't eliminate range check and won't vectorize this loop
        if (c >= ESCAPE_CHARS.size) continue // no need to escape
        val esc = ESCAPE_CHARS[c] ?: continue
        append(value, lastPos, i) // flush prev
        append(esc)
        lastPos = i + 1
    }
    append(value, lastPos, length)
    append(STRING)
}
