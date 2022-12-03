package kotlinx.serialization.json.internal

import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

internal class JsonToJavaStreamWriter(private val stream: OutputStream) : JsonWriter {
    private val buffer = ByteArrayPool.take()
    private var indexInBuffer: Int = 0

    override fun writeLong(value: Long) {
        write(value.toString())
    }

    override fun writeChar(char: Char) {
        writeUtf8CodePoint(char.code)
    }

    override fun write(text: String) {
        text.codePoints().forEachOrdered{
            writeUtf8CodePoint(it)
        }
    }

    override fun writeQuoted(text: String) {
        writeUtf8CodePoint('"'.code)
        text.codePoints().forEachOrdered { ch ->
            if (ch < ESCAPE_MARKERS.size) {
                when (val marker = ESCAPE_MARKERS[ch]) {
                    0.toByte() -> {
                        writeUtf8CodePoint(ch)
                    }
                    1.toByte() -> {
                        ESCAPE_STRINGS[ch]?.let{
                            write(it)
                        }
                    }
                    else -> {
                        writeUtf8CodePoint('\\'.code)
                        writeUtf8CodePoint(marker.toInt().toChar().code)
                    }
                }
            } else {
                writeUtf8CodePoint(ch)
            }
        }
        writeUtf8CodePoint('"'.code)
        flush()
    }

    override fun release() {
        flush()
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

internal class JavaStreamSerialReader(
    stream: InputStream,
    charset: Charset = Charsets.UTF_8
) : SerialReader {
    private val reader = stream.reader(charset)

    override fun read(buffer: CharArray, bufferOffset: Int, count: Int): Int {
        return reader.read(buffer, bufferOffset, count)
    }
}
