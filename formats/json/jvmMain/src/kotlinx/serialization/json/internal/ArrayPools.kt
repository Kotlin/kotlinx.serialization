/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package kotlinx.serialization.json.internal

/*
 * Not really documented kill switch as a workaround for potential
 * (unlikely) problems with memory consumptions.
 */
private val MAX_CHARS_IN_POOL = runCatching {
    System.getProperty("kotlinx.serialization.json.pool.size").toIntOrNull()
}.getOrNull() ?: 2 * 1024 * 1024

internal open class CharArrayPoolBase {
    private val arrays = ArrayDeque<CharArray>()
    private var charsTotal = 0

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

// Byte array pool

internal open class ByteArrayPoolBase {
    private val arrays = ArrayDeque<kotlin.ByteArray>()
    private var bytesTotal = 0

    protected fun take(size: Int): ByteArray {
        /*
         * Initially the pool is empty, so an instance will be allocated
         * and the pool will be populated in the 'release'
         */
        val candidate = synchronized(this) {
            arrays.removeLastOrNull()?.also { bytesTotal -= it.size / 2 }
        }
        return candidate ?: ByteArray(size)
    }

    protected fun releaseImpl(array: ByteArray): Unit = synchronized(this) {
        if (bytesTotal + array.size >= MAX_CHARS_IN_POOL) return@synchronized
        bytesTotal += array.size / 2
        arrays.addLast(array)
    }
}

internal object ByteArrayPool8k : ByteArrayPoolBase() {
    fun take(): ByteArray = super.take(8196)

    fun release(array: ByteArray) = releaseImpl(array)
}


internal object ByteArrayPool : ByteArrayPoolBase() {
    fun take(): ByteArray = super.take(512)

    fun release(array: ByteArray) = releaseImpl(array)
}
