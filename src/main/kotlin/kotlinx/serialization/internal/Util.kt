package kotlinx.serialization.internal

import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

/**
 *  @author Leonid Startsev
 *          sandwwraith@gmail.com
 */

fun <T> List<T>.onlySingleOrNull() = when(this.size) {
    0 -> null
    1 -> this[0]
    else -> throw IllegalStateException("Too much arguments in list")
}

fun InputStream.readExactNBytes(bytes: Int): ByteArray {
    val array = ByteArray(bytes)
    var read = 0
    while (read < bytes) {
        val i = this.read(array, read, bytes - read)
        if (i == -1) throw IOException("Unexpected EOF")
        read += i
    }
    return array
}

fun InputStream.readToByteBuffer(bytes: Int): ByteBuffer {
    val arr = readExactNBytes(bytes)
    val buf = ByteBuffer.allocate(bytes)
    buf.put(arr).flip()
    return buf
}

// Originally taken from javax.xml.bind.DatatypeConverterImpl
object HexConverter {
    fun parseHexBinary(s: String): ByteArray {
        val len = s.length

        // "111" is not a valid hex encoding.
        if (len % 2 != 0) {
            throw IllegalArgumentException("hexBinary needs to be even-length: " + s)
        }

        val out = ByteArray(len / 2)

        var i = 0
        while (i < len) {
            val h = hexToBin(s[i])
            val l = hexToBin(s[i + 1])
            if (h == -1 || l == -1) {
                throw IllegalArgumentException("contains illegal character for hexBinary: " + s)
            }

            out[i / 2] = (h * 16 + l).toByte()
            i += 2
        }

        return out
    }

    private fun hexToBin(ch: Char): Int = when (ch) {
        in '0'..'9' -> ch - '0'
        in 'A'..'F' -> ch - 'A' + 10
        in 'a'..'f' -> ch - 'a' + 10
        else -> -1
    }

    private val hexCode = "0123456789ABCDEF".toCharArray()

    fun printHexBinary(data: ByteArray, lowerCase: Boolean = false): String {
        val r = StringBuilder(data.size * 2)
        for (b in data) {
            r.append(hexCode[b.toInt() shr 4 and 0xF])
            r.append(hexCode[b.toInt() and 0xF])
        }
        return if (lowerCase) r.toString().toLowerCase() else r.toString()
    }
}