package kotlinx.serialization.protobuf.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.protobuf.*

@OptIn(ExperimentalSerializationApi::class)
internal class PackedArrayDecoder(
    proto: ProtoBuf,
    reader: ProtobufReader,
    descriptor: SerialDescriptor,
) : ProtobufDecoder(proto, reader, descriptor) {
    private var nextIndex: Int = 0

    // Tags are omitted in the packed array format
    override fun SerialDescriptor.getTag(index: Int): ProtoDesc = MISSING_TAG

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        throw SerializationException("Packing only supports primitive number types. The input type however was a struct: $descriptor")
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        // We need eof here as there is no tag to read in packed form.
        if (reader.eof) return CompositeDecoder.DECODE_DONE
        return nextIndex++
    }

    override fun decodeTaggedString(tag: ProtoDesc): String {
        throw SerializationException("Packing only supports primitive number types. The actual reading is for string.")
    }
}