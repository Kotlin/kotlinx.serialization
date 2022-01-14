package kotlinx.serialization.protobuf.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
internal class PackedArrayDecoder(
    proto: ProtoBuf,
    reader: ProtobufReader,
    descriptor: SerialDescriptor,
) : ProtobufDecoder(proto, reader, descriptor) {
    private var nextIndex: Int = 0

    override fun SerialDescriptor.getTag(index: Int): ProtoDesc = MISSING_TAG

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        // If we didn't parse a child yet we stay in this object. Only nexted lists would be handled
        // in the way they would be.
        if (nextIndex==0) return this
        return super.beginStructure(descriptor)
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (reader.eof) return CompositeDecoder.DECODE_DONE
        return nextIndex++
    }
}