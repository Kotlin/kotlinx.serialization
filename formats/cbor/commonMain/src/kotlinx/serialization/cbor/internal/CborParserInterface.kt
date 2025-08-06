/*
 * Copyright 2017-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalSerializationApi::class, ExperimentalUnsignedTypes::class)

package kotlinx.serialization.cbor.internal

import kotlinx.serialization.*

/**
 * Common interface for CBOR parsers that can read CBOR data from different sources.
 */
internal interface CborParserInterface {
    // Basic state checks
    fun isNull(): Boolean
    fun isEnd(): Boolean
    fun end()
    
    // Collection operations
    fun startArray(tags: ULongArray? = null): Int
    fun startMap(tags: ULongArray? = null): Int
    
    // Value reading operations
    fun nextNull(tags: ULongArray? = null): Nothing?
    fun nextBoolean(tags: ULongArray? = null): Boolean
    fun nextNumber(tags: ULongArray? = null): Long
    fun nextString(tags: ULongArray? = null): String
    fun nextByteString(tags: ULongArray? = null): ByteArray
    fun nextDouble(tags: ULongArray? = null): Double
    fun nextFloat(tags: ULongArray? = null): Float
    
    // Map key operations
    fun nextTaggedStringOrNumber(): Triple<String?, Long?, ULongArray?>
    
    // Skip operations
    fun skipElement(tags: ULongArray?)
    
    // Tag verification
    fun verifyTagsAndThrow(expected: ULongArray, actual: ULongArray?)
    
    // Additional methods needed for CborTreeReader
    fun nextTag(): ULong
    fun readByte(): Int
    
    // Properties needed for CborTreeReader
    val curByte: Int
}