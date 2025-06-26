/*
 * Copyright 2017-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalSerializationApi::class, ExperimentalUnsignedTypes::class)

package kotlinx.serialization.cbor.internal

import kotlinx.serialization.*
import kotlinx.serialization.cbor.*

/**
 * [CborTreeReader] reads CBOR data from [parser] and constructs a [CborElement] tree.
 */
internal class CborTreeReader(
    private val configuration: CborConfiguration,
    private val parser: CborParser
) {
    /**
     * Reads the next CBOR element from the parser.
     */
    fun read(): CborElement {
        if (parser.isNull()) {
            parser.nextNull()
            return CborNull
        }

        // Try to read different types of CBOR elements
        try {
            return CborBoolean(parser.nextBoolean())
        } catch (e: CborDecodingException) {
            // Not a boolean, continue
        }

        try {
            return readArray()
        } catch (e: CborDecodingException) {
            // Not an array, continue
        }

        try {
            return readMap()
        } catch (e: CborDecodingException) {
            // Not a map, continue
        }

        try {
            return CborByteString(parser.nextByteString())
        } catch (e: CborDecodingException) {
            // Not a byte string, continue
        }

        try {
            return CborString(parser.nextString())
        } catch (e: CborDecodingException) {
            // Not a string, continue
        }

        try {
            return CborNumber.Signed(parser.nextFloat())
        } catch (e: CborDecodingException) {
            // Not a float, continue
        }

        try {
            return CborNumber.Signed(parser.nextDouble())
        } catch (e: CborDecodingException) {
            // Not a double, continue
        }

        try {
            val (value, isSigned) = parser.nextNumberWithSign()
            return if (isSigned) {
                CborNumber.Signed(value)
            } else {
                CborNumber.Unsigned(value.toULong())
            }
        } catch (e: CborDecodingException) {
            // Not a number, continue
        }

        throw CborDecodingException("Unable to decode CBOR element")
    }

    private fun readArray(): CborList {
        val size = parser.startArray()
        val elements = mutableListOf<CborElement>()

        if (size >= 0) {
            // Definite length array
            repeat(size) {
                elements.add(read())
            }
        } else {
            // Indefinite length array
            while (!parser.isEnd()) {
                elements.add(read())
            }
            parser.end()
        }

        return CborList(elements)
    }

    private fun readMap(): CborMap {
        val size = parser.startMap()
        val elements = mutableMapOf<CborElement, CborElement>()

        if (size >= 0) {
            // Definite length map
            repeat(size) {
                val key = read()
                val value = read()
                elements[key] = value
            }
        } else {
            // Indefinite length map
            while (!parser.isEnd()) {
                val key = read()
                val value = read()
                elements[key] = value
            }
            parser.end()
        }

        return CborMap(elements)
    }
}
