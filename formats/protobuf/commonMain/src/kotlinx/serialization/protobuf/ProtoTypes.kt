/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlin.jvm.*

private const val MASK = Int.MAX_VALUE.toLong() shl 32

/**
 * Represents a number format in protobuf encoding.
 *
 * [DEFAULT] is default varint encoding (intXX),
 * [SIGNED] is signed ZigZag representation (sintXX), and
 * [FIXED] is fixedXX type.
 * uintXX and sfixedXX are not supported yet.
 *
 * See [https://developers.google.com/protocol-buffers/docs/proto#scalar]
 * @see ProtoType
 */
@Suppress("NO_EXPLICIT_VISIBILITY_IN_API_MODE_WARNING")
public enum class ProtoNumberType(@JvmField internal val signature: Long) {
    DEFAULT(1L shl 32),
    SIGNED(2L shl 32),
    FIXED(3L shl 32);

    internal fun equalTo(descriptor: ProtoDesc): Boolean {
        return descriptor and MASK == signature
    }
}

/**
 * Instructs to use a particular [ProtoNumberType] for a property of integer number type.
 * Affect [Byte], [Short], [Int], [Long] and [Char] properties and does not affect others.
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class ProtoType(public val type: ProtoNumberType)

internal typealias ProtoDesc = Long

@Suppress("NOTHING_TO_INLINE")
internal inline fun ProtoDesc(protoId: Int, type: ProtoNumberType): ProtoDesc {
    return type.signature or protoId.toLong()
}

internal inline val ProtoDesc.protoId: Int get() = (this and Int.MAX_VALUE.toLong()).toInt()

internal val ProtoDesc.numberType: ProtoNumberType get() = when(this and MASK) {
    ProtoNumberType.DEFAULT.signature -> ProtoNumberType.DEFAULT
    ProtoNumberType.SIGNED.signature -> ProtoNumberType.SIGNED
    else -> ProtoNumberType.FIXED
}

internal fun SerialDescriptor.extractParameters(index: Int): ProtoDesc {
    val annotations = getElementAnnotations(index)
    var protoId: Int = index + 1
    var format: ProtoNumberType = ProtoNumberType.DEFAULT
    for (i in annotations.indices) { // Allocation-friendly loop
        val annotation = annotations[i]
        if (annotation is ProtoId) {
            protoId = annotation.id
        } else if (annotation is ProtoType) {
            format = annotation.type
        }
    }
    return ProtoDesc(protoId, format)
}

internal fun extractProtoId(descriptor: SerialDescriptor, index: Int, zeroBasedDefault: Boolean = false): Int {
    val annotations = descriptor.getElementAnnotations(index)
    for (i in annotations.indices) { // Allocation-friendly loop
        val annotation = annotations[i]
        if (annotation is ProtoId) {
            return annotation.id
        }
    }
    return if (zeroBasedDefault) index else index + 1
}

public class ProtobufDecodingException(message: String) : SerializationException(message)

internal expect fun Int.reverseBytes(): Int
internal expect fun Long.reverseBytes(): Long
