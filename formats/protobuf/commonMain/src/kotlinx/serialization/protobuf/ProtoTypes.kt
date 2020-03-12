/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*

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
public enum class ProtoNumberType {
    DEFAULT, SIGNED, FIXED
}

/**
 * Instructs to use a particular [ProtoNumberType] for a property of integer number type.
 * Affect [Byte], [Short], [Int], [Long] and [Char] properties and does not affect others.
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class ProtoType(public val type: ProtoNumberType)

internal typealias ProtoDesc = Pair<Int, ProtoNumberType>

internal fun extractParameters(descriptor: SerialDescriptor, index: Int, zeroBasedDefault: Boolean = false): ProtoDesc {
    val protoId = descriptor.findAnnotation<ProtoId>(index)
    val idx = protoId?.id ?: (if (zeroBasedDefault) index else index + 1)
    val format = descriptor.findAnnotation<ProtoType>(index)?.type
            ?: ProtoNumberType.DEFAULT
    return idx to format
}

public class ProtobufDecodingException(message: String) : SerializationException(message)

internal inline fun <reified A: Annotation> SerialDescriptor.findAnnotation(elementIndex: Int): A? {
    return getElementAnnotations(elementIndex).find { it is A } as A?
}
