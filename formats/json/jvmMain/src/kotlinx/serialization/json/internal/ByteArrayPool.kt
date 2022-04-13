package kotlinx.serialization.json.internal

internal object ByteArrayPool {
    private val arrays = ArrayDeque<ByteArray>()
    private var charsTotal = 0
    /*
     * Not really documented kill switch as a workaround for potential
     * (unlikely) problems with memory consumptions.
     */
    private val MAX_CHARS_IN_POOL = runCatching {
        System.getProperty("kotlinx.serialization.json.pool.size").toIntOrNull()
    }.getOrNull() ?: 2 * 1024 * 1024 // 2 MB seems to be a reasonable constraint, (1M of chars)

    fun take(): ByteArray {
        /*
         * Initially the pool is empty, so an instance will be allocated
         * and the pool will be populated in the 'release'
         */
        val candidate = synchronized(this) {
            arrays.removeLastOrNull()?.also { charsTotal -= it.size }
        }
        return candidate ?: ByteArray(512)
    }

    fun release(array: ByteArray) = synchronized(this) {
        if (charsTotal + array.size >= MAX_CHARS_IN_POOL) return@synchronized
        charsTotal += array.size
        arrays.addLast(array)
    }
}
