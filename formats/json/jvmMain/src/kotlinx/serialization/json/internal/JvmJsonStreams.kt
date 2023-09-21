package kotlinx.serialization.json.internal

import java.io.InputStream
import java.io.OutputStream

internal class JsonToJavaStreamWriter(private val stream: OutputStream) : InternalJsonWriter {
    private val buffer = ByteArrayPool.take()
    private var charArray = CharArrayPool.take()
    private var indexInBuffer: Int = 0

    override fun writeLong(value: Long) {
        write(value.toString())
    }

    override fun writeChar(char: Char) {
        writeUtf8CodePoint(char.code)
    }

    override fun write(text: String) {
        val length = text.length
        ensureTotalCapacity(0, length)
        text.toCharArray(charArray, 0, 0, length)
        writeUtf8(charArray, length)
    }

    override fun writeQuoted(text: String) {
        ensureTotalCapacity(0, text.length + 2)
        val arr = charArray
        arr[0] = '"'
        val length = text.length
        text.toCharArray(arr, 1, 0, length)
        for (i in 1 until 1 + length) {
            val ch = arr[i].code
            // Do we have unescaped symbols?
            if (ch < ESCAPE_MARKERS.size && ESCAPE_MARKERS[ch] != 0.toByte()) {
                // Go to slow path
                return appendStringSlowPath(i, text)
            }
        }
        // Update the state
        // Capacity is not ensured because we didn't hit the slow path and thus guessed it properly in the beginning

        arr[length + 1] = '"'

        writeUtf8(arr, length + 2)
        flush()
    }

    private fun appendStringSlowPath(currentSize: Int, string: String) {
        var sz = currentSize
        for (i in currentSize - 1 until string.length) {
            /*
             * We ar already on slow path and haven't guessed the capacity properly.
             * Reserve +2 for backslash-escaped symbols on each iteration
             */
            sz = ensureTotalCapacity(sz, 2)
            val ch = string[i].code
            // Do we have unescaped symbols?
            if (ch < ESCAPE_MARKERS.size) {
                /*
                * Escape markers are populated for backslash-escaped symbols.
                * E.g. ESCAPE_MARKERS['\b'] == 'b'.toByte()
                * Everything else is populated with either zeros (no escapes)
                * or ones (unicode escape)
                */
                when (val marker = ESCAPE_MARKERS[ch]) {
                    0.toByte() -> {
                        charArray[sz++] = ch.toChar()
                    }

                    1.toByte() -> {
                        val escapedString = ESCAPE_STRINGS[ch]!!
                        sz = ensureTotalCapacity(sz, escapedString.length)
                        escapedString.toCharArray(charArray, sz, 0, escapedString.length)
                        sz += escapedString.length
                    }

                    else -> {
                        charArray[sz] = '\\'
                        charArray[sz + 1] = marker.toInt().toChar()
                        sz += 2
                    }
                }
            } else {
                charArray[sz++] = ch.toChar()
            }
        }
        ensureTotalCapacity(sz, 1)
        charArray[sz++] = '"'
        writeUtf8(charArray, sz)
        flush()
    }

    private fun ensureTotalCapacity(oldSize: Int, additional: Int): Int {
        val newSize = oldSize + additional
        if (charArray.size <= newSize) {
            charArray = charArray.copyOf(newSize.coerceAtLeast(oldSize * 2))
        }
        return oldSize
    }

    override fun release() {
        flush()
        CharArrayPool.release(charArray)
        ByteArrayPool.release(buffer)
    }

    private fun flush() {
        stream.write(buffer, 0, indexInBuffer)
        indexInBuffer = 0
    }


    @Suppress("NOTHING_TO_INLINE")
    // ! you should never ask for more than the buffer size
    private inline fun ensure(bytesCount: Int) {
        if (rest() < bytesCount) {
            flush()
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    // ! you should never ask for more than the buffer size
    private inline fun write(byte: Int) {
        buffer[indexInBuffer++] = byte.toByte()
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun rest(): Int {
        return buffer.size - indexInBuffer
    }

    /*
    Sources taken from okio library with minor changes, see https://github.com/square/okio
     */
    private fun writeUtf8(string: CharArray, count: Int) {
        require(count >= 0) { "count < 0" }
        require(count <= string.size) { "count > string.length: $count > ${string.size}" }

        // Transcode a UTF-16 Java String to UTF-8 bytes.
        var i = 0
        while (i < count) {
            var c = string[i].code

            when {
                c < 0x80 -> {
                    // Emit a 7-bit character with 1 byte.
                    ensure(1)
                    write(c) // 0xxxxxxx
                    i++
                    val runLimit = minOf(count, i + rest())

                    // Fast-path contiguous runs of ASCII characters. This is ugly, but yields a ~4x performance
                    // improvement over independent calls to writeByte().
                    while (i < runLimit) {
                        c = string[i].code
                        if (c >= 0x80) break
                        write(c) // 0xxxxxxx
                        i++
                    }
                }

                c < 0x800 -> {
                    // Emit a 11-bit character with 2 bytes.
                    ensure(2)
                    write(c shr 6 or 0xc0) // 110xxxxx
                    write(c and 0x3f or 0x80) // 10xxxxxx
                    i++
                }

                c < 0xd800 || c > 0xdfff -> {
                    // Emit a 16-bit character with 3 bytes.
                    ensure(3)
                    write(c shr 12 or 0xe0) // 1110xxxx
                    write(c shr 6 and 0x3f or 0x80) // 10xxxxxx
                    write(c and 0x3f or 0x80) // 10xxxxxx
                    i++
                }

                else -> {
                    // c is a surrogate. Make sure it is a high surrogate & that its successor is a low
                    // surrogate. If not, the UTF-16 is invalid, in which case we emit a replacement
                    // character.
                    val low = (if (i + 1 < count) string[i + 1].code else 0)
                    if (c > 0xdbff || low !in 0xdc00..0xdfff) {
                        ensure(1)
                        write('?'.code)
                        i++
                    } else {
                        // UTF-16 high surrogate: 110110xxxxxxxxxx (10 bits)
                        // UTF-16 low surrogate:  110111yyyyyyyyyy (10 bits)
                        // Unicode code point:    00010000000000000000 + xxxxxxxxxxyyyyyyyyyy (21 bits)
                        val codePoint = 0x010000 + (c and 0x03ff shl 10 or (low and 0x03ff))

                        // Emit a 21-bit character with 4 bytes.
                        ensure(4)
                        write(codePoint shr 18 or 0xf0) // 11110xxx
                        write(codePoint shr 12 and 0x3f or 0x80) // 10xxxxxx
                        write(codePoint shr 6 and 0x3f or 0x80) // 10xxyyyy
                        write(codePoint and 0x3f or 0x80) // 10yyyyyy
                        i += 2
                    }
                }
            }
        }
    }

    /**
     * Sources taken from okio library with minor changes, see https://github.com/square/okio
     */
    private fun writeUtf8CodePoint(codePoint: Int) {
        when {
            codePoint < 0x80 -> {
                // Emit a 7-bit code point with 1 byte.
                ensure(1)
                write(codePoint)
            }

            codePoint < 0x800 -> {
                // Emit a 11-bit code point with 2 bytes.
                ensure(2)
                write(codePoint shr 6 or 0xc0) // 110xxxxx
                write(codePoint and 0x3f or 0x80) // 10xxxxxx
            }

            codePoint in 0xd800..0xdfff -> {
                // Emit a replacement character for a partial surrogate.
                ensure(1)
                write('?'.code)
            }

            codePoint < 0x10000 -> {
                // Emit a 16-bit code point with 3 bytes.
                ensure(3)
                write(codePoint shr 12 or 0xe0) // 1110xxxx
                write(codePoint shr 6 and 0x3f or 0x80) // 10xxxxxx
                write(codePoint and 0x3f or 0x80) // 10xxxxxx
            }

            codePoint <= 0x10ffff -> {
                // Emit a 21-bit code point with 4 bytes.
                ensure(4)
                write(codePoint shr 18 or 0xf0) // 11110xxx
                write(codePoint shr 12 and 0x3f or 0x80) // 10xxxxxx
                write(codePoint shr 6 and 0x3f or 0x80) // 10xxyyyy
                write(codePoint and 0x3f or 0x80) // 10yyyyyy
            }

            else -> {
                throw JsonEncodingException("Unexpected code point: $codePoint")
            }
        }
    }
}

internal class JavaStreamSerialReader(stream: InputStream) : InternalJsonReader {
    // NB: not closed on purpose, it is the responsibility of the caller
    private val reader = CharsetReader(stream, Charsets.UTF_8)

    override fun read(buffer: CharArray, bufferOffset: Int, count: Int): Int {
        return reader.read(buffer, bufferOffset, count)
    }

    fun release() {
        reader.release()
    }
}
