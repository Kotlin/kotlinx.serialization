/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

private typealias DescriptorData<T> = MutableMap<DescriptorSchemaCache.Key<T>, T>

/**
 * A type-safe map for storing custom information (such as format schema), associated with [SerialDescriptor].
 *
 * This map is not thread safe. For thread safety and performance reasons, it is advised to pass it via stack (e.g. in function parameters)
 */
internal class DescriptorSchemaCache {
    private val map: MutableMap<SerialDescriptor, DescriptorData<Any>> = createMapForCache()

    @Suppress("UNCHECKED_CAST")
    public operator fun <T : Any> set(descriptor: SerialDescriptor, key: Key<T>, value: T) {
        map.getOrPut(descriptor, ::hashMapOf)[key as Key<Any>] = value as Any
    }

    public fun <T : Any> getOrPut(descriptor: SerialDescriptor, key: Key<T>, defaultValue: () -> T): T {
        get(descriptor, key)?.let { return it }
        val value = defaultValue()
        set(descriptor, key, value)
        return value
    }

    @Suppress("UNCHECKED_CAST")
    public operator fun <T : Any> get(descriptor: SerialDescriptor, key: Key<T>): T? {
        return map[descriptor]?.get(key as Key<Any>) as? T
    }

    /**
     * A key for associating user data of type [T] with a given [SerialDescriptor].
     */
    public class Key<T : Any> {}
}
