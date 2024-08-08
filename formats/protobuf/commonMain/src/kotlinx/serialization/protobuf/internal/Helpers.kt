/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.protobuf.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.protobuf.*

internal typealias ProtoDesc = Long
internal const val VARINT = 0
internal const val i64 = 1
internal const val SIZE_DELIMITED = 2
internal const val i32 = 5

internal const val ID_HOLDER_ONE_OF = -2

private const val ONEOFMASK = 1L shl 36
private const val INTTYPEMASK = 3L shl 33
private const val PACKEDMASK = 1L shl 32

@Suppress("NOTHING_TO_INLINE")
internal inline fun ProtoDesc(protoId: Int, type: ProtoIntegerType, packed: Boolean = false, oneOf: Boolean = false): ProtoDesc {
    val packedBits = if (packed) PACKEDMASK else 0L
    val oneOfBits = if (oneOf) ONEOFMASK else 0L
    return packedBits or oneOfBits or type.signature or protoId.toLong()
}

internal inline val ProtoDesc.protoId: Int get() = (this and Int.MAX_VALUE.toLong()).toInt()

internal val ProtoDesc.integerType: ProtoIntegerType
    get() = when(this and INTTYPEMASK) {
    ProtoIntegerType.DEFAULT.signature -> ProtoIntegerType.DEFAULT
    ProtoIntegerType.SIGNED.signature -> ProtoIntegerType.SIGNED
    else -> ProtoIntegerType.FIXED
}

internal val SerialDescriptor.isPackable: Boolean
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    get() = when (kind) {
        PrimitiveKind.STRING,
        !is PrimitiveKind -> false
        else -> true
    }

internal val ProtoDesc.isPacked: Boolean
    get() = (this and PACKEDMASK) != 0L

internal val ProtoDesc.isOneOf: Boolean
    get() = (this and ONEOFMASK) != 0L

internal fun ProtoDesc.overrideId(protoId: Int): ProtoDesc {
    return this and (0xFFFFFFF00000000L) or protoId.toLong()
}

internal fun SerialDescriptor.extractParameters(index: Int): ProtoDesc {
    val annotations = getElementAnnotations(index)
    var protoId: Int = index + 1
    var format: ProtoIntegerType = ProtoIntegerType.DEFAULT
    var protoPacked = false
    var isOneOf = false

    for (i in annotations.indices) { // Allocation-friendly loop
        val annotation = annotations[i]
        if (annotation is ProtoNumber) {
            protoId = annotation.number
            checkFieldNumber(protoId, this)
        } else if (annotation is ProtoType) {
            format = annotation.type
        } else if (annotation is ProtoPacked) {
            protoPacked = true
        } else if (annotation is ProtoOneOf) {
            isOneOf = true
        }
    }
    if (isOneOf) {
        // reset protoId to index-based for oneOf field,
        // Decoder will restore the real proto id then from [ProtobufDecoder.index2IdMap]
        // See [kotlinx.serialization.protobuf.internal.ProtobufDecoder.decodeElementIndex] for detail
        protoId = index + 1
    }
    return ProtoDesc(protoId, format, protoPacked, isOneOf)
}

/**
 * Get the proto id from the descriptor of [index] element,
 * or return [ID_HOLDER_ONE_OF] if such element is marked with [ProtoOneOf]
 */
internal fun extractProtoId(descriptor: SerialDescriptor, index: Int, zeroBasedDefault: Boolean): Int {
    val annotations = descriptor.getElementAnnotations(index)
    var result = if (zeroBasedDefault) index else index + 1
    for (i in annotations.indices) { // Allocation-friendly loop
        val annotation = annotations[i]
        if (annotation is ProtoOneOf) {
            // Fast return for one of field
            return ID_HOLDER_ONE_OF
        } else if (annotation is ProtoNumber) {
            result = annotation.number
            // 0 or negative numbers are acceptable for enums
            if (!zeroBasedDefault) {
                checkFieldNumber(result, descriptor)
            }
        }
    }
    return result
}

private fun checkFieldNumber(fieldNumber: Int, descriptor: SerialDescriptor) {
    if (fieldNumber <= 0) {
        throw SerializationException("$fieldNumber is not allowed in ProtoNumber for ${descriptor.serialName}, because protobuf support field values in range 1..2147483647")
    }
}

internal class ProtobufDecodingException(message: String) : SerializationException(message)

internal expect fun Int.reverseBytes(): Int
internal expect fun Long.reverseBytes(): Long


internal fun SerialDescriptor.getAllOneOfSerializerOfField(
    serializersModule: SerializersModule,
): List<SerialDescriptor> {
    return when (this.kind) {
        PolymorphicKind.OPEN -> serializersModule.getPolymorphicDescriptors(this)
        PolymorphicKind.SEALED -> getElementDescriptor(1).elementDescriptors.toList()
        else -> throw IllegalArgumentException("Class ${this.serialName} should be abstract or sealed or interface to be used as @ProtoOneOf property.")
    }.onEach { desc ->
        if (desc.getElementAnnotations(0).none { anno -> anno is ProtoNumber }) {
            throw IllegalArgumentException("${desc.serialName} implementing oneOf type ${this.serialName} should have @ProtoNumber annotation in its single property.")
        }
    }
}

internal fun SerialDescriptor.getActualOneOfSerializer(
    serializersModule: SerializersModule,
    protoId: Int
): SerialDescriptor? {
    return getAllOneOfSerializerOfField(serializersModule).find { it.extractParameters(0).protoId == protoId }
}
