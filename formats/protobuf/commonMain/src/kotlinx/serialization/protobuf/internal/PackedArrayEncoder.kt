package kotlinx.serialization.protobuf.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
internal class PackedArrayEncoder(
    proto: ProtoBuf,
    writer: ProtobufWriter,
    private val innerOutput: ByteArrayOutput,
    val tag: ProtoDesc,
    descriptor: SerialDescriptor
) : ProtobufEncoder(proto, ProtobufWriter(innerOutput), descriptor) {

    constructor(proto: ProtoBuf, writer: ProtobufWriter, tag: ProtoDesc, descriptor: SerialDescriptor) :
            this(proto, writer, ByteArrayOutput(), tag, descriptor)

    private val outerWriter: ProtobufWriter = writer

    // Triggers not writing header
    override fun SerialDescriptor.getTag(index: Int): ProtoDesc = MISSING_TAG

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        throw SerializationException("Packing only supports primitive number types")
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        throw SerializationException("Packing only supports primitive number types")
    }

    override fun encodeTaggedString(tag: ProtoDesc, value: String) {
        throw SerializationException("Packing only supports primitive number types")
    }

    override fun endEncode(descriptor: SerialDescriptor) {
        outerWriter.writeOutput(innerOutput, tag.protoId)
    }
}
