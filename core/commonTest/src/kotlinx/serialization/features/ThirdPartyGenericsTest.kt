package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*
import kotlin.test.*

open class ThirdPartyGenericsTest {
    // This is a 3rd party class that we can't annotate as @Serializable
    data class ThirdPartyBox<T>(val contents: T)

    // This is the item that we put in the ThirdPartyBox, we control it, so can annotate it
    @Serializable
    data class Item(val name: String)

    // The serializer for the ThirdPartyBox<T>
    class BoxSerializer<T>(dataSerializer: KSerializer<T>) : KSerializer<ThirdPartyBox<T>> {
        @Serializable
        data class BoxSurrogate<T>(val contents: T)

        private val strategy = BoxSurrogate.serializer(dataSerializer)
        override val descriptor: SerialDescriptor = strategy.descriptor

        override fun deserialize(decoder: Decoder): ThirdPartyBox<T> {
            return ThirdPartyBox(decoder.decodeSerializableValue(strategy).contents)
        }

        override fun serialize(encoder: Encoder, value: ThirdPartyBox<T>) {
            encoder.encodeSerializableValue(strategy, BoxSurrogate(value.contents))
        }
    }

    // Register contextual serializer for ThirdPartyBox<Item>
    protected val boxWithItemSerializer = BoxSerializer(Item.serializer())
    protected val serializersModule = SerializersModule {
        contextual(boxWithItemSerializer)
    }

    @Test
    fun testSurrogateSerializerFoundForGenericWithKotlinType() {
        val serializer = serializersModule.serializer<ThirdPartyBox<Item>>()
        assertEquals(boxWithItemSerializer.descriptor, serializer.descriptor)
    }
}