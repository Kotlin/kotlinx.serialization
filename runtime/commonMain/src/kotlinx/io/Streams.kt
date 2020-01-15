/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.io

import kotlinx.serialization.*

@Deprecated(message = message, level = DeprecationLevel.ERROR)
expect open class IOException: Exception {
    constructor()
    constructor(message: String)
}

@InternalSerializationApi
expect abstract class InputStream {
    open fun available(): Int
    open fun close()
    abstract fun read(): Int
    open fun read(b: ByteArray): Int
    open fun read(b: ByteArray, offset: Int, len: Int): Int
    open fun skip(n: Long): Long
}

@InternalSerializationApi
expect class ByteArrayInputStream(buf: ByteArray): InputStream {
    override fun read(): Int
}

@InternalSerializationApi
expect abstract class OutputStream {
    open fun close()
    open fun flush()
    open fun write(buffer: ByteArray, offset: Int, count: Int)
    open fun write(buffer: ByteArray)
    abstract fun write(oneByte: Int)

}

@Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // KT-17944
@InternalSerializationApi
expect class ByteArrayOutputStream(): OutputStream {
    override fun write(oneByte: Int)
    fun toByteArray(): ByteArray
    fun size(): Int
}
