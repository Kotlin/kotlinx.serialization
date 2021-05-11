package kotlinx.serialization.json.internal

import java.util.concurrent.*

internal object CharArrayPool {
    private val arrays = ArrayDeque<CharArray>()
    private var charsTotal = 0
    /*
     * Not really documented kill switch as a workaround for potential
     * (unlikely) problems with memory consumptions.
     */
    private val MAX_CHARS_IN_POOL = runCatching {
        System.getProperty("kotlinx.serialization.json.pool.size").toIntOrNull()
    }.getOrNull() ?: 1024 * 1024 // 2 MB seems to be a reasonable constraint, (1M of chars)

    fun take(): CharArray {
        /*
         * Initially the pool is empty, so an instance will be allocated
         * and the pool will be populated in the 'release'
         */
        val candidate = synchronized(this) {
            arrays.removeLastOrNull()?.also { charsTotal -= it.size }
        }
        return candidate ?: CharArray(128)
    }

    fun release(array: CharArray) = synchronized(this) {
        if (charsTotal + array.size >= MAX_CHARS_IN_POOL) return@synchronized
        charsTotal += array.size
        arrays.addLast(array)
    }
}
