/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlin.test.*

class ContextualProviderTest {
    // This is a 3rd party class that we can't annotate as @Serializable
    data class ThirdPartyBox<T>(val contents: T)

    // This is the item that we put in the ThirdPartyBox, we control it, so can annotate it
    @Serializable
    data class Item(val name: String)

    // This is the another item that we put in the ThirdPartyBox, we control it, so can annotate it
    @Serializable
    data class AnotherItem(val value: Int)

    // The serializer for the ThirdPartyBox<T>
    class ThirdPartyBoxSerializer<T>(itemSerializer: KSerializer<T>) : KSerializer<ThirdPartyBox<T>> {
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
        val kclass = ThirdPartyBox::class
        val module = SerializersModule {
            contextual(kclass) { args -> ThirdPartyBoxSerializer(args[0]) }
        }
        fun checkFor(serial: KSerializer<*>) {
            val ser = module.getContextual(kclass, arrayOf(serial))?.descriptor
            assertEquals(serial.descriptor, ser?.getElementDescriptor(0))
        }
        checkFor(Item.serializer())
        checkFor(AnotherItem.serializer())
    }
}
