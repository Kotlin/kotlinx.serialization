package kotlinx.serialization

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlin.test.*

class EncodingExtensionsTest {

    @Serializable(with = BoxSerializer::class)
    class Box(val i: Int)

    @Serializer(forClass = Box::class)
    object BoxSerializer : KSerializer<Box> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Box") {
            element<Int>("i")
        }

        override fun serialize(encoder: Encoder, value: Box) {
            encoder.encodeStructure(descriptor) {
                throw ArithmeticException()
            }
        }

        override fun deserialize(decoder: Decoder): Box {
            decoder.decodeStructure(descriptor) {
                throw ArithmeticException()
            }
        }
    }

    @Test
    fun testEncodingExceptionNotSwallowed() {
        assertFailsWith<ArithmeticException> { Json.encodeToString(Box(1)) }
    }

    @Test
    fun testDecodingExceptionNotSwallowed() {
        assertFailsWith<ArithmeticException> { Json.decodeFromString<Box>("""{"i":1}""") }
    }
}
