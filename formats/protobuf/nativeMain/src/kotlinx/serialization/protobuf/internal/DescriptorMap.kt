/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf.internal

import kotlinx.serialization.descriptors.*
import kotlin.concurrent.*

internal actual fun <V : Any> createDescriptorMap(): DescriptorMap<V> = CopyOnWriteDescriptorMap()

private class CopyOnWriteDescriptorMap<V : Any> : DescriptorMap<V> {
    private val ref = AtomicReference<Map<SerialDescriptor, V>>(emptyMap())

    override fun get(descriptor: SerialDescriptor): V? = ref.value[descriptor]

    override fun putIfAbsent(descriptor: SerialDescriptor, value: V) {
        while (true) {
            val current = ref.value
            if (current.size >= MAX_CACHED_DESCRIPTORS || descriptor in current) return
            val updated = HashMap(current)
            updated[descriptor] = value
            if (ref.compareAndSet(current, updated)) return
        }
    }

    override val size: Int get() = ref.value.size
}
