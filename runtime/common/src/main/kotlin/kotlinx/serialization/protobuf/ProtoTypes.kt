package kotlinx.serialization.protobuf

import kotlinx.serialization.*

public enum class ProtoNumberType {
    DEFAULT, SIGNED, FIXED
}

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class ProtoType(val type: ProtoNumberType)

internal typealias ProtoDesc = Pair<Int, ProtoNumberType>

internal fun extractParameters(desc: SerialDescriptor, index: Int): ProtoDesc {
    val idx = getSerialId(desc, index) ?: index + 1
    val format = desc.findAnnotation<ProtoType>(index)?.type
            ?: ProtoNumberType.DEFAULT
    return idx to format
}


public class ProtobufDecodingException(message: String) : SerializationException(message)
