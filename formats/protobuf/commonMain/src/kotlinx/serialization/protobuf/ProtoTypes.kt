/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*

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
 */
@Suppress("NO_EXPLICIT_VISIBILITY_IN_API_MODE_WARNING")
public enum class ProtoNumberType(internal val signature: Long) {
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

internal val ProtoDesc.numberType: ProtoNumberType get() {
    if (ProtoNumberType.DEFAULT.equalTo(this)) return ProtoNumberType.DEFAULT
    if (ProtoNumberType.SIGNED.equalTo(this)) return ProtoNumberType.SIGNED
    return ProtoNumberType.FIXED
}

internal fun extractParameters(descriptor: SerialDescriptor, index: Int): ProtoDesc {
    val annotations = descriptor.getElementAnnotations(index)
    var protoId: Int = index + 1
    var format: ProtoNumberType = ProtoNumberType.DEFAULT
    for (annotation in annotations) {
        if (annotation is ProtoId) {
            protoId = annotation.id
        } else if (annotation is ProtoType) {
            format = annotation.type
        }
    }
    return ProtoDesc(protoId, format)
}

internal fun extractProtoId(descriptor: SerialDescriptor, index: Int, zeroBasedDefault: Boolean = false): Int {
    val protoId = descriptor.findAnnotation<ProtoId>(index)
    return protoId?.id ?: (if (zeroBasedDefault) index else index + 1)
}

public class ProtobufDecodingException(message: String) : SerializationException(message)

internal inline fun <reified A: Annotation> SerialDescriptor.findAnnotation(elementIndex: Int): A? {
    return getElementAnnotations(elementIndex).find { it is A } as A?
}
