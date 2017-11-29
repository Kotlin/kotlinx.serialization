/*
 * Copyright 2017 JetBrains s.r.o.
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

package kotlinx.io

actual abstract class Writer protected actual constructor() {
    actual open fun write(ch: Int) {
        write(charArrayOf(ch.toChar()), 0, 1)
    }
    actual open fun write(str: String) {
        write(str.toList().toCharArray(), 0, str.length)
    }
    actual abstract fun write(src: CharArray, off: Int, len: Int)
    actual abstract fun flush()
    actual abstract fun close()
}

actual open class PrintWriter actual constructor(val w: Writer): Writer() {
    actual open fun print(s: String) = w.write(s)
    actual open fun print(ch: Char) = w.write(ch.toInt())
    actual open fun print(value: Float)= print(value.toString())
    actual open fun print(value: Double)= print(value.toString())
    actual open fun print(value: Boolean)= print(value.toString())
    actual open fun print(value: Int)= print(value.toString())
    actual open fun print(value: Long)= print(value.toString())
    actual open fun print(value: Any?) = print(value.toString())

    actual open fun println() = w.write(10)
    actual open fun println(s: String) { w.write(s); println() }
    actual open fun println(ch: Char) { w.write(ch.toInt()); println() }
    actual open fun println(value: Float)= println(value.toString())
    actual open fun println(value: Double)= println(value.toString())
    actual open fun println(value: Boolean)= println(value.toString())
    actual open fun println(value: Int)= println(value.toString())
    actual open fun println(value: Long)= println(value.toString())
    actual open fun println(value: Any?) = println(value.toString())

    actual override fun write(src: CharArray, off: Int, len: Int) {
        w.write(src, off, len)
    }
    actual override fun flush() {}
    actual override fun close() {}
}

actual class StringWriter: Writer() {
    private val sb = StringBuilder()

    actual override fun toString(): String = sb.toString()
    actual override fun write(src: CharArray, off: Int, len: Int) {
        src.slice(off until off+len).forEach { sb.append(it) }
    }
    actual override fun flush() {}
    actual override fun close() {}
}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual abstract class Reader protected actual constructor() {
    actual open fun read(): Int {
        val a = CharArray(1)
        read(a, 0, 1)
        return a[0].toInt()
    }
    actual abstract fun read(dst: CharArray, off: Int, len: Int): Int
    actual abstract fun close()
}

actual class StringReader actual constructor(val str: String) : Reader() {

    private var position: Int = 0

    actual override fun read(): Int = when (position) {
        str.length -> -1
        else -> str[position++].toInt()
    }


    actual override fun read(dst: CharArray, off: Int, len: Int): Int {
        var cnt = 0
        for (i in off until off + len) {
            val r = read()
            if (r == -1) return cnt
            cnt++
            dst[i] = r.toChar()
        }
        return len
    }

    actual override fun close() {}
}