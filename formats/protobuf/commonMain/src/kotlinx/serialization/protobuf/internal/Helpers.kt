/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.protobuf.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.protobuf.*

internal typealias ProtoDesc = Long
internal const val VARINT = 0
internal const val i64 = 1
internal const val SIZE_DELIMITED = 2
internal const val i32 = 5

private const val MASK = Int.MAX_VALUE.toLong() shl 32

@Suppress("NOTHING_TO_INLINE")
internal inline fun ProtoDesc(protoId: Int, type: ProtoIntegerType): ProtoDesc {
    return type.signature or protoId.toLong()
}

internal inline val ProtoDesc.protoId: Int get() = (this and Int.MAX_VALUE.toLong()).toInt()

internal val ProtoDesc.integerType: ProtoIntegerType
    get() = when(this and MASK) {
    ProtoIntegerType.DEFAULT.signature -> ProtoIntegerType.DEFAULT
    ProtoIntegerType.SIGNED.signature -> ProtoIntegerType.SIGNED
    else -> ProtoIntegerType.FIXED
}

internal fun SerialDescriptor.extractParameters(index: Int): ProtoDesc {
    val annotations = getElementAnnotations(index)
    var protoId: Int = index + 1
    var format: ProtoIntegerType = ProtoIntegerType.DEFAULT
    for (i in annotations.indices) { // Allocation-friendly loop
        val annotation = annotations[i]
        if (annotation is ProtoNumber) {
            protoId = annotation.number
        } else if (annotation is ProtoType) {
            format = annotation.type
        }
    }
    return ProtoDesc(protoId, format)
}

internal fun extractProtoId(descriptor: SerialDescriptor, index: Int, zeroBasedDefault: Boolean): Int {
    val annotations = descriptor.getElementAnnotations(index)
    for (i in annotations.indices) { // Allocation-friendly loop
        val annotation = annotations[i]
        if (annotation is ProtoNumber) {
            return annotation.number
        }
    }
    return if (zeroBasedDefault) index else index + 1
}

internal class ProtobufDecodingException(message: String) : SerializationException(message)

internal expect fun Int.reverseBytes(): Int
internal expect fun Long.reverseBytes(): Long
