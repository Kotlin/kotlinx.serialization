/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress(
    "DEPRECATION_ERROR", "NO_EXPLICIT_VISIBILITY_IN_API_MODE_WARNING",
    "NO_EXPLICIT_RETURN_TYPE_IN_API_MODE_WARNING"
)
package kotlinx.io

import kotlinx.serialization.*

@Deprecated(message = message, level = DeprecationLevel.ERROR)
actual open class IOException actual constructor(message: String) : Exception(message) {
    actual constructor() : this("IO Exception")
}


@InternalSerializationApi
actual abstract class OutputStream {
    actual open fun close() {
        /* empty */
    }
    actual open fun flush() {
        /* empty */
    }

    actual open fun write(buffer: ByteArray) = write(buffer, 0, buffer.size)

    actual open fun write(buffer: ByteArray, offset: Int, count: Int) {
        // avoid int overflow, check null buffer
        if (offset > buffer.size || offset < 0 || count < 0
                || count > buffer.size - offset) {
            throw IndexOutOfBoundsException()
        }
        for (i in offset..offset + count - 1) {
            write(buffer[i].toInt())
        }
    }

    actual abstract fun write(oneByte: Int)

}

@InternalSerializationApi
actual class ByteArrayOutputStream : OutputStream {
    protected var buf: ByteArray
    protected var count: Int = 0

    actual constructor() : super() {
        buf = ByteArray(32)
    }

    constructor(size: Int) : super() {
        if (size >= 0) {
            buf = ByteArray(size)
        } else {
            throw IllegalArgumentException() //$NON-NLS-1$
        }
    }

    private fun expand(i: Int) {
        /* Can the buffer handle @i more bytes, if not expand it */
        if (count + i <= buf.size) {
            return
        }

        val newbuf = ByteArray((count + i) * 2)
        arraycopy(buf, 0, newbuf, 0, count)
        buf = newbuf
    }

    actual fun size(): Int {
        return count
    }

    actual fun toByteArray(): ByteArray {
        val newArray = ByteArray(count)
        arraycopy(buf, 0, newArray, 0, count)
        return newArray
    }

    override fun write(buffer: ByteArray, offset: Int, count: Int) {
        // avoid int overflow
        if (offset < 0 || offset > buffer.size || count < 0
                || count > buffer.size - offset) {
            throw IndexOutOfBoundsException() //$NON-NLS-1$
        }
        if (count == 0) {
            return
        }

        /* Expand if necessary */
        expand(count)
        arraycopy(buffer, offset, buf, this.count, count)
        this.count += count
    }

    actual override fun write(oneByte: Int) {
        if (count == buf.size) {
            expand(1)
        }
        buf[count++] = oneByte.toByte()
    }

    fun writeTo(out: OutputStream) {
        out.write(buf, 0, count)
    }
}

internal fun arraycopy(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, len: Int) {
    for (i in 0..len - 1) {
        dst[dstPos + i] = src[srcPos + i]
    }
}
