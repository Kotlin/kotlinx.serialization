/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

/**
 * Creates a ConcurrentHashMap on JVM and regular HashMap on other platforms.
 * To make actual use of cache in Kotlin/Native, mark a top-level object with this map
 * as a @[ThreadLocal].
 */
internal actual fun <K, V> createMapForCache(initialCapacity: Int): MutableMap<K, V> = HashMap(initialCapacity)
