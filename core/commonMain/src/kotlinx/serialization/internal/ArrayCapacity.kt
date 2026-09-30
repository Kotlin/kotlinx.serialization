/*
 * Copyright 2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.SerializationException

// Some VMs reserve header words in an array, same soft limit as in java.util.ArrayList
private const val MAX_ARRAY_CAPACITY = Int.MAX_VALUE - 8

/**
 * Returns the new capacity for a growing array: 1.5 times [currentCapacity], but at least [requiredCapacity].
 * Throws [SerializationException] if [requiredCapacity] is negative (overflow at the call site) or too large for an array.
 */
@CoreFriendModuleApi
public fun newArrayCapacity(currentCapacity: Int, requiredCapacity: Int): Int {
    if (requiredCapacity < 0 || requiredCapacity > MAX_ARRAY_CAPACITY) {
        throw SerializationException("Required array capacity exceeds the maximum supported array size of $MAX_ARRAY_CAPACITY elements")
    }
    val grownCapacity = currentCapacity.toLong() + (currentCapacity shr 1)
    return grownCapacity.coerceIn(requiredCapacity.toLong(), MAX_ARRAY_CAPACITY.toLong()).toInt()
}
