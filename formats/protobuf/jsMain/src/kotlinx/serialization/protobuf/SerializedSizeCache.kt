package kotlinx.serialization.protobuf

import kotlinx.serialization.descriptors.SerialDescriptor

internal actual fun createSerializedSizeCache(): SerializedSizeCache = JsHashMap()

private class JsHashMap : SerializedSizeCache {
    private val cache = mutableMapOf<SerialDescriptor, SerializedData>()

    override fun get(descriptor: SerialDescriptor, key: SerializedSizeCacheKey): Int? = cache[descriptor]?.get(key)

    override fun set(descriptor: SerialDescriptor, key: SerializedSizeCacheKey, serializedSize: Int) {
        cache[descriptor] = mapOf(key to serializedSize)
    }
}