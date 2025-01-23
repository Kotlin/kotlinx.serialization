package kotlinx.serialization.protobuf.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.protobuf.*

@OptIn(ExperimentalSerializationApi::class)
internal class PackedArrayEncoder(
    proto: ProtoBuf,
    writer: ProtobufWriter,
    curTag: ProtoDesc,
    descriptor: SerialDescriptor,
    stream: ByteArrayOutput = ByteArrayOutput()
) : NestedRepeatedEncoder(proto, writer, curTag, descriptor, stream) {

    // Triggers not writing header
    override fun SerialDescriptor.getTag(index: Int): ProtoDesc = MISSING_TAG

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        throw SerializationException("Packing only supports primitive number types")
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        throw SerializationException("Packing only supports primitive number types")
    }

    override fun endEncode(descriptor: SerialDescriptor) {
        if (stream.size() > 0) {
            super.endEncode(descriptor)
        }
    }

    override fun encodeTaggedString(tag: ProtoDesc, value: String) {
        throw SerializationException("Packing only supports primitive number types")
    }
}
