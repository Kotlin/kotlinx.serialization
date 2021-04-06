package kotlinx.serialization.features

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlin.test.Test
import kotlin.test.assertFailsWith

open class MultipleContextualGenericsTest {
    // This is a 3rd party class that we can't annotate as @Serializable
    data class ThirdPartyBox<T>(val contents: T)

    // This is the item that we put in the ThirdPartyBox, we control it, so can annotate it
    @Serializable
    data class Item(val name: String)

    // This is the another item that we put in the ThirdPartyBox, we control it, so can annotate it
    @Serializable
    data class AnotherItem(val value: Int)

    // The serializer for the ThirdPartyBox<T>
    class BoxSerializer<T>(itemSerializer: KSerializer<T>) : KSerializer<ThirdPartyBox<T>> {
        @Serializable
        data class BoxSurrogate<T>(val contents: T)

        private val strategy = BoxSurrogate.serializer(itemSerializer)
        override val descriptor: SerialDescriptor = strategy.descriptor

        override fun deserialize(decoder: Decoder): ThirdPartyBox<T> {
            return ThirdPartyBox(decoder.decodeSerializableValue(strategy).contents)
        }

        override fun serialize(encoder: Encoder, value: ThirdPartyBox<T>) {
            encoder.encodeSerializableValue(strategy, BoxSurrogate(value.contents))
        }
    }

    @Test
    fun testRegisterMultipleGenericSerializers() {
        /*
         * The use case here is serializing a third party generic class with different contents: ThirdPartyBox<T>.
         * As is show in the manual, the BoxSerializer is capable of serializing any ThirdPartyBox and takes as its constructor arg the
         * serializer for T.
         *
         * Different from the examples for generics in the manual, we have to use `contextual` here, because we don't control ThirdPartyBox<T>.
         * Unfortunately, adding another contextual serializer for the generic BoxSerializer fails, because SerializersModuleBuilders.registerSerializer()
         * only looks at the toplevel type of generic types to determine if a serializer was already registered.
         *
         * As solution could be to add type parameter info as a differentiator to the map, but this would not be possible for JavaScript,
         * because KClass.typeParameters is not available in JavaScript. How to solve this?
         */
        assertFailsWith(IllegalArgumentException::class) {
            SerializersModule {
                contextual(BoxSerializer(Item.serializer()))
                contextual(BoxSerializer(AnotherItem.serializer()))
            }

        }.also { it.message!!.contains("Serializer for ${ThirdPartyBox::class} already registered in this module") }
    }
}