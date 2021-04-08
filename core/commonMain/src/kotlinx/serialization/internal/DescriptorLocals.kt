package kotlinx.serialization.internal

import kotlinx.serialization.descriptors.*

public interface SerialDescriptorLocal<T: Any> {
    // TODO this design is obviously not sufficient
    fun compute(descriptor: SerialDescriptor): T
}

public interface SerialDescriptorLocalSupport {
    fun <T: Any> getLocal(local: SerialDescriptorLocal<T>): T?
    fun <T: Any> putLocal(local: SerialDescriptorLocal<T>, value: T)

    // classloader and memory leaks
    // fun remove(SerialDescriptorLocal)
    // fun removeAll()
}

public inline fun <T: Any> SerialDescriptorLocalSupport.computeIfAbsent(key: SerialDescriptorLocal<T>): T {
    getLocal(key)?.let { return it }
    val r = key.compute(this as SerialDescriptor)
    putLocal(key, r)
    return r
}

@PublishedApi
internal abstract class AbstractSerialDescriptorLocalSupport : SerialDescriptorLocalSupport {

    // TODO swap to map when there is more than X keys
    private val keys: Array<SerialDescriptorLocal<*>?> = arrayOfNulls(16)
    private val values: Array<Any?> = arrayOfNulls(16)
    // TODO should be atomic
    private var size = 0

    override fun <T: Any> getLocal(local: SerialDescriptorLocal<T>): T? {
        // Linear search >= hash lookups
        for (i in 0 until size) {
            if (keys[i] === local) return values[i] as T?
        }
        return null
    }

    // TODO do linear search
    override fun <T: Any> putLocal(local: SerialDescriptorLocal<T>, value: T) {
        val idx = size++
        keys[idx] = local
        values[idx] = value
    }

}
