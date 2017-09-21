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

impl abstract class Writer impl protected constructor() {
    impl open fun write(ch: Int) {
        write(charArrayOf(ch.toChar()), 0, 1)
    }
    impl open fun write(str: String) {
        write(str.toList().toCharArray(), 0, str.length)
    }
    impl abstract fun write(src: CharArray, off: Int, len: Int)
    impl abstract fun flush()
    impl abstract fun close()
}

impl open class PrintWriter impl constructor(val w: Writer): Writer() {
    impl open fun print(s: String) = w.write(s)
    impl open fun print(ch: Char) = w.write(ch.toInt())
    impl open fun print(value: Float)= print(value.toString())
    impl open fun print(value: Double)= print(value.toString())
    impl open fun print(value: Boolean)= print(value.toString())
    impl open fun print(value: Int)= print(value.toString())
    impl open fun print(value: Long)= print(value.toString())
    impl open fun print(value: Any?) = print(value.toString())

    impl open fun println() = w.write(10)
    impl open fun println(s: String) { w.write(s); println() }
    impl open fun println(ch: Char) { w.write(ch.toInt()); println() }
    impl open fun println(value: Float)= println(value.toString())
    impl open fun println(value: Double)= println(value.toString())
    impl open fun println(value: Boolean)= println(value.toString())
    impl open fun println(value: Int)= println(value.toString())
    impl open fun println(value: Long)= println(value.toString())
    impl open fun println(value: Any?) = println(value.toString())

    impl override fun write(src: CharArray, off: Int, len: Int) {
        w.write(src, off, len)
    }
    impl override fun flush() {}
    impl override fun close() {}
}

impl class StringWriter: Writer() {
    private val sb = StringBuilder()

    impl override fun toString(): String = sb.toString()
    impl override fun write(src: CharArray, off: Int, len: Int) {
        src.slice(off until off+len).forEach { sb.append(it) }
    }
    impl override fun flush() {}
    impl override fun close() {}
}

@Suppress("IMPLEMENTATION_WITHOUT_HEADER")
impl abstract class Reader impl protected constructor() {
    impl abstract fun read(): Int
    impl abstract fun read(dst: CharArray, off: Int, len: Int): Int
    impl open fun close() {}
}

impl class StringReader impl constructor(val str: String) : Reader() {

    private var position: Int = 0

    impl override fun read(): Int = when (position) {
        str.length -> -1
        else -> str[position++].toInt()
    }


    impl override fun read(dst: CharArray, off: Int, len: Int): Int {
        var cnt = 0
        for (i in off until off + len) {
            val r = read()
            if (r == -1) return cnt
            cnt++
            dst[i] = r.toChar()
        }
        return len
    }

    impl override fun close() {}
}