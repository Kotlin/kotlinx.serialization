/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package kotlinx.serialization.json.internal

import java.util.concurrent.locks.StampedLock

/*
 * Not really documented kill switch as a workaround for potential
 * (unlikely) problems with memory consumptions.
 */
private val MAX_CHARS_IN_POOL = runCatching {
    System.getProperty("kotlinx.serialization.json.pool.size")?.toIntOrNull()
}.getOrNull() ?: (2 * 1024 * 1024)

/**
 * Supporting different scenarios and access patterns poses a challenge w.r.t. performance:
 * - `synchronized` sections shows nice performance in a single-threaded scenario
 * - `synchronized` shows significantly worse performance in a contended multithreaded access scenario
 * - `ReentrantLock` shows much better performance in a multithreaded access scenario,
 *    but it is slightly slower than `synchronized` in a single-threaded scenario
 * - `StampedLock`'s write lock is comparable w/ `synchronized` in a single-threaded scenario
 *    and with `ReentrantLock` in a multithreaded scenario
 * - On Android, everything performs worse compared to `synchronized` in a single-threaded scenario,
 *   and there's no `StampedLock` until API level 24.
 *
 * This set of constraints created a monster - we check a platform and choose a lock implementation accordingly.
 */
private object LockSupport {
    @JvmField
    public val isAndroid = System.getProperty("java.vm.name") == "Dalvik"

    class FallbackLockImplementation

    @SuppressAnimalSniffer // StampedLock
    inline fun <T> withLock(lock: Any, block: () -> T): T {
        if (isAndroid || lock is FallbackLockImplementation) {
            synchronized(lock) {
                return block()
            }
        } else {
            lock as StampedLock
            val stamp = lock.writeLock()
            try {
                return block()
            } finally {
                lock.unlockWrite(stamp)
            }
        }
    }

    @SuppressAnimalSniffer // StampedLock
    fun initLock(): Any {
        return if (isAndroid) {
            FallbackLockImplementation()
        } else {
            try {
                StampedLock()
            } catch (_: Throwable) {
                // If, for some reason, isAndroid returned false, but StampedLock is not available,
                // fallback to the synchronized.
                FallbackLockImplementation()
            }
        }
    }
}

internal open class CharArrayPoolBase {
    private val arrays = ArrayList<CharArray>()
    private var charsTotal = 0
    private val lock = LockSupport.initLock()

    @SuppressAnimalSniffer // withLock
    protected fun take(size: Int): CharArray {
        val candidate = LockSupport.withLock(lock) {
            arrays.removeLastOrNull()?.also { charsTotal -= it.size }
        }
        return candidate ?: CharArray(size)
    }

    @SuppressAnimalSniffer // withLock
    protected fun releaseImpl(array: CharArray) = LockSupport.withLock(lock) {
        if (charsTotal + array.size >= MAX_CHARS_IN_POOL) return@withLock
        charsTotal += array.size
        arrays.add(array)
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
    private val arrays = ArrayList<ByteArray>()
    private var bytesTotal = 0
    private val lock = LockSupport.initLock()

    @SuppressAnimalSniffer // withLock
    protected fun take(size: Int): ByteArray {
        /*
         * Initially the pool is empty, so an instance will be allocated
         * and the pool will be populated in the 'release'
         */
        val candidate = LockSupport.withLock(lock) {
            arrays.removeLastOrNull()?.also { bytesTotal -= it.size / 2 }
        }
        return candidate ?: ByteArray(size)
    }

    @SuppressAnimalSniffer // withLock
    protected fun releaseImpl(array: ByteArray): Unit = LockSupport.withLock(lock) {
        if (bytesTotal + array.size >= MAX_CHARS_IN_POOL) return
        bytesTotal += array.size / 2
        arrays.add(array)
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
