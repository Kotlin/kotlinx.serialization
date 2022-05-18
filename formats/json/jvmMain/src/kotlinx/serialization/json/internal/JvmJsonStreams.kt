package kotlinx.serialization.json.internal

import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

internal class JsonToJavaStreamWriter(private val stream: OutputStream) : JsonWriter {
    private val buffer = ByteArrayPool.take()
    private var array = CharArrayPool.take()
    private var index: Int = 0

    override fun writeLong(value: Long) {
        write(value.toString())
    }

    override fun writeChar(char: Char) {
        writeUtf8CodePoint(char.code)
    }

    override fun write(text: String) {
        val length = text.length
        ensureTotalCapacity(0, length)
        text.toCharArray(array, 0, 0, length)
        writeUtf8(array, 0, length)
    }

    override fun writeQuoted(text: String) {
        ensureTotalCapacity(0, text.length + 2)
        val arr = array
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

        writeUtf8(arr, 0, length + 2)
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
                        array[sz++] = ch.toChar()
                    }
                    1.toByte() -> {
                        val escapedString = ESCAPE_STRINGS[ch]!!
                        sz = ensureTotalCapacity(sz, escapedString.length)
                        escapedString.toCharArray(array, sz, 0, escapedString.length)
                        sz += escapedString.length
                    }
                    else -> {
                        array[sz] = '\\'
                        array[sz + 1] = marker.toInt().toChar()
                        sz += 2
                    }
                }
            } else {
                array[sz++] = ch.toChar()
            }
        }
        ensureTotalCapacity(sz, 1)
        array[sz++] = '"'
        writeUtf8(array, 0, sz)
        flush()
    }

    private fun ensureTotalCapacity(oldSize: Int, additional: Int): Int {
        val newSize = oldSize + additional
        if (array.size <= newSize) {
            array = array.copyOf(newSize.coerceAtLeast(oldSize * 2))
        }
        return oldSize
    }

    override fun release() {
        flush()
        CharArrayPool.release(array)
        ByteArrayPool.release(buffer)
    }

    private fun flush() {
        stream.write(buffer, 0, index)
        index = 0
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
        buffer[index++] = byte.toByte()
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun rest(): Int {
        return buffer.size - index
    }

    private fun writeUtf8(string: CharArray, beginIndex: Int, endIndex: Int) {
        require(beginIndex >= 0) { "beginIndex < 0: $beginIndex" }
        require(endIndex >= beginIndex) { "endIndex < beginIndex: $endIndex < $beginIndex" }
        require(endIndex <= string.size) { "endIndex > string.length: $endIndex > ${string.size}" }

        // Transcode a UTF-16 Java String to UTF-8 bytes.
        var i = beginIndex
        while (i < endIndex) {
            var c = string[i].code

            when {
                c < 0x80 -> {
                    // Emit a 7-bit character with 1 byte.
                    ensure(1)
                    write(c) // 0xxxxxxx
                    i++
                    val runLimit = minOf(endIndex, i + rest())

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
                    /* ktlint-disable no-multi-spaces */
                    write(c shr 6 or 0xc0) // 110xxxxx
                    write(c and 0x3f or 0x80) // 10xxxxxx
                    /* ktlint-enable no-multi-spaces */
                    i++
                }

                c < 0xd800 || c > 0xdfff -> {
                    // Emit a 16-bit character with 3 bytes.
                    ensure(3)
                    /* ktlint-disable no-multi-spaces */
                    write(c shr 12 or 0xe0) // 1110xxxx
                    write(c shr 6 and 0x3f or 0x80) // 10xxxxxx
                    write(c and 0x3f or 0x80) // 10xxxxxx
                    /* ktlint-enable no-multi-spaces */
                    i++
                }

                else -> {
                    // c is a surrogate. Make sure it is a high surrogate & that its successor is a low
                    // surrogate. If not, the UTF-16 is invalid, in which case we emit a replacement
                    // character.
                    val low = (if (i + 1 < endIndex) string[i + 1].code else 0)
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
                        /* ktlint-disable no-multi-spaces */
                        write(codePoint shr 18 or 0xf0) // 11110xxx
                        write(codePoint shr 12 and 0x3f or 0x80) // 10xxxxxx
                        write(codePoint shr 6 and 0x3f or 0x80) // 10xxyyyy
                        write(codePoint and 0x3f or 0x80) // 10yyyyyy
                        /* ktlint-enable no-multi-spaces */
                        i += 2
                    }
                }
            }
        }
    }

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
                /* ktlint-disable no-multi-spaces */
                write(codePoint shr 6 or 0xc0) // 110xxxxx
                write(codePoint and 0x3f or 0x80) // 10xxxxxx
                /* ktlint-enable no-multi-spaces */
            }
            codePoint in 0xd800..0xdfff -> {
                // Emit a replacement character for a partial surrogate.
                ensure(1)
                write('?'.code)
            }
            codePoint < 0x10000 -> {
                // Emit a 16-bit code point with 3 bytes.
                ensure(3)
                /* ktlint-disable no-multi-spaces */
                write(codePoint shr 12 or 0xe0) // 1110xxxx
                write(codePoint shr 6 and 0x3f or 0x80) // 10xxxxxx
                write(codePoint and 0x3f or 0x80) // 10xxxxxx
                /* ktlint-enable no-multi-spaces */
            }
            codePoint <= 0x10ffff -> {
                // Emit a 21-bit code point with 4 bytes.
                ensure(4)
                /* ktlint-disable no-multi-spaces */
                write(codePoint shr 18 or 0xf0) // 11110xxx
                write(codePoint shr 12 and 0x3f or 0x80) // 10xxxxxx
                write(codePoint shr 6 and 0x3f or 0x80) // 10xxyyyy
                write(codePoint and 0x3f or 0x80) // 10yyyyyy
                /* ktlint-enable no-multi-spaces */
            }
            else -> {
                throw IllegalArgumentException("Unexpected code point: $codePoint")
            }
        }
    }
}

internal class JavaStreamSerialReader(
    stream: InputStream,
    charset: Charset = Charsets.UTF_8
) : SerialReader {
    private val reader = stream.reader(charset)

    override fun read(buffer: CharArray, bufferOffset: Int, count: Int): Int {
        return reader.read(buffer, bufferOffset, count)
    }
}
