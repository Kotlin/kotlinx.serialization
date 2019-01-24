package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlinx.serialization.internal.onlySingleOrNull

enum class ProtoNumberType {
    DEFAULT, SIGNED, FIXED
}

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class ProtoType(val type: ProtoNumberType)

typealias ProtoDesc = Pair<Int, ProtoNumberType>

internal fun extractParameters(desc: SerialDescriptor, index: Int): ProtoDesc {
    val idx = getSerialId(desc, index) ?: index + 1
    val format = desc.getElementAnnotations(index).filterIsInstance<ProtoType>().onlySingleOrNull()?.type
            ?: ProtoNumberType.DEFAULT
    return idx to format
}


class ProtobufDecodingException(message: String) : SerializationException(message)
