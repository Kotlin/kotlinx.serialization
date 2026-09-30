/*
 * Copyright 2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.SerializationException

// Some VMs reserve header words in an array, same soft limit as in java.util.ArrayList
private const val MAX_ARRAY_CAPACITY = Int.MAX_VALUE - 8

/**
 * Returns the new capacity for a growing array: 1.5 times [currentCapacity], but at least [requiredCapacity].
 * Throws [SerializationException] if [requiredCapacity] is too large for an array.
 */
@CoreFriendModuleApi
public fun newArrayCapacity(currentCapacity: Int, requiredCapacity: Long): Int {
    if (requiredCapacity > MAX_ARRAY_CAPACITY) {
        throw SerializationException("Cannot grow array to $requiredCapacity elements, the maximum supported array size is $MAX_ARRAY_CAPACITY")
    }
    val grownCapacity = currentCapacity.toLong() + (currentCapacity shr 1)
    return grownCapacity.coerceIn(requiredCapacity, MAX_ARRAY_CAPACITY.toLong()).toInt()
}
