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
        when (parser.curByte shr 5) { // Get major type from the first 3 bits
            0 -> { // Major type 0: unsigned integer
                val value = parser.nextNumber()
                return CborPositiveInt(value.toULong())
            }

            1 -> { // Major type 1: negative integer
                val value = parser.nextNumber()
                return CborNegativeInt(value)
            }

            2 -> { // Major type 2: byte string
                return CborByteString(parser.nextByteString())
            }

            3 -> { // Major type 3: text string
                return CborString(parser.nextString())
            }

            4 -> { // Major type 4: array
                return readArray()
            }

            5 -> { // Major type 5: map
                return readMap()
            }

            7 -> { // Major type 7: simple/float/break
                when (parser.curByte) {
                    0xF4 -> {
                        parser.readByte() // Advance parser position
                        return CborBoolean(false)
                    }
                    0xF5 -> {
                        parser.readByte() // Advance parser position
                        return CborBoolean(true)
                    }
                    0xF6, 0xF7 -> {
                        parser.nextNull()
                        return CborNull
                    }
                    NEXT_HALF, NEXT_FLOAT, NEXT_DOUBLE -> return CborDouble(parser.nextDouble()) // Half/Float32/Float64
                    else -> throw CborDecodingException(
                        "Invalid simple value or float type: ${
                            parser.curByte.toString(
                                16
                            )
                        }"
                    )
                }
            }

            else -> throw CborDecodingException("Invalid CBOR major type: ${parser.curByte shr 5}")
        }
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