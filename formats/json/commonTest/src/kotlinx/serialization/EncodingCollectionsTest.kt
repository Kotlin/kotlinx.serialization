package kotlinx.serialization

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlin.test.*

class EncodingCollectionsTest {
    object ListSerializer : KSerializer<List<String>> {
        override val descriptor: SerialDescriptor = ListSerializer(String.serializer()).descriptor

        override fun serialize(encoder: Encoder, value: List<String>) {
            encoder.encodeCollection(descriptor, value) { index, item ->
                encodeStringElement(descriptor, index, item)
            }
        }

        override fun deserialize(decoder: Decoder): List<String> = throw NotImplementedError()
    }

    @Test
    fun testEncoding() {
        assertEquals("""["Hello","World!"]""", Json.encodeToString(ListSerializer, listOf("Hello", "World!")))
    }
}
