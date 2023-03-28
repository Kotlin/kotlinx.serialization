/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package kotlinx.serialization

import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.test.*
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

    class ParametrizedSerializer<T : Any>(val serializer: KSerializer<T>) : KSerializer<ParametrizedWithCustom<T>> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("parametrized (${serializer.descriptor})", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): ParametrizedWithCustom<T> = TODO("Not yet implemented")
        override fun serialize(encoder: Encoder, value: ParametrizedWithCustom<T>) = TODO("Not yet implemented")
    }

    @Serializable(ParametrizedSerializer::class)
    class ParametrizedWithCustom<T>(val i: T) {
        companion object Named
    }

    @Serializable
    sealed interface SealedInterface {
        companion object Named
    }

    @Serializable
    sealed interface SealedInterfaceWithExplicitAnnotation {
        @NamedCompanion
        companion object Named
    }


    @Test
    fun test() {
        assertSame<KSerializer<*>>(Plain.serializer(), serializer(typeOf<Plain>()))

        shouldFail<SerializationException>(beforeKotlin = "1.9.20", onJs = false, onNative = false, onWasm = false) {
            assertSame<KSerializer<*>>(PlainSerializer, serializer(typeOf<PlainWithCustom>()))
        }

        shouldFail<SerializationException>(beforeKotlin = "1.9.20", onJs = false, onNative = false, onWasm = false) {
            assertEquals(
                Parametrized.serializer(Int.serializer()).descriptor.toString(),
                serializer(typeOf<Parametrized<Int>>()).descriptor.toString()
            )
        }

        shouldFail<SerializationException>(beforeKotlin = "1.9.20", onJs = false, onNative = false, onWasm = false) {
            assertEquals(
                ParametrizedWithCustom.serializer(Int.serializer()).descriptor.toString(),
                serializer(typeOf<ParametrizedWithCustom<Int>>()).descriptor.toString()
            )
        }

        shouldFail<SerializationException>(beforeKotlin = "1.9.20", onJs = false, onNative = false, onWasm = false) {
            assertEquals(
                SealedInterface.serializer().descriptor.toString(),
                serializer(typeOf<SealedInterface>()).descriptor.toString()
            )
        }

        // should fail because annotation @NamedCompanion will be placed again by the compilation plugin
        // and they both will be placed into @Container annotation - thus they will be invisible to the runtime
        shouldFail<SerializationException>(sinceKotlin = "1.9.20", onJs = false, onNative = false, onWasm = false) {
            serializer(typeOf<SealedInterfaceWithExplicitAnnotation>())
        }
    }


}