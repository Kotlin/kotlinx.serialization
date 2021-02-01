package kotlinx.serialization.features

import kotlin.test.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

class DerivedContextualSerializerTest {

    @Serializable
    abstract class Message

    @Serializable
    class SimpleMessage(val body: String) : Message()

    @Serializable
    class Holder(@Contextual val message: Message)

    object MessageAsStringSerializer : KSerializer<Message> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("kotlinx.serialization.MessageAsStringSerializer", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Message) {
            // dummy serializer that assumes Message is always SimpleMessage
            check(value is SimpleMessage)
            encoder.encodeString(value.body)
        }

        override fun deserialize(decoder: Decoder): Message {
            return SimpleMessage(decoder.decodeString())
        }
    }

    @Test
    fun testDerivedContextualSerializer() {
        val module = SerializersModule {
            contextual(MessageAsStringSerializer)
        }
        val format = Json { serializersModule = module }
        val data = Holder(SimpleMessage("hello"))
        val serialized = format.encodeToString(data)
        assertEquals("""{"message":"hello"}""", serialized)
        val deserialized = format.decodeFromString<Holder>(serialized)
        assertTrue(deserialized.message is SimpleMessage)
        assertEquals("hello", deserialized.message.body)
    }
}
