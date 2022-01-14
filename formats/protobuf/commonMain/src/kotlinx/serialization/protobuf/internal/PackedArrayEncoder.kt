package kotlinx.serialization.protobuf.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
internal class PackedArrayEncoder constructor(proto: ProtoBuf, writer: ProtobufWriter, descriptor: SerialDescriptor) :
    ProtobufEncoder(proto, writer, descriptor) {

    private var outerStructEncoded = false

    // Triggers not writing header
    override fun SerialDescriptor.getTag(index: Int): ProtoDesc = MISSING_TAG

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        if (! outerStructEncoded) {
            outerStructEncoded = true
            return this
        } else {
            return super.beginCollection(descriptor, collectionSize)
        }
    }
}
