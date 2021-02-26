package kotlinx.serialization.json.internal

import java.util.concurrent.*

internal object CharArrayPool {
    private val arrays = ArrayDeque<CharArray>()
    private var charsTotal = 0
    private val MAX_CHARS_IN_POOL = runCatching {
        System.getProperty("kotlinx.serialization.json.pool.size").toIntOrNull()
    }.getOrNull() ?: 1024 * 1024 // 2 MB seems to be a reasonable contraint

    public fun take(): CharArray {
        val candidate = synchronized(this) {
            arrays.lastOrNull()?.also { charsTotal -= it.size }
        }
        return candidate ?: CharArray(128)
    }

    public fun release(array: CharArray) = synchronized(this) {
        if (charsTotal + array.size >= MAX_CHARS_IN_POOL) return@synchronized
        charsTotal += array.size
        arrays.addLast(array)
    }
}
