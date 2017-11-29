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

expect open class IOException: Exception {
    constructor()
    constructor(message: String)
}

expect abstract class InputStream {
    open fun available(): Int
    open fun close()
    abstract fun read(): Int
    open fun read(b: ByteArray): Int
    open fun read(b: ByteArray, offset: Int, len: Int): Int
    open fun skip(n: Long): Long
}

expect class ByteArrayInputStream(buf: ByteArray): InputStream {
    override fun read(): Int
}

expect abstract class OutputStream {
    open fun close()
    open fun flush()
    open fun write(buffer: ByteArray, offset: Int, count: Int)
    open fun write(buffer: ByteArray)
    abstract fun write(oneByte: Int)

}

@Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // KT-17944
expect class ByteArrayOutputStream(): OutputStream {
    override fun write(oneByte: Int)
    open fun toByteArray(): ByteArray
    open fun size(): Int
}