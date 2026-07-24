/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
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
    @Suppress("UNUSED") private val configuration: CborConfiguration,
    private val parser: StreamingCborParser
) {
    /**
     * Reads the next CBOR element from the parser.
     */
    fun read(): CborElement {
        // Read any tags before the actual value
        val tags = readTags()

        if (parser.isEof()) throw CborDecodingException("Unexpected EOF")

        val majorType = parser.curByte and MAJOR_TYPE_MASK
        val result = when (majorType) { // Get major type from the first 3 bits
            HEADER_POSITIVE -> { // Major type 0: unsigned integer
                val value = parser.nextULong()
                CborInteger(value, isPositive = true, tags = tags)
            }

            HEADER_NEGATIVE -> { // Major type 1: negative integer
                val value = parser.nextULong() + 1uL
                CborInteger(value, isPositive = false, tags = tags)
            }

            HEADER_BYTE_STRING -> { // Major type 2: byte string
                CborByteString(parser.nextByteString(), tags = tags)
            }

            HEADER_STRING -> { // Major type 3: text string
                CborString(parser.nextString(), tags = tags)
            }

            HEADER_ARRAY -> { // Major type 4: array
                readArray(tags)
            }

            HEADER_MAP -> { // Major type 5: map
                readMap(tags)
            }

            HEADER_FP_AND_SIMPLE -> { // Major type 7: simple/float/break
                when (parser.curByte) {
                    FALSE, TRUE -> {
                        CborBoolean(parser.nextBoolean(null), tags = tags)
                    }

                    NULL -> {
                        parser.nextNull(null)
                        CborNull(tags = tags)
                    }

                    UNDEFINED -> {
                        parser.skipElement(null)
                        CborUndefined(tags = tags)
                    }

                    // Half/Float32/Float64
                    NEXT_HALF, NEXT_FLOAT, NEXT_DOUBLE -> CborFloat(parser.nextDouble(null), tags = tags)
                    else -> throw CborDecodingException(
                        "Invalid simple value or float type: ${parser.curByte.toString(16).uppercase()}"
                    )
                }
            }

            else -> {
                val errByte = parser.curByte shr 5
                throw CborDecodingException("Invalid CBOR major type: $errByte")
            }
        }
        return result
    }

    /**
     * Reads any tags preceding the current value.
     * @return An array of tags, possibly empty
     */
    private fun readTags(): ULongArray {
        if (parser.curByte and MAJOR_TYPE_MASK != HEADER_TAG) return EMPTY_TAGS

        val tags = mutableListOf<ULong>()
        while ((parser.curByte and MAJOR_TYPE_MASK) == HEADER_TAG) { // Major type 6: tag
            tags.add(parser.nextTag())
        }
        return tags.toULongArray()
    }

    private fun readArray(tags: ULongArray): CborArray {
        val size = parser.startArray(null)
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

        return CborArray(elements, tags = tags)
    }

    private fun readMap(tags: ULongArray): CborMap {
        val size = parser.startMap(null)
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

        return CborMap(elements, tags = tags)
    }
}
