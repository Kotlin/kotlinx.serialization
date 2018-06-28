package kotlinx.serialization.hashing

import com.google.common.hash.*
import kotlinx.serialization.*

/**
 * Primitive used to calculate any kinds of hash or checksum for arbitraty object
 * as long as it's supported by serialization framework.
 *
 * Behaviour of hasher is unspecified, if output is used more than once after [makeLongHash] or [makeByteArrayHash] calls
 * if [reset] was not called.
 *
 * TODO:
 * Extract resettability into different interface?
 * Consider implementing analogue of [HashCode] and family of default methods to simplify basic implementation,
 * but allow to make garbage-free versions of methods if necessary
 */
interface Hasher {

    /**
     * Output realted to current instance of hasher which is used by [KSerializer]
     * to feed object into hasher.
     *
     * Implementation is free to cache or reuse same output multiple times, so
     * any third-party caching of such outputs is prohibited
     */
    val output: KOutput


    fun makeLongHash(): Long

    fun makeByteArrayHash(): ByteArray

    /**
     * Resets internal state of the hasher to be reused.
     * @throws [UnsupportedOperationException] if reset is not supported by the underlying implementation
     */
    fun reset(): Unit = throw UnsupportedOperationException("Current hasher ($this) is not resettable")
}
