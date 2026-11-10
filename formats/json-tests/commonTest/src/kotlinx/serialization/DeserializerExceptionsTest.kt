package kotlinx.serialization

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.test.checkDecodingException
import kotlin.test.*

class DeserializerExceptionsTest: JsonTestBase() {

    @Serializable(with = BoxSerializer::class)
    class Box(val i: Int)

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

    @Serializable
    data class Rgb(
        val r: Int,
        val g: Int,
        val b: Int,
    ) {
        init {
            require(r in 0..255) { "r is out of range: $r" }
            require(g in 0..255) { "g is out of range: $g" }
            require(b in 0..255) { "b is out of range: $b" }
        }
    }

    @Test
    fun testEncodingExceptionNotSwallowed() {
        assertFailsWith<ArithmeticException> { Json.encodeToString(Box(1)) }
    }

    @Test
    fun testConstructorExceptionIsCause() = parametrizedTest { mode ->
        val string = """{"r":256,"g":256,"b":256}"""
        checkDecodingException(mode, { Json.decodeFromString<Rgb>(string) }) {
            message("Deserializer caused 'r is out of range: 256' exception in the decoder")
            path("$")
            input(string)
            cause<IllegalArgumentException> { "r is out of range: 256" }
        }
    }

    @Test
    fun testDecodingExceptionSwallowed() = parametrizedTest { mode ->
        val string = """{"i":1}"""
        checkDecodingException(mode, { Json.decodeFromString<Box>(string) }) {
            message("Deserializer caused an exception in the decoder")
            path("$")
            input(string)
            cause<ArithmeticException>()
        }
    }
}
