/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalSerializationApi::class, ExperimentalUnsignedTypes::class)

package kotlinx.serialization.cbor.internal

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.cbor.*
import kotlinx.serialization.cbor.internal.CborWriter.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*
import kotlin.experimental.*

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

internal const val HEADER_BYTE_STRING: Byte = 0b010_00000
internal const val HEADER_STRING: Byte = 0b011_00000
internal const val HEADER_NEGATIVE: Byte = 0b001_00000
internal const val HEADER_ARRAY: Int = 0b100_00000
internal const val HEADER_MAP: Int = 0b101_00000
internal const val HEADER_TAG: Int = 0b110_00000

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
    return kotlin.runCatching { getElementAnnotations(index).find { it is ByteString } != null }.getOrDefault(false)
}


internal fun SerialDescriptor.isInlineByteString(): Boolean {
    // inline item classes should only have 1 item
    return isInline && isByteString(0)
}

@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.getValueTags(index: Int): ULongArray? {
    return kotlin.runCatching { (getElementAnnotations(index).find { it is ValueTags } as ValueTags?)?.tags }
        .getOrNull()
}

@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.getKeyTags(index: Int): ULongArray? {
    return kotlin.runCatching { (getElementAnnotations(index).find { it is KeyTags } as KeyTags?)?.tags }.getOrNull()
}

@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.getCborLabel(index: Int): Long? {
    return kotlin.runCatching { getElementAnnotations(index).filterIsInstance<CborLabel>().firstOrNull()?.label }
        .getOrNull()
}

@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.hasArrayTag(): Boolean {
    return annotations.filterIsInstance<CborArray>().isNotEmpty()
}
