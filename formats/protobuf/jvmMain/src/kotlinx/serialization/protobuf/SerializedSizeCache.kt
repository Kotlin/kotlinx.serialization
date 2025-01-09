package kotlinx.serialization.protobuf

import kotlinx.serialization.descriptors.SerialDescriptor
import java.util.concurrent.ConcurrentHashMap

internal actual fun createSerializedSizeCache(): SerializedSizeCache {
    return ConcurrentHashMapSerializedCache()
}

private class ConcurrentHashMapSerializedCache : SerializedSizeCache {
    private val cache = ConcurrentHashMap<SerialDescriptor, SerializedData>()

    override fun get(descriptor: SerialDescriptor, key: SerializedSizeCacheKey): Int? = cache[descriptor]?.get(key)

    override fun set(descriptor: SerialDescriptor, key: SerializedSizeCacheKey, serializedSize: Int) {
        cache[descriptor] = mapOf(key to serializedSize)
    }
}
