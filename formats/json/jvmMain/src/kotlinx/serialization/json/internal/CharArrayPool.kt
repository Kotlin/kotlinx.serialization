/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package kotlinx.serialization.json.internal

import java.util.concurrent.*

internal open class CharArrayPoolBase {
    private val arrays = ArrayDeque<CharArray>()
    private var charsTotal = 0

    /*
     * Not really documented kill switch as a workaround for potential
     * (unlikely) problems with memory consumptions.
     */
    private val MAX_CHARS_IN_POOL = runCatching {
        System.getProperty("kotlinx.serialization.json.pool.size").toIntOrNull()
    }.getOrNull() ?: 1024 * 1024 // 2 MB seems to be a reasonable constraint, (1M of chars)

    protected fun take(size: Int): CharArray {
        /*
         * Initially the pool is empty, so an instance will be allocated
         * and the pool will be populated in the 'release'
         */
        val candidate = synchronized(this) {
            arrays.removeLastOrNull()?.also { charsTotal -= it.size }
        }
        return candidate ?: CharArray(size)
    }

    protected fun releaseImpl(array: CharArray): Unit = synchronized(this) {
        if (charsTotal + array.size >= MAX_CHARS_IN_POOL) return@synchronized
        charsTotal += array.size
        arrays.addLast(array)
    }
}

internal object CharArrayPool : CharArrayPoolBase() {
    fun take(): CharArray = super.take(128)

    // Can release array of an arbitrary size
    fun release(array: CharArray) = releaseImpl(array)
}

// Pools char arrays of size 16K
internal actual object CharArrayPoolBatchSize : CharArrayPoolBase() {

    actual fun take(): CharArray = super.take(BATCH_SIZE)

    actual fun release(array: CharArray) {
        require(array.size == BATCH_SIZE) { "Inconsistent internal invariant: unexpected array size ${array.size}" }
        releaseImpl(array)
    }
}
