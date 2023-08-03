/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("SERIALIZER_TYPE_INCOMPATIBLE")

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.test.*
import kotlin.test.Test
import kotlin.test.assertEquals


class SealedInterfacesSerializationTest {
    interface A

    sealed interface B

    @Serializable
    sealed interface C

    @Serializable(DummySerializer::class)
    sealed interface D

    @Serializable(DummySerializer::class)
    interface E

    @Serializable
    @Polymorphic
    sealed interface F

    @Serializable
    class ImplA : A, B, C, D, E, F

    @Serializable
    class ImplB : A, B, C, D, E, F

    @Serializable
    class Holder(
        val a: A,
        val b: B,
        val c: C,
        val d: D,
        val e: E,
        @Polymorphic val polyC: C,
        val f: F
    )

    class DummySerializer : KSerializer<Any> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Dummy")

        override fun serialize(encoder: Encoder, value: Any) {
            error("serialize")
        }

        override fun deserialize(decoder: Decoder): Any {
            error("deserialize")
        }
    }

    private fun SerialDescriptor.haveSealedSubclasses() {
        assertEquals(PolymorphicKind.SEALED, kind)
        val subclasses = getElementDescriptor(1).elementDescriptors.map { it.serialName.substringAfterLast('.') }
        assertEquals(listOf("ImplA", "ImplB"), subclasses)
    }

    private fun SerialDescriptor.isDummy() = serialName == "Dummy"

    private fun SerialDescriptor.isPolymorphic() = kind == PolymorphicKind.OPEN

    operator fun SerialDescriptor.get(i: Int) = getElementDescriptor(i)

    @Test
    fun testInHolder() {
        val desc = Holder.serializer().descriptor
        desc[0].isPolymorphic()
        desc[1].isPolymorphic()
        desc[2].haveSealedSubclasses()
        desc[3].isDummy()
        desc[4].isDummy()
        desc[5].isPolymorphic()
        desc[6].isPolymorphic()
    }

    @Test
    fun testGenerated() {
        C.serializer().descriptor.haveSealedSubclasses()
    }

    @Test
    fun testResolved() {
        serializer<C>().descriptor.haveSealedSubclasses()
    }


}
