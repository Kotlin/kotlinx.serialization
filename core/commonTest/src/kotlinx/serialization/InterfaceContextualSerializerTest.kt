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

    @Serializable
    data class Box<T>(val boxed: T)

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
        assertEquals(PolymorphicKind.OPEN, serializer<IApiError>().descriptor.kind)
        assertEquals(PolymorphicKind.OPEN, serializer(typeOf<IApiError>()).descriptor.kind)
    }

    @Test
    fun testCustom() {
        assertSame(CustomSer, serializer<HasCustom>())
        assertSame(CustomSer, serializer(typeOf<HasCustom>()) as KSerializer<HasCustom>)
    }

    // JVM - intrinsics kick in
    @Test
    fun testContextual() {
        val module = serializersModuleOf(IApiError::class, MyApiErrorSerializer)
        assertSame(MyApiErrorSerializer, module.serializer(typeOf<IApiError>()) as KSerializer<IApiError>)
        shouldFail<AssertionError>(beforeKotlin = "2.0.0", onJvm = true, onWasm = false, onNative = false, onJs = false ) {
            assertSame(MyApiErrorSerializer, module.serializer<IApiError>())
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
    fun testInsideBox() {
        val module = serializersModuleOf(IApiError::class, MyApiErrorSerializer)
        assertEquals(MyApiErrorSerializer.descriptor, module.serializer(typeOf<Box<IApiError>>()).descriptor.elementDescriptors.first())
        shouldFail<AssertionError>(beforeKotlin = "2.0.0", onWasm = false, onNative = false, onJs = false ) {
            assertEquals(
                MyApiErrorSerializer.descriptor,
                module.serializer<Box<IApiError>>().descriptor.elementDescriptors.first()
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

    class Unrelated

    object UnrelatedSerializer: KSerializer<Unrelated> {
        override val descriptor: SerialDescriptor
            get() = TODO("Not yet implemented")

        override fun serialize(encoder: Encoder, value: Unrelated) {
            TODO("Not yet implemented")
        }

        override fun deserialize(decoder: Decoder): Unrelated {
            TODO("Not yet implemented")
        }
    }

    @Test
    fun interfaceSerializersAreCachedInsideIfModuleIsNotFilledWithInterface() = jvmOnly {
        // Caches are implemented on JVM
        val module = serializersModuleOf(Unrelated::class, UnrelatedSerializer)
        val p1 = module.serializer(typeOf<List<IApiError>>())
        assertEquals(PolymorphicKind.OPEN, p1.descriptor.elementDescriptors.first().kind)
        val p2 = module.serializer(typeOf<List<IApiError>>())
        assertSame(p1, p2)
    }

    @Test
    fun interfaceSerializersAreCachedTopLevelIfModuleIsNotFilledWithInterface() = jvmOnly {
        val module = serializersModuleOf(Unrelated::class, UnrelatedSerializer)
        val p1 = module.serializer(typeOf<IApiError>())
        assertEquals(PolymorphicKind.OPEN, p1.descriptor.kind)
        val p2 = module.serializer(typeOf<IApiError>())
        assertSame(p1, p2)
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
        if (!isNative() && !isWasm()) {
            assertEquals(PolymorphicKind.OPEN, serializer(typeOf<Parametrized<String>>()).descriptor.kind)
        }
        val md = SerializersModule {
            contextual(Parametrized::class) { PSer(it[0]) }
        }
        assertEquals("PSer<kotlin.String>", md.serializer(typeOf<Parametrized<String>>()).descriptor.serialName)
        shouldFail<AssertionError>(beforeKotlin = "2.0.0", onWasm = false, onNative = false, onJs = false ) {
            assertEquals("PSer<kotlin.String>", md.serializer<Parametrized<String>>().descriptor.serialName)
        }
    }

    @Serializable
    sealed interface SealedI

    @Test
    fun sealedInterfacesAreNotAffected() {
        val module = serializersModuleOf(IApiError::class, MyApiErrorSerializer)
        module.doTest<SealedI> {
            assertEquals(PolymorphicKind.SEALED, it.descriptor.kind)
        }
        module.doTest<List<SealedI>> {
            assertEquals(PolymorphicKind.SEALED, it.descriptor.elementDescriptors.first().kind)
        }
        module.doTest<Box<SealedI>> {
            assertEquals(PolymorphicKind.SEALED, it.descriptor.elementDescriptors.first().kind)
        }
    }

    object SealedSer: KSerializer<SealedI> {
        override val descriptor: SerialDescriptor
            get() = PrimitiveSerialDescriptor("SealedSer", PrimitiveKind.INT)

        override fun serialize(encoder: Encoder, value: SealedI) {
            TODO("Not yet implemented")
        }

        override fun deserialize(decoder: Decoder): SealedI {
            TODO("Not yet implemented")
        }
    }

    @Test
    fun sealedInterfacesAreNotOverriden() {
        val module = serializersModuleOf(SealedI::class, SealedSer)
        module.doTest<SealedI> {
            assertEquals(PolymorphicKind.SEALED, it.descriptor.kind)
        }
        module.doTest<List<SealedI>> {
            assertEquals(PolymorphicKind.SEALED, it.descriptor.elementDescriptors.first().kind)
        }
        module.doTest<Box<SealedI>> {
            assertEquals(PolymorphicKind.SEALED, it.descriptor.elementDescriptors.first().kind)
        }
    }
}
