package kotlinx.serialization.cbor

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.test.Test
import kotlin.test.assertFailsWith

@OptIn(ExperimentalSerializationApi::class, ExperimentalUnsignedTypes::class)
class CborEncoderEncodeTagsValidationTest {

    private object DanglingRootTagsSerializer : KSerializer<Unit> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("DanglingRootTags")

        override fun serialize(encoder: Encoder, value: Unit) {
            (encoder as CborEncoder).encodeTags(ulongArrayOf(1u))
        }

        override fun deserialize(decoder: Decoder): Unit = Unit
    }

    private object DanglingTagsInStructureSerializer : KSerializer<Unit> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("DanglingTagsInStructure") {
            element("x", Int.serializer().descriptor)
        }

        override fun serialize(encoder: Encoder, value: Unit) {
            val composite = encoder.beginStructure(descriptor)
            (composite as CborEncoder).encodeTags(ulongArrayOf(1u))
            composite.endStructure(descriptor)
        }

        override fun deserialize(decoder: Decoder): Unit = Unit
    }

    @Test
    fun danglingEncodeTagsFailsForByteEncoding_indefinite() {
        val cbor = Cbor { useDefiniteLengthEncoding = false }
        assertFailsWith<SerializationException> {
            cbor.encodeToByteArray(DanglingRootTagsSerializer, Unit)
        }
        assertFailsWith<SerializationException> {
            cbor.encodeToByteArray(DanglingTagsInStructureSerializer, Unit)
        }
    }

    @Test
    fun danglingEncodeTagsFailsForByteEncoding_definite() {
        val cbor = Cbor { useDefiniteLengthEncoding = true }
        assertFailsWith<SerializationException> {
            cbor.encodeToByteArray(DanglingRootTagsSerializer, Unit)
        }
        assertFailsWith<SerializationException> {
            cbor.encodeToByteArray(DanglingTagsInStructureSerializer, Unit)
        }
    }

    @Test
    fun danglingEncodeTagsFailsForStructuredEncoding() {
        val cbor = Cbor {}
        assertFailsWith<SerializationException> {
            cbor.encodeToCborElement(DanglingRootTagsSerializer, Unit)
        }
        assertFailsWith<SerializationException> {
            cbor.encodeToCborElement(DanglingTagsInStructureSerializer, Unit)
        }
    }
}
