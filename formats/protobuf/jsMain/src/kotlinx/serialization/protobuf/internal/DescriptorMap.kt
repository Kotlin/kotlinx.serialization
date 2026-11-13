/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf.internal

import kotlinx.serialization.descriptors.*

internal actual fun <V : Any> createDescriptorMap(): DescriptorMap<V> = SingleThreadedDescriptorMap()

private class SingleThreadedDescriptorMap<V : Any> : DescriptorMap<V> {
    private val map = HashMap<SerialDescriptor, V>()

    override fun get(descriptor: SerialDescriptor): V? = map[descriptor]

    override fun putIfAbsent(descriptor: SerialDescriptor, value: V) {
        if (map.size < MAX_CACHED_DESCRIPTORS && descriptor !in map) map[descriptor] = value
    }

    override val size: Int get() = map.size
}
