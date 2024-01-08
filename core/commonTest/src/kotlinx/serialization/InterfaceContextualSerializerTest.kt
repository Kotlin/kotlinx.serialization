/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.test.*
import kotlin.reflect.*
import kotlin.test.*

// Imagine this is a 3rd party interface
interface IApiError {
    val code: Int
}

@Serializable(CustomSer::class)
interface HasCustom


object CustomSer: KSerializer<HasCustom> {
    override val descriptor: SerialDescriptor
        get() = TODO("Not yet implemented")

    override fun serialize(encoder: Encoder, value: HasCustom) {
        TODO("Not yet implemented")
    }

    override fun deserialize(decoder: Decoder): HasCustom {
        TODO("Not yet implemented")
    }
}

@Suppress("UNCHECKED_CAST")
class InterfaceContextualSerializerTest {

    object MyApiErrorSerializer : KSerializer<IApiError> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IApiError", PrimitiveKind.INT)

        override fun serialize(encoder: Encoder, value: IApiError) {
            encoder.encodeInt(value.code)
        }

        override fun deserialize(decoder: Decoder): IApiError {
            val code = decoder.decodeInt()
            return object : IApiError {
                override val code: Int = code
            }
        }
    }

    private inline fun <reified T> SerializersModule.doTest(block: (KSerializer<T>) -> Unit) {
        block(this.serializer<T>())
        block(this.serializer(typeOf<T>()) as KSerializer<T>)
    }

    // Native, WASM - can't retrieve serializer (no .isInterface)
    @Test
    fun testDefault() {
        if (isNative() || isWasm()) return
        EmptySerializersModule().doTest<IApiError> {
            assertEquals(PolymorphicKind.OPEN, it.descriptor.kind)
            assertContains(it.toString(), "kotlinx.serialization.PolymorphicSerializer")
        }
    }

    // Native, WASM - can't retrieve serializer (no .isInterface)
    @Test
    fun testCustom() {
        if (isNative() || isWasm()) return
        assertSame(CustomSer, serializer<HasCustom>())
        assertSame(CustomSer, serializer(typeOf<HasCustom>()) as KSerializer<HasCustom>)
    }

    // JVM - intrinsics kick in
    @Test
    fun testContextual() {
        val module = serializersModuleOf(IApiError::class, MyApiErrorSerializer)
        assertSame(MyApiErrorSerializer, module.serializer(typeOf<IApiError>()) as KSerializer<IApiError>)
        shouldFail<AssertionError>(beforeKotlin = "2.0.0", onJvm = true, onWasm = false, onNative = false, onJs = false ) {
            assertSame(MyApiErrorSerializer, module.serializer<IApiError>() as KSerializer<IApiError>)
        }
    }

    // JVM - intrinsics kick in
    @Test
    fun testInsideList() {
        val module = serializersModuleOf(IApiError::class, MyApiErrorSerializer)
        assertEquals(MyApiErrorSerializer.descriptor, module.serializer(typeOf<List<IApiError>>()).descriptor.elementDescriptors.first())
        shouldFail<AssertionError>(beforeKotlin = "2.0.0", onWasm = false, onNative = false, onJs = false ) {
            assertEquals(
                MyApiErrorSerializer.descriptor,
                module.serializer<List<IApiError>>().descriptor.elementDescriptors.first()
            )
        }
    }

    @Test
    fun testWithNullability() {
        val module = serializersModuleOf(IApiError::class, MyApiErrorSerializer)
        assertEquals(MyApiErrorSerializer.nullable.descriptor, module.serializer(typeOf<IApiError?>()).descriptor)
        shouldFail<AssertionError>(beforeKotlin = "2.0.0", onWasm = false, onNative = false, onJs = false ) {
            assertEquals(MyApiErrorSerializer.nullable.descriptor, module.serializer<IApiError?>().descriptor)
        }
    }

    @Test
    fun testWithNullabilityInsideList() {
        val module = serializersModuleOf(IApiError::class, MyApiErrorSerializer)
        assertEquals(MyApiErrorSerializer.nullable.descriptor, module.serializer(typeOf<List<IApiError?>>()).descriptor.elementDescriptors.first())
        shouldFail<AssertionError>(beforeKotlin = "2.0.0", onWasm = false, onNative = false, onJs = false ) {
            assertEquals(
                MyApiErrorSerializer.nullable.descriptor,
                module.serializer<List<IApiError?>>().descriptor.elementDescriptors.first()
            )
        }
    }
}
