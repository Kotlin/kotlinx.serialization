/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.test.*
import kotlinx.serialization.test.shouldFail
import org.junit.Test
import kotlin.reflect.*
import kotlin.test.*

interface JApiError {
    val code: Int
}

class InterfaceContextualSerializerTestJvm {
    object MyApiErrorSerializer : KSerializer<JApiError> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("JApiError", PrimitiveKind.INT)

        override fun serialize(encoder: Encoder, value: JApiError) {
            encoder.encodeInt(value.code)
        }

        override fun deserialize(decoder: Decoder): JApiError {
            val code = decoder.decodeInt()
            return object : JApiError {
                override val code: Int = code
            }
        }
    }

    @Test
    fun testDefault() {
        assertEquals(PolymorphicKind.OPEN, serializer(typeTokenOf<JApiError>()).descriptor.kind)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun testContextual() {
        val module = serializersModuleOf(JApiError::class, MyApiErrorSerializer)
        assertSame(MyApiErrorSerializer, module.serializer(typeTokenOf<JApiError>()) as KSerializer<JApiError>)
    }

    @Test
    fun testInsideList() {
        val module = serializersModuleOf(JApiError::class, MyApiErrorSerializer)
        assertSame(MyApiErrorSerializer.descriptor, module.serializer(typeTokenOf<List<JApiError>>()).descriptor.elementDescriptors.first())
    }

    interface Parametrized<T> {
        val param: List<T>
    }

    class PSer<T>(val tSer: KSerializer<T>): KSerializer<Parametrized<T>> {
        override val descriptor: SerialDescriptor
            get() = buildClassSerialDescriptor("PSer<${tSer.descriptor.serialName}>")

        override fun serialize(encoder: Encoder, value: Parametrized<T>) {
            TODO("Not yet implemented")
        }

        override fun deserialize(decoder: Decoder): Parametrized<T> {
            TODO("Not yet implemented")
        }
    }

    @Test
    fun testParametrizedInterface() {
        assertEquals(PolymorphicKind.OPEN, serializer(typeTokenOf<Parametrized<String>>()).descriptor.kind)
        val md = SerializersModule {
            contextual(Parametrized::class) { PSer(it[0]) }
        }
        assertEquals("PSer<kotlin.String>", md.serializer(typeTokenOf<Parametrized<String>>()).descriptor.serialName)
    }
}
