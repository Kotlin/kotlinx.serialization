package kotlinx.serialization.json.internal

import java.io.*
import java.nio.*
import java.nio.charset.*

internal class CharsetReader(
    private val inputStream: InputStream,
    private val charset: Charset
) {
    private val decoder: CharsetDecoder
    private val byteBuffer: ByteBuffer

    // Surrogate-handling in cases when a single char is requested, but two were read
    private var hasLeftoverPotentiallySurrogateChar = false
    private var leftoverChar = 0.toChar()

    init {
        decoder = charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
        byteBuffer = ByteBuffer.wrap(ByteArrayPool8k.take())
        byteBuffer.flip() // Make empty
    }

    @Suppress("NAME_SHADOWING")
    fun read(array: CharArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        require(offset in 0 until array.size && length >= 0 && offset + length <= array.size) {
            "Unexpected arguments: $offset, $length, ${array.size}"
        }

        var offset = offset
        var length = length
        var bytesRead = 0
        if (hasLeftoverPotentiallySurrogateChar) {
            array[offset] = leftoverChar
            offset++
            length--
            hasLeftoverPotentiallySurrogateChar = false
            bytesRead = 1
            if (length == 0) return bytesRead
        }
        if (length == 1) {
            // Treat single-character array reads just like read()
            val c = oneShotReadSlowPath()
            if (c == -1) return if (bytesRead == 0) -1 else bytesRead
            array[offset] = c.toChar()
            return bytesRead + 1
        }
        return doRead(array, offset, length) + bytesRead
    }

    private fun doRead(array: CharArray, offset: Int, length: Int): Int {
        var charBuffer = CharBuffer.wrap(array, offset, length)
        if (charBuffer.position() != 0) {
            charBuffer = charBuffer.slice()
        }
        var isEof = false
        while (true) {
            val cr = decoder.decode(byteBuffer, charBuffer, isEof)
            if (cr.isUnderflow) {
                if (isEof) break
                if (!charBuffer.hasRemaining()) break
                val n = fillByteBuffer()
                if (n < 0) {
                    isEof = true
                    if (charBuffer.position() == 0 && !byteBuffer.hasRemaining()) break
                    decoder.reset()
                }
                continue
            }
            if (cr.isOverflow) {
                assert(charBuffer.position() > 0)
                break
            }
            cr.throwException()
        }
        if (isEof) decoder.reset()
        return if (charBuffer.position() == 0) -1
        else charBuffer.position()
    }

    private fun fillByteBuffer(): Int {
        byteBuffer.compact()
        try {
            // Read from the input stream, and then update the buffer
            val limit = byteBuffer.limit()
            val position = byteBuffer.position()
            val remaining = if (position <= limit) limit - position else 0
            val bytesRead = inputStream.read(byteBuffer.array(), byteBuffer.arrayOffset() + position, remaining)
            if (bytesRead < 0) return bytesRead
            // Method `position(I)LByteBuffer` does not exist in Java 8. For details, see comment for `flip` in `init` method
            (byteBuffer as Buffer).position(position + bytesRead)
        } finally {
            byteBuffer.flip()
        }
        return byteBuffer.remaining()
    }

    private fun oneShotReadSlowPath(): Int {
        // Return the leftover char, if there is one
        if (hasLeftoverPotentiallySurrogateChar) {
            hasLeftoverPotentiallySurrogateChar = false
            return leftoverChar.code
        }

        val array = CharArray(2)
        val bytesRead = read(array, 0, 2)
        return when (bytesRead) {
            -1 -> -1
            1 -> array[0].code
            2 -> {
                leftoverChar = array[1]
                hasLeftoverPotentiallySurrogateChar = true
                array[0].code
            }
            else -> error("Unreachable state: $bytesRead")
        }
    }

    public fun release() {
        ByteArrayPool8k.release(byteBuffer.array())
    }
}
