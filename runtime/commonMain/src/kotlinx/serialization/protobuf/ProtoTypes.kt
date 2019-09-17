/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*

public enum class ProtoNumberType {
    DEFAULT, SIGNED, FIXED
}

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class ProtoType(val type: ProtoNumberType)

internal typealias ProtoDesc = Pair<Int, ProtoNumberType>

internal fun extractParameters(desc: SerialDescriptor, index: Int, zeroBasedDefault: Boolean = false): ProtoDesc {
    val idx = getSerialId(desc, index) ?: (if (zeroBasedDefault) index else index + 1)
    val format = desc.findAnnotation<ProtoType>(index)?.type
            ?: ProtoNumberType.DEFAULT
    return idx to format
}


public class ProtobufDecodingException(message: String) : SerializationException(message)
