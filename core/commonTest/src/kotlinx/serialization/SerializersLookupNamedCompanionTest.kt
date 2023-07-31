/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.*
import kotlinx.serialization.test.runSince
import kotlin.reflect.*
import kotlin.test.*

class SerializersLookupNamedCompanionTest {
    @Serializable
    class Plain(val i: Int) {
        companion object Named
    }

    @Serializable
    class Parametrized<T>(val value: T) {
        companion object Named
    }


    @Serializer(forClass = PlainWithCustom::class)
    object PlainSerializer

    @Serializable(PlainSerializer::class)
    class PlainWithCustom(val i: Int) {
        companion object Named
    }

    class ParametrizedSerializer<T: Any>(val serializer: KSerializer<T>): KSerializer<ParametrizedWithCustom<T>> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("parametrized (${serializer.descriptor})", PrimitiveKind.STRING)
        override fun deserialize(decoder: Decoder): ParametrizedWithCustom<T> = TODO("Not yet implemented")
        override fun serialize(encoder: Encoder, value: ParametrizedWithCustom<T>) = TODO("Not yet implemented")
    }

    @Serializable(ParametrizedSerializer::class)
    class ParametrizedWithCustom<T>(val i: T) {
        companion object Named
    }



    @Test
    fun testPlainClass() = runSince("1.9.20") {
        assertSame<KSerializer<*>>(Plain.serializer(), serializer(typeOf<Plain>()))

        assertEquals(
            Parametrized.serializer(Int.serializer()).descriptor.toString(),
            serializer(typeOf<Parametrized<Int>>()).descriptor.toString()
        )

        assertSame<KSerializer<*>>(PlainSerializer, serializer(typeOf<PlainWithCustom>()))

        assertEquals(
            ParametrizedWithCustom.serializer(Int.serializer()).descriptor.toString(),
            serializer(typeOf<ParametrizedWithCustom<Int>>()).descriptor.toString()
        )
    }

}