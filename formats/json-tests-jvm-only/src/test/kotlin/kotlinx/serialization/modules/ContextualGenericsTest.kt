/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlin.test.*

open class ContextualGenericsTest {
    // This is a 3rd party class that we can't annotate as @Serializable
    data class ThirdPartyBox<T>(val contents: T)

    // This is the item that we put in the ThirdPartyBox, we control it, so can annotate it
    @Serializable
    data class Item(val name: String)

    // This is the another item that we put in the ThirdPartyBox, we control it, so can annotate it
    @Serializable
    data class AnotherItem(val value: Int)

    // The serializer for the ThirdPartyBox<T>
    class ThirdPartyBoxSerializer<T>(dataSerializer: KSerializer<T>) : KSerializer<ThirdPartyBox<T>> {
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
    protected val boxWithItemSerializer = ThirdPartyBoxSerializer(Item.serializer())
    protected val serializersModuleStatic = SerializersModule {
        contextual(boxWithItemSerializer)
    }

    protected val serializersModuleWithProvider = SerializersModule {
        contextual(ThirdPartyBox::class) { args -> ThirdPartyBoxSerializer(args[0]) }
    }

    @Test
    fun testSurrogateSerializerFoundForGenericWithKotlinType() {
        val serializer = serializersModuleStatic.serializer<ThirdPartyBox<Item>>()
        assertEquals(boxWithItemSerializer.descriptor, serializer.descriptor)
    }

    @Test
    fun testSerializerFoundForContextualGeneric() {
        val serializerA = serializersModuleWithProvider.serializer<ThirdPartyBox<Item>>()
        assertEquals(Item.serializer().descriptor, serializerA.descriptor.getElementDescriptor(0))
        val serializerB = serializersModuleWithProvider.serializer<ThirdPartyBox<AnotherItem>>()
        assertEquals(AnotherItem.serializer().descriptor, serializerB.descriptor.getElementDescriptor(0))
    }

    @Test
    fun testModuleProvidesMultipleGenericSerializers() {
        fun checkFor(serial: KSerializer<*>) {
            val serializer = serializersModuleWithProvider.getContextual(ThirdPartyBox::class, listOf(serial))?.descriptor
            assertEquals(serial.descriptor, serializer?.getElementDescriptor(0))
        }
        checkFor(Item.serializer())
        checkFor(AnotherItem.serializer())
    }
}
