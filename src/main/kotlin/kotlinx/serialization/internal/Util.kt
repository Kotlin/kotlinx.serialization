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