/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import java.util.concurrent.*

internal actual fun <K, V> createMapForCache(initialCapacity: Int): MutableMap<K, V> =
    ConcurrentHashMap(initialCapacity)
