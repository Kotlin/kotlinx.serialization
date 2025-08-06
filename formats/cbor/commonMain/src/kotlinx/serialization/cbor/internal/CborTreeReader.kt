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
    //no config values make sense here, because we have no "schema".
    //we cannot validate tags, or disregard nulls, can we?!
    //still, this needs to go here, in case it evolves to a point where we need to respect certain config values
    private val configuration: CborConfiguration,
    private val parser: CborParserInterface
) {
    /**
     * Reads the next CBOR element from the parser.
     */
    fun read(): CborElement {
        // Read any tags before the actual value
        val tags = readTags()

        val result = when (parser.curByte shr 5) { // Get major type from the first 3 bits
            0 -> { // Major type 0: unsigned integer
                val value = parser.nextNumber()
                CborPositiveInt(value.toULong(), tags)
            }

            1 -> { // Major type 1: negative integer
                val value = parser.nextNumber()
                CborNegativeInt(value, tags)
            }

            2 -> { // Major type 2: byte string
                CborByteString(parser.nextByteString(), tags)
            }

            3 -> { // Major type 3: text string
                CborString(parser.nextString(), tags)
            }

            4 -> { // Major type 4: array
                readArray(tags)
            }

            5 -> { // Major type 5: map
                readMap(tags)
            }

            7 -> { // Major type 7: simple/float/break
                when (parser.curByte) {
                    0xF4 -> {
                        parser.readByte() // Advance parser position
                        CborBoolean(false, tags)
                    }

                    0xF5 -> {
                        parser.readByte() // Advance parser position
                        CborBoolean(true, tags)
                    }

                    0xF6, 0xF7 -> {
                        parser.nextNull()
                        CborNull(tags)
                    }
                    // Half/Float32/Float64
                    NEXT_HALF, NEXT_FLOAT, NEXT_DOUBLE -> CborDouble(parser.nextDouble(), tags)
                    else -> throw CborDecodingException(
                        "Invalid simple value or float type: ${parser.curByte.toString(16)}"
                    )
                }
            }

            else -> throw CborDecodingException("Invalid CBOR major type: ${parser.curByte shr 5}")
        }
        return result
    }

    /**
     * Reads any tags preceding the current value.
     * @return An array of tags, possibly empty
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    private fun readTags(): ULongArray {
        val tags = mutableListOf<ULong>()

        // Read tags (major type 6) until we encounter a non-tag
        while ((parser.curByte shr 5) == 6) { // Major type 6: tag
            val tag = parser.nextTag()
            tags.add(tag)
        }

        return tags.toULongArray()
    }


    private fun readArray(tags: ULongArray): CborList {
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

        return CborList(elements, tags)
    }

    private fun readMap(tags: ULongArray): CborMap {
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

        return CborMap(elements, tags)
    }
}
