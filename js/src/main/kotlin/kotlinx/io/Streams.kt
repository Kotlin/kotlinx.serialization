package kotlinx.io

impl open class IOException impl constructor(message: String) : Exception(message) {
    impl constructor(): this("IO Exception")
}

impl abstract class InputStream {

    impl open fun available(): Int {
        return 0
    }

    impl open fun close() {
        /* empty */
    }

    impl abstract fun read(): Int

    impl open fun read(b: ByteArray) = read(b, 0, b.size)

    impl open fun read(b: ByteArray, offset: Int, len: Int): Int {
        // Force null check for b first!
        if (offset > b.size || offset < 0) {
            throw IndexOutOfBoundsException()
        }
        if (len < 0 || len > b.size - offset) {
            throw IndexOutOfBoundsException()
        }
        for (i in 0..len - 1) {
            var c: Int
            try {
                c = read()
                if (c == -1) {
                    return if (i == 0) -1 else i
                }
            } catch (e: IOException) {
                if (i != 0) {
                    return i
                }
                throw e
            }

            b[offset + i] = c.toByte()
        }
        return len
    }


    impl open fun skip(n: Long): Long {
        if (n <= 0) {
            return 0
        }
        var skipped: Long = 0
        var toRead = if (n < 4096) n.toInt() else 4096
        // We are unsynchronized, so take a local copy of the skipBuf at some
        // point in time.
        var localBuf = skipBuf
        if (localBuf == null || localBuf.size < toRead) {
            // May be lazily written back to the static. No matter if it
            // overwrites somebody else's store.
            localBuf = ByteArray(toRead)
            skipBuf = localBuf
        }
        while (skipped < n) {
            val read = read(localBuf, 0, toRead)
            if (read == -1) {
                return skipped
            }
            skipped += read.toLong()
            if (read < toRead) {
                return skipped
            }
            if (n - skipped < toRead) {
                toRead = (n - skipped).toInt()
            }
        }
        return skipped
    }

    companion object {

        private var skipBuf: ByteArray? = null
    }
}

impl class ByteArrayInputStream : InputStream {

    protected var buf: ByteArray
    protected var pos: Int = 0
    protected var mark: Int = 0
    protected var count: Int = 0


    impl constructor(buf: ByteArray) {
        this.mark = 0
        this.buf = buf
        this.count = buf.size
    }

    constructor(buf: ByteArray, offset: Int, length: Int) {
        this.buf = buf
        pos = offset
        mark = offset
        count = if (offset + length > buf.size) buf.size else offset + length
    }

    override fun available(): Int {
        return count - pos
    }


    impl override fun read(): Int {
        return if (pos < count) buf[pos++].toInt() and 0xFF else -1
    }

    fun read(b: ByteArray?, offset: Int, len: Int): Int {
        if (b == null) {
            throw NullPointerException()
        }
        // avoid int overflow
        if (offset < 0 || offset > b.size || len < 0
                || len > b.size - offset) {
            throw IndexOutOfBoundsException()
        }
        // Are there any bytes available?
        if (this.pos >= this.count) {
            return -1
        }
        if (len == 0) {
            return 0
        }

        val copylen = if (this.count - pos < len) this.count - pos else len
        arraycopy(buf, pos, b, offset, copylen)
        pos += copylen
        return copylen
    }

    override fun skip(n: Long): Long {
        if (n <= 0) {
            return 0
        }
        val temp = pos
        pos = if (this.count - pos < n) this.count else (pos + n).toInt()
        return (pos - temp).toLong()
    }
}


impl abstract class OutputStream {
    impl open fun close() {
        /* empty */
    }
    impl open fun flush() {
        /* empty */
    }

    impl open fun write(buffer: ByteArray) = write(buffer, 0, buffer.size)

    impl open fun write(buffer: ByteArray, offset: Int, count: Int) {
        // avoid int overflow, check null buffer
        if (offset > buffer.size || offset < 0 || count < 0
                || count > buffer.size - offset) {
            throw IndexOutOfBoundsException()
        }
        for (i in offset..offset + count - 1) {
            write(buffer[i].toInt())
        }
    }

    impl abstract fun write(oneByte: Int)

}

impl class ByteArrayOutputStream : OutputStream {
    protected var buf: ByteArray
    protected var count: Int = 0

    impl constructor() : super() {
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

    impl open fun size(): Int {
        return count
    }

    impl open fun toByteArray(): ByteArray {
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

    impl override fun write(oneByte: Int) {
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
