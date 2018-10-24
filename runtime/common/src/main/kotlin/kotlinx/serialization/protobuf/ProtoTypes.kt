package kotlinx.serialization.protobuf

import kotlinx.serialization.*

enum class ProtoNumberType {
    DEFAULT, SIGNED, FIXED
}

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class ProtoType(val type: ProtoNumberType)

typealias ProtoDesc = Pair<Int, ProtoNumberType>

// needed until K/N will get ability to synthesize @SerialInfo annotations
internal expect fun extractParameters(desc: SerialDescriptor, index: Int): ProtoDesc


class ProtobufDecodingException(message: String) : SerializationException(message)
