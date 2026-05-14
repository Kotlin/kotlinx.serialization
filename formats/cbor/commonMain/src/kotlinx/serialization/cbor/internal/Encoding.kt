/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalSerializationApi::class, ExperimentalUnsignedTypes::class)

package kotlinx.serialization.cbor.internal

import kotlinx.serialization.*
import kotlinx.serialization.cbor.*
import kotlinx.serialization.descriptors.*

internal const val FALSE = 0xf4
internal const val TRUE = 0xf5
internal const val NULL = 0xf6
internal const val EMPTY_MAP = 0xa0

internal const val NEXT_HALF = 0xf9
internal const val NEXT_FLOAT = 0xfa
internal const val NEXT_DOUBLE = 0xfb

internal const val BEGIN_ARRAY = 0x9f
internal const val BEGIN_MAP = 0xbf
internal const val BREAK = 0xff

internal const val ADDITIONAL_INFORMATION_INDEFINITE_LENGTH = 0x1f

internal const val MAJOR_TYPE_MASK: Int = 0b111_00000
internal const val ADDITIONAL_INFO_MASK: Int = 0b000_11111

internal const val HEADER_BYTE_STRING: Int = 0b010_00000
internal const val HEADER_STRING: Int = 0b011_00000
internal const val HEADER_POSITIVE: Byte = 0b000_00000
internal const val HEADER_NEGATIVE: Byte = 0b001_00000
internal const val HEADER_ARRAY: Int = 0b100_00000
internal const val HEADER_MAP: Int = 0b101_00000
internal const val HEADER_TAG: Int = 0b110_00000
internal const val HEADER_FP_AND_SIMPLE: Int = 0b111_00000

/** Value to represent an indefinite length CBOR item within a "length stack". */
internal const val LENGTH_STACK_INDEFINITE = -1

internal const val HALF_PRECISION_EXPONENT_BIAS = 15
internal const val HALF_PRECISION_MAX_EXPONENT = 0x1f
internal const val HALF_PRECISION_MAX_MANTISSA = 0x3ff

internal const val SINGLE_PRECISION_EXPONENT_BIAS = 127
internal const val SINGLE_PRECISION_MAX_EXPONENT = 0xFF

internal const val SINGLE_PRECISION_NORMALIZE_BASE = 0.5f


@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.isByteString(index: Int): Boolean {
    return getElementAnnotations(index).find { it is ByteString } != null
}


internal fun SerialDescriptor.isInlineByteString(): Boolean {
    // inline item classes should only have 1 item
    return isInline && isByteString(0)
}

@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.getValueTags(index: Int): ULongArray? = findAnnotation<ValueTags>(index)?.tags

@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.getKeyTags(index: Int): ULongArray? = findAnnotation<KeyTags>(index)?.tags

@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.getCborLabel(index: Int): Long? = findAnnotation<CborLabel>(index)?.label

@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.hasArrayTag(): Boolean {
    return annotations.any { it is CborArray }
}

@OptIn(ExperimentalSerializationApi::class)
internal inline fun <reified A : Annotation> SerialDescriptor.findAnnotation(elementIndex: Int): A? =
    getElementAnnotations(elementIndex).firstOrNull { it is A } as A?


@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.getObjectTags(): ULongArray? {
    return annotations.filterIsInstance<ObjectTags>().firstOrNull()?.tags
}

internal val Int.majorTypeName: String
    get() = when (this and MAJOR_TYPE_MASK) {
        HEADER_BYTE_STRING -> "byte string"
        HEADER_STRING -> "string"
        HEADER_ARRAY -> "array"
        HEADER_MAP -> "map"
        HEADER_TAG -> "tag"
        HEADER_POSITIVE.toInt() -> "unsigned integer"
        HEADER_NEGATIVE.toInt() -> "negative integer"
        HEADER_FP_AND_SIMPLE -> {
            val value = this and ADDITIONAL_INFO_MASK
            "simple value - ${value.simpleValueKindName}"
        }
        else -> throw UnsupportedOperationException("Unreachable")
    }

// See https://datatracker.ietf.org/doc/html/rfc8949#section-3.3
private val Int.simpleValueKindName: String
    get() = when (this) {
        20 -> "false"
        21 -> "true"
        22 -> "null"
        23 -> "undefined"
        24 -> "value 32..255 in the next byte"
        25 -> "half-precision floating point number"
        26 -> "single-precision floating point number"
        27 -> "double-precision floating point number"
        in 28..30 -> "reserved"
        31 -> "break for indefinite length items"
        else -> "unassigned"
    }
